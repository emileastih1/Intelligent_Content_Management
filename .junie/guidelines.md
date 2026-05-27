Project development guidelines (advanced)

This document captures project-specific practices and gotchas to speed up development and debugging. It assumes familiarity with Java, Maven, Spring Boot, MockMvc/JUnit, Docker, Liquibase, Elasticsearch, and OpenAI integrations.

1) Build and configuration

- Java/Maven
  - Java 17 is required (see pom.xml <java.version>17</java.version>), built on Spring Boot 3.2.x.
  - Standard build targets: mvn clean verify (runs unit tests and integration-test phase), mvn -DskipTests package for a quick local artifact.

- Spring Boot app
  - Default port/context: 8085 with servlet context path /idm.
  - Run locally: mvn spring-boot:run or run the DomainDrivenApplication main class in your IDE.
  - Virtual threads enabled: spring.threads.virtual.enabled=true.

- External services (Docker Compose integration)
  - Spring Boot Docker Compose support is enabled and wired to src/main/resources/docker/docker-compose.yml via spring.docker.compose.file=classpath:docker/docker-compose.yml. It is not skipped for tests (spring.docker.compose.skip.in-tests=false). If the application context attempts to start with this enabled and Docker is available, dependent services (PostgreSQL/pgvector, pgAdmin, Elasticsearch) can be started for you.
  - Ensure Docker Desktop is running. On Windows/WSL2 for Elasticsearch, set vm.max_map_count:
    - wsl -d docker-desktop
    - sysctl -w vm.max_map_count=262144
    - sysctl vm.max_map_count
  - Services/ports from docker-compose.yml:
    - PostgreSQL (ankane/pgvector): 5434 -> 5432, user postgres, password toor
    - pgAdmin: 8088 -> 80, default email emileastih1@gmail.com / password toor
    - Elasticsearch: 9200 -> 9200, single-node, container elasticsearch01

- Application configuration (src/main/resources/application.yml)
  - Database: jdbc:postgresql://localhost:5434/doc_management_db, username postgresspring-postgresql-db, password toor; Liquibase changelog at db/changelog/DocumentContent-DOC_MANAGEMENT_DB/changeLog-master.xml with default schema documentcontent.
  - Elasticsearch: host localhost:9200; index name document.
  - OpenAI: spring.ai.openai.api-key is read from env var OPENAI_API_KEY; model defaults to gpt-3.5-turbo-1106.
  - springdoc: Swagger UI at /swagger-ui.html (under the /idm context path when running), API docs at /v3/rest-api-docs. App entry point is http://localhost:8085/idm.

- Maven plugins with behavioral implications
  - spring-boot-maven-plugin has executions bound to pre-integration-test (start) and post-integration-test (stop). During mvn verify, the app is started for the integration-test phase and then stopped.
  - springdoc-openapi-maven-plugin is bound to integration-test to generate OpenAPI into OpenAPIDescription/{swagger-file}.json by calling http://localhost:8085/v3/rest-api-docs/Public-API. Ensure the app is healthy and serving that group; otherwise generation will fail the build in integration-test. Running mvn -DskipITs test or mvn -DskipTests package avoids this.
  - structure-maven-plugin prints the project file structure during compile; harmless but noisy in CI logs.

2) Testing

- Test technologies and patterns
  - Primary stack: JUnit 5 (jupiter), Spring Boot Test, Spring Security Test, MockMvc. Tests currently prefer controller unit tests using MockMvc in standalone mode.
  - AbstractRestTest provides a light harness:
    - Builds a stand-alone MockMvc with BackendExceptionHandler registered as @ControllerAdvice.
    - Provides ObjectMapper setup with WRAP_ROOT_VALUE=false.
    - To test a controller, extend AbstractRestTest<T extends BaseRestController> and implement getController() returning the controller under test (you will typically inject mocked collaborators).

