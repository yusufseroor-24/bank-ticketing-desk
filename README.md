**BankDesk Project**
-
**Project Title:**
**BankDesk** - Ticket & Case Management for Banking Operations

**Project Description:**
BankDesk is a Spring Boot microservices platform that allows different users in a bank to perform different functionalities. Customers of the bank are able to raise tickets and allow the internal staff (agents) to triage, claim, and resolve those tickets. It is built as a multipurpose ticket case management platform, solving problems internally such as AML and fraud as two categories for internal employees.
Beyond ticketing, BankDesk adds SLA monitoring with automatic alerts and monthly reports, automatic fraud/AML ticket creation from simulated bank transactions, virus scanning of uploaded files, live notifications over SSE, KYC identity verification, and AI assistance for drafting and triaging tickets.

**Project Purpose:**
To demonstrate the importance of visibility of ticketing systems using SLA enforcement to provide effective ticket resolution by providing a layered backend that models an operational workflow with business logic, CRUD endpoints, safe handling of simultaneous users, and background processing, while staying extensible to new features. The application was split into microservices, tested with Cucumber, and deployed to AWS through a Jenkins pipeline.

**Main Project Features:**
- JWT authentication with email verification and password management
- Role Based authorization and access control (Customer, Agent, Admin)
- Category specific ticket workflow with enforced status transitions
- Ticket claim with double-claim prevention
- SLA enforcement depending on ticket priority to allow visibility and transparency of ticket status
- Escalation with automatic priority SLA recalculation
- Comments, file attachments, and ticket history per ticket
- Notifications via Server Sent Events (SSE) and email
- Admin functionality: user management (soft delete of users), soft delete and restore of tickets, category management, audit log
- Automatic fraud/AML ticket creation
- KYC identity verification with Didit
- AI-assisted ticket drafting, triage and summaries
- CI/CD pipeline and AWS deployment

Project Technologies
-
- Java 17
- Spring Boot 4.1.2
- Spring Cloud Gateway
- Apache Kafka
- JWT
- PostgreSQL (AWS RDS) and pgAdmin 4
- Maven
- Swagger
- Bucket4j: (Rate Limiting)
- Server-Sent Events (SSE): (Notifications)
- Gmail SMTP (email)
- Spring Scheduling and async thread pools (background jobs)
- VirusTotal API v3 (file hash scanning)
- Didit (KYC verification)
- OpenAI API (AI assistance)
- Docker and Docker Compose
- Jenkins (CI/CD)
- Cucumber and RestAssured (BDD tests)
- AWS: EC2, RDS, ECR, CloudWatch, IAM
- ngrok (HTTPS address)
- Git / Github
- Trello

Project Architecture
-
The project follows the standard layered architecture inside each service:

**Controller —> Service —> Repository —> Database**

DTOs are used to shape the request and responses of database entities so sensitive fields are not exposed, and Mappers are used to convert entities to DTOs.
Additionally, a dedicated workflow class is used to hold category specific workflow and status transition rules as a map.
A GlobalExceptionHandler with @RestControllerAdvice is used to intercept every thrown exception and convert it into a JSON error. Lastly, a security package holds a JWT filter that validates tokens on every request, with no server side sessions.

Microservices & Deployment
-
BankDesk was split from one Spring Boot application into six services that share one PostgreSQL database and communicate through Kafka:

| Service | Port | Responsibility |
| ------- | ---- | -------------- |
| api-gateway | 8080 | Single entry point; serves the frontend and routes `/api/**` to the services |
| identity-service | 8081 | Register/login, JWT, users, KYC (Didit) |
| ticket-service | 8082 | Tickets, workflow, SLA, attachments, AI endpoints |
| fraud-service | 8083 | Transactions, fraud/AML rules, alerts |
| notification-service | 8084 | Live notifications (SSE) and email |
| ai-service | 8085 | Internal AI drafting, triage and summaries |

**Request flow:** browser -> ngrok HTTPS -> api-gateway -> service -> PostgreSQL (RDS). Services publish events (KYC results, notifications, fraud events) through Kafka.

**CI/CD (Jenkins):** the `Jenkinsfile` runs the Cucumber tests, builds the JARs and Docker images, pushes them to AWS ECR, then deploys to EC2 over SSH using `docker-compose.prod.yml`. Jenkins checks the `main` branch for new pushes **every 5 minutes**, so every push is tested, built and deployed automatically with no manual step. A build can also be started by hand with **Build with Parameters**.

