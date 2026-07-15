# Policy Proposal Processing API

A simplified insurance Policy Proposal Processing system built with Spring Boot, simulating
customer onboarding, policy proposal creation, business-rule validation, proposal submission,
and audit trail generation. Built as a 2-day backend evaluation assignment.

**All data is stored in-memory using `java.util.concurrent` Java Collections. No database is
used anywhere in this application** — all state is lost on restart, by design.

---

## Tech Stack

| Technology | Version |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.0 |
| Build Tool | Maven |
| Storage | In-memory (`ConcurrentHashMap`) — no database |
| Testing | JUnit 5 |

> **Note on framework version:** the assignment brief specifies Spring Boot 3.x. Spring Boot 3.5
> (the final 3.x line) reached open-source end-of-life on June 30, 2026 and has since been
> removed from Spring Initializr. Spring Boot 4.1.0 was used instead as the closest actively
> supported successor — see `DESIGN_DECISIONS.md` for details. The REST/validation/Jackson APIs
> used here are identical between 3.x and 4.x (the `jakarta.*` namespace migration happened back
> in Spring Boot 3.0).

---

## Project Setup

### Prerequisites
- JDK 25 (or newer) installed and configured as your project SDK
- Maven (or use the bundled `./mvnw` wrapper if present)

### Running the application

```bash
mvn spring-boot:run
```

or build a jar and run it directly:

```bash
mvn clean package
java -jar target/PolicyProvision-0.0.1-SNAPSHOT.jar
```

The application starts on **`http://localhost:8080`**.

On startup, `ReferenceDataInitializer` automatically seeds the in-memory Reference Master with:
- `POLICY_TERM` → `10, 15, 20, 25, 30`
- `PAYMENT_FREQUENCY` → `MONTHLY, QUARTERLY, HALF_YEARLY, ANNUALLY`

### Running tests

```bash
mvn test
```

---

## Business Rules Enforced

| Rule | Constraint |
|---|---|
| Customer Age | 18 to 65 years |
| Policy Term | 10 / 15 / 20 / 25 / 30 years (validated against Reference Master) |
| Sum Assured | Rs. 1,00,000 to Rs. 5,00,00,000 |
| Minimum Annual Premium | Rs. 5,000 |
| PAN | Mandatory on the customer record if annual premium exceeds Rs. 50,000 |
| Nominee | Mandatory; cannot match the customer's name |
| Payment Frequency | Must exist in the Reference Master |

Business rules are re-validated both when a proposal is **created** and again when it is
**submitted**, since reference data or the customer's PAN could change in between.

---

## API Reference

Base URL: `http://localhost:8080`

### Reference Master

| Method | Endpoint | Description |
|---|---|---|
| GET | `/reference-master/{category}` | Returns valid values for a category (`POLICY_TERM`, `PAYMENT_FREQUENCY`) |

**Sample:**
```
GET /reference-master/POLICY_TERM
```
```json
["10", "15", "20", "25", "30"]
```

### Customer

| Method | Endpoint | Description |
|---|---|---|
| POST | `/customers` | Creates a customer |
| GET | `/customers?page=0&size=10` | Paginated list of all customers |
| GET | `/customers/{id}` | Get a specific customer |
| PUT | `/customers/{id}` | Update a customer (re-validates all fields) |
| DELETE | `/customers/{id}` | Delete a customer *(bonus)* — blocked (409) if proposals reference it |

**Sample request:**
```
POST /customers
Content-Type: application/json

{
  "fullName": "Aarav Sharma",
  "dateOfBirth": "1996-04-12",
  "email": "aarav@example.com",
  "phone": "9876543210",
  "pan": "ABCDE1234F"
}
```

**Sample response (201 Created):**
```json
{
  "id": "CUST-0001",
  "fullName": "Aarav Sharma",
  "dateOfBirth": "1996-04-12",
  "age": 30,
  "email": "aarav@example.com",
  "phone": "9876543210",
  "pan": "AB******4F",
  "createdAt": "2026-07-15T10:00:00Z",
  "updatedAt": "2026-07-15T10:00:00Z"
}
```
*(PAN is masked in every response — bonus: masked PII)*

