import { boldsign } from './boldsign.js';
import { docusign } from './docusign.js';

/**
 * Every provider implements the same interface:
 *
 *   name: string
 *   send({ title, message, reference, signer: { name, email }, fields, pdf: Uint8Array })
 *       -> { providerId, state }
 *       fields: [{ type: 'signature' | 'initials' | 'date', page, x, y, width, height }]
 *       in PDF points, origin top-left, pages from 1 (what the app sends).
 *   status(providerId) -> state            (see states.js)
 *   download(providerId) -> Uint8Array     (completed PDF including the audit certificate)
 *   parseWebhook(request, bodyText) -> { providerId, state } | null
 *       throws HttpError(401) when the signature header does not verify.
 */
export function createProvider(env, fetchImpl = fetch) {
  const name = (env.ESIGN_PROVIDER || 'boldsign').toLowerCase();
  if (name === 'boldsign') return boldsign(env, fetchImpl);
  if (name === 'docusign') return docusign(env, fetchImpl);
  throw new Error(`Unknown ESIGN_PROVIDER "${name}"`);
}

export function providerByName(name, env, fetchImpl = fetch) {
  return createProvider({ ...env, ESIGN_PROVIDER: name }, fetchImpl);
}
