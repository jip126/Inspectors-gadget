import { HttpError, base64ToBytes, json, randomId, readJson, sha256Hex } from './http.js';
import { createProvider, providerByName } from './esign/index.js';
import { PENDING } from './esign/states.js';
import { play as playApi } from './google/play.js';

/**
 * Inspector's Gadget relay. The Android app calls this; it holds every secret.
 *
 *   POST /v1/accounts                    register a company install -> { accountId, token }
 *   GET  /v1/me                          entitlement
 *   POST /v1/subscriptions/play          verify a Google Play purchase
 *   POST /v1/envelopes                   send a document for signature
 *   GET  /v1/envelopes/:id               state (refreshed from the provider when stale)
 *   GET  /v1/envelopes/:id/document      completed PDF
 *   POST /v1/webhooks/boldsign|docusign  provider callbacks
 *   POST /v1/webhooks/play?token=...     Play real-time developer notifications (Pub/Sub push)
 */
export default {
  async fetch(request, env, ctx) {
    return handle(request, env, { fetch: globalThis.fetch.bind(globalThis) });
  },
};

const STALE_MS = 30_000;
const MAX_PDF_BYTES = 15 * 1024 * 1024;

export async function handle(request, env, deps = {}) {
  const fetchImpl = deps.fetch || globalThis.fetch.bind(globalThis);
  const now = deps.now || (() => Date.now());
  try {
    const url = new URL(request.url);
    const path = url.pathname.replace(/\/+$/, '');
    const m = request.method;

    if (m === 'GET' && path === '/health') return json({ ok: true });
    if (m === 'POST' && path === '/v1/accounts') return await register(request, env, now);
    if (m === 'POST' && path.startsWith('/v1/webhooks/')) {
      const which = path.slice('/v1/webhooks/'.length);
      if (which === 'play') return await playWebhook(request, env, url, fetchImpl, now);
      return await providerWebhook(which, request, env, fetchImpl, now);
    }

    const account = await authenticate(request, env);
    if (m === 'GET' && path === '/v1/me') return json(entitlement(account, env, now));
    if (m === 'POST' && path === '/v1/subscriptions/play') return await verifyPlay(request, env, account, fetchImpl, now);
    if (m === 'POST' && path === '/v1/envelopes') return await sendEnvelope(request, env, account, fetchImpl, now);
    let r = path.match(/^\/v1\/envelopes\/([\w-]+)$/);
    if (m === 'GET' && r) return await getEnvelope(r[1], env, account, fetchImpl, now);
    r = path.match(/^\/v1\/envelopes\/([\w-]+)\/document$/);
    if (m === 'GET' && r) return await getDocument(r[1], env, account, fetchImpl);
    throw new HttpError(404, 'Not found');
  } catch (e) {
    if (e instanceof HttpError) return json({ error: e.message }, e.status);
    console.error(e);
    return json({ error: 'Server error' }, 500);
  }
}

// ---------------------------------------------------------------- accounts

async function register(request, env, now) {
  const b = await readJson(request);
  const id = randomId('acc_');
  const token = randomId('igt_', 32);
  await env.DB.prepare(
    'INSERT INTO accounts (id, company_name, email, install_id, token_hash, created_at) VALUES (?, ?, ?, ?, ?, ?)',
  ).bind(id, str(b.companyName, 200), str(b.email, 200), str(b.installId, 100), await sha256Hex(token), now()).run();
  return json({ accountId: id, token }, 201);
}

async function authenticate(request, env) {
  const h = request.headers.get('authorization') || '';
  const token = h.startsWith('Bearer ') ? h.slice(7).trim() : '';
  if (!token) throw new HttpError(401, 'Missing token');
  const row = await env.DB.prepare('SELECT * FROM accounts WHERE token_hash = ?').bind(await sha256Hex(token)).first();
  if (!row) throw new HttpError(401, 'Invalid token');
  return row;
}

export function entitlement(a, env, now) {
  const active = !!a.plan && a.plan_expires_at > now();
  const trialLeft = Math.max(0, Number(env.TRIAL_ENVELOPES ?? 1) - a.trial_envelopes_used);
  return {
    accountId: a.id,
    companyName: a.company_name,
    active,
    plan: active ? a.plan : '',
    expiresAt: active ? a.plan_expires_at : 0,
    trialEnvelopesLeft: active ? 0 : trialLeft,
  };
}

// ---------------------------------------------------------------- subscriptions

async function verifyPlay(request, env, account, fetchImpl, now) {
  const b = await readJson(request);
  if (!b.purchaseToken) throw new HttpError(400, 'purchaseToken required');
  const v = await (await getPlay(env, fetchImpl)).verifySubscription(b.purchaseToken);
  await env.DB.prepare('UPDATE accounts SET plan = ?, plan_expires_at = ?, play_token = ? WHERE id = ?')
    .bind(v.active ? v.plan : '', v.active ? v.expiresAt : 0, b.purchaseToken, account.id).run();
  const fresh = await env.DB.prepare('SELECT * FROM accounts WHERE id = ?').bind(account.id).first();
  return json(entitlement(fresh, env, now));
}

