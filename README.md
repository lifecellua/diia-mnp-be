# MNP for Diia - backend service

This is the backend service that lets people in Ukraine move their mobile number to
lifecell directly from **Diia**, the national digital government app. The user opens Diia,
fills in a short form, signs the contract, and the number is transferred. No shop visit,
no paperwork.

lifecell was the **first mobile operator in Ukraine** to offer this. The service is in
production and runs inside the Diia platform.

> Upstream repository: [lifecellua/diia-mnp-be](https://github.com/lifecellua/diia-mnp-be) ·
> Licence: EUPL-1.2


## What the service does

A number transfer is not one request. It is a process that can take **up to 40 days** and
involves several parties: the user, Diia, lifecell, the other mobile operator, and government
registers.

The service handles the whole process:

1. Check if the number can be moved
2. Read the user's ID documents from government registers (through Diia)
3. Show tariffs and collect the user's choice
4. Confirm the phone number with a one-time password
5. Generate the contract and get the user's signature
6. Create the transfer request with the operator
7. Track the request and tell the user what is happening

The user can also see all their requests, order a SIM card, or cancel a transfer.

---

## How it is built

### A state machine that lives in the database

The process is long and the user can close the app at any time. So the service does not keep
the process in memory. It is a **state machine with 24 stages and 31 transitions**, and the
current stage is saved in PostgreSQL.

- Every stage has one handler class. There are **24 handlers with 62 methods** -
  `enter`, `form` and `leave`.
- Every change is also written to a history table, so we can see the full path later.
- If the user comes back, the service reads the last stage from the database and continues.
  A session is valid for **15 minutes**.
- If a user starts a new process while an old one is still open, the old one is cancelled.
  Only one active process per user.

This means the service itself is stateless. Any instance can continue any user's process.

### One contract, two protocols

There is a single `.proto` file. It defines **28 methods**. The same 28 methods are available
as **gRPC** and as **REST**, because the proto file uses `google.api.http` annotations and the
gateway translates between them.

The proto file is also published as a Maven artifact, so other teams use the same contract
instead of writing their own copy.

### Talking to the telecom core

The service sends **12 operations** to the lifecell backend over **RabbitMQ**. It is a
request/reply pattern: the service sends a message with a correlation ID and waits for the
answer on the same queue.

- Queues are **quorum queues**, so messages survive a node failure.
- Retry is 3 attempts with exponential backoff: 5 seconds, then ×3, up to 5 minutes.

### Reading documents from government registers

The service never talks to government registers directly. It asks Diia's document services,
and Diia talks to the registers. Five document types are supported:

| Document | Register |
| --- | --- |
| Internal passport | State Migration Service |
| Foreign passport | State Migration Service |
| Driver licence | Ministry of Internal Affairs |
| Pension card | Pension Fund |
| Taxpayer card (RNOKPP) | State Tax Service |

If the internal passport is missing, the service tries the foreign passport, then the driver
licence.

### Keeping two systems in sync

The service and the operator system are separate. Messages can be lost. A pod can restart in
the middle of a request.

So there is a **daily reconciliation job**. Every day at 18:00 it compares both sides:

- requests that exist here but not at the operator → marked as missing
- requests that exist at the operator but not here → marked as extra

**ShedLock** makes sure only one instance runs the job, even when several instances are up.
This is consistency by reconciliation, not by distributed transaction.

---

## Rules that shape the design

Number portability in Ukraine is regulated. Some numbers in the code are legal limits, not
technical choices:

| Rule | Value |
| --- | --- |
| Wait before you can move the number again | 30 days |
| How long a request stays active | 40 days |
| Window for finding lost requests | 41 days |
| User session | 15 minutes |

The operator reports **18 different request states**. The service maps them to 10 messages
that a normal person can understand.

---

## Tech stack

**Java 21** · **Spring Boot 3.4** · gRPC and Protobuf · RabbitMQ ·
PostgreSQL with Spring Data JDBC · Flyway · ShedLock · Micrometer and Prometheus · Gradle

Data access uses **Spring Data JDBC**, not JPA. The data model is small - 6 tables - and
explicit SQL was easier to reason about than lazy loading.

IDs are **UUIDv7**, so they are unique and still sort by time. That keeps index writes local
instead of random.

---

## Repository layout

```
proto/          the API contract - gRPC and REST come from this file
src/main/java/
  model/fsm/    state machine: Stage, State, Context, Session, Request, Response
  service/      one handler class per stage, plus integration services
  model/db/     entities and repositories
src/main/resources/
  db/           Flyway migration
  forms/        UI form templates for the Diia design system
  data/         tariffs and request-state descriptions
docs/           diagrams and notes
```

## Links

- Upstream: [lifecellua/diia-mnp-be](https://github.com/lifecellua/diia-mnp-be)
- Android client: [lifecellua/diia-mnp-android](https://github.com/lifecellua/diia-mnp-android)
- Diia: [diia.gov.ua](https://diia.gov.ua)
- lifecell: [lifecell.ua](https://www.lifecell.ua)