**AWS:**
- The database is on RDS PostgreSQL and is private; only the EC2 security group can connect to it.
- The EC2 server uses an IAM instance role to pull images from ECR and write logs to CloudWatch.
- **CloudWatch:** every container's logs are sent to AWS CloudWatch (log group `bankdesk`) with Docker's `awslogs` driver, so logs can be read in the AWS console even after a restart.
- Pipeline credentials live in Jenkins, and application secrets live in a `.env` file on the server.

**HTTPS webhook:** the Didit KYC webhook needs a public HTTPS address, so an ngrok container with a static domain runs next to the gateway.

**Live addresses** (all reach the same application):

| Address | What it is |
| ------- | ---------- |
| `http://13.61.217.119` | The server's fixed (Elastic) IP, plain HTTP |
| `http://ec2-13-61-217-119.eu-north-1.compute.amazonaws.com` | The AWS public DNS name of the same server, plain HTTP |
| `https://olive-replica-nutcase.ngrok-free.dev` | HTTPS through ngrok. Needed for the Didit KYC webhook. On the first visit ngrok shows a warning page; click **Visit Site** |

Open `/simulation.html` on any of them for the Concurrency simulation.

Testing (Cucumber)
-
BDD tests for ticket-service live in `ticket-service/src/test/resources/features` (ticket creation, claiming, fraud events, resilience). They run on H2 with an embedded Kafka and a fake identity service, so no external setup is needed:
`cd ticket-service && ./mvnw test`
The Jenkins pipeline runs them on every build and stops if any fail.

General Approach
-
BankDesk started as a narrower AML/fraud-detection idea, but was deliberately broadened after feedback that a single-purpose system doesn't demonstrate extensibility well. The final design treats multiple categories for the ticketing system, including AML and fraud, alongside KYC review, card disputes, and other banking operations. The project data structure was visualized using an ERD, which determined the initial point of the project. Generalized tasks and sub tasks were created and tracked using Trello.
BankDesk was built incrementally, creating branches on git and merging into develop once each feature was tested. Authentication and authorization were built first, since later features need to know who is calling and whether they are allowed to. Then the exceptions were created and edited throughout to allow easier debugging. The ticketing core concepts came next, including the entity models, category status and transitions, ticket assignment, and SLA. Later came additional features like comments, attachments, SSE, search and pagination, audit logging and ticket history, and rate limiting.
A second phase added operational features: SLA policies and notifications, safe handling of simultaneous requests, asynchronous email, automatic fraud/AML ticket creation, the monthly SLA report, and VirusTotal attachment scanning. Each was again built on its own branch and merged into develop once tested.
Lastly, the application was split into services, covered by Cucumber tests, containerized, and deployed to AWS. The final code was merged into main.

Race Condition: Double Claim
-
**The problem.** Two agents press "Claim" on the same unassigned ticket at the same moment. A naive version reads the ticket, sees it has no agent, and saves its own agent id. Both requests read before either one writes, so both succeed and the second silently overwrites the first. Both agents are told they own the ticket, but only one of them does.

**Why a normal check is not enough.** Reading the ticket and checking `getAssignedToId() == null` in Java is a check-then-act sequence. Another request can change the row between the check and the save.

**The fix.** The check and the write happen in one atomic database statement (`TicketRepository.claimIfUnassigned`):

```sql
UPDATE tickets
SET assigned_to_id = :agentId, status = 'ASSIGNED', version = version + 1, updated_at = :now
WHERE id = :ticketId AND assigned_to_id IS NULL AND status = :expectedStatus
```

PostgreSQL locks the row while updating. The first request changes 1 row. The second request waits, re-evaluates the `WHERE` clause, finds the ticket already assigned, and changes 0 rows. `TicketService.claimTicket` treats "0 rows updated" as a lost race and returns **409 Conflict** ("This ticket was just claimed by another agent").

**Related protections.**
- **Optimistic locking:** `Ticket` has a `@Version` field, so two status changes made from stale data cannot overwrite each other.
- **One-time tokens:** password-reset and email-verification tokens are marked used with an atomic update, so a link cannot be used twice at the same moment.
- **Fraud alerts:** each alert has a unique `dedup_key` with `ON CONFLICT DO NOTHING`, so many detection threads create one alert and one ticket.
- **Monthly SLA report:** claimed with `INSERT ... ON CONFLICT`, so two admins cannot generate the same month twice.

**Proof:** The Cucumber scenario where two agents claim the same ticket fires both claims at once and checks that exactly one succeeds and the other gets a 409. You can also watch it live in the Concurrency simulation.

