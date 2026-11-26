import { test } from 'node:test';
import assert from 'node:assert/strict';
import { handle } from '../src/index.js';
import { hmacSha256, bytesToBase64 } from '../src/http.js';
import { _resetDocusignTokenCache } from '../src/esign/docusign.js';
import { _resetPlayTokenCache } from '../src/google/play.js';
import { fakeD1, fakeFetch, jsonResponse, req, rsaKeyPem } from './helpers.js';

const PDF = bytesToBase64(new TextEncoder().encode('%PDF-1.4 test'));
const FIELDS = [
  { type: 'signature', page: 1, x: 316, y: 600, width: 180, height: 38 },
  { type: 'initials', page: 2, x: 120, y: 700, width: 60, height: 16 },
  { type: 'date', page: 1, x: 496, y: 646, width: 76, height: 14 },
];
const envelopeBody = (extra = {}) => ({
  title: 'Inspection Agreement - 12 Oak St',
  message: 'Please sign',
  reference: 'i123',
  signer: { name: 'Pat Client', email: 'pat@example.com' },
  fields: FIELDS,
  pdfBase64: PDF,
  ...extra,
});

async function register(env, deps) {
  const r = await handle(req('POST', '/v1/accounts', { body: { companyName: 'Acme Inspections', email: 'a@acme.test', installId: 'x' } }), env, deps);
  assert.equal(r.status, 201);
  return r.json();
}

function boldsignFetch(state = { status: 'InProgress' }) {
  return fakeFetch([
    ['/v1/document/send', () => jsonResponse({ documentId: 'bs-doc-1' })],
    ['/v1/document/properties', () => jsonResponse(state)],
    ['/v1/document/download', () => new Response(new TextEncoder().encode('%PDF signed'), { headers: { 'content-type': 'application/pdf' } })],
  ]);
}

test('accounts: register, then authenticated entitlement', async () => {
  const env = { DB: fakeD1(), TRIAL_ENVELOPES: '1' };
  const { accountId, token } = await register(env);
  assert.match(accountId, /^acc_/);
  const me = await (await handle(req('GET', '/v1/me', { token }), env)).json();
  assert.deepEqual(me, { accountId, companyName: 'Acme Inspections', active: false, plan: '', expiresAt: 0, trialEnvelopesLeft: 1 });
  assert.equal((await handle(req('GET', '/v1/me', { token: 'nope' }), env)).status, 401);
  assert.equal((await handle(req('GET', '/v1/me'), env)).status, 401);
  // the raw token is never stored
  const row = env.DB.raw.prepare('SELECT token_hash FROM accounts').get();
  assert.notEqual(row.token_hash, token);
});

test('boldsign: trial envelope, then 402 until subscribed; status and download', async () => {
  const state = { status: 'InProgress' };
  const f = boldsignFetch(state);
  let t = 1_000_000;
  const deps = { fetch: f, now: () => t };
  const env = { DB: fakeD1(), ESIGN_PROVIDER: 'boldsign', BOLDSIGN_API_KEY: 'k', TRIAL_ENVELOPES: '1' };
  const { token } = await register(env, deps);

  const sent = await handle(req('POST', '/v1/envelopes', { token, body: envelopeBody() }), env, deps);
  assert.equal(sent.status, 201);
  const e = await sent.json();
  assert.equal(e.provider, 'boldsign');
  assert.equal(e.state, 'sent');

  const call = f.calls.find((c) => c.url.endsWith('/v1/document/send'));
  assert.equal(call.init.headers['X-API-KEY'], 'k');
  const body = JSON.parse(call.init.body);
  assert.equal(body.Signers[0].EmailAddress, 'pat@example.com');
  assert.deepEqual(body.Signers[0].FormFields.map((x) => x.FieldType), ['Signature', 'Initial', 'DateSigned']);
  assert.deepEqual(body.Signers[0].FormFields[0].Bounds, { X: 316, Y: 600, Width: 180, Height: 38 });
  assert.equal(body.Files[0], `data:application/pdf;base64,${PDF}`);

  // second request: trial used up
  const again = await handle(req('POST', '/v1/envelopes', { token, body: envelopeBody() }), env, deps);
  assert.equal(again.status, 402);

  // fresh state is served from the database without calling the provider
  const n = f.calls.length;
  assert.equal((await (await handle(req('GET', `/v1/envelopes/${e.id}`, { token }), env, deps)).json()).state, 'sent');
  assert.equal(f.calls.length, n);

  // document is refused until completed
  assert.equal((await handle(req('GET', `/v1/envelopes/${e.id}/document`, { token }), env, deps)).status, 409);

  // stale: polls BoldSign
  t += 60_000;
  state.status = 'Completed';
  assert.equal((await (await handle(req('GET', `/v1/envelopes/${e.id}`, { token }), env, deps)).json()).state, 'completed');
  const doc = await handle(req('GET', `/v1/envelopes/${e.id}/document`, { token }), env, deps);
  assert.equal(doc.status, 200);
  assert.equal(doc.headers.get('content-type'), 'application/pdf');
  assert.equal(await doc.text(), '%PDF signed');

  // another account cannot see it
  const other = await register(env, deps);
  assert.equal((await handle(req('GET', `/v1/envelopes/${e.id}`, { token: other.token }), env, deps)).status, 404);
});

