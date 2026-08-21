# CinemaBooking Backend

Spring Boot backend for a full-stack cinema ticket booking platform. The API covers movie and cinema operations, showtimes, seat availability, temporary seat holds, bookings, payments, QR tickets, staff check-in, user access control, analytics, and audit trails.

This repository is the backend part of the project. The React client is maintained separately in [`cinema-booking-client`](https://github.com/Thandinh/cinema-booking-client).

## Why this project is worth reviewing

- Java 21 and Spring Boot 3.5 with a clear `Controller -> Service -> Repository` structure.
- 100+ REST endpoints grouped by business domain.
- Concurrency-safe seat holding for multiple users selecting the same showtime.
- JWT access/refresh sessions with rotation, revocation, permission-based RBAC, and cinema-scoped staff access.
- VNPay and SePay/VietQR payment adapters with callback/webhook reconciliation.
- QR ticket generation, email delivery, and staff check-in from a camera or QR image.
- Flyway-managed PostgreSQL schema, focused indexes/constraints, Caffeine caching, and JPA projections.
- Unit, integration, security, payment, and Testcontainers-based database tests.

## Core workflows

### Booking and seat availability

1. A user selects a showtime and requests a temporary hold for one or more seats.
2. The backend validates availability inside a transaction and changes the seat state to `HOLD` with an expiry time.
3. The user creates a `PENDING` booking and completes payment before the hold expires.
4. A successful payment moves the booking and payment to `SUCCESS`, marks the seats as `BOOKED`, creates QR tickets, and sends the ticket email.
5. Failed, cancelled, or expired flows release the seats. A scheduler also cleans up expired holds and pending bookings.

The hold path combines pessimistic locking, optimistic locking with `@Version`, conditional updates, and unique/partial-unique database constraints. Seat events are published after commit over STOMP/WebSocket so other clients can update the same seat map without a page refresh.

### Payments

- **VNPay**: sandbox payment URL, checksum validation, amount reconciliation, and callback processing.
- **SePay/VietQR**: QR payment data, API key/HMAC verification, transfer amount/content reconciliation, and webhook processing.
- **Payment events**: callback attempts and state transitions are persisted for reconciliation and auditability.
- **Refunds**: the application records and manages an internal refund request/result workflow. It does not claim an automatic provider refund API.
- **MoMo**: configuration is present but disabled by default until valid credentials are supplied.

### Authentication and authorization

- Username/password login with BCrypt password hashing.
- JWT access and refresh tokens with refresh-token rotation and revocation.
- Google Identity Services ID token verification on the server.
- Permission-based RBAC for `ADMIN`, `STAFF`, and `USER` capabilities.
- Staff cinema scope checks so staff can operate only on assigned cinemas.
- Rate limiting for password/Google login, refresh-token requests, and seat-hold requests.
- Authentication and administration audit logs.

Rate limiting currently uses a fixed-window, in-memory service. It is appropriate for a single application instance; a multi-instance deployment should move the counter store to a shared system such as Redis or an API gateway.

### Ticket and check-in

- Signed ticket QR payloads and QR image rendering with ZXing.
- Email tickets rendered from server-side templates.
- Staff check-in from a live camera or uploaded QR image.
- Validation of ticket status, booking status, assigned cinema, showtime, check-in window, and ticket reuse.

## Architecture

```text
com.cinema.booking
|-- controller/       REST endpoints and request binding
|-- service/          application interfaces and implementations
|-- repository/       Spring Data JPA repositories and projections
|-- entity/           PostgreSQL-backed domain entities
|-- dto/              request/response contracts
|-- security/         JWT, authentication, authorization, rate limits, schedulers
|-- websocket/        STOMP broker configuration and seat events
|-- payment/          payment gateway adapters and reconciliation
|-- audit/            administration audit interception
|-- exception/        error codes and unified exception responses
|-- configuration/    application, cache, mail, and API configuration
`-- util/             shared validation and security utilities
```

### Main API areas

| Area | Base path |
| --- | --- |
| Authentication | `/auth` |
| Movies | `/api/v1/movies` |
| Cinemas | `/api/v1/cinemas` |
| Rooms and seats | `/api/v1/rooms`, `/api/v1/seats` |
| Showtimes | `/api/v1/showtimes` |
| Bookings | `/api/v1/bookings` |
| Payments | `/api/v1/payments` |
| Tickets | `/api/v1/tickets` |
| Promotions | `/api/v1/promotions` |
| Users | `/api/v1/users` |
| Analytics | `/api/v1/analytics` |
| Audit logs | `/api/v1/admin/audit-logs`, `/api/v1/admin/auth-audit-logs` |

## Technology stack

- **Language/runtime:** Java 21
- **Backend:** Spring Boot 3.5, Spring Web, Spring Data JPA, Hibernate, Spring Security, OAuth2 Resource Server, Bean Validation
- **Security:** Nimbus JOSE + JWT, BCrypt, Google ID token verification
- **Database:** PostgreSQL 15, Flyway, JPA indexes/constraints, JSONB where appropriate
- **Caching and realtime:** Caffeine, STOMP over native WebSocket
- **Payments and messaging:** VNPay sandbox, SePay/VietQR integration, SMTP, Thymeleaf
- **QR:** ZXing
- **API documentation:** Springdoc OpenAPI / Swagger UI
- **Testing:** JUnit 5, Spring Boot Test, Spring Security Test, Testcontainers PostgreSQL
- **Build and local infrastructure:** Maven Wrapper, Docker Compose

## Prerequisites

- Java 21+
- Docker Desktop (required for PostgreSQL and Testcontainers)
- Git
- Maven is optional; the repository includes `mvnw.cmd`
- `psql`, DBeaver, or pgAdmin is optional for loading sample data

## Get started locally

After cloning the repository, open a terminal in the backend directory and create a local environment file from the template. The values below assume the default Docker/PostgreSQL setup; change them when your local ports or credentials are different.

```powershell
Copy-Item .env.example .env
```

On macOS/Linux, use `cp .env.example .env` instead.

For a local demo, update at least the database credentials and JWT secret in `.env`:

```env
DB_NAME=cinema_booking
DB_USER=cinema_user
DB_PASSWORD=change-me
DB_HOST=localhost
DB_PORT_EXTERNAL=5433
DB_PORT_INTERNAL=5432

SERVER_PORT=8080
APP_FRONTEND_URL=http://localhost:5173
APP_BACKEND_URL=http://localhost:8080

JWT_SECRET=replace-with-a-long-random-secret-at-least-32-characters
SQL_INIT_MODE=never
FLYWAY_ENABLED=true
```

Never commit a real `.env` file, payment secret, SMTP password, Google credential, or production JWT secret.

Start the local PostgreSQL service with Docker:

```powershell
docker compose up -d
```

The default compose configuration exposes PostgreSQL on `localhost:5433` and stores data in the `pgdata` Docker volume.

With the database running, start the API:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend is available at `http://localhost:8080`.

Flyway runs automatically on startup and validates the JPA schema after migrations are applied. New databases should not use `database.sql` as the normal startup path.

Once the application has started, the API documentation is available at:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Example seat-hold request:

```http
POST /api/v1/bookings/hold
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "showtimeId": "<showtime-uuid>",
  "seatIds": ["<seat-uuid-a1>", "<seat-uuid-a2>"]
}
```

UUID values and `holdUntil` are generated from the current database state. The response is returned through the common `ApiResponse<HoldSeatResponse>` envelope.

## Database and sample data

Flyway migrations are stored in:

```text
src/main/resources/db/migration
```

The repository also contains SQL utilities for local demos:

| File | Use |
| --- | --- |
| `database/database.sql` | Manual clean schema/reset when explicitly needed |
| `database/mock-data.sql` | Movies, cinemas, rooms, seats, showtimes, promotions, demo users, and booking/payment states |
| `database/rbac-permissions.sql` | Role and permission synchronization |

To load sample data into the Docker database after the application has started once:

```powershell
psql -h localhost -p 5433 -U cinema_user -d cinema_booking -f database/mock-data.sql
```

The application bootstrap admin is disabled by default. To create the first admin, temporarily set `APP_BOOTSTRAP_ADMIN_ENABLED=true` with a non-default username/password, start the application once, then disable it again.

## Integrations

### Google login

Configure the same client ID on the backend and frontend:

```env
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
VITE_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
```

The client sends a Google ID token to `/auth/google`; the backend verifies the token before issuing the application's JWT pair. Add `http://localhost:5173` to the authorized JavaScript origins in Google Cloud Console.

### Email

Mailtrap is suitable for local development:

```env
MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=your-mailtrap-username
MAIL_PASSWORD=your-mailtrap-password
```

Email is used for account verification, password reset, and paid ticket delivery.

### Payment callbacks

Payment return/webhook URLs must be reachable by the provider. For local mobile demonstrations, expose the appropriate frontend or backend route through an HTTPS tunnel and update `VNP_RETURN_URL`, `APP_BACKEND_URL`, and provider settings accordingly.

## Tests and quality checks

Run the full backend test suite:

```powershell
.\mvnw.cmd test
```

The integration suite uses Testcontainers PostgreSQL, runs Flyway migrations, and isolates test data from the local `.env` database. Docker Desktop must be running for integration tests.

Useful focused commands:

```powershell
.\mvnw.cmd -Dtest=FixedWindowRateLimitServiceTest test
.\mvnw.cmd -Dtest=BookingWorkflowIntegrationTest test
.\mvnw.cmd -Dtest=BookingPaymentSecurityIntegrationTest test
.\mvnw.cmd -Dtest=PaymentCallbackIntegrationTest test
.\mvnw.cmd clean package
```

## Related repository

- Frontend: [`Thandinh/cinema-booking-client`](https://github.com/Thandinh/cinema-booking-client)
- Backend: [`Thandinh/cinema-booking-system`](https://github.com/Thandinh/cinema-booking-system)

## Deployment notes

This repository is designed for local development, testing, and portfolio demonstration. Before a production deployment:

- use a managed PostgreSQL instance and a shared cache/rate-limit store for multiple backend replicas;
- put the API behind HTTPS and a reverse proxy/API gateway;
- store JWT, payment, OAuth, and SMTP secrets in a secret manager;
- configure real provider callback URLs and verify webhook signatures;
- add backups, monitoring, log retention, and a CI/CD pipeline;
- do not use `mock-data.sql` or default demo credentials in production.
