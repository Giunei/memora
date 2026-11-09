# ADR 0003 - AI is an optional, server-side, swappable capability

Status: accepted
Date: 2026-11-08

## Context

Gemini is the initial AI provider. Three constraints shape the design:

1. **Cost and quota.** The free tier is rate-limited. The product must work in
   full — studying, reviewing, progressing — with AI switched off entirely.
2. **Secrecy.** The API key must never reach the browser or a committed file.
3. **Replaceability.** A different provider must be adoptable without touching
   the rest of the application.

## Decision

### Server-side only

All provider calls originate from the Java backend. The key is read from the
`GEMINI_API_KEY` environment variable, bound into `AiProperties`, and never
serialised into any response. The frontend only ever learns *whether* AI is
available, through `GET /api/ai/status`.

### AI is a feature flag, not a dependency

`memora.ai.enabled` defaults to `false`. Every AI entry point is a request-time,
user-initiated action — content generation on demand. Nothing in a study or
review flow calls a provider, so an exhausted quota or an outage degrades one
optional button, never the learning engine.

When AI is disabled or misconfigured, `AiUnavailableException` maps to
`503 Service Unavailable` with a stable error code the frontend can branch on.

### Port and adapter

Generation capabilities are declared as an interface in `com.memora.ai`, with the
provider implementation behind it. Swapping providers means adding one adapter
and one conditional bean.

**Scope note for this stage:** only the configuration, availability contract and
status endpoint are implemented. The Gemini adapter and the generation methods
land with the first real generation feature (flashcards from text), so the port
is introduced together with a working implementation rather than as an empty
interface with a stub behind it.

### Planned guard rails for the adapter

- Structured output enforced via Gemini's `responseMimeType: application/json`
  plus an explicit `responseSchema`, deserialised into backend DTOs.
- Bean Validation applied to the deserialised payload — a syntactically valid
  response can still be semantically wrong, and unvalidated model output must
  never reach the database.
- Request timeout from `memora.ai.request-timeout`.
- Per-user daily quota from `memora.ai.daily-request-limit-per-user`, enforced
  server-side.

## Consequences

- Positive: zero AI spend by default; no key exposure; the core product is never
  blocked by a third party.
- Positive: the frontend can hide or disable AI affordances based on real server
  state instead of guessing.
- Negative: AI-generated content needs a review step before it becomes study
  material, which is additional UX surface. This is the right trade-off — model
  output is a draft, not a fact.