test('envelopes: input validation', async () => {
  const env = { DB: fakeD1(), ESIGN_PROVIDER: 'boldsign', BOLDSIGN_API_KEY: 'k', TRIAL_ENVELOPES: '5' };
  const deps = { fetch: boldsignFetch() };
  const { token } = await register(env, deps);
  const bad = async (b) => (await handle(req('POST', '/v1/envelopes', { token, body: envelopeBody(b) }), env, deps)).status;
  assert.equal(await bad({ signer: { email: 'not-an-email' } }), 400);
  assert.equal(await bad({ fields: [{ type: 'initials', page: 1, x: 1, y: 1, width: 1, height: 1 }] }), 400);
  assert.equal(await bad({ fields: [{ type: 'signature', page: 1, x: -5, y: 1, width: 1, height: 1 }] }), 400);
  assert.equal(await bad({ pdfBase64: '' }), 400);
});

test('boldsign webhook: verifies signature and updates state', async () => {
  const f = boldsignFetch();
  const env = { DB: fakeD1(), ESIGN_PROVIDER: 'boldsign', BOLDSIGN_API_KEY: 'k', BOLDSIGN_WEBHOOK_SECRET: 'whsec', TRIAL_ENVELOPES: '1' };
  const deps = { fetch: f };
  const { token } = await register(env, deps);
  const e = await (await handle(req('POST', '/v1/envelopes', { token, body: envelopeBody() }), env, deps)).json();

  const body = JSON.stringify({ event: { eventType: 'Completed' }, data: { documentId: 'bs-doc-1' } });
  const ts = '1700000000';
  const sig = [...(await hmacSha256('whsec', `${ts}.${body}`))].map((x) => x.toString(16).padStart(2, '0')).join('');

  const forged = await handle(req('POST', '/v1/webhooks/boldsign', { body, headers: { 'x-boldsign-signature': `t=${ts}, s0=deadbeef` } }), env, deps);
  assert.equal(forged.status, 401);

  const ok = await handle(req('POST', '/v1/webhooks/boldsign', { body, headers: { 'x-boldsign-signature': `t=${ts}, s0=${sig}` } }), env, deps);
  assert.equal(ok.status, 200);
  assert.equal((await (await handle(req('GET', `/v1/envelopes/${e.id}`, { token }), env, deps)).json()).state, 'completed');
});

test('docusign: JWT auth, envelope tabs, status, Connect webhook', async () => {
  _resetDocusignTokenCache();
  const { pem, publicKey } = await rsaKeyPem();
  let envStatus = 'sent';
  const f = fakeFetch([
    ['/oauth/token', () => jsonResponse({ access_token: 'ds-token', expires_in: 3600 })],
    [/\/envelopes$/, () => jsonResponse({ envelopeId: 'ds-env-1', status: 'sent' })],
    [/\/envelopes\/ds-env-1$/, () => jsonResponse({ status: envStatus })],
    ['/documents/combined', () => new Response('%PDF ds')],
  ]);
  let t = 5_000_000;
  const deps = { fetch: f, now: () => t };
  const env = {
    DB: fakeD1(), ESIGN_PROVIDER: 'docusign', TRIAL_ENVELOPES: '1',
    DOCUSIGN_INTEGRATION_KEY: 'ik', DOCUSIGN_USER_ID: 'uid', DOCUSIGN_ACCOUNT_ID: 'acct', DOCUSIGN_PRIVATE_KEY: pem,
    DOCUSIGN_CONNECT_HMAC_KEY: 'hmac', PUBLIC_URL: 'https://relay.test',
  };
  const { token } = await register(env, deps);
  const e = await (await handle(req('POST', '/v1/envelopes', { token, body: envelopeBody() }), env, deps)).json();
  assert.equal(e.provider, 'docusign');

  // JWT assertion is a valid RS256 signature with the expected claims
  const auth = f.calls.find((c) => c.url.includes('/oauth/token'));
  const assertion = new URLSearchParams(auth.init.body).get('assertion');
  const [h, p, s] = assertion.split('.');
  const claims = JSON.parse(Buffer.from(p, 'base64url').toString());
  assert.equal(claims.iss, 'ik');
  assert.equal(claims.sub, 'uid');
  assert.equal(claims.scope, 'signature impersonation');
  const valid = await crypto.subtle.verify('RSASSA-PKCS1-v1_5', publicKey, Buffer.from(s, 'base64url'), new TextEncoder().encode(`${h}.${p}`));
  assert.ok(valid);

  const create = f.calls.find((c) => /\/accounts\/acct\/envelopes$/.test(c.url));
  assert.equal(create.init.headers.Authorization, 'Bearer ds-token');
  const env1 = JSON.parse(create.init.body);
  const tabs = env1.recipients.signers[0].tabs;
  assert.deepEqual(tabs.signHereTabs, [{ documentId: '1', pageNumber: '1', xPosition: '316', yPosition: '600' }]);
  assert.equal(tabs.initialHereTabs[0].pageNumber, '2');
  assert.equal(tabs.dateSignedTabs.length, 1);
  assert.equal(env1.eventNotification.url, 'https://relay.test/v1/webhooks/docusign');

  t += 60_000;
  envStatus = 'delivered';
  assert.equal((await (await handle(req('GET', `/v1/envelopes/${e.id}`, { token }), env, deps)).json()).state, 'viewed');

  const body = JSON.stringify({ event: 'envelope-completed', data: { envelopeId: 'ds-env-1' } });
  const sig = bytesToBase64(await hmacSha256('hmac', body));
  assert.equal((await handle(req('POST', '/v1/webhooks/docusign', { body, headers: { 'x-docusign-signature-1': 'bad' } }), env, deps)).status, 401);
  assert.equal((await handle(req('POST', '/v1/webhooks/docusign', { body, headers: { 'x-docusign-signature-1': sig } }), env, deps)).status, 200);
  assert.equal((await (await handle(req('GET', `/v1/envelopes/${e.id}`, { token }), env, deps)).json()).state, 'completed');
  assert.equal(await (await handle(req('GET', `/v1/envelopes/${e.id}/document`, { token }), env, deps)).text(), '%PDF ds');
});

