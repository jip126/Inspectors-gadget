import { HttpError, call } from '../http.js';
import { signJwtRS256 } from './jwt.js';

let cached = null; // { email, token, exp }

/**
 * Google Play Developer API: verifies subscription purchases made in the app.
 * Env: GOOGLE_SERVICE_ACCOUNT_JSON (service account linked in Play Console with "View financial data"
 * and "Manage orders and subscriptions"), ANDROID_PACKAGE, SUBSCRIPTION_PRODUCT.
 */
export function play(env, fetchImpl = fetch) {
  const pkg = env.ANDROID_PACKAGE || 'com.inspectorsgadget.app';
  const api = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${pkg}`;

  async function token() {
    if (!env.GOOGLE_SERVICE_ACCOUNT_JSON) throw new HttpError(503, 'Google Play is not configured');
    const sa = JSON.parse(env.GOOGLE_SERVICE_ACCOUNT_JSON);
    const now = Math.floor(Date.now() / 1000);
    if (cached && cached.email === sa.client_email && cached.exp > now + 60) return cached.token;
    const assertion = await signJwtRS256({
      iss: sa.client_email,
      scope: 'https://www.googleapis.com/auth/androidpublisher',
      aud: 'https://oauth2.googleapis.com/token',
      iat: now,
      exp: now + 3600,
    }, sa.private_key);
    const r = await call(fetchImpl, 'https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: { 'content-type': 'application/x-www-form-urlencoded' },
      body: `grant_type=${encodeURIComponent('urn:ietf:params:oauth:grant-type:jwt-bearer')}&assertion=${assertion}`,
    }, 'Google auth');
    const t = await r.json();
    cached = { email: sa.client_email, token: t.access_token, exp: now + (t.expires_in || 3600) };
    return t.access_token;
  }

  return {
    /**
     * Returns { active, plan, expiresAt, productId } for a purchase token, acknowledging new purchases
     * (Play refunds purchases that are not acknowledged within 3 days).
     */
    async verifySubscription(purchaseToken) {
      const auth = { Authorization: `Bearer ${await token()}` };
      const r = await call(fetchImpl, `${api}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`,
        { headers: auth }, 'Google Play');
      const s = await r.json();
      const items = (s.lineItems || []).filter((li) => !env.SUBSCRIPTION_PRODUCT || li.productId === env.SUBSCRIPTION_PRODUCT);
      let best = null;
      for (const li of items) {
        const exp = Date.parse(li.expiryTime || 0) || 0;
        if (!best || exp > best.expiresAt) {
          best = { productId: li.productId, plan: li.offerDetails?.basePlanId || '', expiresAt: exp };
        }
      }
      const activeStates = ['SUBSCRIPTION_STATE_ACTIVE', 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD'];
      const active = !!best && activeStates.includes(s.subscriptionState) && best.expiresAt > Date.now();
      if (best && s.acknowledgementState === 'ACKNOWLEDGEMENT_STATE_PENDING') {
        await call(fetchImpl,
          `${api}/purchases/subscriptions/${encodeURIComponent(best.productId)}/tokens/${encodeURIComponent(purchaseToken)}:acknowledge`,
          { method: 'POST', headers: { ...auth, 'content-type': 'application/json' }, body: '{}' }, 'Google Play ack');
      }
      return { active, plan: best?.plan || '', expiresAt: best?.expiresAt || 0, productId: best?.productId || '' };
    },
  };
}

export function _resetPlayTokenCache() {
  cached = null;
}
