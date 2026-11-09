# ADR 0002 - Stateless access tokens with rotating refresh tokens in an HttpOnly cookie

Status: accepted
Date: 2026-11-08

## Context

The platform needs session authentication for a browser SPA. The common tutorial
approach — issue a long-lived JWT and store it in `localStorage` — is simple but
has two well-known problems:

1. Any successful XSS on the page can read `localStorage` and exfiltrate a
   credential that stays valid until it expires.
2. A stateless JWT cannot be revoked. Logout is a client-side illusion, and a
   stolen token keeps working.

## Decision

Two distinct credentials with different lifetimes, storage and trust levels.

### Access token

- JWT, HMAC-SHA256, signed with `MEMORA_SECURITY_JWT_SECRET` (env var, minimum
  32 bytes, validated at startup).
- Short lived (15 minutes by default).
- Claims: `sub` (user id), `email`, `roles`, `iat`, `exp`, `jti`.
- Sent in the `Authorization: Bearer` header.
- **Held in memory only on the client** (an Angular signal). Never written to
  `localStorage` or `sessionStorage`, so XSS has no persistent credential to
  steal and a page reload cannot leak it.

### Refresh token

- Not a JWT. A 256-bit value from `SecureRandom`, base64url encoded.
- Stored in the database **only as a SHA-256 hash**, so a database dump does not
  yield usable credentials.
- Delivered as an `HttpOnly`, `SameSite=Lax`, path-scoped (`/api/auth`) cookie.
  JavaScript cannot read it; the browser only sends it to the auth endpoints.
- **Rotated on every use**: refreshing consumes the presented token and issues a
  new one. A replayed token is detected as already-consumed, which revokes the
  whole token family for that user — the standard reuse-detection response to a
  stolen refresh token.
- Revocable, because it is a database row. Logout is real.

`Secure` is driven by `MEMORA_SECURITY_COOKIE_SECURE` so local HTTP development
works while production over HTTPS gets the flag.

### Session restoration

On bootstrap the SPA calls `POST /api/auth/refresh`. If the cookie is present and
valid, the user gets a fresh access token and stays logged in. If not, the app
renders as anonymous. This is what makes an in-memory access token practical.

### Development: same-origin via dev-server proxy

The Angular dev server proxies `/api` to `http://localhost:8080`. The browser
therefore sees a single origin, which means:

- no CORS preflight in development,
- no `SameSite`/third-party-cookie friction,
- the production topology (SPA behind a reverse proxy that also serves `/api`) is
  exercised from day one.

CORS stays configurable through `MEMORA_SECURITY_ALLOWED_ORIGINS` for deployments
that genuinely split origins, and is disabled when that property is empty.

## Consequences

- Positive: no persistent credential reachable from JavaScript; revocable
  sessions; refresh-token theft is detectable.
- Positive: the API remains stateless for normal requests — only `/api/auth`
  touches token storage.
- Negative: more moving parts than a single JWT, and a brief anonymous flash on
  first paint while `/api/auth/refresh` is in flight. Handled with an app-level
  initialiser so guarded routes do not resolve before the attempt completes.
- Negative: refresh requires a database round trip. Acceptable: it happens once
  per access-token lifetime, not per request.