### Proposal

| Method | Endpoint | Description |
|---|---|---|
| POST | `/proposals` | Creates a proposal (status `DRAFT`) after validating business rules |
| GET | `/proposals/{id}` | Get a specific proposal |
| POST | `/proposals/{id}/submit` | Re-validates, generates policy number, sets status `SUBMITTED`, writes an audit record |
| DELETE | `/proposals/{id}` | Delete a proposal *(bonus)* — only allowed while still in `DRAFT` |

**Sample request:**
```
POST /proposals
Content-Type: application/json

{
  "customerId": "CUST-0001",
  "policyTermYears": 20,
  "sumAssured": 500000,
  "annualPremium": 12000,
  "paymentFrequency": "MONTHLY",
  "nomineeName": "Priya Sharma",
  "nomineeRelationship": "Spouse"
}
```

**Sample response (201 Created):**
```json
{
  "id": "PROP-0001",
  "customerId": "CUST-0001",
  "policyTermYears": 20,
  "sumAssured": 500000,
  "annualPremium": 12000,
  "paymentFrequency": "MONTHLY",
  "nomineeName": "Priya Sharma",
  "nomineeRelationship": "Spouse",
  "status": "DRAFT",
  "policyNumber": null,
  "createdAt": "2026-07-15T10:05:00Z",
  "submittedAt": null
}
```

**Submit:**
```
POST /proposals/PROP-0001/submit
```
```json
{
  "id": "PROP-0001",
  "status": "SUBMITTED",
  "policyNumber": "POL-0001",
  "submittedAt": "2026-07-15T10:06:00Z",
  ...
}
```

### Audit

| Method | Endpoint | Description |
|---|---|---|
| GET | `/audits?page=0&size=10` | Paginated list of all audit records |
| GET | `/audits/entity/{proposalId}` | Audit records for one proposal *(bonus: audit lookup by entity ID)* |

**Sample response:**
```json
{
  "content": [
    {
      "id": "AUD-0001",
      "proposalId": "PROP-0001",
      "customerId": "CUST-0001",
      "policyNumber": "POL-0001",
      "action": "PROPOSAL_SUBMITTED",
      "details": "Proposal PROP-0001 submitted; policy number POL-0001 generated",
      "timestamp": "2026-07-15T10:06:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

---

## Error Responses

All errors return a consistent shape:

```json
{
  "timestamp": "2026-07-15T10:10:00Z",
  "status": 400,
  "error": "Business Rule Violation",
  "message": "Sum assured must be between 100000 and 50000000",
  "path": "/proposals",
  "fieldErrors": null
}
```

| Status | Meaning |
|---|---|
| 400 | Bean validation failure or business rule violation |
| 404 | Resource not found |
| 409 | Invalid state transition (e.g. re-submitting, deleting a submitted proposal, deleting a customer with proposals) |
| 500 | Unexpected server error |

---

## End-to-End Flow (matches the assignment's expected flow)

1. `GET /reference-master/POLICY_TERM` and `GET /reference-master/PAYMENT_FREQUENCY` — check valid values
2. `POST /customers` — create a customer, then `GET /customers/{id}` to verify
3. `POST /proposals` — create a proposal for that customer
4. `POST /proposals/{id}/submit` — submit it
5. Submission validates business rules, generates a policy number, updates status, writes an audit record
6. `GET /audits` or `GET /audits/entity/{proposalId}` — verify the audit entry was created

---

## Bonus Enhancements Included

- `DELETE /customers/{id}` and `DELETE /proposals/{id}`
- Masked PAN in all customer GET responses
- `GET /audits/entity/{proposalId}` — audit lookup by entity ID
- Pagination on `GET /customers` and `GET /audits` (`?page=&size=`)
- Request logging filter (method, URI, status, duration logged for every request)

See `DESIGN_DECISIONS.md` for the reasoning behind each.
