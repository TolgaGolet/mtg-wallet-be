# MTG Wallet BE

[![Deployment Status](https://img.shields.io/badge/status-live-brightgreen)](https://mtgwallet-be.onrender.com)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-green)](https://spring.io/projects/spring-boot)

MTG Wallet BE is backend application for [MTG Wallet FE](https://github.com/TolgaGolet/mtg-wallet-fe) designed to keep
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
- **Exception Logging:** Exceptions are logged automatically for debugging
- **Health Monitoring:** Health can be monitored through [status page](https://kk23vgsq.status.cron-job.org/)

## 🚀 Live Demo

Live demo at: https://mtgwallet.onrender.com

## 🛠️ Technology Stack

- **Backend**: Java 21, Spring Boot 3.1.5
- **Security**: Spring Security, JWT, 2FA, Rate Limiting
- **Database**: PostgreSQL
- **Email**: Spring Mail
- **Build Tool**: Maven

## 🚦 Getting Started

### Prerequisites

- Java 21
- PostgreSQL

### Configuration

1. Set the active profile in `application.properties`:

    ```properties
    spring.profiles.active=test
    ```

2. Configure your database and security parameters in application-{profile}.properties