Concurrency Lab (simulation)
-
`simulation.html` is an interactive page that showcases how BankDesk handles concurrency. It fires real parallel requests at the running backend and draws each request on a timeline, with a pass/fail verdict. It is simple to use: open `/simulation.html` on the gateway (for example `https://olive-replica-nutcase.ngrok-free.dev/simulation.html` or `http://localhost:8080/simulation.html`), press **Connect demo accounts**, then pick a demo and press its button. It has nine demos: ticket claim race, optimistic locking, one-time reset token, monthly report claim, fraud alert dedup, email thread pool, VirusTotal queue, SLA scheduler and the SSE registry.

**How to run the simulation**
1. Start the system (`docker compose up --build` locally, or use the deployed URL). Demo accounts are seeded automatically.
2. Open `http://localhost:8080/simulation.html` (or `https://<your-ngrok-domain>/simulation.html`). Open it through the gateway, not as a local file, because it calls the API on the same address.
3. Click **Connect demo accounts**. The fields are pre-filled with the seeded admin, agent and customer accounts.
4. Pick a demo from the left menu and press its button. Each demo explains the problem and the code that solves it, then shows a pass/fail verdict.
5. For the live stream on the right, tick only the roles you need. Don't use the normal app logged in as the same user at the same time (only one live connection per user is kept).

Notes: the VirusTotal demo needs `VIRUSTOTAL_API_KEY` and takes a few minutes because scans are spaced 16 seconds apart. The email demo sends real emails to the customer's inbox.

User Stories
-
The user stories of the project are provided in the Trello board linked below.
- **Trello Board link:** https://trello.com/b/ZsWVql0g/bankticketingsystem

Project ERD
-
<img width="2296" height="1771" alt="BankDesk" src="https://github.com/user-attachments/assets/ad27a2d6-72e7-402d-a513-f49c7d3fa33e" />


Project Planning (Trello)
-
The project is planned using a Trello Board.
**Trello Board Link:** https://trello.com/b/ZsWVql0g/bankticketingsystem

API Documentation
-
All requests go through the gateway: `http://13.61.217.119` (or the HTTPS ngrok address) when deployed, and `http://localhost:8080` when run locally.

**Swagger** Each service has its own Swagger UI. The service ports are only opened when running locally with Docker Compose. On the deployed server only the gateway is public and the services stay private on purpose, so Swagger is not exposed there.

| Service | Swagger |
| ------- | ------- |
| identity-service | http://localhost:8081/swagger-ui/index.html |
| ticket-service | http://localhost:8082/swagger-ui/index.html |
| fraud-service | http://localhost:8083/swagger-ui/index.html |
| notification-service | http://localhost:8084/swagger-ui/index.html |
| ai-service | http://localhost:8085/swagger-ui/index.html |

