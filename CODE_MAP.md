# Code map (start here)

This repository is **one Spring Boot application** and a PostgreSQL database. The Java folders are responsibilities inside the same app, not separate servers. Every Java source and test file now starts with a plain-English purpose comment; every handwritten method has a short explanation.

## The five files to read first

1. [`PolicyController.java`](src/main/java/com/segurosbolivar/policy/api/PolicyController.java) receives the URL and request body.
2. [`PolicyService.java`](src/main/java/com/segurosbolivar/policy/service/PolicyService.java) organizes the database transaction.
3. [`Policy.java`](src/main/java/com/segurosbolivar/policy/domain/Policy.java) applies insurance rules and calculates renewal.
4. [`CoreOutboxEvent.java`](src/main/java/com/segurosbolivar/policy/integration/outbox/CoreOutboxEvent.java) saves a pending reminder to notify CORE.
5. [`CoreMockController.java`](src/main/java/com/segurosbolivar/policy/api/CoreMockController.java) receives that notification in the demo.

**One renewal:** HTTP request → controller → service → policy rule → save policy and pending reminder → worker sends reminder to the CORE mock. The mock is not a real CORE.

## Build, configuration, and documentation

| File | Purpose |
|---|---|
| [`CODE_MAP.md`](CODE_MAP.md) | This English index of every repository file and the five-file reading path. |
| [`.dockerignore`](.dockerignore) | Excludes generated and editor files from the Docker build context. |
| [`.editorconfig`](.editorconfig) | Sets whitespace rules shared by editors. |
| [`.env.example`](.env.example) | Example local API key for Compose; not a production secret. |
| [`.gitignore`](.gitignore) | Excludes builds, local secrets, and editor files from Git. |
| [`.github/dependabot.yml`](.github/dependabot.yml) | Schedules dependency update checks. |
| [`.github/workflows/assessment-delivery.yml`](.github/workflows/assessment-delivery.yml) | Tests the assessment branch and uploads build artifacts. |
| [`.github/workflows/ci.yml`](.github/workflows/ci.yml) | Tests regular development pushes and pull requests. |
| [`Dockerfile`](Dockerfile) | Builds/tests the JAR and runs it in a Java container. |
| [`LICENSE`](LICENSE) | Legal terms for this repository; not application logic. |
| [`Makefile`](Makefile) | Short names for test, run, Docker, PDF, and packaging commands. |
| [`README.md`](README.md) | Run instructions, HTTP contract, decisions, and limitations. |
| [`REQUIREMENTS.md`](REQUIREMENTS.md) | Maps each assessment requirement to its evidence. |
| [`compose.yml`](compose.yml) | Starts the API and PostgreSQL together. |
| [`pom.xml`](pom.xml) | Maven dependencies and build/test configuration. |
| [`docs/technical-assessment.tex`](docs/technical-assessment.tex) | LaTeX source of the two-page submitted answer. |
| [`docs/verification.md`](docs/verification.md) | Records original test evidence and what was not claimed at submission. |
| [`scripts/build-docs.sh`](scripts/build-docs.sh) | Compiles the submitted LaTeX answer to PDF. |
| [`scripts/package-submission.sh`](scripts/package-submission.sh) | Verifies committed code and packages a source ZIP and PDF. |
| [`scripts/smoke.py`](scripts/smoke.py) | Runs repeatable real HTTP checks against a running API. |
| [`src/main/resources/application.yml`](src/main/resources/application.yml) | Default H2, API, CORE mock, security, and metrics settings. |
| [`src/main/resources/application-postgres.yml`](src/main/resources/application-postgres.yml) | PostgreSQL settings selected by the Docker profile. |
| [`src/main/resources/db/migration/V1__create_policy_schema.sql`](src/main/resources/db/migration/V1__create_policy_schema.sql) | Creates policy, risk, and outbox tables. Left byte-for-byte unchanged so existing Flyway databases accept its checksum. |

## Java application files

