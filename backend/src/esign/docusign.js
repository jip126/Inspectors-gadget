import { HttpError, bytesToBase64, call, hmacSha256, safeEqual } from '../http.js';
import { signJwtRS256 } from '../google/jwt.js';
import { COMPLETED, DECLINED, EXPIRED, SENT, VIEWED, VOIDED } from './states.js';

const STATUS = {
  created: SENT,
  sent: SENT,
  delivered: VIEWED,
  signed: VIEWED,
  completed: COMPLETED,
  declined: DECLINED,
  voided: VOIDED,
  expired: EXPIRED,
};

const TABS = { signature: 'signHereTabs', initials: 'initialHereTabs', date: 'dateSignedTabs' };

let cachedToken = null; // { key, token, exp }

/**
 * DocuSign eSignature REST API v2.1 with JWT grant (service integration, no user login).
 * Env: DOCUSIGN_INTEGRATION_KEY, DOCUSIGN_USER_ID, DOCUSIGN_ACCOUNT_ID,
 *      DOCUSIGN_PRIVATE_KEY (PKCS#8 PEM), DOCUSIGN_CONNECT_HMAC_KEY,
 *      DOCUSIGN_OAUTH_HOST (account-d.docusign.com for demo, account.docusign.com for production),
 *      DOCUSIGN_BASE_PATH (e.g. https://demo.docusign.net/restapi), PUBLIC_URL (optional, for webhooks).
 */
export function docusign(env, fetchImpl) {
  const oauthHost = env.DOCUSIGN_OAUTH_HOST || 'account-d.docusign.com';
  const basePath = (env.DOCUSIGN_BASE_PATH || 'https://demo.docusign.net/restapi').replace(/\/$/, '');
  const accountUrl = () => `${basePath}/v2.1/accounts/${env.DOCUSIGN_ACCOUNT_ID}`;

  async function token() {
    for (const k of ['DOCUSIGN_INTEGRATION_KEY', 'DOCUSIGN_USER_ID', 'DOCUSIGN_ACCOUNT_ID', 'DOCUSIGN_PRIVATE_KEY']) {
      if (!env[k]) throw new HttpError(503, 'DocuSign is not configured');
    }
    const key = `${env.DOCUSIGN_INTEGRATION_KEY}:${env.DOCUSIGN_USER_ID}`;
    const now = Math.floor(Date.now() / 1000);
    if (cachedToken && cachedToken.key === key && cachedToken.exp > now + 60) return cachedToken.token;
    const assertion = await signJwtRS256({
      iss: env.DOCUSIGN_INTEGRATION_KEY,
      sub: env.DOCUSIGN_USER_ID,
      aud: oauthHost,
      iat: now,
      exp: now + 3600,
      scope: 'signature impersonation',
    }, env.DOCUSIGN_PRIVATE_KEY);
    const r = await call(fetchImpl, `https://${oauthHost}/oauth/token`, {
      method: 'POST',
      headers: { 'content-type': 'application/x-www-form-urlencoded' },
      body: `grant_type=${encodeURIComponent('urn:ietf:params:oauth:grant-type:jwt-bearer')}&assertion=${assertion}`,
    }, 'DocuSign auth');
    const t = await r.json();
    cachedToken = { key, token: t.access_token, exp: now + (t.expires_in || 3600) };
    return t.access_token;
  }

  const authed = async (extra = {}) => ({ Authorization: `Bearer ${await token()}`, ...extra });

  return {
    name: 'docusign',

    async send({ title, message, reference, signer, fields, pdf }) {
      const tabs = {};
      for (const f of fields) {
        const k = TABS[f.type] || 'signHereTabs';
        (tabs[k] = tabs[k] || []).push({
          documentId: '1',
          pageNumber: String(f.page),
          xPosition: String(Math.round(f.x)),
          yPosition: String(Math.round(f.y)),
        });
      }
      const envelope = {
        emailSubject: title.slice(0, 100),
        emailBlurb: message,
        documents: [{ documentId: '1', name: `${title}.pdf`, fileExtension: 'pdf', documentBase64: bytesToBase64(pdf) }],
        recipients: {
          signers: [{ recipientId: '1', routingOrder: '1', name: signer.name || signer.email, email: signer.email, tabs }],
        },
        customFields: reference
          ? { textCustomFields: [{ name: 'inspection', value: String(reference), show: 'false' }] }
          : undefined,
        status: 'sent',
      };
      if (env.PUBLIC_URL) {
        envelope.eventNotification = {
          url: `${env.PUBLIC_URL.replace(/\/$/, '')}/v1/webhooks/docusign`,
          requireAcknowledgment: 'true',
          includeHMAC: 'true',
          eventData: { version: 'restv2.1', format: 'json' },
          envelopeEvents: ['sent', 'delivered', 'completed', 'declined', 'voided'].map((s) => ({ envelopeEventStatusCode: s })),
        };
      }
      const r = await call(fetchImpl, `${accountUrl()}/envelopes`, {
        method: 'POST',
        headers: await authed({ 'content-type': 'application/json' }),
        body: JSON.stringify(envelope),
      }, 'DocuSign');
      const out = await r.json();
      return { providerId: out.envelopeId, state: STATUS[out.status] || SENT };
    },

    async status(providerId) {
      const r = await call(fetchImpl, `${accountUrl()}/envelopes/${encodeURIComponent(providerId)}`, {
        headers: await authed(),
      }, 'DocuSign');
      const e = await r.json();
      return STATUS[String(e.status || '').toLowerCase()] || SENT;
    },

    async download(providerId) {
      const r = await call(fetchImpl,
        `${accountUrl()}/envelopes/${encodeURIComponent(providerId)}/documents/combined?certificate=true`,
        { headers: await authed() }, 'DocuSign');
      return new Uint8Array(await r.arrayBuffer());
    },

    /** DocuSign Connect (JSON). Header "X-DocuSign-Signature-1" = base64 HMAC-SHA256 of the raw body. */
    async parseWebhook(request, bodyText) {
      if (!env.DOCUSIGN_CONNECT_HMAC_KEY) throw new HttpError(503, 'DocuSign Connect HMAC key not set');
      const mac = bytesToBase64(await hmacSha256(env.DOCUSIGN_CONNECT_HMAC_KEY, bodyText));
      const sigs = [1, 2, 3].map((i) => request.headers.get(`x-docusign-signature-${i}`)).filter(Boolean);
      if (!sigs.some((s) => safeEqual(s, mac))) throw new HttpError(401, 'Bad signature');
      const body = JSON.parse(bodyText);
      const providerId = body?.data?.envelopeId;
      const event = String(body?.event || '').replace(/^envelope-/, '');
      const state = STATUS[event];
      if (!providerId || !state) return null;
      return { providerId, state };
    },
  };
}

export function _resetDocusignTokenCache() {
  cachedToken = null;
}

