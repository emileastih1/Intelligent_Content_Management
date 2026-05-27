# Intelligent Document Management System
This project is a Java-based Intelligent Document Management System (DMS) that leverages cutting-edge AI technologies to revolutionize the way you manage and interact with documents. 
It incorporates advanced features powered by artificial intelligence to offer unparalleled efficiency and productivity. 

Our primary objective is to empower users with the ability to interact with the document repository conversationally. 
By leveraging Language Model-based Systems (LLMs), our DMS allows users to query the repository in a natural and conversational manner

The system aims to intelligently categorizes, searches, and analyzes your documents, allowing you to focus on what truly matters.
Our system leverages cutting-edge AI technologies to revolutionize the way you manage and interact with documents.

## [Architecture](Architecture.md)

This project employs a blend of architectural patterns, incorporating Domain-Driven Design (DDD), Command Query Responsibility Segregation (CQRS), hexagonal architecture, and Domain Events. 
It has been enhanced to include support for Elasticsearch, providing powerful search capabilities directly integrated with the Java API Client recommended for Spring Boot 3.x applications.

Additionally, support for Spring AI using OpenAI has been added to perform retrieval augmented generation. 
This is facilitated by utilizing the pgVector extension for PostgreSQL as our vector store, seamlessly integrated into the document lifecycle when adding a new document.

## Spring AI - OpenAI Configuration

To use this project, you should create an OpenAI account ,if you don't already have one, and store your OpenAI key in the `application.yaml` file. In my case, I used an environment variable called `OPENAI_API_KEY`.

You can also adapt the model to use via the property `spring.ai.openai.chat.model`.

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        model: "gpt-3.5-turbo-1106"
```

Make sure to replace `"gpt-3.5-turbo-1106"` ,that I am using, with the model you intend to use for chat generation.

## Database Setup and Tools

## Liquibase for Database Versioning
Liquibase is used to manage database migrations and schema changes. Changesets are organized in the `src/main/resources/db/changelog` directory, allowing for structured and version-controlled database updates.

### pgAdmin (Docker container)

- **Container Name:** spring-pgadmin
- **URL:** [http://localhost:8088/browser/](http://localhost:8088/browser/)
- **Username:** emileastih1@gmail.com
- **Password:** toor

Configure pgAdmin to connect to the PostgreSQL instance as shown in the image below:

![pgAdmin Configuration](img.png)

### PostgreSQL (Docker container)

- **Container Name:** spring-postgresql
- **URL:** [http://localhost:5432/](http://localhost:5432/)
- **Password:** toor
- **Port:** 5432

### Elasticsearch Setup

The project now includes Elasticsearch support, utilizing the Java API Client which is the recommended approach for Spring Boot 3.x versions. This is in response to the deprecation of the High Level REST Client in favor of the Java API Client since Elasticsearch 7.15.0. Basic Add and Get operations on Elasticsearch documents have been implemented.

- **Elasticsearch URL:** [http://localhost:9200/](http://localhost:9200/)
- **Query Documents:** [http://localhost:9200/document/_search?size=1000](http://localhost:9200/document/_search?size=1000)

## Application Access

The application can be accessed at:
- [http://localhost:8085/idm](http://localhost:8085/idm)

## Swagger UI

Explore the API using Swagger UI at:
- [http://localhost:8085/idm/swagger-ui/index.html](http://localhost:8085/idm/swagger-ui/index.html)

## Windows Docker Setup

To ensure Elasticsearch runs smoothly on Docker for Windows, especially within a WSL2 environment, execute the following commands:

```bash
wsl -d docker-desktop
sysctl -w vm.max_map_count=262144
sysctl vm.max_map_count
```

## Support

If you have any questions, encounter issues, or need assistance with this project, please don't hesitate to open an issue on GitHub.

[Open an Issue](https://github.com/emileastih1/architecture_ddd/issues)

Alternatively, you can reach out to us via email at:

📧 [emileastih1@gmail.com](mailto:emileastih1@gmail.com)

We're committed to addressing your concerns and providing the support you need to ensure your experience with this project is smooth and successful.

## Dependency Audit

This project includes a simple audit to report dependency vulnerabilities and available updates across supported package managers in this repo.

How to run (Windows PowerShell):

- Ensure Java and Maven are installed and available on your PATH.
- From the project root, run:

```
./audit-dependencies.ps1
```

What it does:
- Uses OWASP Dependency-Check to scan for known vulnerabilities and collects IDs, severity, and when possible a fixed version.
- Uses the Versions Maven Plugin to list available dependency updates and groups them into patch, minor, and major.
- Skips/batches majors and flags them as potentially breaking changes.
- Detects Docker images referenced by docker-compose.yml and lists them for manual tag review.

Output:
- A Markdown summary file named `dependency-audit-<YYYY-MM-DD>.md` in the project root.

Next steps:
- If you would like, we can automatically apply PATCH/MINOR updates and run tests. Major updates will be listed for manual review because they might introduce breaking changes.