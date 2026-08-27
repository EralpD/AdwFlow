-- Keep V1 unchanged: existing databases must retain their Flyway checksums.
ALTER TABLE app_users ADD COLUMN role VARCHAR(24) NOT NULL DEFAULT 'USER';

-- ADMIN takes precedence for accounts that previously had both authorities.
UPDATE app_users
SET role = 'ADMIN'
WHERE EXISTS (
    SELECT 1 FROM app_user_roles
    WHERE app_user_roles.user_id = app_users.id AND app_user_roles.role = 'ADMIN'
);

ALTER TABLE app_users ADD CONSTRAINT ck_app_users_role CHECK (role IN ('USER', 'ADMIN'));
ALTER TABLE app_users ADD COLUMN email_verified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE app_users ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE app_users ADD CONSTRAINT ck_app_users_auth_version CHECK (auth_version >= 0);

-- Do not silently mark existing accounts as email-verified.
DROP TABLE app_user_roles;
