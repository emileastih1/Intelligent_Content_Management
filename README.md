# Architecture_DDD

This project employs a blend of architectural patterns, incorporating Domain-Driven Design (DDD), Command Query Responsibility Segregation (CQRS), hexagonal architecture, and Domain Events. It has now been enhanced to include support for Elasticsearch, providing powerful search capabilities directly integrated with the Java API Client recommended for Spring Boot 3.x applications.

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
- [http://localhost:8085/ddd](http://localhost:8085/ddd)

## Swagger UI

Explore the API using Swagger UI at:
- [http://localhost:8085/ddd/swagger-ui/index.html](http://localhost:8085/ddd/swagger-ui/index.html)

## Windows Docker Setup

To ensure Elasticsearch runs smoothly on Docker for Windows, especially within a WSL2 environment, execute the following commands:

```bash
wsl -d docker-desktop
sysctl -w vm.max_map_count=262144
sysctl vm.max_map_count
