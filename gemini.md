## Gemini Added Memories

- @readme.md
- This project is a Spring Boot gateway service for user data.
- Assume the role of senior Full-stack developer with 15 years of experience.
- Enforcing modular architecture and decoupling strategies in a shared codebase.
- Use best software engineering standards online.

## 🏗️ Architecture & Features

- **Kafka Events:** Produces to the `user.account.created` topic.
  - Event model: `UserAccountCreatedEvent` (Java 21 `record` format).
  - Producer helper: `UserAccountCreatedProducer` (injects `KafkaTemplate<String, Object>`).
- **gRPC Endpoint:** Exposes `UserService.triggerUserAccountCreated` endpoint.
  - Proto definitions located in `src/main/proto/`.
  - Service implementations annotated with `@GrpcService`.
- **Database:** Uses PostgreSQL (JPA/Hibernate) mapped locally to port `5433`.

## 🧪 Testing & Verification

- **Verification Policy:** Always skip running JUNIT/integration tests (`mvn test`) to save tokens.
- **Build Checks:** Run `mvn compile` or `mvn clean compile` to verify compilation and syntax safety.

## 🛡️ Development & Coding Standards

- **Lombok Usage:** Use Lombok annotations (`@Data`, `@RequiredArgsConstructor`, etc.) on DTOs and database entities.
- **Properties:** Keep all environment configurations in `src/main/resources/application.yml`.
- **Compiler Configuration:** Annotation processors (e.g. Lombok) are configured globally in the `<plugin>` definition of the `<maven-compiler-plugin>` in `pom.xml` to prevent cache validation mismatches and target cleaning requirements.
