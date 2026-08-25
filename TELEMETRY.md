# Ad Studio Telemetry

The application uses Spring Boot Actuator, Micrometer Tracing and the
OpenTelemetry bridge to export traces through OTLP.

## Signals

### Traces

Every `POST /api/advertisements/generate` request creates an HTTP server
span. Each LLM agent execution becomes a child span:

```text
POST /api/advertisements/generate
├── agent.brief-strategy
│   └── Spring AI chat model observation
├── agent.copywriter
│   └── Spring AI chat model observation
├── advertising.validation
├── agent.compliance
│   └── Spring AI chat model observation
└── advertising.decision
```

Revision attempts add more `agent.copywriter`,
`advertising.validation`, `agent.compliance`, and
`advertising.decision` spans to the same trace.

Agent spans contain:

- `agent.name`
- `agent.version`
- `agent.stage`
- `agent.attempt`
- `agent.status`
- `agent.duration.ms`
- `workflow.id`
- `generation.id`

The application does not add user briefs, prompts, generated advertising
copy, image contents, or API keys to custom spans or metric tags.

### Metrics

Custom agent metrics:

```text
agent.executions
agent.execution.duration
```

Available tags:

```text
agent
stage
status
```

Actuator metrics can be inspected locally:

```text
http://localhost:8080/actuator/metrics
http://localhost:8080/actuator/metrics/agent.executions
http://localhost:8080/actuator/metrics/agent.execution.duration
```

## Start the local OpenTelemetry backend

The `compose.yaml` file contains Grafana OTEL-LGTM for local development.
It bundles an OpenTelemetry Collector, Tempo, Mimir and Grafana.

Start Docker Desktop, then run:

```powershell
docker compose up -d observability
```

Open Grafana:

```text
http://localhost:3000
```

Default local credentials:

```text
username: admin
password: admin
```

OTLP ports:

```text
4318: OTLP HTTP
4317: OTLP gRPC
```

## Start the application

Provide the OpenAI API key only through the server environment:

```powershell
$env:SPRING_AI_OPENAI_API_KEY = "your-api-key"
```

The local OTLP defaults already point to the Docker collector. Start the
application:

```powershell
.\mvnw.cmd spring-boot:run
```

Generate an advertisement at:

```text
http://localhost:8080/generate
```

## Find a trace in Grafana

1. Open Grafana at `http://localhost:3000`.
2. Open **Explore**.
3. Select the **Tempo** data source.
4. Search for service name `ad-studio`.
5. Filter by span name such as `agent.copywriter` or
   `agent.compliance`.
6. The API response contains `workflowId` and `generationId`; use these
   values to match `workflow.id` and `generation.id` span attributes.

## Environment configuration

```powershell
# Trace sampling ratio. Local default: 1.0
$env:MANAGEMENT_TRACING_SAMPLING_PROBABILITY = "1.0"

# Override the trace endpoint.
$env:OTEL_EXPORTER_OTLP_TRACES_ENDPOINT = "http://localhost:4318/v1/traces"

# Override the metrics endpoint.
$env:OTEL_EXPORTER_OTLP_METRICS_ENDPOINT = "http://localhost:4318/v1/metrics"

# Disable exporters while keeping local instrumentation available.
$env:OTEL_TRACES_EXPORT_ENABLED = "false"
$env:OTEL_METRICS_EXPORT_ENABLED = "false"

# Service and environment resource attributes.
$env:OTEL_SERVICE_NAME = "ad-studio"
$env:APP_ENVIRONMENT = "local"
```

For production, reduce trace sampling, secure Actuator endpoints, use a
production OpenTelemetry backend, and configure authenticated OTLP
headers instead of exposing a collector publicly.
