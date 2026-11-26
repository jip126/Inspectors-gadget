import { base64ToBytes, base64url } from '../http.js';

/** Signs an RS256 JWT with a PEM private key (PKCS#8, "BEGIN PRIVATE KEY"). */
export async function signJwtRS256(claims, pem, header = {}) {
  const key = await crypto.subtle.importKey(
    'pkcs8',
    pemToBytes(pem),
    { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
    false,
    ['sign'],
  );
  const enc = (o) => base64url(new TextEncoder().encode(JSON.stringify(o)));
  const input = `${enc({ alg: 'RS256', typ: 'JWT', ...header })}.${enc(claims)}`;
  const sig = await crypto.subtle.sign('RSASSA-PKCS1-v1_5', key, new TextEncoder().encode(input));
  return `${input}.${base64url(new Uint8Array(sig))}`;
}

export function pemToBytes(pem) {
  const b64 = pem.replace(/\\n/g, '\n').replace(/-----[^-]+-----/g, '').replace(/\s+/g, '');
  return base64ToBytes(b64);
}
