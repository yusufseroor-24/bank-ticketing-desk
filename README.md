Project Information
- 
Project Title: 
BankDesk - Ticket & Case Management for Banking Operations

Project Description: 
BankDesk is a Spring Boot REST API that allows different users in a bank to perform different functionalities. Customers of the bank are able to raise tickets and allow the internal staff (agents) to triage, claim, and resolve those tickets. It is built as a multipurpose ticket case management platform, solving problems internally such as AML and fraud as two categories for internal employees.
Other categories include (KYC review, card dispute, IT/Security incidents, and complaints), with each of these categories having their own status workflow and visibility rules. 

Project Purpose:
To demonstrate the importance of visibility of ticketing systems using SLA enforcement to provide effective ticket resolution by providing layered backend that models an operational workflow with business logic and CURD endpoints while allowing adaptability to new features and extensibility. 

Main Project Features:
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

Controller —> Service —> Repository —> Database 

DTOs are used to shape the request and responses of database entities so sensitive fields are not exposed, Mappers are then used to cover entities to DTOs.
Additionally, a dedicated workflow class is used to hold category specific workflow and status transition rules as a map. 
A GlobalExeceptionHandler with @RestControllerAdvice is used to intercept every thrown execution and convert it into a JSON error. Lastly, a security package which holds a JWT security based filter creation and validation of tokens on requests with no server side sessions. 

General Approach
- 


User Stories
- 
The user stories of the project are provided in the Trello board linked below. 
Trello Board link: https://trello.com/b/ZsWVql0g/bankticketingsystem

Project ERD
-
<img width="1893" height="1492" alt="BankDesk" src="https://github.com/user-attachments/assets/1f4fc368-75eb-4877-b9e2-1df8aca4de32" />

Project Planning (Trello)
-
The project is planned using a Trello Board. 
Trello Board Link: https://trello.com/b/ZsWVql0g/bankticketingsystem

API Documentation
- 
When the app is running, interactive Swagger can be used to test endpoints.
Swagger link: http://localhost:8080/swagger-ui/index.html#/


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

Major Challenges
- 

Future Improvements 
- 
- Logic integration to each category: Add enhanced business logic for each category ex. CVSS integration to security related tickets, structural resolution to AML tickets, etc.
- Passed SLA deadline Cron Job: a scheduled cron job that flags tickets with SLA passed their deadline, notifies assigned agent, and increases the priority of the ticket 
- SLA report Cron Job: a scheduled monthly report sent to the admin demonstrating statistics of SLA performance of the tickets
- Automated ticket creation: Automatically create an AML ticket from suspicious transactions (rule that checks transaction, rate limit, and account data)
- An enhanced frontend

Resources
- 
- Bucket4j: https://www.baeldung.com/spring-bucket4j - Rate limiting implementation
- Mailtrap: https://mailtrap.io - Development email sandbox for verification/reset emails
- dbdiagram.io: https://dbdiagram.io - ERD creation
- Server-Sent Events (SSE): https://www.baeldung.com/spring-server-sent-events - Notification implementation
