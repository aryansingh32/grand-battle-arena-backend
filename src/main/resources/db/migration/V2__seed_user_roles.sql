INSERT INTO user_roles (user_id, role_id)
SELECT u.id,
       CASE WHEN u.role = 'ADMIN' THEN ar_admin.id ELSE ar_user.id END
FROM users u
CROSS JOIN LATERAL (
    SELECT id FROM app_roles WHERE code = 'USER'
) ar_user
CROSS JOIN LATERAL (
    SELECT id FROM app_roles WHERE code = 'ADMIN'
) ar_admin
ON CONFLICT DO NOTHING;
