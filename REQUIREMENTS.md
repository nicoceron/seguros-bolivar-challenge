# Seguros Bolivar Technical Assessment - Requirement Register

This file is the durable source of truth for the assessment. Requirement identifiers are
referenced from code, tests, documentation, and the final traceability matrix.

## Submission constraints

| ID | Requirement | Acceptance evidence |
|---|---|---|
| SUB-01 | Answer all five modules in one document or package. | `docs/technical-assessment.tex` and generated PDF cover Modules 1-5. |
| SUB-02 | Submit the practical solution as a GitHub link, not a Drive folder or code attachment. | Repository URL appears in the README and final PDF. |
| SUB-03 | Written answers should be delivered as PDF; ZIP is accepted when source is included. | `Nicolas_Ceron_Prueba_Tecnica.pdf` and optional `.zip` are generated under `output/`. |
| SUB-04 | Final files must use `Nombre_Apellido_Prueba_Tecnica.pdf` or `.zip`. | Stable output names use `Nicolas_Ceron_Prueba_Tecnica.*`. |
| SUB-05 | Do not submit loose images, password-protected files, RAR/7z archives, empty files, or incomplete drafts. | Build scripts produce an unencrypted PDF and standards-compliant ZIP only. |
| SUB-06 | Deadline: Thursday, July 30, 2026 before 12:00 (America/Bogota). | Delivery checklist in the final report calls out the deadline. |
| SUB-07 | Responses and implementation should be in English unless the test requires Spanish. | Prose and code use English; mandated routes and domain values retain Spanish. |

## Module 1 - System design

### Domain and capabilities

| ID | Requirement |
|---|---|
| SD-01 | Model rental policies of type `INDIVIDUAL` and `COLECTIVA`. |
| SD-02 | Collective policies are purchased by real-estate agencies or co-property administrations, insure tenants, and benefit landlords. |
| SD-03 | In an individual policy, the policyholder and insured party are the tenant, and the beneficiary is the landlord. |
| SD-04 | A collective policy has one or many risks; an individual policy has exactly one risk. |
| SD-05 | Every policy has an effective period, monthly rent, and premium. |
| SD-06 | Premium equals monthly rent multiplied by the number of months in the effective period. |
| SD-07 | A policy may renew for the same duration as its initial effective period. |
| SD-08 | Renewal adjusts monthly rent using the IPC (Colombian consumer price index). |
| SD-09 | The platform must create, retrieve, and modify individual and collective policies. |
| SD-10 | Support rule-based automatic renewal. |
| SD-11 | Emit email/SMS notification events on creation and renewal. |
| SD-12 | Integrate with the legacy transactional insurance CORE. |
| SD-13 | Provide 24/7 availability and resilience. |
| SD-14 | Collective-policy operations add risks and cancel risks. |
| SD-15 | Individual-policy operations support the frontend's required journeys. |
| SD-16 | Every action that changes a policy or risk state must call an agnostic edit service exposed through a WebLogic middleware layer that keeps CORE current. |

### Required design answers

| ID | Requirement |
|---|---|
| SD-17 | Present a high-level architecture. |
| SD-18 | Select exactly three architectural patterns and justify each selection. |
| SD-19 | Describe the main data model without detailed SQL. |
| SD-20 | Explain scalability. |
| SD-21 | Explain logs and observability. |
| SD-22 | Explain fault tolerance. |
| SD-23 | Explain API versioning. |
| SD-24 | Diagram Policy Service, Risk Service, Notification Service, CORE integration adapter, database, and API Gateway. |

## Module 2 - Hands-on API

### Mandatory endpoints

| ID | Requirement | Expected behavior |
|---|---|---|
| API-01 | `GET /polizas` | List policies filtered by `tipo` and `estado`. |
| API-02 | `GET /polizas/{id}/riesgos` | List the policy's risks. |
| API-03 | `POST /polizas/{id}/renovar` | Increase rent and premium by IPC and set status to `RENOVADA`. |
| API-04 | `POST /polizas/{id}/cancelar` | Cancel the policy. |
| API-05 | `POST /polizas/{id}/riesgos` | Add a risk only when the policy is `COLECTIVA`. |
| API-06 | `POST /riesgos/{id}/cancelar` | Cancel a risk. |
| API-07 | `POST /core-mock/evento` | Accept `{"evento":"ACTUALIZACION","polizaId":555}` and log that delivery to CORE was attempted. |

### Mandatory rules and delivery content

| ID | Requirement |
|---|---|
| BR-01 | An individual policy can have only one risk. |
| BR-02 | A cancelled policy cannot be renewed. |
| BR-03 | Cancelling a policy cancels all of its risks. |
| BR-04 | Adding a risk validates that the policy is collective. |
| SEC-01 | Require header `x-api-key: 123456`. |
| CODE-01 | Use Spring Boot with controller, service, and repository layers. |
| CODE-02 | Provide basic `Poliza` and `Riesgo` entities. |
| CODE-03 | Deliver functional code for every mandatory endpoint. |
| CODE-04 | Implement the business validations. |
| CODE-05 | Include a README with execution instructions. |

## Module 3 - Database optimization

| ID | Requirement |
|---|---|
| DB-01 | Analyze the supplied join between a 10-million-row `orders` table and a 500,000-row `customers` table filtered by `c.country = 'México'`. |
| DB-02 | Describe at least three distinct optimization strategies. |

## Module 4 - Git and GitHub

| ID | Requirement |
|---|---|
| GIT-01 | Explain how to bring one urgent security-fix commit from `main` into `feature/new-login` without bringing other `main` changes. |
| GIT-02 | State the command or strategy and justify it. |

## Module 5 - Technical leadership

Given eight developers, 40% technical debt in key services, ten critical incidents in the
last month, a three-week feature deadline, missing standards/irregular review, and two
junior developers with significant gaps:

| ID | Requirement |
|---|---|
| LEAD-01 | State five priorities for the first two weeks. |
| LEAD-02 | Explain how to organize the team to improve speed and quality. |
| LEAD-03 | Define metrics for evaluating engineering-area performance. |
| LEAD-04 | Define mandatory engineering practices. |
| LEAD-05 | Explain how to manage business pressure without compromising quality. |

## Implicit evaluation criteria

These criteria are not additional product scope; they make explicit what the assessment's
stated evaluation categories imply.

| ID | Inferred criterion | Evidence expected |
|---|---|---|
| IMP-01 | Correctness under invalid transitions and concurrent updates. | Domain invariants, optimistic locking, tests, consistent HTTP errors. |
| IMP-02 | Production-minded reliability around the legacy dependency. | Ports/adapters, transactional outbox, retry/backoff, idempotent event identity. |
| IMP-03 | Maintainable architecture without premature distributed complexity. | Modular monolith with enforceable boundaries and an extraction path. |
| IMP-04 | Secure-by-default behavior beyond a string comparison. | Constant-time key comparison, secret externalization, no credential logging. |
| IMP-05 | Operability and evaluator usability. | Health/metrics, structured logs, OpenAPI UI, Docker Compose, seeded local data. |
| IMP-06 | Financial-data correctness. | `BigDecimal`, explicit scale/rounding, database precision constraints. |
| IMP-07 | Reproducibility and professional delivery. | Automated tests, CI, deterministic documentation build, meaningful Git history. |
| IMP-08 | Traceability. | Requirement-to-code/test/document matrix in the final report. |

