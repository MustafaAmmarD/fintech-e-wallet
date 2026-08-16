# 🏦 FinTech E-Wallet Backend API

A secure, robust RESTful API built with Java and Spring Boot to power the E-Wallet ecosystem. Features JWT authentication, KYC verification, and full wallet transaction processing.

## 🏗️ System Architecture

This backend serves as the core engine for both the Mobile App and the Admin Dashboard.

- **Authentication**: JWT-based stateless authentication with Spring Security.
- **Database**: PostgreSQL for robust relational data storage.
- **Containerization**: Fully Dockerized with `docker-compose` for rapid deployment.
- **Storage**: Local file system for KYC document uploads.

## ✨ Key Features

- **User Authentication**: Register, Login, and secure Token Refresh.
- **Wallet Operations**: Balance inquiry, Deposits, Withdrawals.
- **Peer-to-Peer Transfers**: Secure money transfers with recipient verification.
- **KYC System**: Document upload and verification workflows.
- **Admin Endpoints**: Manage users, freeze accounts, view system-wide stats.

## 🛠️ Technology Stack

- **Language**: Java 17+
- **Framework**: Spring Boot 3.x
- **Security**: Spring Security, JWT
- **Database**: PostgreSQL, Spring Data JPA / Hibernate
- **Deployment**: Docker, Docker Compose
- **Build Tool**: Maven

## 🚀 Getting Started

1. Clone this repository:
   ```bash
   git clone https://github.com/MustafaAmmarD/fintech-e-wallet.git
   ```
2. Make sure you have Docker and Docker Compose installed.
3. Start the application and database:
   ```bash
   docker-compose up -d
   ```
4. The API will be available at `http://localhost:8080`.
5. Access the Swagger UI API documentation at `http://localhost:8080/swagger-ui.html`.

## 📄 License

This project is licensed under the MIT License.
