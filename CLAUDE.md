# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Spring Boot 4.1 / Java 21 REST backend for MTG Wallet (personal finance: accounts, transactions, payees, categories, profit/loss). Frontend lives in a separate repo (`TolgaGolet/mtg-wallet-fe`). PostgreSQL via JPA (Hibernate 7), Lombok, MapStruct, JWT (jjwt 0.12) + TOTP 2FA, Mailjet for email. JSON is Jackson 3 with `spring.jackson.use-jackson2-defaults=true` so output matches what the frontend was built against (`JsonContractTest` pins it). Dependabot (`.github/dependabot.yml`) opens weekly update PRs for Maven and the Dockerfile.

## Commands

Use the Maven wrapper (`mvnw.cmd` on Windows, `./mvnw` elsewhere):

```bash
./mvnw spring-boot:run                       # run (profile from application.properties)
./mvnw spring-boot:run -Dspring-boot.run.profiles=disabled-security   # run with auth disabled
./mvnw clean package -DskipTests             # build jar (the Dockerfile runs the same wrapper; image runs as non-root with a HEALTHCHECK on /api/actuator/health)
./mvnw test                                  # all tests (offline: H2, no DB needed); coverage in target/site/jacoco/index.html
./mvnw test -Dtest=MtgWalletBeApplicationTests            # single test class
./mvnw test -Dtest=MtgWalletBeApplicationTests#contextLoads   # single test method
```

There is no linter configured. Tests are JUnit 5 + Mockito unit tests plus a `@SpringBootTest` context-load test that runs on the `unittest` profile (in-memory H2, `src/test/resources/application-unittest.properties`), so the build works offline. Add `@ActiveProfiles("unittest")` to any new Spring context test.

The server runs under context path `/api` (e.g. `http://localhost:8080/api/transaction/search`). Swagger UI is enabled in the test and disabled-security profiles.

## Profiles & configuration

