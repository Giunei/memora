# ADR 0001 - Modular monolith organised by business module

Status: accepted
Date: 2026-11-08

## Context

Memora is a learning platform whose long-term scope includes several specialised
learning modules (languages first, then exam preparation, programming, science,
general knowledge) sharing a single learning engine. The team is small and the
product has no scaling pressure yet, but the domain is broad and will grow.

Microservices were explicitly ruled out for this stage. The risk we need to
manage is therefore not deployment topology, it is **domain entanglement**: a
single `com.memora.service` package that slowly turns into a ball of mud.

## Decision

A single Spring Boot application, organised **by business module** rather than by
technical layer. Each module is a top-level package under `com.memora` and
contains at most three sub-packages:

```
com.memora.<module>
├── web/        REST controllers
│   └── dto/    request and response records - the HTTP contract
├── service/    use cases, business rules, transaction boundaries
└── domain/     JPA entities, enums, Spring Data repositories
```

JPA entities are never returned from a controller; every endpoint speaks in
`web/dto` records.

Modules in this stage: `shared`, `users`, `authentication`, `languages`,
`gamification`, `analytics`, `ai`.

Modules planned for the next stages: `learning`, `exercises`, `reviews`.

### Why three sub-packages and not a full hexagonal layout

A textbook ports-and-adapters layout (`api` / `application` / `domain` /
`infrastructure`) multiplied by ten business modules produces ~40 packages before
a single feature exists. That is the "excessively technical and fragmented"
structure we want to avoid. Three sub-packages are enough to keep the dependency
rule meaningful:

| Package | May depend on |
|---|---|
| `web` | its own `service`, `domain` and `web.dto`; other modules' `web.dto` |
| `web.dto` | its own `domain` (for mapping factories) |
| `service` | its own `domain` and `web.dto`; other modules' `service`, `domain`, `web.dto` |
| `domain` | other modules' `domain` |

The rule that actually matters is the one at the top: **nothing depends on a
module's controllers.** Contract records are deliberately shareable — a
`RegisterRequest` is both the HTTP body and the use-case input, and splitting it
into two identical records would be ceremony, not design.

Dependency inversion is applied selectively, where it buys something concrete.
The `ai` module is the one place where a port/adapter split is justified today,
because the provider must be swappable and the whole product must run without it
(see ADR 0003).

### Cross-module references

Modules may depend on another module's `service` and `domain` packages, but never
on its `web` package. Allowed direction in this stage:

```
authentication -> users, gamification
analytics      -> users, gamification, languages
languages      -> users
gamification   -> users
```

JPA associations **are** used across module boundaries when the relationship is a
genuine aggregate reference (`UserLanguage -> User`, `UserProgress -> User`).
The alternative — storing only a `userId` with no foreign key — would trade real
referential integrity and efficient joins for a boundary that nothing enforces at
runtime anyway. Given the explicit requirement to avoid a modelling so generic
that it hurts queries, integrity and join quality win.

## Consequences

- Positive: features are read and changed in one place; module boundaries are
  visible in the package tree; extracting a module into its own service later is
  mechanical rather than archaeological.
- Positive: no cyclic module dependencies in the current graph.
- Negative: the boundary is a convention, not a compiler-enforced rule. The
  mitigation is an ArchUnit test suite asserting the dependency rules, planned
  for the next stage once more than one module has non-trivial internals.
- Negative: cross-module JPA associations mean a future module extraction has to
  replace those associations with identifiers. Accepted knowingly.