test('play: verifies, acknowledges and activates; RTDN cancels', async () => {
  _resetPlayTokenCache();
  const { pem } = await rsaKeyPem();
  let sub = {
    subscriptionState: 'SUBSCRIPTION_STATE_ACTIVE',
    acknowledgementState: 'ACKNOWLEDGEMENT_STATE_PENDING',
    lineItems: [{ productId: 'inspectors_gadget_pro', expiryTime: '2099-01-01T00:00:00Z', offerDetails: { basePlanId: 'annual' } }],
  };
  const f = fakeFetch([
    ['oauth2.googleapis.com/token', () => jsonResponse({ access_token: 'g-token', expires_in: 3600 })],
    [':acknowledge', () => jsonResponse({})],
    ['/purchases/subscriptionsv2/tokens/', () => jsonResponse(sub)],
  ]);
  const deps = { fetch: f };
  const env = {
    DB: fakeD1(), TRIAL_ENVELOPES: '1', ANDROID_PACKAGE: 'com.inspectorsgadget.app', SUBSCRIPTION_PRODUCT: 'inspectors_gadget_pro',
    GOOGLE_SERVICE_ACCOUNT_JSON: JSON.stringify({ client_email: 'sa@x.iam.gserviceaccount.com', private_key: pem }),
    PLAY_PUSH_TOKEN: 'push-secret',
  };
  const { token } = await register(env, deps);
  const r = await handle(req('POST', '/v1/subscriptions/play', { token, body: { productId: 'inspectors_gadget_pro', purchaseToken: 'ptok' } }), env, deps);
  const ent = await r.json();
  assert.equal(ent.active, true);
  assert.equal(ent.plan, 'annual');
  assert.ok(f.calls.some((c) => c.url.includes('/purchases/subscriptions/inspectors_gadget_pro/tokens/ptok:acknowledge')));
  assert.ok(f.calls.some((c) => c.url.includes('/applications/com.inspectorsgadget.app/purchases/subscriptionsv2/tokens/ptok')));

  // Play says it expired; the push notification updates the account
  sub = { ...sub, subscriptionState: 'SUBSCRIPTION_STATE_EXPIRED', acknowledgementState: 'ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED' };
  const data = Buffer.from(JSON.stringify({ subscriptionNotification: { purchaseToken: 'ptok', notificationType: 13 } })).toString('base64');
  assert.equal((await handle(req('POST', '/v1/webhooks/play?token=wrong', { body: { message: { data } } }), env, deps)).status, 401);
  assert.equal((await handle(req('POST', '/v1/webhooks/play?token=push-secret', { body: { message: { data } } }), env, deps)).status, 200);
  const me = await (await handle(req('GET', '/v1/me', { token }), env, deps)).json();
  assert.equal(me.active, false);
});

test('unconfigured provider answers 503, not a crash', async () => {
  const env = { DB: fakeD1(), ESIGN_PROVIDER: 'boldsign', TRIAL_ENVELOPES: '1' };
  const { token } = await register(env);
  const r = await handle(req('POST', '/v1/envelopes', { token, body: envelopeBody() }), env, { fetch: boldsignFetch() });
  assert.equal(r.status, 503);
});
