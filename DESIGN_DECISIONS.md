# Design Decisions

## Architecture

The application follows a strict layered architecture:

```
Controller  →  Service  →  Repository
   (HTTP)      (business logic)   (in-memory storage)
```

- **Controller** — only responsible for HTTP concerns: request/response mapping, status codes,
  triggering `@Valid` bean validation. Contains no business logic.
- **Service** — owns every business rule (age range, sum assured range, premium minimum, PAN
  requirement, nominee rule, reference-master membership, state transitions). Controllers never
  talk to repositories directly.
- **Repository** — pure in-memory storage, backed by `ConcurrentHashMap`. No JPA, no JDBC, no
  embedded database of any kind, per the assignment's explicit "no database" requirement.

Additional packages beyond the minimum requested structure, added deliberately:

| Package | Why it was added |
|---|---|
| `dto.request` / `dto.response` | Keeps the API contract (what a client sends/receives) decoupled from the internal domain model. Lets the PAN-masking bonus live cleanly in one place (`CustomerResponse.from()`) instead of leaking into the domain model or controller. |
| `config` | Holds `ReferenceDataInitializer` (seeds Reference Master at startup — has to live somewhere outside a controller/service) and `RequestLoggingFilter` (the request-logging bonus, which is a servlet filter, not a service). |
| `util` | `IdGenerator` (shared sequential-ID logic used by all three ID-issuing services) and `PanMasker` (a stateless helper, not business logic tied to one domain). |

`ErrorResponse` was moved into the `exception` package (alongside `GlobalExceptionHandler` and
the custom exceptions) since it exists purely to serve exception handling.

---

## Data Model & Storage

- **Customer**: `id, fullName, dateOfBirth, email, phone, pan, createdAt, updatedAt`. PAN lives
  on the customer record (not per-proposal) since a PAN belongs to a person, not a policy — this
  also makes the "PAN mandatory above a premium threshold" rule meaningful across multiple
  proposals for the same customer, not just the one being created.
- **Proposal**: `id, customerId, policyTermYears, sumAssured, annualPremium, paymentFrequency,
  nomineeName, nomineeRelationship, status (DRAFT/SUBMITTED), policyNumber, createdAt,
  submittedAt`. Deliberately modeled as a two-stage entity (`DRAFT` → `SUBMITTED`) rather than
  created-and-submitted-in-one-call, matching the assignment's explicit separate `POST /proposals`
  and `POST /proposals/{id}/submit` endpoints.
- **AuditRecord**: append-only, created exclusively as a side effect of `ProposalService
  .submitProposal()`. There is intentionally no public endpoint to create an audit record
  directly — it should only ever exist because a submission happened.
- **Reference Master**: `Map<String category, List<String> values>`, seeded once at startup via
  a `CommandLineRunner` (`ReferenceDataInitializer`) with two categories: `POLICY_TERM` and
  `PAYMENT_FREQUENCY`.

**Thread safety**: every repository is backed by `ConcurrentHashMap`. ID generation
(`IdGenerator`) uses a `ConcurrentHashMap<String, AtomicLong>` keyed by entity prefix, so
`CUST-`, `PROP-`, `POL-`, and `AUD-` sequences are each independent and gap-free even under
concurrent requests, without needing a global lock.

---

## Validation Strategy: Two Layers

1. **API-boundary validation** (Jakarta Bean Validation, `@Valid` on request DTOs) — checks
   structural correctness: required fields, email format, PAN pattern, positive numbers. This
   runs before the request ever reaches the service layer and returns `400` with a
   field-level error map.
2. **Business validation** (in `CustomerService` / `ProposalService`) — checks rules that need
   business context: age computed from date of birth, sum assured/premium ranges, PAN
   requirement (depends on the linked customer), nominee-vs-customer comparison, and
   reference-master membership. These throw `BusinessValidationException`, mapped to `400`.

Business rules are **re-validated at both creation and submission time** for proposals. This is
deliberate: reference data or the customer's PAN could change between when a proposal is
drafted and when it's submitted, and the submission step is the one that actually issues a
policy number — so it's the more consequential moment to be strict about correctness.

---

## Exception Handling

Three custom exceptions, each mapped to a distinct, meaningful status code by a single
`@RestControllerAdvice` (`GlobalExceptionHandler`):

| Exception | HTTP Status | Used for |
|---|---|---|
| `ResourceNotFoundException` | 404 | Customer/Proposal ID doesn't exist |
| `BusinessValidationException` | 400 | Any business rule violation |
| `InvalidStateException` | 409 | Re-submitting a submitted proposal; deleting a submitted proposal; deleting a customer with existing proposals |

