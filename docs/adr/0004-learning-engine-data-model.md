# ADR 0004 - Learning engine data model and FSRS scheduling

Status: accepted (design), implementation scheduled for stage 2
Date: 2026-11-08

## Context

The spaced-repetition engine is the product's main technical differentiator. The
modelling question is how to support many exercise formats (vocabulary recall,
cloze, translation, grammar correction, reading comprehension, listening) without
either a table per format or a schema so generic that queries become unreadable.

This ADR records the agreed design so that stage 1 can prepare the module
boundaries without the schema being improvised later.

## Decision

### Content vs. scheduling are different concerns

Two independent axes, deliberately not merged:

- **Content** — what is being learned. Shared across all users, authorable and
  reusable: `learning_item`, `exercise`.
- **Scheduling state** — what *this* user's memory of an item looks like. One row
  per (user, item): `review_card`, plus an append-only `review_history`.

Keeping them apart means content can be edited or re-authored without destroying
anyone's memory state, and a user's scheduling state can be reset without
touching content.

### Hybrid typing for exercises: typed columns + a validated JSON payload

`exercise` carries the columns every format shares as real, indexable columns
(`type`, `learning_item_id`, `prompt`, `cefr_level`, `difficulty`), and the
format-specific part in a single `jsonb` `payload` column (distractors for
multiple choice, blank positions for cloze, audio reference for listening, and so
on).

Rationale: the queries the engine actually runs — "due cards for this user in this
topic, interleaved across types" — filter exclusively on the typed columns, so
they stay plain indexed SQL. The polymorphic part is only read *after* an exercise
has been selected, which is precisely the access pattern a document column suits.
A table per type would require a union for every scheduling query; a fully
generic entity-attribute-value design would make all of them unreadable.

`payload` is never trusted raw: each `ExerciseType` has a corresponding DTO and
is validated on write, so the JSON is schema-checked by the application even
though the database only sees `jsonb`.

### FSRS, implemented to specification

Scheduling uses FSRS (Free Spaced Repetition Scheduler). The per-card state it
requires is persisted explicitly rather than derived:

| Field | Purpose |
|---|---|
| `stability` | FSRS memory stability (days) |
| `difficulty` | FSRS item difficulty |
| `state` | `NEW`, `LEARNING`, `REVIEW`, `RELEARNING` |
| `due_at` | next scheduled review |
| `last_reviewed_at` | elapsed-time input for the next calculation |
| `reps`, `lapses` | counters |

`review_history` records every attempt: timestamp, the four-way grade (`AGAIN`,
`HARD`, `GOOD`, `EASY`), elapsed time since the previous review, response
duration, and the resulting stability/difficulty. Two reasons this is append-only
and complete: the FSRS parameters can be re-optimised per user from the real
review log, and any scheduling bug can be replayed from history.

Explicit non-goal: no simplified interval heuristic will be labelled "FSRS". The
implementation follows the published algorithm, with unit tests asserting the
computed stability, difficulty and intervals against reference values from the
specification. Those tests are the acceptance criteria for the module.

### Interleaving

Because scheduling state is per (user, item) and exercise type is a typed column,
an interleaved session is a query concern, not a schema concern: select due cards
across topics and types, then order them so consecutive items differ in topic and
format. No structural change is needed to enable it.

## Consequences

- Positive: new exercise formats need a new `ExerciseType` plus a payload DTO —
  no migration, no change to scheduling queries.
- Positive: scheduling queries stay simple indexed SQL on `(user_id, due_at)`.
- Positive: FSRS parameter re-optimisation per user is possible from day one
  because the review log is complete.
- Negative: `jsonb` payloads are validated by the application, not the database.
  Enforcement depends on all writes going through the service layer.
- Negative: FSRS is materially more complex than SM-2 and must be test-driven.
  Accepted — it is the feature, not an incidental detail.
