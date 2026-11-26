-- One row per inspection company using the app.
CREATE TABLE IF NOT EXISTS accounts (
  id TEXT PRIMARY KEY,
  company_name TEXT NOT NULL DEFAULT '',
  email TEXT NOT NULL DEFAULT '',
  install_id TEXT NOT NULL DEFAULT '',
  token_hash TEXT NOT NULL UNIQUE,
  plan TEXT NOT NULL DEFAULT '',            -- 'monthly' | 'annual' | ''
  plan_expires_at INTEGER NOT NULL DEFAULT 0, -- ms since epoch
  play_token TEXT NOT NULL DEFAULT '',
  trial_envelopes_used INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS accounts_play_token ON accounts (play_token);

-- One row per signature request. `id` is ours; `provider_id` is BoldSign's documentId or DocuSign's envelopeId.
CREATE TABLE IF NOT EXISTS envelopes (
  id TEXT PRIMARY KEY,
  account_id TEXT NOT NULL REFERENCES accounts (id),
  provider TEXT NOT NULL,
  provider_id TEXT NOT NULL,
  state TEXT NOT NULL,                      -- sent | viewed | completed | declined | expired | voided
  reference TEXT NOT NULL DEFAULT '',       -- the app's inspection id
  signer_email TEXT NOT NULL DEFAULT '',
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS envelopes_account ON envelopes (account_id);
CREATE UNIQUE INDEX IF NOT EXISTS envelopes_provider ON envelopes (provider, provider_id);
