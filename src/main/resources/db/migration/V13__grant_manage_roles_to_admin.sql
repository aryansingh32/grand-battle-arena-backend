-- Ensure ADMIN can access role/user-management endpoints protected by PERM_MANAGE_ROLES.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
JOIN app_permissions p ON p.code = 'MANAGE_ROLES'
WHERE r.code = 'ADMIN'
ON CONFLICT DO NOTHING;
