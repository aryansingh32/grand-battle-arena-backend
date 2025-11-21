# Grand Battle Arena – API & RBAC Reference

Authoritative catalog of the Spring Boot backend endpoints, DTO payloads, and role/permission wiring so Flutter and the React admin panel stay in sync.

---

## 1. RBAC Overview

| Role Code | Description | Granted Permissions |
|-----------|-------------|----------------------|
| `SUPER_ADMIN` | Full platform access; inherits every other role. | All `PERM_*` plus `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_OPERATOR`, `ROLE_USER`. |
| `ADMIN` | Day‑to‑day operations lead. | `PERM_MANAGE_TOURNAMENTS`, `PERM_MANAGE_WALLET`, `PERM_MANAGE_TRANSACTIONS`, `PERM_VIEW_ANALYTICS`, `PERM_MANAGE_NOTIFICATIONS`, `PERM_VIEW_AUDIT`, plus role hierarchy down to `ROLE_USER`. |
| `MANAGER` | Tournament programming + comms. | `PERM_MANAGE_TOURNAMENTS`, `PERM_MANAGE_NOTIFICATIONS`, inherits `ROLE_USER`. |
| `OPERATOR` | Finance ops (wallet + payouts). | `PERM_MANAGE_WALLET`, `PERM_MANAGE_TRANSACTIONS`, inherits `ROLE_USER`. |
| `USER` | Regular player scope. | Authenticated access to self-service APIs. |
| `BANNED` | Special Spring Security role for suspended users; locked to `/api/banned/**`. | Access only to appeal endpoints. |

**Permission codes (database `app_permissions.code`):**

- `PERM_MANAGE_TOURNAMENTS`
- `PERM_MANAGE_WALLET`
- `PERM_MANAGE_TRANSACTIONS`
- `PERM_VIEW_ANALYTICS`
- `PERM_MANAGE_NOTIFICATIONS`
- `PERM_VIEW_AUDIT`
- `PERM_MANAGE_ROLES`

### Changing / Inspecting Roles

All RBAC management endpoints live under `/api/admin/roles` and require `PERM_MANAGE_ROLES` (or `ROLE_SUPER_ADMIN`).

| Action | Method & Path | Request Body | Response |
|--------|---------------|--------------|----------|
| List all roles & descriptions | `GET /api/admin/roles` | — | `[{ code, description }]` |
| Read a user’s current roles | `GET /api/admin/roles/users/{firebaseUID}` | — | `{ firebaseUID, roles: ["ADMIN", ...] }` |
| Replace a user’s roles | `PUT /api/admin/roles/users/{firebaseUID}` | `{ "roles": ["ADMIN","OPERATOR"] }` | Echoes updated role list |

> The `RbacService` automatically injects `ROLE_USER` when no assignment exists and applies the role hierarchy shown above.

---

## 2. API Catalog

Endpoints are grouped by consumer. Unless stated otherwise, requests require a valid Firebase Bearer token supplied via the `Authorization: Bearer <idToken>` header.

### 2.1 Public + Flutter Mobile APIs

#### Platform & App Configuration

- `GET /api/app/version` → `{ minSupported, latest, playStoreUrl }` - App version information (public).
- `GET /api/filters` → `{ games: [...], teamSizes: [...], maps: [...], timeSlots: [...] }` - Filter metadata (public).
- `GET /api/public/stats` → platform statistics.
- `GET /api/public/info` → platform information and features.
- `GET /api/public/registration/requirements` → registration requirements.
- `GET /api/public/health` → health check endpoint.

#### Tournaments

- `GET /api/public/tournaments` → `List<TournamentsDTO>` (upcoming list, public).
- `GET /api/public/tournaments/{id}` → limited tournament snapshot (public).
- `GET /api/public/tournaments/stats` → aggregated public metrics.
- `GET /api/public/tournaments/categories` → static metadata for filter chips.
- `GET /api/tournaments` → full authenticated catalog (supports filters client-side).
- `GET /api/tournaments/{id}` → complete `TournamentsDTO` used by detail view.
- `GET /api/tournaments/status/{status}` → list by `UPCOMING|ONGOING|COMPLETED|CANCELLED`.
- `GET /api/tournaments/{id}/credentials` → returns `{ gameId, gamePassword }` (used after booking).

#### Banners (Dynamic)

- `GET /api/banners` → `List<BannerDTO>` - Get active banners (public, filtered by date and active status).

#### Slots & Booking

