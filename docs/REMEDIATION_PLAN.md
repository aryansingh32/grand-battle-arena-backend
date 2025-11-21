# Grand Battle Arena Backend Remediation Plan

## 1. RBAC & Security
- Introduce Flyway migrations for `app_roles`, `app_permissions`, `role_permissions`, `user_roles` tables.
- Seed roles: SUPER_ADMIN, ADMIN, MANAGER, OPERATOR, USER, along with granular permissions (`TOURNAMENT_WRITE`, `WALLET_CREDIT`, `WALLET_DEBIT`, `ANALYTICS_READ`, `AUDIT_VIEW`, `NOTIFICATION_SEND`).
- Replace enum-based role checks with authority-based `@PreAuthorize` expressions backed by DB roles.
- Provide `/api/admin/roles` endpoints for assigning/removing roles per user.

## 2. Wallet Ledger & Transactions
- Add `wallet_ledger` table capturing direction, amount, balance_after, reference metadata, and actor info.
- Update wallet/transaction services to write ledger entries, enforce idempotency, and expose `/api/wallets/{uid}/ledger`.
- Use pessimistic locks on wallet rows when mutating balances.

## 3. Booking & Slot Integrity
- Enforce team-size routing (solo via `/book`, multi via `/book-team`) and guard slot regeneration against existing bookings (auto refund + audit).
- Fix `/api/slots/{id}/summary` to return both counts and `slots` array with casing expected by Flutter/admin.
- Ensure booking + wallet deduction is atomic with ledger/audit logging.

## 4. Feature Parity Endpoints
- Implement `/api/app/version`, `/api/filters`, `/api/banners` (public + admin CRUD) per frontend requirements.
- Extend tournament DTO/model to include `rules`, `participants`, `scoreboard`, prize tiers, per-kill reward, and implement scorecard endpoints.
- Add `/api/admin/analytics/...` endpoints for dashboard, tournament analytics, finance, and expose data required by admin panel/Flutter.
- Expand notification DTOs (`title`, `type`, `data`, `isRead`) and allow `/api/notifications/stats` for authenticated users.

## 5. Audit & Observability
- Replace stubbed audit endpoint with real pagination/filtering powered by `audit_logs` + new `admin_audit_log` table (before/after JSON, request_id, ip, actor_role).
- Introduce structured logging (JSON), request correlation, Micrometer timers, secured Actuator/Prometheus endpoints.

## 6. Redis & Caching
- Use Redis for banner/filter caches, tournament list snapshots, reminder dedup keys, analytics caching; document TTLs/eviction plan for Railway limits.

## 7. Deployment & Tooling
- Add Flyway configuration/migrations, `.env.example`, `README_DEPLOY.md`, `Procfile`, GitHub Actions (build/test/flyway/deploy), and `application-railway.yml` profile.
- Remove committed secrets; switch Firebase/admin creds to env vars or Railway variables.

## 8. Tests & QA
- Add unit/integration tests for RBAC, wallet ledger invariants, booking concurrency, scorecard endpoints, banners/filters, analytics controllers.
- Publish verification checklist + Postman/OpenAPI updates for Flutter/admin teams.
