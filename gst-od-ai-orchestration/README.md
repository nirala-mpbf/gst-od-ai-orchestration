# GST OD AI Orchestration — AgentScope Java 2 HarnessAgent

Spring Boot AI orchestration service. The AI Service connects to **AgentGateway's MCP listener** through `agent-workspace/tools.json`; AgentGateway routes tools to your existing FastMCP server, which calls Brain Service. AgentScope `HarnessAgent` provides the reasoning/tool loop.

## Prerequisites
- JDK 21 and Maven 3.9+ (or Docker)
- Reachable AgentGateway MCP endpoint, with FastMCP Brain tools exposed
- LLM model endpoint and credentials configured for AgentScope's selected model provider

## IMPORTANT: LLM gateway configuration
`AGENTSCOPE_MODEL` is a model identifier, **not** an LLM URL. The default `openai:gpt-4o-mini` uses AgentScope's OpenAI model integration; it does not automatically route through AgentGateway. **Before deploying, configure the AgentScope OpenAI-compatible model provider with your AgentGateway LLM base URL and API key** (or set the supported provider base-URL environment configuration for your exact AgentScope version). This integration is intentionally not represented as working without verifying your gateway's actual URL and authentication. The MCP URL in `tools.json` must likewise match the gateway's real listener.

## Run
1. Update `agent-workspace/tools.json` to the reachable AgentGateway MCP URL.
2. Configure your AgentScope model provider to use the LLM gateway endpoint.
3. `mvn clean package`
4. `java -jar target/gst-od-ai-orchestration-0.1.0.jar`
5. Send requests from `postman/chat-examples.http`.

Docker: `docker compose up --build` (requires AgentGateway network connectivity and LLM provider configuration).

## MCP discovery
`AgentConfig` calls `.workspace(Path.of(workspace))`. During `builder.build()`, HarnessAgent reads `<workspace>/tools.json`, registers MCP servers, and exposes discovered tools to its reasoning loop. No individual Brain tools are hardcoded in Java. Restart/rebuild agent instances to pick up changes.

## Notes / limitations
- **Not verified compiled**: this environment does not have Maven or external dependency resolution. Check the AgentScope API against your chosen release before production use.
- Session continuity requires an appropriate durable AgentScope state/log backend; stable IDs alone do not guarantee persistence across fresh instances. Wire a persistent backend before relying on multi-turn behavior.
- Authentication is not implemented. `userId` from the request is POC-only; derive it from JWT/security context in production.
- Document bytes must be uploaded through an authenticated upload API before asking the agent to register their document IDs.
- `tools.json` does not itself register REST endpoints: FastMCP must expose them as MCP tools and AgentGateway must route them.
- A shared HarnessAgent.Builder's concurrent-build behavior should be verified for your exact AgentScope release.
- The example deliberately contains no manual HITL and no keyword-based tool router.

## Official references
- https://java.agentscope.io/v2/en/docs/harness/architecture
- https://java.agentscope.io/v2/en/docs/harness/workspace
- https://java.agentscope.io/v2/en/docs/quickstart
