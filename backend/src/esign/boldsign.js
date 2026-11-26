import { HttpError, bytesToBase64, call, hmacSha256, safeEqual } from '../http.js';
import { COMPLETED, DECLINED, EXPIRED, SENT, VIEWED, VOIDED } from './states.js';

const FIELD_TYPES = { signature: 'Signature', initials: 'Initial', date: 'DateSigned' };

const STATUS = {
  inprogress: SENT,
  waitingforothers: SENT,
  needsattention: SENT,
  completed: COMPLETED,
  declined: DECLINED,
  expired: EXPIRED,
  revoked: VOIDED,
};

const EVENTS = {
  sent: SENT,
  viewed: VIEWED,
  signed: VIEWED,
  completed: COMPLETED,
  declined: DECLINED,
  expired: EXPIRED,
  revoked: VOIDED,
};

/**
 * BoldSign API (https://developers.boldsign.com). Auth: X-API-KEY header.
 * Env: BOLDSIGN_API_KEY, BOLDSIGN_WEBHOOK_SECRET, BOLDSIGN_API_BASE (optional).
 */
export function boldsign(env, fetchImpl) {
  const base = (env.BOLDSIGN_API_BASE || 'https://api.boldsign.com').replace(/\/$/, '');
  const headers = (extra = {}) => {
    if (!env.BOLDSIGN_API_KEY) throw new HttpError(503, 'BoldSign is not configured');
    return { 'X-API-KEY': env.BOLDSIGN_API_KEY, ...extra };
  };

  return {
    name: 'boldsign',

    async send({ title, message, signer, fields, pdf }) {
      const body = {
        Title: title,
        Message: message,
        Signers: [
          {
            Name: signer.name || signer.email,
            EmailAddress: signer.email,
            SignerType: 'Signer',
            FormFields: fields.map((f, i) => ({
              Id: `${f.type}_${i + 1}`,
              FieldType: FIELD_TYPES[f.type] || 'Signature',
              PageNumber: f.page,
              Bounds: { X: f.x, Y: f.y, Width: f.width, Height: f.height },
              IsRequired: true,
            })),
          },
        ],
        Files: [`data:application/pdf;base64,${bytesToBase64(pdf)}`],
        ExpiryDays: 30,
      };
      const r = await call(fetchImpl, `${base}/v1/document/send`, {
        method: 'POST',
        headers: headers({ 'content-type': 'application/json' }),
        body: JSON.stringify(body),
      }, 'BoldSign');
      const out = await r.json();
      return { providerId: out.documentId, state: SENT };
    },

    async status(providerId) {
      const r = await call(fetchImpl, `${base}/v1/document/properties?documentId=${encodeURIComponent(providerId)}`, {
        headers: headers(),
      }, 'BoldSign');
      const p = await r.json();
      const st = STATUS[String(p.status || '').toLowerCase()] || SENT;
      if (st === SENT && (p.signerDetails || []).some((s) => s.isViewed)) return VIEWED;
      return st;
    },

    async download(providerId) {
      const r = await call(fetchImpl, `${base}/v1/document/download?documentId=${encodeURIComponent(providerId)}`, {
        headers: headers(),
      }, 'BoldSign');
      return new Uint8Array(await r.arrayBuffer());
    },

    /** Header "X-BoldSign-Signature: t=<unix>, s0=<hex HMAC-SHA256 of `${t}.${body}`>". */
    async parseWebhook(request, bodyText) {
      if (!env.BOLDSIGN_WEBHOOK_SECRET) throw new HttpError(503, 'BoldSign webhook secret not set');
      const header = request.headers.get('x-boldsign-signature') || '';
      const parts = Object.fromEntries(header.split(',').map((kv) => kv.trim().split('=')));
      const mac = await hmacSha256(env.BOLDSIGN_WEBHOOK_SECRET, `${parts.t}.${bodyText}`);
      const hex = [...mac].map((x) => x.toString(16).padStart(2, '0')).join('');
      if (!parts.t || !safeEqual(hex, parts.s0 || '')) throw new HttpError(401, 'Bad signature');
      const body = JSON.parse(bodyText);
      const type = String(body?.event?.eventType || '').toLowerCase();
      const state = EVENTS[type];
      const providerId = body?.data?.documentId;
      if (!state || !providerId) return null; // e.g. the "Verification" ping
      return { providerId, state };
    },
  };
}
