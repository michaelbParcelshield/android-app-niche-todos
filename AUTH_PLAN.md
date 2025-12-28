ABOUTME: Authentication plan for Android app and .NET API.
ABOUTME: Single source of truth for auth flow, contracts, and checklists.

# Auth Plan

## Goal and Scope
- Provide Google login for the Android app.
- Issue API-owned JWTs for access to the .NET backend.
- Keep auth simple for an MVP; add complexity only if needed.

## Non-goals
- Support for multiple clients or web apps.
- Non-Google identity providers.

## Decisions Made
- Android app + public .NET API.
- Google login is the only identity provider.
- Backend verifies Google ID tokens and issues its own JWTs.

## Auth Flow (Text)
1. Android app uses Google Sign-In and obtains a Google ID token.
2. App sends the ID token to `POST /auth/google`.
3. API verifies the ID token with Google.
4. API upserts user by Google `sub`.
5. API issues JWT access token (and optional refresh token).
6. App uses JWT for all protected API calls.

## Token Policy
- Access token TTL: 15 minutes.
- Refresh token: 30 days with rotation on use.
- Device storage: Android Keystore-backed encrypted storage (e.g., EncryptedSharedPreferences).

## Backend Contract

### POST /auth/google
Request:
```json
{ "idToken": "GOOGLE_ID_TOKEN" }
```

Success Response:
```json
{
  "accessToken": "JWT",
  "expiresInSeconds": 900,
  "refreshToken": "REFRESH_TOKEN_IF_USED",
  "user": {
    "id": "INTERNAL_USER_ID",
    "email": "user@example.com",
    "name": "User Name"
  }
}
```

Error Cases:
- 400: Missing or invalid `idToken`
- 401: Token verification failed
- 500: Unexpected error

### POST /auth/refresh (if used)
Request:
```json
{ "refreshToken": "REFRESH_TOKEN" }
```

Success Response:
```json
{
  "accessToken": "JWT",
  "expiresInSeconds": 900,
  "refreshToken": "NEW_REFRESH_TOKEN"
}
```

### POST /auth/logout (if used)
Request:
```json
{ "refreshToken": "REFRESH_TOKEN" }
```

## JWT Claims
- `sub`: internal user id
- `email`: user email
- `name`: user display name
- `roles`: optional, if needed later

## Data Model

### users
- `id` (uuid)
- `google_sub` (string, unique)
- `email` (string)
- `name` (string)
- `created_at` (timestamp)
- `updated_at` (timestamp)

### refresh_tokens (if used)
- `id` (uuid)
- `user_id` (uuid)
- `token_hash` (string)
- `expires_at` (timestamp)
- `revoked_at` (timestamp, nullable)
- `created_at` (timestamp)

## Security Requirements
- Validate Google ID token audience and issuer.
- Require HTTPS for all API traffic.
- Store refresh tokens hashed.
- Rotate refresh tokens on use.
- Short access token TTL.

## Frontend Checklist (Android)
- Configure Google Sign-In with web client ID.
- Obtain Google ID token from sign-in flow.
- Send ID token to `/auth/google`.
- Store access token securely.
- Attach JWT to `Authorization: Bearer <token>` on API calls.
- Handle token expiration and re-auth.

## Backend Checklist (.NET)
- Implement `POST /auth/google`.
- Verify Google ID token (audience + issuer).
- Upsert user by `google_sub`.
- Issue JWT and return response contract.
- Protect API endpoints with JWT auth middleware.
- (If used) implement refresh token flow and storage.

## Testing Plan (TDD Required)
- `POST /auth/google` with invalid token returns 401.
- `POST /auth/google` with valid token returns 200 and JWT.
- New user token creates user record.
- Existing user token does not create duplicate.
- Protected endpoint returns 401 without JWT.
- Protected endpoint returns 200 with valid JWT.
- (If used) refresh token rotation invalidates old token.

## Open Questions
- None right now.
