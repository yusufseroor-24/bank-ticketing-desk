Project Information
- 
**Project Title:** 
**BankDesk** - Ticket & Case Management for Banking Operations

**Project Description:** 
BankDesk is a Spring Boot REST API that allows different users in a bank to perform different functionalities. Customers of the bank are able to raise tickets and allow the internal staff (agents) to triage, claim, and resolve those tickets. It is built as a multipurpose ticket case management platform, solving problems internally such as AML and fraud as two categories for internal employees.
Other categories include (KYC review, card dispute, IT/Security incidents, and complaints), with each of these categories having their own status workflow and visibility rules. 

**Project Purpose:**
To demonstrate the importance of visibility of ticketing systems using SLA enforcement to provide effective ticket resolution by providing layered backend that models an operational workflow with business logic and CURD endpoints while allowing adaptability to new features and extensibility. 

**Main Project Features:**
- JWT authentication with email verification and password management 
- Role Based authorization and access control (Customer, Agent, Admin)
- Category specific ticket workflow with enforced status transition 
- Ticket claim with double-claim prevention
- SLA enforcement depending on ticket priority to allow visibility and transparency of ticket status
- Escalation with automatic priority SLA recalculation 
- Comments, file attachments, and ticket history per ticket 
- Notifications via Server Sent Events (SSE)
- Admin functionality: user management, soft delete, category management, log

Project Technologies
- 
- Java 17 
- Spring Boot 4.1.1
- JWT
- PostgreSQL
- pgAdmin 4
- Maven
- Swagger
- Bucket4j: (Rate Limiting)
- Server-Sent Events (SSE): (Notifications)
- Mailtrap (Email sandbox)
- Git / Github 
- Trello

Project Architecture
-
The project follows the standard layered architecture:

**Controller —> Service —> Repository —> Database**

DTOs are used to shape the request and responses of database entities so sensitive fields are not exposed, Mappers are then used to cover entities to DTOs.
Additionally, a dedicated workflow class is used to hold category specific workflow and status transition rules as a map. 
A GlobalExeceptionHandler with @RestControllerAdvice is used to intercept every thrown execution and convert it into a JSON error. Lastly, a security package which holds a JWT security based filter creation and validation of tokens on requests with no server side sessions. 

General Approach
- 
BankDesk started as a narrower AML/fraud-detection idea, but was deliberately broadened after feedback that a single-purpose system doesn't demonstrate extensibility well. The final design treats multiple categories of for ticketing system including AML and fraud as one of the categories alongside, KYC review, card disputes, and other banking operations. The project data structure was visualized using ERD, which determined the initial point of the project. A generalized tasks and sub tasks were created and tracked using Trello.
BnakDesk was built incrementally, creating branches on git and merging into develop once each feature is tested. Authentication and authorization was built first, since the features later needs to know these calling and if they are authorized to do it. Then the exceptions were created and edited on throughout to allow easier debugging. The ticketing core concepts came next including the table entity models, category status and transitions, ticket assignment, and SLA. Later came the additional features like comments, attachments, SSE, search and pagination, Audit logging and ticket history, and rate limiting.

User Stories
- 
The user stories of the project are provided in the Trello board linked below. 
- **Trello Board link:** https://trello.com/b/ZsWVql0g/bankticketingsystem

Project ERD
-
<img width="1893" height="1492" alt="BankDesk" src="https://github.com/user-attachments/assets/1f4fc368-75eb-4877-b9e2-1df8aca4de32" />

Project Planning (Trello)
-
The project is planned using a Trello Board. 
**Trello Board Link:** https://trello.com/b/ZsWVql0g/bankticketingsystem

API Documentation
- 
When the app is running, interactive Swagger can be used to test endpoints.
- **Swagger link:** http://localhost:8080/swagger-ui/index.html#/
  
