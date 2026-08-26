# Ad Studio Telemetry

The application uses Spring Boot Actuator, Micrometer Tracing and the
OpenTelemetry bridge to export traces through OTLP.

Traces can be sent either to the local Grafana OTEL-LGTM stack or
directly to Langfuse. The Langfuse integration uses its supported OTLP
HTTP ingestion endpoint; no second tracing SDK is required.

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
- `langfuse.trace.name`
- `langfuse.session.id`
- `langfuse.trace.metadata.workflow_id`
- `langfuse.trace.metadata.generation_id`
- `langfuse.observation.type`
- filterable agent name, version, and stage observation metadata

The application does not add user briefs, prompts, generated advertising
copy, image contents, or API keys to custom spans or metric tags.

The same workflow ID is used as the Langfuse session ID. Consequently,
the initial copy workflow trace and the separate per-candidate visual
generation traces can be found together in one Langfuse session.

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

## Send traces to Langfuse

Create a Langfuse project and copy its public and secret API keys. The
application does not read or store those two values directly. Instead,
Langfuse Basic Auth expects their Base64-encoded `public:secret` pair.

Prepare the value in PowerShell:

```powershell
$env:LANGFUSE_PUBLIC_KEY = "pk-lf-..."
$env:LANGFUSE_SECRET_KEY = "sk-lf-..."
$langfuseKeyPair = "${env:LANGFUSE_PUBLIC_KEY}:${env:LANGFUSE_SECRET_KEY}"
$env:LANGFUSE_AUTH_STRING = [Convert]::ToBase64String(
    [Text.Encoding]::UTF8.GetBytes($langfuseKeyPair)
)
```

Activate the Langfuse profile and start the application:

```powershell
$env:SPRING_PROFILES_ACTIVE = "langfuse"
$env:OPENAI_API_KEY = "your-openai-api-key"
.\mvnw.cmd spring-boot:run
```

The profile defaults to Langfuse Cloud's EU data region:

```text
https://cloud.langfuse.com/api/public/otel/v1/traces
```

Override the endpoint for another region or a self-hosted instance:

```powershell
$env:LANGFUSE_OTEL_TRACES_ENDPOINT = "https://us.cloud.langfuse.com/api/public/otel/v1/traces"
```

The profile sends the `x-langfuse-ingestion-version: 4` header so that
directly ingested traces use Langfuse's real-time v4 ingestion path.
Metrics export is disabled by default in this profile because Langfuse
is configured here as a trace backend, not as the metrics backend.

After generating an advertisement, open the Langfuse project and filter
by:

```text
Trace name: advertisement-generation
Trace name: advertisement-visual-generation
Session ID: <workflowId returned by the API>
Metadata generation_id: <generationId returned by the API>
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
