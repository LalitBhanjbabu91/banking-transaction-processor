# Banking Transaction Processor

A simple Spring Boot service for processing banking transactions across multiple accounts.

The implementation was developed as a coding kata with an emphasis on:

- Test-driven development
- Clean and simple design
- Object-oriented domain modelling
- Edge-case handling
- Transaction consistency
- Clear API contracts

## Features

The service supports:

- Creating accounts with unique IDs and initial balances
- Depositing money
- Withdrawing money
- Transferring money between accounts
- Preventing overdrafts
- Rejecting zero and negative transaction amounts
- Preventing transfers to the same account
- Maintaining a transaction ledger per account
- Querying account balances
- Querying transaction history

## Technology

- Java
- Spring Boot
- Spring MVC
- Spring Data JPA
- H2 Database
- Jakarta Bean Validation
- JUnit 5
- Mockito
- MockMvc
- Maven

## Design

The application follows a simple layered architecture:

```text
Controller
    |
    v
BankingService
    |
    +---- Account domain model
    |
    +---- AccountRepository
    |
    +---- TransactionRepository
    |
    v
H2 Database
```

### Domain responsibility

`Account` owns the rules that protect its balance.

For example:

- Deposits must be greater than zero.
- Withdrawals must be greater than zero.
- Withdrawals cannot exceed the available balance.

This keeps account invariants close to the state they protect instead of duplicating those rules in controllers or services.

`BankingService` coordinates application use cases such as:

- Loading accounts
- Invoking domain operations
- Persisting account changes
- Recording ledger entries
- Managing transactional boundaries

## Money representation

`BigDecimal` is used for monetary values instead of `double` to avoid floating-point precision problems.

## Transaction ledger

Each transaction contains:

- Account
- Transaction type
- Amount
- Timestamp

Supported transaction types:

- `DEPOSIT`
- `WITHDRAWAL`
- `TRANSFER_OUT`
- `TRANSFER_IN`

A transfer creates two ledger entries:

- `TRANSFER_OUT` for the source account
- `TRANSFER_IN` for the destination account

This allows each account to maintain its own transaction history.

## Transaction consistency

Deposit, withdrawal, and transfer operations are annotated with `@Transactional`.

A transfer updates:

1. Source account
2. Destination account
3. Source ledger entry
4. Destination ledger entry

If any database operation fails, the complete operation is rolled back.

An integration test verifies rollback behaviour by intentionally failing ledger persistence.

## REST APIs

### Create account

**POST** `/api/accounts`

Request:

```json
{
  "initialBalance": 1000.00
}
```

Example response:

```json
{
  "accountId": 1,
  "balance": 1000.00
}
```

Status:

```text
201 Created
```

### Deposit

**POST** `/api/accounts/{accountId}/deposit`

Request:

```json
{
  "amount": 500.00
}
```

Status:

```text
200 OK
```

### Withdraw

**POST** `/api/accounts/{accountId}/withdraw`

Request:

```json
{
  "amount": 200.00
}
```

Status:

```text
200 OK
```

### Transfer

**POST** `/api/accounts/transfer`

Request:

```json
{
  "sourceAccountId": 1,
  "destinationAccountId": 2,
  "amount": 300.00
}
```

Status:

```text
200 OK
```

### Get account balance

**GET** `/api/accounts/{accountId}/balance`

Example response:

```json
{
  "accountId": 1,
  "balance": 1200.00
}
```

### Get transaction history

**GET** `/api/accounts/{accountId}/transactions`

Example response:

```json
[
  {
    "id": 1,
    "accountId": 1,
    "type": "TRANSFER_OUT",
    "amount": 300.00,
    "timestamp": "2026-08-24T17:33:35Z"
  }
]
```

## Validation and error handling

The application validates business and API-level constraints including:

- Missing amounts
- Zero amounts
- Negative amounts
- Missing transfer account IDs
- Insufficient balance
- Non-existent accounts
- Transfers to the same account

Jakarta Bean Validation is used to validate API requests before they reach the service layer.

Business exceptions are translated into consistent HTTP responses using `GlobalExceptionHandler`.

Example response for an account that does not exist:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Account not found with id: 99",
  "timestamp": "2026-08-24T17:33:35Z"
}
```

## Testing approach

The implementation was developed incrementally using a TDD-style workflow:

1. Write a test describing the expected behaviour.
2. Observe the failing test.
3. Implement the minimum behaviour required to make the test pass.
4. Refactor while keeping the test suite green.
5. Add edge-case and integration coverage as the design evolved.

The test suite covers:

- Account domain rules
- Deposit operations
- Withdrawal operations
- Transfer operations
- Invalid amounts
- Insufficient balances
- Missing accounts
- Same-account transfers
- Ledger creation
- REST API validation
- Exception-to-HTTP response mapping
- Transaction rollback behaviour
- Database integration

Tests include:

- Domain unit tests with JUnit 5
- Service unit tests with Mockito
- Controller tests with MockMvc
- Integration tests with Spring Boot and H2

Run the complete test suite with:

```bash
mvn clean test
```

At the time of submission, the complete test suite passes successfully.

## Running the application

### Prerequisites

- Java
- Maven, or the included Maven Wrapper

### Run with Maven

```bash
mvn spring-boot:run
```

Or, using the Maven Wrapper:

```bash
./mvnw spring-boot:run
```

The application starts by default on:

```text
http://localhost:8080
```

## H2 Console

The project uses an in-memory H2 database.

When the H2 console is enabled in the application configuration, it can be accessed at:

```text
http://localhost:8080/h2-console
```

Use the JDBC URL configured in `application.properties`.

Useful queries:

```sql
SELECT * FROM account;
SELECT * FROM transactions;
```

## Design decisions and trade-offs

### Database choice

H2 is used as the in-memory database for this kata to keep the application simple, self-contained, and easy to run without external infrastructure.

The persistence layer uses Spring Data JPA, so a production-grade relational database such as PostgreSQL or MySQL could be introduced with minimal changes to the application architecture.

For a production system, I would use a persistent relational database and extend the integration tests using Testcontainers.

### Domain model

Balance validation belongs to the `Account` domain object rather than the controller.

This keeps business invariants close to the state they protect and prevents controllers from becoming responsible for domain rules.

### Transaction boundaries

`@Transactional` is applied at the service layer because deposit, withdrawal, and especially transfer operations may involve multiple database writes that should succeed or fail as a single unit.

For example, a transfer should never leave the source account debited if the destination update or ledger persistence fails.

### DTOs

Request and response DTOs are used to keep the external REST API separate from the persistence/domain model.

Java records are used for DTOs because they are concise data carriers and avoid unnecessary boilerplate.

### Simplicity

The solution intentionally remains a single Spring Boot application.

Introducing microservices, messaging, distributed transactions, or other infrastructure would add complexity that is not required by the kata.

The focus is instead on correctness, domain modelling, tests, transaction consistency, and readable code.

## Limitations and future improvements

Given more time, I would consider:

- Optimistic or pessimistic locking for concurrent balance updates
- Idempotency keys to protect against duplicate transaction requests
- Authentication and authorization
- Currency support
- Pagination for large transaction histories
- Database migrations using Flyway or Liquibase
- PostgreSQL or MySQL for persistent storage
- Testcontainers for database integration testing
- OpenAPI/Swagger documentation
- Structured logging and correlation IDs
- Metrics and observability
- Additional concurrency and load testing

These were intentionally left out to keep the kata focused on the core requirements and maintain a simple, understandable implementation.