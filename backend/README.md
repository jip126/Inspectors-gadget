# Inspector's Gadget relay

A small backend the Android app talks to. It keeps every secret off the phone (e-signature API keys, the Google Play service account), sends agreements for signature through **BoldSign or DocuSign**, and tracks which inspection companies have an active subscription.

It runs on Cloudflare Workers with a D1 (SQLite) database. Both are free at this volume.

```
App ──HTTPS──> Relay ──> BoldSign or DocuSign ──email──> Client signs
 ^               ^  <──webhook── provider
 └──status, PDF──┘  <──Play notifications── Google Play
```

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/v1/accounts` | Register a company install. Returns `{ accountId, token }`. |
| GET | `/v1/me` | Entitlement: `{ active, plan, expiresAt, trialEnvelopesLeft }` |
| POST | `/v1/subscriptions/play` | Verify a Google Play purchase token and activate the plan |
| POST | `/v1/envelopes` | Send a PDF for signature (needs an active plan or a trial envelope) |
| GET | `/v1/envelopes/:id` | State: `sent`, `viewed`, `completed`, `declined`, `expired`, `voided` |
| GET | `/v1/envelopes/:id/document` | Completed PDF with the provider's audit certificate |
| POST | `/v1/webhooks/boldsign` | BoldSign callbacks (HMAC verified) |
| POST | `/v1/webhooks/docusign` | DocuSign Connect callbacks (HMAC verified) |
| POST | `/v1/webhooks/play?token=...` | Google Play real-time developer notifications |

Each account sees only its own envelopes. Tokens are stored as SHA-256 hashes.

## Choosing the provider

Set `ESIGN_PROVIDER` in `wrangler.toml` to `boldsign` or `docusign`. The app does not change. Envelopes remember which provider sent them, so switching later does not break requests already out.

Adapters live in `src/esign/`. Each implements `send`, `status`, `download` and `parseWebhook` (see `src/esign/index.js`).

## Setup checklist

### 1. Cloudflare

1. Create a free Cloudflare account and install Wrangler: `npm install -g wrangler`, then `wrangler login`.
2. `wrangler d1 create inspectors-gadget` and paste the `database_id` into `wrangler.toml`.
3. `npm run db:init` to create the tables.
4. `npm run deploy`. Note the `https://inspectors-gadget-relay.<you>.workers.dev` URL.
5. Put that URL in `PUBLIC_URL` (wrangler.toml) and in `Config.RELAY_URL` in the app, then rebuild the APK.

### 2a. BoldSign (recommended)

1. Sign up at boldsign.com and start the API plan (sandbox is free for testing).
2. API, API Key: create a key. `wrangler secret put BOLDSIGN_API_KEY`.
3. API, Webhooks: add `https://<relay>/v1/webhooks/boldsign` for Sent, Viewed, Signed, Completed, Declined, Expired, Revoked. Copy the signing secret: `wrangler secret put BOLDSIGN_WEBHOOK_SECRET`.
4. Switch the API base from sandbox to production when you go live (`BOLDSIGN_API_BASE`, if your region uses a different host).

### 2b. DocuSign (alternative)

1. Create a developer account at developers.docusign.com.
2. Apps and Keys: add an app (integration key), generate an RSA key pair, add any redirect URI.
3. Convert the private key to PKCS#8: `openssl pkcs8 -topk8 -nocrypt -in private.key -out private-pkcs8.pem`.
4. Grant consent once by opening, in a browser signed in as the sending user:
   `https://account-d.docusign.com/oauth/auth?response_type=code&scope=signature%20impersonation&client_id=<INTEGRATION_KEY>&redirect_uri=<REDIRECT_URI>`
5. Secrets: `DOCUSIGN_INTEGRATION_KEY`, `DOCUSIGN_USER_ID` (API Username), `DOCUSIGN_ACCOUNT_ID` (API Account ID), `DOCUSIGN_PRIVATE_KEY` (contents of the PKCS#8 file).
6. Settings, Connect: enable HMAC, add a key: `wrangler secret put DOCUSIGN_CONNECT_HMAC_KEY`.
7. Going live needs DocuSign's go-live review, then set `DOCUSIGN_OAUTH_HOST = "account.docusign.com"` and your production `DOCUSIGN_BASE_PATH`.

### 3. Google Play subscriptions

1. Google Play Console: create the app with package `com.inspectorsgadget.app`. A Play developer account costs a one-time $25.
2. Monetize, Subscriptions: create product `inspectors_gadget_pro` with two base plans, `monthly` (auto-renewing, 1 month) and `annual` (auto-renewing, 1 year). Prices: monthly $39.99, annual $400 (matches `Config.PRICE_*` in the app, which is shown until Play prices load).
3. Google Cloud: create a service account, download its JSON key, and in Play Console, Users and permissions, invite its email with "View financial data" and "Manage orders and subscriptions". `wrangler secret put GOOGLE_SERVICE_ACCOUNT_JSON` (paste the whole JSON).
4. Real-time developer notifications: create a Pub/Sub topic, grant `google-play-developer-notifications@system.gserviceaccount.com` publisher on it, set it in Play Console, Monetize, Monetization setup. Add a push subscription to `https://<relay>/v1/webhooks/play?token=<random>` and `wrangler secret put PLAY_PUSH_TOKEN` with the same random value.

### Google Play Billing in the app

The app builds without Gradle, and the Play Billing Library is only distributed as a Gradle/AAR dependency. The subscription logic is already behind `Billing.Gateway` (`DemoGateway` today). For the Play Store release:

1. Move the build to Gradle (Android Studio can import the `src/` and `res/` folders as-is).
2. Add `com.android.billingclient:billing`, write `PlayGateway implements Billing.Gateway`, and return it from `Billing.gateway()`.
3. After each purchase or restore, call `RelayClient.verifyPlayPurchase(productId, purchaseToken)` and pass the result to `Account.activate(...)`.

## Tests

```
npm test
```

Runs on Node 22+ with no installs. Provider and Google APIs are faked; D1 is replaced with `node:sqlite`.

## Not built yet

- Several inspectors in one company sharing one account (join code or invite). Today each install registers its own account.
- Sender identity per company (BoldSign "OnBehalfOf", DocuSign per-account users), so the email to the client comes from the inspection company's name.
- Rate limiting on `/v1/accounts`.
- Delivering final reports through a link.