| Method | URL | Functionality | Access |
| ------ | --- | ------------- | ------ |
| POST | `/api/auth/register` | Register a new account | Public |
| POST | `/api/auth/login` | Log in, returns JWT | Public |
| GET | `/api/auth/verify-email` | Verify email via token | Public |
| POST | `/api/auth/resend-verification` | Resend verification email | Public |
| POST | `/api/auth/forget-password` | Request password reset link | Public |
| POST | `/api/auth/reset-password` | Reset password via token | Public |
| GET | `/api/users/me` | Get own profile | Private |
| PUT | `/api/users/me` | Update own full name | Private |
| POST | `/api/users/change-password` | Change own password | Private |
| POST | `/api/users/me/profile-pic` | Upload profile picture | Private |
| GET | `/api/categories` | List categories (filtered by role) | Private |
| POST | `/api/tickets/create` | Create a ticket | Customer |
| POST | `/api/tickets/create/internal` | Create an internal/staff ticket | Agent, Admin |
| GET | `/api/tickets/{id}` | Get a ticket by ID | Private (owner/staff) |
| GET | `/api/tickets/my-tickets` | List own tickets | Customer |
| GET | `/api/tickets` | Search/filter/paginate tickets | Private |
| PUT | `/api/tickets/{id}/claim` | Claim an unassigned ticket | Agent |
| PUT | `/api/tickets/{id}/status` | Change ticket status | Agent, Admin |
| PUT | `/api/tickets/{id}/escalate` | Escalate a ticket | Agent, Admin |
| POST | `/api/tickets/{id}/comments` | Add a comment | Private (owner/staff) |
| GET | `/api/tickets/{id}/comments` | List comments | Private (owner/staff) |
| POST | `/api/tickets/{id}/attachments` | Upload file(s) to a ticket | Private (owner/staff) |
| GET | `/api/tickets/attachments/{id}` | Download an attachment | Private (owner/staff) |
| DELETE | `/api/tickets/attachments/{id}` | Delete an attachment | Private (owner/staff) |
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
| GET | `/api/admin/audit-logs` | View the audit log | Admin |

Project Installation
- 
1. Clone the repository 
2. Configure PostgreSQL 
3. Configure environment variables (mail trap MAIL_USER & MAIL_PASSWORD)
4. Configure the application (application-dev.properties) 
5. Seed the database (Seeder classes are provided - no manual steps required)
6. Start the application
7. Access the APIs

Unsolved Problems 
- 
- **Category business logic:** the categories are listed with their status, but each status lacks logic and documentation of what has happened to the ticket in each status
- **Attachment retrieval:** attachments can be uploaded, downloaded and deleted by ID, but there is no endpoint that lists a ticket's attachments, so customers never know the IDs they need to do the action needed
- **Password reset link:** the reset email points to the email-verification endpoint, and there is no reset-password form to complete the flow (current system takes the token and uses it in the body as parameter) 

Major Challenges
- 
- **Calculating SLA:** An early design calculated due dates from both the category and a per-category default priority using a multiplier, which produced inconsistent results across categories. This was simplified to a fixed SLA-by-priority, which is decided directly by the customer at ticket creation
- **Category-specific status workflows:** each category needed its own allowed status transitions. Hardcoding them in if statements became unmanageable, so they were moved into a map of status to allowed next statuses, which a single validation method checks
  
Future Improvements 
- 
- **Logic integration to each category:** Add enhanced business logic for each category ex. CVSS integration to security related tickets, structural resolution to AML tickets, etc.
- **Passed SLA deadline Cron Job:** a scheduled cron job that flags tickets with SLA passed their deadline, notifies assigned agent, and increases the priority of the ticket 
- **SLA report Cron Job:** a scheduled monthly report sent to the admin demonstrating statistics of SLA performance of the tickets
- **Automated ticket creation:** Automatically create an AML ticket from suspicious transactions (rule that checks transaction, rate limit, and account data)
- **An enhanced frontend**

Resources
- 
| Resource | URL | Used for |
| -------- | --- | -------- |
| Bucket4j | https://www.baeldung.com/spring-bucket4j | Rate limiting implementation |
| Mailtrap | https://mailtrap.io | Development email sandbox for verification/reset emails |
| dbdiagram.io | https://dbdiagram.io | ERD creation |
| SSE (Server-Sent Events) | https://www.baeldung.com/spring-server-sent-events | Notification implementation |