`MethodArgumentNotValidException` (bean validation failures) is handled separately and returns a
field-level error map so API consumers know exactly which field(s) failed and why.

---

## ID Generation

Sequential, human-readable IDs (`CUST-0001`, `PROP-0001`, `POL-0001`, `AUD-0001`) rather than
UUIDs. Trade-off: sequential IDs are easier to read/debug/demo during evaluation and match the
tone of the assignment brief, at the cost of being predictable/guessable — acceptable here since
this is an in-memory evaluation exercise with no real security boundary, not a production system.

---

## Bonus Enhancements — Rationale

- **DELETE endpoints**: added for both Customer and Proposal, but guarded — a customer can't be
  deleted while proposals reference it, and a proposal can't be deleted once submitted (it has
  an audit trail by then). Both return `409 Conflict` rather than silently succeeding or
  cascading deletes, since silent cascade-delete of a submitted policy's history felt like the
  wrong default for an insurance system, even a simulated one.
- **Masked PAN**: only the PAN is masked (first 2 / last 2 characters visible, middle
  characters replaced with `*`), not the customer's name — this was a scoping decision to keep
  the bonus simple, since PAN is the field most directly tied to real, sensitive PII, while the
  name is needed as-is for the nominee-comparison business rule to be legible in responses.
- **Audit lookup by entity ID**: `GET /audits/entity/{proposalId}` — the assignment's audit
  requirement is at the proposal level, so "entity" here means proposal ID specifically, rather
  than a generic polymorphic entity-type lookup.
- **Pagination**: added to `GET /customers` and `GET /audits`, the two endpoints most likely to
  grow large during a demo/evaluation session. `GET /proposals` has no list endpoint at all in
  the spec (only `GET /proposals/{id}`), so pagination wasn't relevant there.
- **Request logging**: a single `OncePerRequestFilter` logging method, URI, response status, and
  duration for every request — lightweight, no external logging dependency needed.

---

## Framework Version: Spring Boot 4.1.0 instead of 3.x

The assignment brief specifies Spring Boot 3.x. At the time of building this project, Spring
Boot 3.5 (the final 3.x line) had just reached open-source end-of-life (June 30, 2026) and had
already been removed from Spring Initializr — it was not selectable via start.spring.io or
IntelliJ's project generator. Spring Boot 4.1.0 was used as the closest actively supported
successor.

This has minimal practical impact on the submitted code: the `jakarta.*` namespace migration
(the biggest breaking change historically associated with a Spring Boot major version) already
happened in Spring Boot 3.0, so all validation annotations, servlet APIs, and REST annotations
used here are identical to what 3.x code would look like. The main visible difference is the
modularized starter name (`spring-boot-starter-webmvc` instead of the older
`spring-boot-starter-web`).

---

## Assumptions Made

- **Nominee comparison** is done by comparing `nomineeName` (trimmed, case-insensitive) against
  the customer's `fullName`. The spec doesn't define exactly how "cannot be the same as the
  customer" should be checked, and no separate nominee-ID concept exists in the brief.
- **PAN format** is validated at the API boundary against the standard Indian PAN pattern
  (`AAAAA9999A`) when a PAN is supplied, but PAN itself is optional on customer creation — it
  only becomes mandatory contextually, when a linked proposal's annual premium exceeds Rs.
  50,000, per the stated business rule.
- **Payment frequency values** (`MONTHLY, QUARTERLY, HALF_YEARLY, ANNUALLY`) were chosen as
  reasonable defaults for the Reference Master seed data, since the assignment doesn't enumerate
  the exact set — any submitted value is validated against whatever the Reference Master
  actually contains, so this list can be changed in one place (`ReferenceDataInitializer`)
  without touching validation logic.
- **`GET /customers` and `GET /audits`** default to `page=0&size=10` when no query parameters
  are supplied.

---

## Trade-offs Considered

- **In-memory `ConcurrentHashMap` vs. a lightweight embedded structure like a synchronized
  `TreeMap`**: `ConcurrentHashMap` was chosen for better concurrent read/write throughput without
  needing manual synchronization, at the cost of no guaranteed iteration order (acceptable, since
  `findAll()` doesn't need to return entities in creation order for this use case).
- **DTOs vs. exposing domain models directly**: added a small amount of boilerplate (a `from()`
  factory method per response DTO) in exchange for keeping the domain model free of
  presentation concerns (like PAN masking) and making the API contract independent of internal
  refactors.
- **Re-validating business rules on submit vs. trusting the draft**: re-validating costs a small
  amount of duplicate work per submission, but avoids a real correctness gap — reference data or
  a customer's PAN could legitimately change between draft creation and submission.
