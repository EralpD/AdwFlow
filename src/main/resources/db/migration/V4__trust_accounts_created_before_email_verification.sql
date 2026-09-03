-- Email verification was not enforced before this migration. Preserve access for those existing accounts.
-- New accounts are inserted after this one-time migration and remain unverified until they consume a Redis code.
UPDATE app_users
SET email_verified_at = created_at
WHERE email_verified_at IS NULL;