- `POST /api/slots/book` → body `SlotsDTO` (`tournamentId`, `slotNumber`, `playerName`); books a specific slot.
- `POST /api/slots/book-team` → `TeamBookingRequestDTO` (`tournamentId`, `players[]`).
- `POST /api/slots/book-next/{tournamentId}` → `{ "playerName": "..." }`; auto-picks next slot.
- `GET /api/slots/{tournamentId}/summary` → `{ totalSlots, bookedCount, availableCount, fillRate, slots: [...] }` - Always returns slots array (never null).
- `GET /api/slots/{tournamentId}` → `List<SlotsDTO>` for grids.
- `GET /api/slots/my-bookings` → authenticated player’s current slots.
- `DELETE /api/slots/{slotId}/cancel` → user-initiated cancellation (refund w/ ledger entry).

#### Wallet & Transactions

- `GET /api/wallets/{firebaseUID}` + `/{firebaseUID}/ledger` → self or admin view.
- `POST /api/wallets/{firebaseUID}/add` / `/deduct` / `/balance` / `/transfer` → **admin only** (see §2.2).
- `GET /api/transactions/history` → player history (`TransactionTableDTO` list).
- `POST /api/transactions/deposit` → `DepositRequestDTO { transactionUID, amount }`.
- `POST /api/transactions/withdraw` → `{ "amount": 500 }`.
- `DELETE /api/transactions/{id}/cancel` → cancel pending request owned by caller.

#### Notifications & Device Tokens

- `POST /api/users/device-token` → `{ "deviceToken": "fcm..." }`.
- `GET /api/notifications/my` → `List<NotificationsDTO>`.
- `PATCH /api/notifications/{notificationId}/read` → marks as read.
- `GET /api/notifications/stats` → per-user notification counters.

#### Payments (Recharge QR)

- `GET /api/v1/payments/qr/{amount}` or `POST /api/v1/payments/qr` → `PaymentResponseDTO`.
- `GET /api/v1/payments/amounts` → `[ { amount, coin, upiIdQrLink, isActive } ]`.
- `GET /api/v1/payments/health` → service heartbeat.

#### User Profile

- `POST /api/users/me` → upserts current user (`UserDTO`) after Firebase login.
- `GET /api/users/me` → returns enriched profile (role, status, timestamps).

#### Banned User Surface

> When Spring assigns `ROLE_BANNED`, only the following endpoints are reachable.

- `GET /api/banned/status`
- `POST /api/banned/appeal`
- `GET /api/banned/appeal/guidelines`

### 2.2 Admin Panel APIs (Protected)

Permissions listed per endpoint. Multiple permissions may satisfy the predicate because of role hierarchy.

#### Dashboard & Analytics (`PERM_VIEW_ANALYTICS`)

- `GET /api/admin/dashboard` – composite stats (users/tournaments/wallets/transactions/notifications).
- `GET /api/admin/system/health`
- `GET /api/admin/finance/overview?startDate&endDate`
- `GET /api/tournaments/stats`
- `GET /api/wallets/stats`
- `GET /api/transactions/stats`
- `GET /api/notifications/stats`

#### User Management (`PERM_MANAGE_ROLES`)

- `GET /api/admin/users?page=&size=&role=&status=&search=`
- `PUT /api/admin/users/bulk-status`
- `GET /api/admin/users/{firebaseUID}/activity`
- `GET /api/users` – raw list
- `GET /api/users/{firebaseUID}`, `/role/{role}`, `/status/{status}`, `/search`
- `PUT /api/users/{firebaseUID}/role`
- `PUT /api/users/{firebaseUID}/status`
- `GET /api/users/stats`

#### Tournament Ops (`PERM_MANAGE_TOURNAMENTS`)

- `POST /api/tournaments`
- `PUT /api/tournaments/{id}/status`
- `PUT /api/tournaments/{id}/start-time?startTime=ISO`
- `PUT /api/tournaments/{id}/game-credentials`
- `DELETE /api/tournaments/{id}`
- `POST /api/admin/tournaments/bulk-status`
- `GET /api/admin/tournaments/{id}/analytics`
- `POST /api/slots/tournaments/{tournamentId}/generate`
- `DELETE /api/slots/{slotId}/admin-cancel`

#### Wallet & Ledger (`PERM_MANAGE_WALLET`)

