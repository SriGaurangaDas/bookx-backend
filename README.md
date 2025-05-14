## BookX-Backend

BookX-Backend is a Spring Boot REST API that powers a book exchange platform. It provides user registration, JWT-based authentication, and secure profile retrieval.

---

## Table of Contents

- [Features](#features)  
- [Tech Stack](#tech-stack)  
- [Getting Started](#getting-started)  
  - [Prerequisites](#prerequisites)  
  - [Installation](#installation)  
  - [Running the App](#running-the-app)  
- [Configuration](#configuration)  
- [API Endpoints](#api-endpoints)  
  - [Authentication](#authentication)  
  - [Users](#users)  
- [Security](#security)  
- [Testing](#testing)  
- [Contributing](#contributing)  
- [License](#license)  

---

## Features

- **User Registration** (`POST /api/auth/register`)  
- **Login & JWT Generation** (`POST /api/auth/login`)  
- **Secure Profile Retrieval** (`GET /api/users/{username}`)  
- **Password Encryption** with BCrypt  
- **Stateless** session management via JWT  

## Tech Stack

- Java 17  
- Spring Boot 3.4.5  
- Spring Security & JWT  
- Spring Data JPA  
- MySQL (H2 for tests)  
- Maven  

## Getting Started

### Prerequisites

- Java 17+  
- Maven 3.6+  
- MySQL database (or use H2 for local)  

### Installation

1. **Clone the repo**  
   ```bash
   git clone https://github.com/SriGaurangaDas/bookx-backend.git
   cd bookx-backend

2. **Configure**
   Edit `src/main/resources/application.properties` with your DB credentials and JWT secret.

3. **Build**

   ```bash
   ./mvnw clean install
   ```

### Running the App

```bash
./mvnw spring-boot:run
```

The app will start on **[http://localhost:8080](http://localhost:8080)**.

## Configuration

```properties
# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/bookx
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASS
spring.jpa.hibernate.ddl-auto=update

# JWT
jwt.secret=YourJWTSecretKey
jwt.expirationMs=86400000
```

## API Endpoints

### Authentication

| Method | Endpoint             | Request Body                                                                                  | Success Response                |
| ------ | -------------------- | --------------------------------------------------------------------------------------------- | ------------------------------- |
| POST   | `/api/auth/register` | `{ "username", "email", "password", "fullName", "latitude", "longitude", "profileImageUrl" }` | `200 OK` + created `User`       |
| POST   | `/api/auth/login`    | `{ "username", "password" }`                                                                  | `200 OK` + `{ "token": "..." }` |

### Users

| Method | Endpoint                | Headers                             | Success Response          |
| ------ | ----------------------- | ----------------------------------- | ------------------------- |
| GET    | `/api/users/{username}` | `Authorization: Bearer <jwt-token>` | `200 OK` + `User` profile |

## Security

* **JWTFilter**: Intercepts and validates JWT on protected routes.
* **SecurityConfig**: Disables CSRF, sets stateless sessions, and configures route permissions.
* **PasswordEncoderConfig**: Registers a BCrypt `PasswordEncoder` bean.

## Testing

* **Unit Tests**: Mockito + MockMvc for controllers and services.
* **Integration Tests**: `@SpringBootTest` + `TestRestTemplate` with H2 in-memory database.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/XYZ`)
3. Commit your changes (`git commit -m "Add XYZ"`)
4. Push to your fork (`git push origin feature/XYZ`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.