async function playWebhook(request, env, url, fetchImpl, now) {
  if (!env.PLAY_PUSH_TOKEN || url.searchParams.get('token') !== env.PLAY_PUSH_TOKEN) throw new HttpError(401, 'Bad token');
  const b = await readJson(request);
  let note;
  try {
    note = JSON.parse(new TextDecoder().decode(base64ToBytes(b?.message?.data || '')));
  } catch {
    return json({ ok: true }); // not ours; acknowledge so Pub/Sub stops retrying
  }
  const token = note?.subscriptionNotification?.purchaseToken;
  if (!token) return json({ ok: true });
  const account = await env.DB.prepare('SELECT * FROM accounts WHERE play_token = ?').bind(token).first();
  if (!account) return json({ ok: true });
  const v = await (await getPlay(env, fetchImpl)).verifySubscription(token);
  await env.DB.prepare('UPDATE accounts SET plan = ?, plan_expires_at = ? WHERE id = ?')
    .bind(v.active ? v.plan : '', v.active ? v.expiresAt : 0, account.id).run();
  return json({ ok: true });
}

async function getPlay(env, fetchImpl) {
  return playApi(env, fetchImpl);
}

// ---------------------------------------------------------------- envelopes

async function sendEnvelope(request, env, account, fetchImpl, now) {
  const ent = entitlement(account, env, now);
  if (!ent.active && ent.trialEnvelopesLeft <= 0) throw new HttpError(402, 'Subscription required');
  const b = await readJson(request);
  const email = str(b?.signer?.email, 200);
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) throw new HttpError(400, 'Valid signer email required');
  if (!b.pdfBase64) throw new HttpError(400, 'pdfBase64 required');
  const pdf = base64ToBytes(b.pdfBase64);
  if (pdf.length > MAX_PDF_BYTES) throw new HttpError(413, 'PDF too large');
  const fields = (Array.isArray(b.fields) ? b.fields : []).map(field);
  if (!fields.some((f) => f.type === 'signature')) throw new HttpError(400, 'At least one signature field required');

  const provider = createProvider(env, fetchImpl);
  const sent = await provider.send({
    title: str(b.title, 200) || 'Agreement',
    message: str(b.message, 2000),
    reference: str(b.reference, 100),
    signer: { name: str(b?.signer?.name, 200), email },
    fields,
    pdf,
  });
  const id = randomId('env_');
  const t = now();
  await env.DB.prepare(
    'INSERT INTO envelopes (id, account_id, provider, provider_id, state, reference, signer_email, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)',
  ).bind(id, account.id, provider.name, sent.providerId, sent.state, str(b.reference, 100), email, t, t).run();
  if (!ent.active) {
    await env.DB.prepare('UPDATE accounts SET trial_envelopes_used = trial_envelopes_used + 1 WHERE id = ?').bind(account.id).run();
  }
  return json({ id, provider: provider.name, state: sent.state }, 201);
}

async function findEnvelope(id, env, account) {
  const e = await env.DB.prepare('SELECT * FROM envelopes WHERE id = ? AND account_id = ?').bind(id, account.id).first();
  if (!e) throw new HttpError(404, 'Not found');
  return e;
}

async function getEnvelope(id, env, account, fetchImpl, now) {
  const e = await findEnvelope(id, env, account);
  let state = e.state;
  if (PENDING.has(state) && now() - e.updated_at > STALE_MS) {
    state = await providerByName(e.provider, env, fetchImpl).status(e.provider_id);
    await env.DB.prepare('UPDATE envelopes SET state = ?, updated_at = ? WHERE id = ?').bind(state, now(), e.id).run();
  }
  return json({ id: e.id, provider: e.provider, state });
}

async function getDocument(id, env, account, fetchImpl) {
  const e = await findEnvelope(id, env, account);
  if (e.state !== 'completed') throw new HttpError(409, 'Not completed');
  const pdf = await providerByName(e.provider, env, fetchImpl).download(e.provider_id);
  return new Response(pdf, { headers: { 'content-type': 'application/pdf' } });
}

async function providerWebhook(name, request, env, fetchImpl, now) {
  if (!['boldsign', 'docusign'].includes(name)) throw new HttpError(404, 'Not found');
  const body = await request.text();
  const ev = await providerByName(name, env, fetchImpl).parseWebhook(request, body);
  if (ev) {
    await env.DB.prepare('UPDATE envelopes SET state = ?, updated_at = ? WHERE provider = ? AND provider_id = ? AND state NOT IN (\'completed\', \'declined\', \'voided\', \'expired\')')
      .bind(ev.state, now(), name, ev.providerId).run();
  }
  return json({ ok: true });
}

// ---------------------------------------------------------------- helpers

function str(v, max) {
  return typeof v === 'string' ? v.trim().slice(0, max) : '';
}

function field(f) {
  const type = ['signature', 'initials', 'date'].includes(f?.type) ? f.type : null;
  const nums = ['page', 'x', 'y', 'width', 'height'].map((k) => Number(f?.[k]));
  if (!type || nums.some((n) => !Number.isFinite(n) || n < 0)) throw new HttpError(400, 'Invalid field');
  const [page, x, y, width, height] = nums;
  return { type, page: Math.max(1, Math.round(page)), x, y, width, height };
}
