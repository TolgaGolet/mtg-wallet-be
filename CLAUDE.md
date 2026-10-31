# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Spring Boot 3.1.5 / Java 21 REST backend for MTG Wallet (personal finance: accounts, transactions, payees, categories, profit/loss). Frontend lives in a separate repo (`TolgaGolet/mtg-wallet-fe`). PostgreSQL via JPA, Lombok, MapStruct, JWT + TOTP 2FA, Mailjet for email.

## Commands

Use the Maven wrapper (`mvnw.cmd` on Windows, `./mvnw` elsewhere):

```bash
./mvnw spring-boot:run                       # run (profile from application.properties)
./mvnw spring-boot:run -Dspring-boot.run.profiles=disabled-security   # run with auth disabled
./mvnw clean package -DskipTests             # build jar (what the Dockerfile does)
./mvnw test                                  # all tests
./mvnw test -Dtest=MtgWalletBeApplicationTests            # single test class
./mvnw test -Dtest=MtgWalletBeApplicationTests#contextLoads   # single test method
```

There is no linter configured. The only test is a `@SpringBootTest` context-load test, which needs a reachable database from the active profile.

The server runs under context path `/api` (e.g. `http://localhost:8080/api/transaction/search`). Swagger UI is enabled in the test and disabled-security profiles.

## Profiles & configuration

- `application.properties` sets `spring.profiles.active` (default `test`), the `/api` context path, and `server.error.include-message`.
- `test` — local dev against a remote Render Postgres; `prod` — every value comes from env vars (`PROD_DB_HOST`, `PROD_JWT_SECRET`, etc.); `disabled-security` — swaps `SecurityConfig` for `DisabledSecurityConfig` (all requests permitted) via `@Profile`.
- Custom properties live under `mtgWallet.*` (`security.*` for JWT/rate limiting, `mailjet.*`, `applicationConfig.timeZone`, `applicationConfig.defaultPageSize`, `frontend.url`).
- The JVM default timezone is set from `mtgWallet.applicationConfig.timeZone` at startup in `MtgWalletBeApplication`.
- Schema is managed by Hibernate `ddl-auto=update`; there are no migration files.
- Gotcha: `SecurityParams.DEFAULT_PAGE_SIZE` has a `@Value` annotation on a `static final` field, which Spring ignores — the page size is effectively hard-coded to 10.

## Architecture

Layered, one vertical slice per domain (Account, Category, Payee, Transaction, Home, Settings, User):

`api/*Api` (`@RestController`) → `service/*Service` interface + `*ServiceImpl` (`@Transactional`) → `repository/*Repository` (Spring Data JPA, `JpaSpecificationExecutor`) → `entity/*`.

- **Request/response/DTO split**: controllers take `api/request/*Request` (validated with `@Validated`), services work on `service/dto/*Dto`, controllers return `api/response/*Response`. Conversions go through MapStruct mappers in `mapper/` (`componentModel = "spring"`); entity↔DTO maps `user.id` ↔ `userId`.
- **Search endpoints** are `POST /<domain>/search` with a request body + `pageNo` query param; filtering is built with JPA Criteria in `specification/*Specification.search(...)`. Name searches lowercase with the Turkish locale (`Locale.forLanguageTag("tr")`).
- **Per-user data isolation is done in services, not security config**: services call `userService.getCurrentLoggedInUser()` (from the `SecurityContext`) to scope queries/set `userId`, and `userService.validateUserIdIfItsTheCurrentUser(entity.getUser().getId())` before update/delete. New endpoints touching user data must do the same.
- **Account balances are denormalized**: `TransactionServiceImpl` updates account balances on create, and rolls back the old effect before reapplying on update/delete. Transactions also store `sourceAccountNewBalance`/`targetAccountNewBalance` snapshots. TRANSFER requires a distinct target account with the same currency; EXPENSE/INCOME must not have a target. `payeeId == -1` on transaction create/update means "create a new payee from `payeeName`".
- **Soft delete & limits**: entities have a `Status` (ACTIVE/INACTIVE/DELETED); deletes set `DELETED`. Per-user caps are constants in the service impls (`MAX_ALLOWED_ACCOUNT_COUNT`, `MAX_ALLOWED_CATEGORY_COUNT`, `MAX_ALLOWED_PAYEE_COUNT`).
- **Enums** (`enums/`) carry a short DB `value` + display `label`, are serialized as JSON objects (`@JsonFormat(shape = OBJECT)`), resolved via a static `of(value)` factory, and persisted through `@Converter(autoApply = true)` classes in `enums/converter/`. Requests send enum `value`s (e.g. `typeValue`). "Screen enum" endpoints (`GET /<domain>/create/enums`) return dropdown data for the frontend.
- **Auditing**: entities extend `entity/auditing/Auditable` (created/modified by/date), populated via `@EnableJpaAuditing` + `AuditAwareImpl`.
- **Service logging**: annotate service methods with `@Loggable`; `aspect/LoggableAspect` persists request/response/status/timing to the `ServiceLog` table.

## Errors

- Throw the checked `MtgWalletGenericException` with a message from `exception/enums/GenericExceptionMessages` for expected errors. To add a new error, add an enum constant there.
- `MtgWalletExceptionHandler` (`@ControllerAdvice`) logs every exception to `ServiceLog` and returns a plain-string body ending with the log ID. HTTP status for generic exceptions is chosen by a `switch` on the `GenericExceptionMessages` constant in that handler (default 500) — edit that switch to map a new error to a different status. Unexpected exceptions never expose their message.

## Security

- Stateless JWT (`security/service/JwtService`, `security/filter/JwtAuthenticationFilter`) with access + refresh tokens stored in `UserToken`; `RateLimitingFilter` (Caffeine-backed) runs before the JWT filter.
- Public paths, CORS allowed origins, and the bearer prefix are constants in `security/SecurityParams` — add new unauthenticated endpoints to `AUTH_WHITELIST` there.
- Auth flows (register, email verification, password reset, account recovery, TOTP 2FA) live in `security/api` + `security/service`; tokens for those flows are separate entities (`EmailVerificationToken`, `PasswordResetToken`, `AccountRecoveryToken`).
- Method-level authorization uses `@PreAuthorize` (`@EnableMethodSecurity`); `AccessDeniedException` maps to 403.

## Related repos

- **Frontend**: `../mtg-wallet-fe` (React 18 + Mantine, CRA dev server on `http://localhost:3000`). It has its own `CLAUDE.md`. It calls this API through `REACT_APP_*` base URL/paths in its `.env` and relies on the `POST <entity>/search` + `pageNo` and Spring page (`content`, `totalPages`) conventions described above.
- Changing a request/response shape, an endpoint path, an enum's `value`/`label`, or an error message the UI displays needs a matching change in the frontend. New frontend origins go in `SecurityParams.CORS_ALLOWED_ORIGINS`.