- `application.properties` sets `spring.profiles.active=${ACTIVE_PROFILE}`, the `/api` context path, and `server.error.include-message`.
- Tracked properties only hold `${ENV}` placeholders. Local values live in the gitignored `.env.properties` (template: `.env.properties.example`), imported via `spring.config.import=optional:file:.env.properties`. Never put real values in `src/main/resources`.
- `test` — local dev against a remote Render Postgres; `prod` — every value comes from env vars (`PROD_DB_HOST`, `PROD_JWT_SECRET_KEY`, etc.), `show-sql` off; `disabled-security` — swaps `SecurityConfig` for `DisabledSecurityConfig` (all requests permitted) via `@Profile`.
- Custom properties live under `mtgWallet.*` (`security.*` for JWT/rate limiting, `mailjet.*`, `applicationConfig.timeZone`, `applicationConfig.defaultPageSize` (bound by `config/ApplicationConfigProperties`), `serviceLog.retentionDays` (default 180), `frontend.url`).
- The JVM default timezone is set from `mtgWallet.applicationConfig.timeZone` at startup in `MtgWalletBeApplication`.
- Schema is managed by Hibernate `ddl-auto=update`; there are no migration files. `update` never alters existing constraints, so adding a constant to an `@Enumerated(STRING)` field needs a manual `ALTER TABLE <t> DROP CONSTRAINT <t>_<col>_check;` on every DB (Hibernate's CHECK still lists only the old values).

## Architecture

Layered, one vertical slice per domain (Account, Category, Payee, Transaction, Home, Settings, User):

`api/*Api` (`@RestController`) → `service/*Service` interface + `*ServiceImpl` (`@Transactional`) → `repository/*Repository` (Spring Data JPA, `JpaSpecificationExecutor`) → `entity/*`.

- **Request/response/DTO split**: controllers take `api/request/*Request` (validated with `@Validated`; a request class with Lombok `@Builder` must also have `@NoArgsConstructor` + `@AllArgsConstructor`, because Jackson 3 can't deserialize it otherwise; `JsonContractTest` checks every `@RequestBody` type), services work on `service/dto/*Dto`, controllers return `api/response/*Response`. Conversions go through MapStruct mappers in `mapper/` (`componentModel = "spring"`); entity↔DTO maps `user.id` ↔ `userId`.
- **Search endpoints** are `POST /<domain>/search` with a request body + `pageNo` query param; filtering is built with JPA Criteria in `specification/*Specification.search(...)`. Name searches lowercase with the Turkish locale (`Locale.forLanguageTag("tr")`).
- **Per-user data isolation is done in services, not security config**: services call `userService.getCurrentUserId()` / `getCurrentLoggedInUser()` to scope queries/set `userId`, and `userService.validateUserIdIfItsTheCurrentUser(entity.getUser().getId())` before update/delete. New endpoints touching user data must do the same. These read the `WalletUser` principal that `JwtAuthenticationFilter` already loaded (no query); `getCurrentUserEntity()` gives the managed row when a service must change the user.
- **User changes are intent-based** (`setDefaultPayeeFor*`, `changePassword`, `updateProfile`, `anonymizeCurrentUser` on `UserService`): load the managed user, set the field, let dirty checking save it. There is no "full user" DTO; never return the password hash or TOTP secret from an endpoint.
- **Limit and name checks** use `countByUserIdAndStatus` and names-only queries (`findNames...`), compared in Java with `Names.sameName`, not full entity lists. New-user defaults are created in two batched inserts by `DefaultDataService`.
- **Account balances are denormalized**: `TransactionServiceImpl` updates account balances on create, and rolls back the old effect before reapplying on update/delete. Transactions also store `sourceAccountNewBalance`/`targetAccountNewBalance` snapshots. `Account` has `@Version` (optimistic locking), so concurrent writes to one account fail with 409 (handled in `MtgWalletExceptionHandler`) instead of losing updates; keep `version` flowing through `AccountDto`. All balance changes go through `applyBalanceEffect(type, source, target, amount, APPLY|ROLLBACK)`. TRANSFER requires a distinct target account with the same currency; EXPENSE/INCOME must not have a target. `payeeId == TransactionCreateRequest.NEW_PAYEE_ID` (`-1`, sent by the frontend) on transaction create/update means "create a new payee from `payeeName`".
- **Soft delete & limits**: entities have a `Status` (ACTIVE/INACTIVE/DELETED); deletes set `DELETED`. Per-user caps live in `service/DomainLimits` (not in `*ServiceImpl`, so callers depend on no implementation). Name uniqueness is case-insensitive via `util/Names.sameName` (Turkish locale); account/category/payee names share `api/request/ValidationPatterns.NAME_REGULAR_EXPRESSION`, mirrored in the frontend forms.
- **Categories**: one level of nesting only. Parent rules (exists and ACTIVE, not self, no grandparent, same `transactionType`) are in `CategoryServiceImpl.validateParent`; the stored `isParent` flag is re-derived from active children (`refreshIsParent`) whenever a parent changes. Deleting a category with active children is refused (409).
- **Not found** always throws `*_NOT_FOUND` (`getAccountById`, `getPayee`, `getCategory`, ...); services never return null for a missing entity.
- **Enums** (`enums/`) carry a short DB `value` + display `label`, are serialized as JSON objects (`@JsonFormat(shape = OBJECT)`), resolved via a static `of(value)` factory, and persisted through `@Converter(autoApply = true)` classes in `enums/converter/`. Requests send enum `value`s (e.g. `typeValue`). "Screen enum" endpoints (`GET /<domain>/create/enums`) return dropdown data for the frontend as plain lists (not `Page`); only `POST /<domain>/search` returns Spring pages.
- **Auditing**: entities extend `entity/auditing/Auditable` (created/modified by/date), populated via `@EnableJpaAuditing` + `AuditAwareImpl`.
- **Service logging**: annotate service methods with `@Loggable`; `aspect/LoggableAspect` (outermost advice, `@Order(HIGHEST_PRECEDENCE)`) persists request/response/status/timing to the `ServiceLog` table via `ServiceLogWriter` (own `REQUIRES_NEW` transaction, never throws). `ServiceLogCleanupJob` deletes rows older than `mtgWallet.serviceLog.retentionDays` daily. Use `@Loggable(logArgs = false)` (or `logResult = false`) when args/results hold tokens or PII; `safeArgs`/`emailArgs` keep chosen args (emails masked). E.g. `MailServiceImpl` logs masked recipient + subject, never the body with reset/verification links.

## Errors

- Throw the checked `new MtgWalletGenericException(GenericExceptionMessages.X)` for expected errors. Each enum constant carries its message **and HTTP status**; to add an error, add a constant with both.
- Never use 401/403 for errors caused by user input (wrong password, TOTP code): the frontend's axios interceptor treats any 401/403 as "refresh token and retry". Use 400.
- `MtgWalletExceptionHandler` (`@ControllerAdvice`) returns a plain-string body `"An error occurred: <message>. <logId>"` and logs to `ServiceLog` (via `ServiceLogWriter`, so a DB outage can't break the response). Bean-validation errors, malformed JSON and unknown enum values (`InvalidEnumValueException`, thrown by the enums' `of()`) return 400; any other `IllegalArgumentException` is a 500 (validation errors without a `ServiceLog` row). Unexpected exceptions never expose their message.

## Security

- Stateless JWT (`security/service/JwtService`, `security/filter/JwtAuthenticationFilter`). Tokens carry a `typ` claim (`access`/`refresh`); both are stored in `UserToken` (`tokenType` BEARER/REFRESH) and must match an active row of their own type, so login, refresh, logout and account deletion revoke all of a user's tokens. Whitelisted endpoints that read a bearer token (refresh, TOTP setup/enable/disable) must use `AuthenticationService.resolveUserFromBearer`, which enforces token type and revocation; `RateLimitingFilter` (Caffeine-backed, keyed by `request.getRemoteAddr()`) runs before the JWT filter. Don't read `X-Forwarded-For` directly: `server.forward-headers-strategy=native` resolves the real client IP only from trusted proxies (Render); prod reads Cloudflare's `True-Client-IP` (`application-prod.properties`), since XFF would yield a shared Cloudflare edge IP.
- Public paths, CORS allowed origins, and the bearer prefix are constants in `security/SecurityParams` — add new unauthenticated endpoints to `AUTH_WHITELIST` there.
- Auth flows (register, email verification, password reset, account recovery, TOTP 2FA) live in `security/api` + `security/service`; tokens for those flows are separate entities (`EmailVerificationToken`, `PasswordResetToken`, `AccountRecoveryToken`).
- Method-level authorization uses `@PreAuthorize` (`@EnableMethodSecurity`); `AccessDeniedException` maps to 403.

## Related repos

- **Frontend**: `../mtg-wallet-fe` (React 18 + Mantine, CRA dev server on `http://localhost:3000`). It has its own `CLAUDE.md`. It calls this API through `REACT_APP_*` base URL/paths in its `.env` and relies on the `POST <entity>/search` + `pageNo` and Spring page (`content`, `totalPages`) conventions described above.
- `POST /transaction/search` returns `TransactionListResponse` (same nested field names the UI reads, without internal ids like `userId`).
- Changing a request/response shape, an endpoint path, an enum's `value`/`label`, or an error message the UI displays needs a matching change in the frontend. New frontend origins go in `SecurityParams.CORS_ALLOWED_ORIGINS`.