| Method | URL | Functionality | Access |
| ------ | --- | ------------- | ------ |
| POST | `/api/auth/register` | Register a new account | Public |
| POST | `/api/auth/login` | Log in, returns JWT | Public |
| GET | `/api/auth/verify-email` | Verify email via token (single-use) | Public |
| POST | `/api/auth/resend-verification` | Resend verification email | Public |
| POST | `/api/auth/forget-password` | Request password reset link | Public |
| POST | `/api/auth/reset-password` | Reset password via token (single-use) | Public |
| GET | `/api/users/me` | Get own profile | Private |
| PUT | `/api/users/me` | Update own full name | Private |
| POST | `/api/users/change-password` | Change own password | Private |
| POST | `/api/users/me/profile-pic` | Upload profile picture | Private |
| GET | `/api/users/me/profile-pic` | Get own profile picture | Private |
| POST | `/api/kyc/start` | Start a KYC verification session | Customer |
| GET | `/api/kyc/status` | Get own KYC status | Customer |
| POST | `/api/kyc/webhook` | Receive KYC results from Didit (signature-checked) | Didit |
| GET | `/api/categories` | List categories (filtered by role) | Private |
| POST | `/api/tickets/create` | Create a ticket | Customer |
| POST | `/api/tickets/create/internal` | Create an internal/staff ticket | Agent, Admin |
| POST | `/api/tickets/ai/draft` | AI draft of a ticket from a short description | Customer |
| GET | `/api/tickets/{id}` | Get a ticket by ID | Private (owner/staff) |
| GET | `/api/tickets/my-tickets` | List own tickets | Customer |
| GET | `/api/tickets` | Search/filter/paginate tickets | Agent, Admin |
| PUT | `/api/tickets/{id}/claim` | Claim an unassigned ticket | Agent |
| PUT | `/api/tickets/{id}/status` | Change ticket status | Agent, Admin |
| PUT | `/api/tickets/{id}/escalate` | Escalate a ticket | Agent, Admin |
| POST | `/api/tickets/{id}/ai/triage` | AI triage suggestion | Agent, Admin |
| POST | `/api/tickets/{id}/ai/summary` | AI summary of the ticket | Agent, Admin |
| POST | `/api/tickets/{id}/comments` | Add a comment | Private (owner/staff) |
| GET | `/api/tickets/{id}/comments` | List comments | Private (owner/staff) |
| POST | `/api/tickets/{id}/attachments` | Upload file(s) to a ticket | Private (owner/staff) |
| GET | `/api/tickets/{id}/attachments` | List a ticket's attachments | Private (owner/staff) |
| GET | `/api/tickets/attachments/{id}` | Download an attachment | Private (owner/staff) |
| DELETE | `/api/tickets/attachments/{id}` | Delete an attachment | Private (owner/staff) |
| POST | `/api/tickets/attachments/{id}/rescan` | Re-run the VirusTotal scan | Agent, Admin |
| GET | `/api/tickets/{id}/history` | View ticket history | Private (owner/staff) |
| GET | `/api/notifications/subscribe` | Subscribe to live updates (SSE) | Private |
| GET | `/api/admin/users` | List all users | Admin |
| PUT | `/api/admin/{userId}/role` | Change a user's role | Admin |
| PUT | `/api/admin/{userId}/deactivate` | Soft-delete a user | Admin |
| PUT | `/api/admin/{userId}/reactivate` | Restore a user | Admin |
| POST | `/api/admin/categories` | Create a category | Admin |
| PUT | `/api/admin/categories/{id}` | Update a category | Admin |
| POST | `/api/admin/users/{agentId}/categories` | Assign categories to an agent | Admin |
| PUT | `/api/admin/tickets/{id}/reopen` | Reopen a closed ticket | Admin |
| PUT | `/api/admin/tickets/{id}/reassign` | Reassign a ticket to another agent | Admin |
| PUT | `/api/admin/tickets/{id}/soft-delete` | Soft delete a ticket (hidden from everyone, kept in the database) | Admin |
| PUT | `/api/admin/tickets/{id}/restore` | Restore a soft-deleted ticket | Admin |
| GET | `/api/admin/tickets/deleted` | List soft-deleted tickets | Admin |
| GET | `/api/admin/audit-logs` | View the audit log | Admin |
| GET | `/api/admin/sla-policies` | List SLA policies | Admin |
| PUT | `/api/admin/sla-policies/{policyId}` | Update an SLA policy | Admin |
| POST | `/api/admin/sla-reports?month=YYYY-MM` | Generate the SLA report for a month | Admin |
| GET | `/api/admin/sla-reports` | List SLA reports | Admin |
| GET | `/api/admin/sla-reports/latest/download` | Download the latest SLA report (CSV) | Admin |
| POST | `/api/admin/transactions` | Record a bank transaction (runs the fraud rules) | Admin |
| POST | `/api/admin/transactions/simulate` | Simulate a fraud/AML scenario | Admin |
| GET | `/api/admin/fraud-alerts` | List fraud alerts | Admin |

The ai-service endpoints (`/internal/ai/draft-ticket`, `/triage`, `/summarize`) are internal. They need the `X-Internal-Key` header and are only called by ticket-service.

Project Installation
-
**Run locally**
1. Clone the repository.
2. Create a `.env` file in the project root with the values used by `docker-compose.yml` (JWT secret, internal API key, Gmail app password, VirusTotal key, Didit keys, OpenAI key).
3. Start everything: `docker compose up --build` (PostgreSQL, pgAdmin, Kafka and all six services).
4. Open `http://localhost:8080`. Seeder classes create demo users and data automatically, so no manual steps are required.

