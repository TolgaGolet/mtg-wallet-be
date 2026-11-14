# MTG Wallet BE

[![Deployment Status](https://img.shields.io/badge/status-live-brightgreen)](https://mtgwallet-be.onrender.com)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-green)](https://spring.io/projects/spring-boot)

MTG Wallet BE is the backend application for [MTG Wallet FE](https://github.com/TolgaGolet/mtg-wallet-fe), designed to keep
track of your daily transactions, account balances, and much more in the future.

## 🌟 Features

- **Transaction Tracking**: Record, categorize and view transaction history
- **Account Balances**: Keep track of your accounts' balances easily
- **Profits and Losses**: Calculate your profits and losses by different intervals
- **Account Security**: Secure account system with Spring Security JWT
- **Email Verification**: Email verifications while registering and account recovery processes
- **Two-Factor Authentication (2FA)**: Secure login system with a TOTP
- **Rate Limiting**: Rate limiting for preventing abuse
- **Auditing:** Easy auditing entity listener
- **Service Logging:** Aspect oriented service logging with an annotation
- **Exception Logging:** Server errors are logged automatically for debugging
- **Health Monitoring:** Health can be monitored through [status page](https://kk23vgsq.status.cron-job.org/)

## 🚀 Live Demo

Live demo at: https://mtgwallet.onrender.com

## 🛠️ Technology Stack

- **Backend**: Java 21, Spring Boot 4.1 (Spring MVC, Data JPA / Hibernate 7, Jackson 3), MapStruct, Lombok
- **Security**: Spring Security, JWT (jjwt), TOTP 2FA, Rate Limiting (Caffeine)
- **Database**: PostgreSQL (H2 in memory for tests)
- **Email**: Mailjet
- **Build Tool**: Maven (wrapper included)

## 🚦 Getting Started

### Prerequisites

- Java 21
- PostgreSQL (not needed for the tests)

### Configuration

Tracked properties files only hold `${ENV}` placeholders. For local runs:

1. Copy `.env.properties.example` to `.env.properties` (gitignored) and fill in the values: `ACTIVE_PROFILE`
   (`test` for local development), database, JWT, Mailjet and frontend URL settings.
2. In production, set the same values as environment variables (the `prod` profile reads `PROD_*` variables).

### Run and test

```bash
./mvnw spring-boot:run                       # run with the profile from ACTIVE_PROFILE
./mvnw spring-boot:run -Dspring-boot.run.profiles=disabled-security   # run with auth disabled
./mvnw test                                  # all tests, offline (H2)
./mvnw clean package -DskipTests             # build the jar
```

The API is served under `/api` (for example `http://localhost:8080/api/transaction/search`). See
[CLAUDE.md](CLAUDE.md) for the architecture, conventions and more commands.