- `GET /api/wallets`
- `POST /api/wallets/{firebaseUID}`
- `GET /api/wallets/{firebaseUID}` (admin can view any user)
- `GET /api/wallets/{firebaseUID}/ledger`
- `POST /api/wallets/{firebaseUID}/add`
- `POST /api/wallets/{firebaseUID}/deduct`
- `PUT /api/wallets/{firebaseUID}/balance`
- `POST /api/wallets/transfer`
- `POST /api/admin/wallets/emergency-adjustment`

#### Transactions (`PERM_MANAGE_TRANSACTIONS`)

- `GET /api/transactions`
- `GET /api/transactions/pending`
- `GET /api/transactions/{id}`
- `PUT /api/transactions/{id}/approve`
- `PUT /api/transactions/{id}/reject`

#### Notifications (`PERM_MANAGE_NOTIFICATIONS`)

- `POST /api/notifications` – broadcast by audience.
- `POST /api/notifications/user/{firebaseUID}`
- `POST /api/notifications/tournament/{id}/credentials`
- `POST /api/notifications/tournament/{id}/reminder`
- `POST /api/notifications/tournament/{id}/result`
- `POST /api/notifications/wallet-transaction`
- `POST /api/notifications/reward-distribution`
- `POST /api/admin/notifications/emergency-broadcast`
- `POST /api/admin/notifications/maintenance`
- `GET /api/notifications/admin`
- `DELETE /api/notifications/{notificationId}`

#### Banners Management (`PERM_MANAGE_TOURNAMENTS`)

- `GET /api/banners/admin` → `List<BannerDTO>` - Get all banners (admin view).
- `GET /api/banners/{id}` → `BannerDTO` - Get banner by ID.
- `POST /api/banners` → `BannerDTO` - Create new banner.
- `PUT /api/banners/{id}` → `BannerDTO` - Update banner.
- `DELETE /api/banners/{id}` → `{ message }` - Delete banner.
- `PATCH /api/banners/{id}/toggle` → `BannerDTO` - Toggle banner active status.

#### Payments Configuration (`PERM_MANAGE_WALLET`)

- `POST /api/v1/payments/admin/create` → `AdminPaymentResponseDTO` - Create new QR code.
- `PUT /api/v1/payments/admin/{amount}` → `AdminPaymentResponseDTO` - Update existing QR code.
- `PATCH /api/v1/payments/admin/{amount}/toggle` → `AdminPaymentResponseDTO` - Toggle QR code status.
- `DELETE /api/v1/payments/admin/{amount}` → `{ message }` - Delete QR code.
- `POST /api/v1/payments/admin/all` → `List<AdminPaymentResponseDTO>` - Get all QR codes.

#### Audit, Reports, Settings (`PERM_VIEW_AUDIT` / `PERM_VIEW_ANALYTICS`)

- `GET /api/admin/audit/logs`
- `POST /api/admin/reports/generate`
- `GET /api/admin/settings`
- `PUT /api/admin/settings`
- `POST /api/admin/export`
- `POST /api/admin/cleanup`

---

## 3. DTO Reference

> DTOs live under `src/main/java/com/esport/EsportTournament/dto`. Below are the fields the clients should serialize/deserialize.

### UserDTO

```json
{
  "firebaseUserUID": "string",
  "userName": "string",
  "email": "string",
  "role": "USER|ADMIN",
  "status": "ACTIVE|INACTIVE|BANNED",
  "createdAt": "2025-11-20T20:15:00",
  "avatarUrl": "...",        // optional (future)
  "deviceToken": "..."       // returned when stored
}
```

### TournamentsDTO

```json
{
  "id": 42,
  "title": "Free Fire Showdown",
  "name": "Free Fire Showdown",
  "game": "Free Fire",
  "map": "Bermuda",
  "imageLink": "https://cdn...",
  "prizePool": 10000,
  "entryFee": 50,
  "maxPlayers": 48,
  "teamSize": "SOLO|DUO|SQUAD|HEXA",
  "status": "UPCOMING|ONGOING|COMPLETED|CANCELLED",
  "startTime": "2025-11-22T18:30:00",
  "rules": ["Rule 1", "Rule 2"],
  "registeredPlayers": 12,
  "participants": [
    { "playerName": "Player1", "slotNumber": 7, "userId": "firebaseUID1" },
    { "playerName": "Player2", "slotNumber": 8, "userId": "firebaseUID2" }
  ],
  "scoreboard": [
    { "playerName": "Winner", "teamName": "Team Alpha", "kills": 15, "coinsEarned": 500, "placement": 1 }
  ],
  "perKillReward": 5,
  "firstPrize": 5000,
  "secondPrize": 3000,
  "thirdPrize": 2000,
  "gameId": "ROOM123",
  "gamePassword": "pass123"
}
```

