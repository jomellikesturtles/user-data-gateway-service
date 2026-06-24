# User Data Gateway Service

A Spring Boot service providing user-related data operations, event publishing via Kafka, and communication via gRPC.

---

## Technical Stack
- **Java:** Version 21
- **Spring Boot:** Version 4.1.0
- **Spring gRPC:** Version 0.12.0
- **Kafka:** Spring Kafka
- **Database:** PostgreSQL (JPA/Hibernate)

---

## Infrastructure Setup

To run the database and Kafka message broker:
```bash
docker compose -f ../mdb-platform/docker-compose.yml up -d postgres-db kafka
```

### Local Mappings
- **PostgreSQL Database:** `localhost:5433` (DB: `mdb_prod`, user/password: `postgres/postgres`)
- **Kafka Bootstrap Server:** `localhost:9093`

---

## Configuration

All local settings are defined in [application.yml](src/main/resources/application.yml).

---

## Key Features

### 1. Kafka Producer (`user.account.created`)
Publishes event data to Kafka topic when user accounts are created.
- **Event Definition:** `UserAccountCreatedEvent` record.
- **Producer Class:** `UserAccountCreatedProducer`

### 2. gRPC Endpoint
Exposes a gRPC service for triggering account created events.
- **Proto definition:** [UserService.proto](src/main/proto/UserService.proto)
- **Service implementation:** [UserServiceImpl](src/main/java/com/mdb/user_data_gateway_service/grpc/UserServiceImpl.java)

---

## Building and Running

### 1. Generate Proto / Compile
To compile the Protobuf messages and gRPC services (generates files in `target/generated-sources/protobuf/`):
```bash
mvn protobuf:compile protobuf:compile-custom
```
Or simply run a full project compilation (which triggers proto generation automatically):
```bash
mvn compile
```

### 2. Run Locally
Start the Spring Boot application locally. Use `-DskipTests` (camelCase, no spaces) to bypass tests:
```bash
mvn spring-boot:run -DskipTests
```
Or using the Maven Wrapper:
```bash
./mvnw spring-boot:run -DskipTests
```