**Deploy to AWS**
1. Create an RDS PostgreSQL database (private), ECR repositories `bankdesk/<service>`, and an EC2 instance with Docker and an IAM role that can read ECR and write CloudWatch logs.
2. On the server, create `~/bankdesk/.env` with the RDS endpoint, secrets and the ngrok token and domain.
3. In Jenkins, add the credentials `aws-access-key-id`, `aws-secret-access-key` and `ec2-ssh-key`, and create a pipeline job from this repository (branch `main`).
4. Run **Build with Parameters** with PUSH_TO_ECR and DEPLOY_TO_EC2 ticked. After that, Jenkins checks `main` every 5 minutes and deploys new pushes by itself.
5. Open the public ngrok HTTPS address (or the server's IP over HTTP).

Unsolved Problems
-
- **Pipeline runs on a laptop:** Jenkins runs in Docker on a developer machine rather than on a hosted server.
- **ngrok free tier:** the HTTPS address belongs to one ngrok account and only one tunnel per domain can run at a time.

Major Challenges
-
- **Calculating SLA:** An early design calculated due dates from both the category and a per-category default priority using a multiplier, which produced inconsistent results across categories. This was simplified to a fixed SLA-by-priority, which is decided directly by the customer at ticket creation.
- **Category-specific status workflows:** each category needed its own allowed status transitions. Hardcoding them in if statements became unmanageable, so they were moved into a map of status to allowed next statuses, which a single validation method checks.
- **Duplicate automatic alerts:** many transactions could trigger the same fraud pattern. Each alert has a unique dedup key claimed atomically, so one pattern creates one ticket.
- **External services:** the VirusTotal free trial has a rate limit. To avoid triggering it, a thread pool runs on one worker that waits 16 seconds between calls.
- **Splitting the monolith:** services now call each other, so cross-service concerns (JWT checking, the internal API key, Kafka events) had to be introduced, and the tests needed a fake identity service.
- **KYC tickets:** the KYC result event first created a staff ticket for every result. It now only opens a review ticket when the result is "in review".
- **AWS and HTTPS:** KYC webhook required an HTTPS address, which was solved with an ngrok container on the server.

Future Improvements
-
- **Business-hours SLA calendars:** pausing the SLA while waiting for the customer.
- **Approval Workflow:** integrate a workflow with multiple approvals for sensitive status changes or actions.
- **Logic integration to each category:** add enhanced business logic for each category, e.g. CVSS integration for security tickets, structured resolution for AML tickets.
- **A real domain and HTTPS certificate** instead of the ngrok address, and Jenkins on a hosted server.
- **Separate databases per service** and a managed Kafka cluster.
- **An enhanced frontend**

Resources
-
| Resource | URL | Used for |
| -------- | --- | -------- |
| Bucket4j | https://www.baeldung.com/spring-bucket4j | Rate limiting implementation |
| Mailtrap | https://mailtrap.io | Development email sandbox |
| dbdiagram.io | https://dbdiagram.io | ERD creation |
| SSE (Server-Sent Events) | https://www.baeldung.com/spring-server-sent-events | Notification implementation |
| FATF Public Statements | https://fiaumalta.org/news/fatf-public-statements-19th-june-2026/ | High-risk & grey-list country lists for the fraud/AML rules |
| MessageDigest (Java 17 API) | https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/security/MessageDigest.html | Computing file hashes |
| SHA-256 Hashing in Java | https://www.baeldung.com/sha-256-hashing-java | SHA-256 hash of uploaded attachments |
| VirusTotal API Overview | https://docs.virustotal.com/docs/api-overview | Scan attachments using file hashes |
| EICAR Anti-Malware Test File | https://www.eicar.org/download-anti-malware-testfile/ | Safe test file for the malicious-attachment |
| Rosetta Code: IBAN | https://rosettacode.org/wiki/IBAN | IBAN validation. Working code in many languages, including a Java class (Java 8+) that follows the same steps; it links to Wikipedia for the explanation |
| DEV Community: How to validate an IBAN (mod-97 check digit explained) | https://dev.to/rosariov25/how-to-validate-an-iban-mod-97-check-digit-explained-1436 | Explains the rearrangement, the letter conversion and the remainder-of-1 rule |
| GeeksforGeeks: Luhn Algorithm | https://www.geeksforgeeks.org/dsa/luhn-algorithm/ | Card number checksum validation |
| Baeldung: Validate a Credit Card Number in Java | https://www.baeldung.com/java-validate-cc-number | Luhn check implementation in Java |
| Credit Card Validator: Luhn Algorithm | https://www.creditcardvalidator.org/articles/luhn-algorithm | Explanation of how the Luhn algorithm works |
| Spring Cloud Gateway (Server WebMVC) | https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc/starter.html | API gateway setup and routing |
| Cucumber | https://cucumber.io/docs/guides/10-minute-tutorial/ | BDD tests |
| Didit | https://docs.didit.me | KYC identity verification |
| Jenkins | https://www.jenkins.io/doc/book/pipeline/ | CI/CD pipeline |
| AWS ECR / RDS / EC2 | https://docs.aws.amazon.com | Container registry, database and server |
| ngrok | https://ngrok.com/docs | HTTPS tunnel for the KYC webhook |
