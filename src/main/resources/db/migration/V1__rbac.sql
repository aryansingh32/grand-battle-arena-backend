CREATE TABLE IF NOT EXISTS app_roles (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS app_permissions (
    id SERIAL PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id INTEGER NOT NULL REFERENCES app_roles(id) ON DELETE CASCADE,
    permission_id INTEGER NOT NULL REFERENCES app_permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS user_roles (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES app_roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (user_id, role_id)
);

-- Seed roles
INSERT INTO app_roles (code, description) VALUES
    ('SUPER_ADMIN', 'Full system access'),
    ('ADMIN', 'Manage tournaments, wallets, notifications and analytics'),
    ('MANAGER', 'Manage tournaments and slots'),
    ('OPERATOR', 'Operate wallets and transactions'),
    ('USER', 'Regular player access')
ON CONFLICT (code) DO NOTHING;

-- Seed permissions
INSERT INTO app_permissions (code, description) VALUES
    ('MANAGE_TOURNAMENTS', 'Create/update/delete tournaments and slots'),
    ('MANAGE_WALLET', 'Adjust wallet balances and transfers'),
    ('MANAGE_TRANSACTIONS', 'Approve or reject deposits/withdrawals'),
    ('VIEW_ANALYTICS', 'Access admin analytics dashboard'),
    ('MANAGE_NOTIFICATIONS', 'Send broadcasts and targeted pushes'),
    ('VIEW_AUDIT', 'View audit logs'),
    ('MANAGE_ROLES', 'Assign or revoke admin roles')
ON CONFLICT (code) DO NOTHING;

-- Helper function to get ids
WITH role_ids AS (
    SELECT code, id FROM app_roles
), perm_ids AS (
    SELECT code, id FROM app_permissions
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role_ids r
JOIN perm_ids p ON 1=1
WHERE (r.code = 'SUPER_ADMIN')
ON CONFLICT DO NOTHING;

-- ADMIN permissions
WITH role_ids AS (
    SELECT code, id FROM app_roles WHERE code = 'ADMIN'
), perm_ids AS (
    SELECT code, id FROM app_permissions WHERE code IN (
        'MANAGE_TOURNAMENTS',
        'MANAGE_WALLET',
        'MANAGE_TRANSACTIONS',
        'VIEW_ANALYTICS',
        'MANAGE_NOTIFICATIONS',
        'VIEW_AUDIT'
    )
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role_ids r CROSS JOIN perm_ids p
ON CONFLICT DO NOTHING;

-- MANAGER permissions
WITH role_ids AS (
    SELECT code, id FROM app_roles WHERE code = 'MANAGER'
), perm_ids AS (
    SELECT code, id FROM app_permissions WHERE code IN (
        'MANAGE_TOURNAMENTS',
        'MANAGE_NOTIFICATIONS'
    )
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role_ids r CROSS JOIN perm_ids p
ON CONFLICT DO NOTHING;

-- OPERATOR permissions
WITH role_ids AS (
    SELECT code, id FROM app_roles WHERE code = 'OPERATOR'
), perm_ids AS (
    SELECT code, id FROM app_permissions WHERE code IN (
        'MANAGE_WALLET',
        'MANAGE_TRANSACTIONS'
    )
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role_ids r CROSS JOIN perm_ids p
ON CONFLICT DO NOTHING;