### SlotsDTO & TeamBookingRequestDTO

```json
// SlotsDTO
{
  "id": 1001,
  "tournamentId": 42,
  "slotNumber": 12,
  "firebaseUserUID": "playerUid",
  "playerName": "IGN",
  "status": "AVAILABLE|BOOKED|CANCELLED",
  "bookedAt": "2025-11-20T21:05:00"
}

// TeamBookingRequestDTO
{
  "tournamentId": 42,
  "players": [
    { "slotNumber": 1, "playerName": "Leader" },
    { "slotNumber": 2, "playerName": "Mate" }
  ]
}
```

### WalletDTO & WalletLedgerDTO

```json
{
  "id": 55,
  "firebaseUserUID": "playerUid",
  "coins": 1234,
  "lastUpdated": "2025-11-20T21:10:11"
}
```

```json
{
  "id": 8801,
  "walletId": 55,
  "userId": "playerUid",
  "direction": "CREDIT|DEBIT",
  "amount": 200,
  "balanceAfter": 1434,
  "referenceType": "BOOKING|REFUND|ADMIN_ADJUSTMENT|TRANSACTION",
  "referenceId": "txn_abc",
  "metadata": { "tournamentId": 42 },
  "createdBy": "firebaseUID or system",
  "createdAt": "2025-11-20T21:10:11"
}
```

### TransactionTableDTO & DepositRequestDTO

```json
// TransactionTableDTO
{
  "id": "txn_123",
  "firebaseUserUID": "playerUid",
  "transactionUID": "UPI Ref",
  "amount": 500,
  "type": "DEPOSIT|WITHDRAWAL|CUSTOM",
  "status": "PENDING|APPROVED|REJECTED|CANCELLED",
  "createdAt": "2025-11-20T21:00:00"
}

// DepositRequestDTO
{
  "transactionUID": "C123UPI",
  "amount": 500
}
```

### NotificationsDTO

```json
{
  "id": 777,
  "firebaseUserUID": "playerUid",
  "title": "Tournament Reminder",
  "message": "Match starts in 15 mins",
  "type": "SYSTEM|TOURNAMENT|WALLET",
  "data": { "tournamentId": 42 },
  "isRead": false,
  "createdAt": "2025-11-20T21:15:00"
}
```

### PaymentRequest / Response DTOs

```json
// PaymentRequestDTO (admin create/update)
{
  "amount": 100,
  "coin": 120,
  "upiIdQrLink": "https://cdn/qr.png",
  "adminPassword": "********"
}

// PaymentResponseDTO (user)
{
  "amount": 100,
  "coin": 120,
  "upiIdQrLink": "https://cdn/qr.png",
  "isActive": true,
  "notes": "Send ref id via Telegram"
}

// AvailableAmountDTO
{
  "amount": 100,
  "coin": 120,
  "upiIdQrLink": "https://cdn/qr.png",
  "isActive": true
}
```

### BannerDTO

```json
{
  "id": 1,
  "imageUrl": "https://cdn.example.com/banner1.jpg",
  "title": "Free Fire Tournament",
  "description": "Win big prizes!",
  "actionUrl": "https://youtube.com/...",
  "type": "IMAGE|VIDEO|AD",
  "order": 1,
  "isActive": true,
  "startDate": "2025-11-01T00:00:00Z",
  "endDate": "2025-11-30T00:00:00Z",
  "createdAt": "2025-11-01T00:00:00Z",
  "updatedAt": "2025-11-01T00:00:00Z"
}
```

---

## 4. Integration Tips

1. **Authentication** – Every protected call must supply the Firebase ID token. When testing via Postman, hit `/api/users/me` first to ensure the token is valid.
2. **Role-sensitive UI** – Admin panel should read `/api/admin/roles/users/{me}` once at login and drive feature flags off the returned list.
3. **DTO drift** – Keep Flutter/admin TypeScript models aligned with the JSON blueprints above; mismatched casing (e.g., `bookedAt` vs `booked_at`) will break serialization.
4. **Error handling** – Central exceptions return `{ "message": "...", "errorCode": "...", "timestamp": "..." }`; surface `message` directly to operators where safe.

This document should be versioned with backend changes; whenever you add a controller method, update the corresponding section so mobile + admin teams know how to consume it.