| File | Purpose |
|---|---|
| [`PolicyManagementApplication.java`](src/main/java/com/segurosbolivar/policy/PolicyManagementApplication.java) | Starts the single Spring Boot app and enables the scheduled outbox worker. |
| [`ApiExceptionHandler.java`](src/main/java/com/segurosbolivar/policy/api/ApiExceptionHandler.java) | Turns validation, business-rule, and concurrency errors into clear HTTP responses. |
| [`CoreMockController.java`](src/main/java/com/segurosbolivar/policy/api/CoreMockController.java) | Pretends to be CORE: logs an HTTP event and returns 202 without changing a real CORE. |
| [`PolicyController.java`](src/main/java/com/segurosbolivar/policy/api/PolicyController.java) | Receives policy HTTP requests and passes each operation to PolicyService. |
| [`RiskController.java`](src/main/java/com/segurosbolivar/policy/api/RiskController.java) | Receives HTTP requests to read or cancel one risk. |
| [`CreatePolicyRequest.java`](src/main/java/com/segurosbolivar/policy/api/dto/CreatePolicyRequest.java) | Lists and validates the fields a client may send to create a policy. |
| [`PageResponse.java`](src/main/java/com/segurosbolivar/policy/api/dto/PageResponse.java) | Shapes a policy list into results plus page numbers. |
| [`PolicyResponse.java`](src/main/java/com/segurosbolivar/policy/api/dto/PolicyResponse.java) | Shapes a policy response without exposing the database entity directly. |
| [`RenewPolicyRequest.java`](src/main/java/com/segurosbolivar/policy/api/dto/RenewPolicyRequest.java) | Receives and validates the IPC percentage for renewal. |
| [`RiskRequest.java`](src/main/java/com/segurosbolivar/policy/api/dto/RiskRequest.java) | Receives and validates a risk address and tenant name. |
| [`RiskResponse.java`](src/main/java/com/segurosbolivar/policy/api/dto/RiskResponse.java) | Shapes the risk data returned to a client. |
| [`ApiKeyFilter.java`](src/main/java/com/segurosbolivar/policy/config/ApiKeyFilter.java) | Checks x-api-key before protected HTTP requests reach a controller. |
| [`CorrelationIdFilter.java`](src/main/java/com/segurosbolivar/policy/config/CorrelationIdFilter.java) | Adds a tracking ID to each request and its log messages. |
| [`DemoDataInitializer.java`](src/main/java/com/segurosbolivar/policy/config/DemoDataInitializer.java) | Creates two sample policies when the database is empty so the demo has data. |
| [`OpenApiConfiguration.java`](src/main/java/com/segurosbolivar/policy/config/OpenApiConfiguration.java) | Describes the API and its key to Swagger; it does not apply business rules. |
| [`DomainRuleViolationException.java`](src/main/java/com/segurosbolivar/policy/domain/DomainRuleViolationException.java) | Represents a broken policy rule, which the API reports as HTTP 409. |
| [`Money.java`](src/main/java/com/segurosbolivar/policy/domain/Money.java) | Keeps cent rounding and percentage calculations in one place. |
| [`Policy.java`](src/main/java/com/segurosbolivar/policy/domain/Policy.java) | Stores a policy and applies its rules for risks, renewal, and cancellation. |
| [`PolicyStatus.java`](src/main/java/com/segurosbolivar/policy/domain/PolicyStatus.java) | Lists the allowed policy states. |
| [`PolicyType.java`](src/main/java/com/segurosbolivar/policy/domain/PolicyType.java) | Distinguishes individual policies from collective policies. |
| [`Risk.java`](src/main/java/com/segurosbolivar/policy/domain/Risk.java) | Stores a covered property and tenant, and supports cancellation. |
| [`RiskStatus.java`](src/main/java/com/segurosbolivar/policy/domain/RiskStatus.java) | Lists the allowed risk states. |
| [`CoreEventPayload.java`](src/main/java/com/segurosbolivar/policy/integration/core/CoreEventPayload.java) | Defines the JSON sent by the worker to the CORE mock. |
| [`CoreEventPublisher.java`](src/main/java/com/segurosbolivar/policy/integration/core/CoreEventPublisher.java) | Defines event delivery so tests can replace the HTTP implementation. |
| [`CoreOutboxDispatcher.java`](src/main/java/com/segurosbolivar/policy/integration/core/CoreOutboxDispatcher.java) | Regularly finds pending CORE events and asks the processor to handle them. |
| [`CoreOutboxProcessor.java`](src/main/java/com/segurosbolivar/policy/integration/core/CoreOutboxProcessor.java) | Attempts delivery and marks an event sent or schedules a retry. |
| [`HttpCoreEventPublisher.java`](src/main/java/com/segurosbolivar/policy/integration/core/HttpCoreEventPublisher.java) | Sends an event to the CORE mock over HTTP. |
| [`CoreOutboxEvent.java`](src/main/java/com/segurosbolivar/policy/integration/outbox/CoreOutboxEvent.java) | Stores the durable reminder to notify CORE, including its status and attempts. |
| [`OutboxStatus.java`](src/main/java/com/segurosbolivar/policy/integration/outbox/OutboxStatus.java) | Lists pending, sent, and permanently failed delivery states. |
| [`CoreOutboxEventRepository.java`](src/main/java/com/segurosbolivar/policy/repository/CoreOutboxEventRepository.java) | Reads and locks saved CORE event reminders. |
| [`PolicyRepository.java`](src/main/java/com/segurosbolivar/policy/repository/PolicyRepository.java) | Reads, saves, and locks policies in the database. |
| [`RiskRepository.java`](src/main/java/com/segurosbolivar/policy/repository/RiskRepository.java) | Reads risks and finds their parent policy before a change. |
| [`PolicyService.java`](src/main/java/com/segurosbolivar/policy/service/PolicyService.java) | Coordinates policy and risk operations inside database transactions. |
| [`ResourceNotFoundException.java`](src/main/java/com/segurosbolivar/policy/service/ResourceNotFoundException.java) | Represents a missing ID, which the API reports as HTTP 404. |

## Test files

| File | What it proves |
|---|---|
| [`PolicyApiEdgeCasesTest.java`](src/test/java/com/segurosbolivar/policy/api/PolicyApiEdgeCasesTest.java) | Checks invalid input, security, missing IDs, and repeated cancellations over HTTP. |
| [`PolicyApiIntegrationTest.java`](src/test/java/com/segurosbolivar/policy/api/PolicyApiIntegrationTest.java) | Checks the main HTTP requirements: listing, renewal, cancellation, risks, and CORE mock. |
| [`PolicyTest.java`](src/test/java/com/segurosbolivar/policy/domain/PolicyTest.java) | Checks Policy business rules without HTTP. |
| [`CoreOutboxProcessorTest.java`](src/test/java/com/segurosbolivar/policy/integration/core/CoreOutboxProcessorTest.java) | Checks delivery states, retries, and failures. |
| [`HttpCoreEventPublisherTest.java`](src/test/java/com/segurosbolivar/policy/integration/core/HttpCoreEventPublisherTest.java) | Checks HTTP payloads and responses with a local test server. |
| [`PolicyTransactionTest.java`](src/test/java/com/segurosbolivar/policy/service/PolicyTransactionTest.java) | Checks rollback and concurrent updates across policy, risks, and outbox. |
