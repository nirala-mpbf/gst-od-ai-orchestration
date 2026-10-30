package com.example.ai.config;

import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.tools.ToolsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.List;

@Configuration
public class AgentConfig {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public HarnessAgent.Builder gstOdAgentBuilder(
//            @Value("${app.agent.workspace}") String workspace,
            @Value("${agent.gateway.base-url}") String gatewayBaseUrl,
            @Value("${agent.gateway.api-key}") String gatewayApiKey,
            @Value("${agent.model}") String model,
            @Value("${agent.gateway.mcp-url:http://localhost:4000/mcp}")
            String mcpUrl) {

        OpenAIChatModel chatModel = OpenAIChatModel.builder()
                .baseUrl(gatewayBaseUrl)
                .apiKey(gatewayApiKey)
                .modelName(model)
                .build();

        Toolkit toolkit = new Toolkit();
        McpClientWrapper mcpClientWrapper =
                McpClientBuilder.create("brain-tools")
                        .streamableHttpTransport(mcpUrl)
                        .buildAsync()
                        .block();

        // registerMcpClient is reactive and does not run until subscribed. Wait for
        // discovery to finish before HarnessAgent copies the toolkit in build().
        toolkit.registerMcpClient(mcpClientWrapper).block();

        // This API has no HITL confirmation/resume endpoint. AgentScope's default policy asks
        // for confirmation on non-read-only MCP tools, which suspends the call before MCP
        // `tools/call` and currently becomes an empty answer in ChatService. Allow only the
        // workflow operations this application exposes; Brain remains responsible for validating
        // each requested transition.
//        PermissionContextState workflowPermissions = PermissionContextState.builder()
//                .addAllowRule("get_current_status", new PermissionRule(
//                        "get_current_status", null, PermissionBehavior.ALLOW, "application"))
//                .addAllowRule("verify_otp", new PermissionRule(
//                        "verify_otp", null, PermissionBehavior.ALLOW, "application"))
//                .addAllowRule("submit_profile", new PermissionRule(
//                        "submit_profile", null, PermissionBehavior.ALLOW, "application"))
//                .addAllowRule("submit_account_statement", new PermissionRule(
//                        "submit_account_statement", null, PermissionBehavior.ALLOW, "application"))
//                .addAllowRule("submit_udyam_document", new PermissionRule(
//                        "submit_udyam_document", null, PermissionBehavior.ALLOW, "application"))
//                .build();

        // Apply the same allowlist in code as well as workspace/tools.json, so an unreadable or
        // misplaced workspace config cannot expose filesystem tools to the model.
//        ToolsConfig toolsConfig = new ToolsConfig();
//        toolsConfig.setAllow(List.of(
//                "get_current_status",
//                "verify_otp",
//                "submit_profile",
//                "submit_account_statement",
//                "submit_udyam_document"));

//        ToolsConfig toolsConfig = new ToolsConfig();
//        toolsConfig.setAllow(List.of(
//                "processWorkflowCommand"));

        PermissionContextState workflowPermissions =
                PermissionContextState.builder()
                        .addAllowRule(
                                "processWorkflowCommand",
                                new PermissionRule(
                                        "processWorkflowCommand",
                                        null,
                                        PermissionBehavior.ALLOW,
                                        "application"
                                )
                        )
                        .build();

        ToolsConfig toolsConfig = new ToolsConfig();
        toolsConfig.setAllow(
                List.of("processWorkflowCommand")
        );

        return HarnessAgent.builder()
                .name("gst-od-assistant")
                .agentId("gst-od-assistant")
                .toolkit(toolkit)
                .model(chatModel)
                .disableSubagents()
                .disableShellTool()
                .disableMemoryTools()
                .disableDynamicSkills()
                .enableAgentTracingLog(true)
                .permissionContext(workflowPermissions)
                .toolsConfig(toolsConfig);
//                .workspace(Path.of(workspace));
    }
}