- Running tests
  - From project root:
    - Run a single test class: mvn -Dtest=fully.qualified.ClassName test
    - Run tests by method: mvn -Dtest=fully.qualified.ClassName#methodName test
    - Run the whole suite: mvn test or mvn verify (the latter will also trigger integration-test behaviors and OpenAPI generation; see above).
  - From IDE: Use the built-in JUnit runner. No special VM args required for unit tests. Ensure Docker Desktop is not interfering if you don’t want services started; you can temporarily set spring.docker.compose.enabled=false via environment or JVM system property.

- Adding new tests
  - Controller tests: extend AbstractRestTest and return a controller instance with mocked dependencies.
    - Example outline:
      - class MyControllerTest extends AbstractRestTest<MyController> {
          private final MyService myService = mock(MyService.class);
          @Override protected MyController getController() { return new MyController(myService); }
          @Test void shouldReturnOk() throws Exception { mockMvc.perform(get("/idm/api/..."))... }
        }
    - Because MockMvc is configured in standalone mode, you won’t get full Spring context wiring; register any needed ControllerAdvice or filters explicitly if they are not already in AbstractRestTest.
  - Service/domain unit tests: write plain JUnit tests under src/test/java matching the package of the class under test; avoid touching the Spring context for speed.

- Demo: creating and running a simple test (validated)
  - We created a minimal unit test for ErrorMessageConstants to demonstrate the flow, executed it successfully, and then removed it to keep the repository clean:
    - File (temporary): src/test/java/com/ea/architecture/domain/driven/presentation/exception/ErrorMessageConstantsTest.java
    - Command used: mvn -Dtest=com.ea.architecture.domain.driven.presentation.exception.ErrorMessageConstantsTest test (equivalent to IDE run)
    - Result: Passed 1/1 tests
    - The test file was removed after verification; only this guidelines document remains as a change.

3) Additional development and debugging notes

- API surface and docs
  - Swagger UI: http://localhost:8085/idm/swagger-ui/index.html
  - OpenAPI JSON generation is automated in integration-test; ensure the endpoint group Public-API is configured and the app is able to start with its external dependencies or that those calls are mocked/disabled when generating.

- Data and migrations
  - Liquibase runs at startup against schema documentcontent. Use pgAdmin at http://localhost:8088/browser/ to inspect the database while developing. The initial scripts and multi-database/user creation are handled by docker-entrypoint scripts in src/main/resources/docker/db-postgres.

- Search and indexing
  - Elasticsearch Java client (co.elastic.clients) is used. Index defaults in application.yml under elastic.index.name=document. Review DocumentElasticSearchQueryAdapter for query logic and adapt cautiously—paging and size settings are configured under "elastic" properties.
  - If you change index names/mappings, also update application.yml and any mapping code; keep vm.max_map_count and Docker memory limits in mind for Windows/WSL.

- Security and error handling
  - Error codes/messages centralized in ErrorMessageConstants.
  - BackendExceptionHandler is registered in AbstractRestTest so controller tests assert consistent error shapes without a full Spring context.

- Performance/dev UX
  - Virtual threads are enabled; watch for blocking drivers/APIs. PostgreSQL JDBC and Elasticsearch client are fine but be cautious with any custom blocking I/O in request threads.

- CI considerations
  - If your CI runner lacks Docker privileges or Elasticsearch/pgvector resources, prefer mvn -DskipITs -DskipOpenApi=true verify or disable the springdoc plugin via -Dskip=true on the plugin execution using Maven profiles. Alternatively, disable Spring Boot Docker Compose at runtime with -Dspring.docker.compose.enabled=false.

- Updating dependencies
  - Use ./audit-dependencies.ps1 (Windows PowerShell) to generate a dependency audit with vulnerability and update suggestions. Patch/minor bumps are typically safe; majors may be breaking.

Appendix: quick commands

- Run app locally: mvn spring-boot:run
- Run unit tests only: mvn -DskipITs test
- Full verify with OpenAPI generation: mvn verify
- Package without tests: mvn -DskipTests package
- Regenerate OpenAPI only: mvn -DskipTests -Dspring-boot.run.wait=120000 verify (ensure the app can start and the API docs URL is reachable)
