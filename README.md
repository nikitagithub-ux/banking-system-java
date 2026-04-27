# Banking System (Java)

A Java-based banking system with OOP design, JDBC database integration, and a REST API.

## Features
- Savings and Current account types with OOP inheritance
- Deposit, withdrawal, and inter-account transfers
- Login authentication with lockout after failed attempts
- JDBC-backed persistence with MySQL
- REST API server for banking operations

## Tech Stack
- Java, JDBC, MySQL, OOP

## Setup
1. Create a `.env` file in the root with:
    - DB_URL=jdbc:mysql://localhost:3306/banking_system
    - DB_USER=root
    - DB_PASSWORD=your_password
2. Run Main.java to start the application