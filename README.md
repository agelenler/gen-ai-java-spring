# gen-ai-java-spring

Code repository for "Complete GenAI with Java & Spring AI: LLMs, RAG, AI Agents" Udemy course,
demonstrating Gen AI features with Java and Spring Boot.
It contains a main Spring Boot application (GenAIJavaSpringApplication) and a `posture-service` Spring Boot application used by the main app.
The project also includes Docker assets for observability (Prometheus, Grafana, Loki, Jaeger, Tempo) and local Postgres support for vector stores.

This README briefly explains the repository structure and shows how to:
- run the observability Docker Compose
- run the two Spring Boot applications in IntelliJ

Repository layout (high level)
- `src/` - main Java Spring Boot application (main app)
  - main class: `com.genai.java.spring.GenAIJavaSpringApplication`
  - default server port: 8081 (configured in `src/main/resources/application.yml`)
- `posture-service/` - separate Spring Boot application
  - main class: `com.genai.posture.PostureServiceApplication`
  - intended to run on port 8082 (used by the main app via configuration)
- `docker/observability/observability-compose.yml` - Docker Compose for observability stack
- `docker/postgres/pgvector.yml` - Docker Compose for Postgres with init script

Prerequisites
- Docker Engine and Docker Compose v2 (docker compose) installed and running
- Java 25 (project uses `java.version` = 25 in the root `pom.xml`) and a compatible JDK installed
- Maven (optional if running from IntelliJ) or IntelliJ IDEA with Maven support

1) Run the observability Docker Compose

The repository includes an observability compose file at `docker/observability/observability-compose.yml` which brings up Prometheus, Grafana, Loki, Jaeger, Tempo, and related provisioning.

From the repository root run (macOS / zsh):

```bash
# Start the observability stack in detached mode
docker compose -f docker/observability/observability-compose.yml up -d

# Tail logs (optional)
docker compose -f docker/observability/observability-compose.yml logs -f

# Stop and remove the stack
docker compose -f docker/observability/observability-compose.yml down
```

Notes:
- Grafana dashboards and datasources are pre-provisioned under `docker/observability/grafana/`.
- The stack expects services to export Prometheus metrics and traces to the usual ports (Prometheus, OTLP endpoint, etc.). See `src/main/resources/application.yml` for the app's OTLP endpoint and Prometheus settings.

2) Postgres for local development

There is a `docker/postgres` folder with `init.sql` and `pgvector.yml`. This repo's default datasource in `application.yml` points at `jdbc:postgresql://localhost:5433/postgres`. To run Postgres locally in Docker, run the pgvector.yml file that maps port 5433 and initialises pgvector.

3) Run the applications in IntelliJ

Open the project
- File -> Open... -> select the project root (this repository). IntelliJ will detect the Maven project and import dependencies.

Create / run Run Configurations (two ways: use 'Run' gutter next to the main method, or create explicit run configurations):

A) Using the Run gutter (quick)
- Open `src/main/java/com/genai/java/spring/GenAIJavaSpringApplication.java` and click the green Run icon next to the `main` method.
- Open `posture-service/src/main/java/com/genai/posture/PostureServiceApplication.java` and click the green Run icon next to its `main` method.

B) Creating explicit Spring Boot run configurations (recommended for controlling VM args / env vars)
1. Run -> Edit Configurations...
2. Click the + and choose 'Spring Boot' (or 'Application' if Spring Boot option not available)
3. For the main app configuration:
   - Name: GenAIJavaSpringApplication
   - Main class: `com.genai.java.spring.GenAIJavaSpringApplication`
   - Module: select the main `gen-ai-java-spring` module
   - Working directory: project root
   - Environment variables: set any required secrets (e.g. OPENAI_API_KEY, HUGGINGFACE_API_KEY, COHERE_API_KEY) or configure them in your shell/IDE run environment
4. For the posture service configuration:
   - Name: PostureServiceApplication
   - Main class: `com.genai.posture.PostureServiceApplication`
   - Module: select the `posture-service` module
   - Working directory: `posture-service`
   - Environment variables: none required by default

Ports and wiring
- Main app: default port 8081 (see `src/main/resources/application.yml`).
- Posture service: expected by the main app at `http://localhost:8082` (configured in the main app's `application.yml` under `app.agent.posture-tool.url`). If you run the posture service on a different port, update the main app's configuration.

Environment and secrets
- This project references API keys via environment variables in `application.yml` (for example OPENAI_API_KEY, HUGGINGFACE_API_KEY, COHERE_API_KEY). Configure these either in your IDE run configuration environment variables or export them in your shell before launching IntelliJ (or use a local secrets file if you prefer).

Quick tips
- If you rely on a local Postgres, make sure it's running on the correct port and that the credentials in `application.yml` match.
- If you only want to run the Java apps without the observability stack, you can skip the Docker Compose step.
- If you change application ports, update `app.agent.posture-tool.url` in the main app's config to point to the posture service.

Troubleshooting
- Maven/IDE import errors: reimport the Maven project in IntelliJ (right-click the pom.xml or use the Maven tool window -> Reimport All).
- Port conflicts: check with `lsof -i :8081` / `lsof -i :8082` and stop conflicting processes.
- Database schema: the main app's vectorstore/datasource configuration may attempt to initialize schemas; delete or reset the DB if needed when testing schema initialization.