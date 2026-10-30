package com.example.ai.service;
//
//import com.example.ai.api.ChatController.ChatRequest;
//import com.example.ai.api.ChatController.ChatResponse;
//import com.example.ai.dto.PromptResponse;
//import io.agentscope.core.agent.RuntimeContext;
//import io.agentscope.core.message.UserMessage;
//import io.agentscope.harness.agent.HarnessAgent;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Mono;
//
//@Service
//public class ChatService {
//    private final HarnessAgent.Builder builder;
//
//    private final PromptService promptService;
//    private static final String WORKFLOW = "ddod";
//    private static final String MASTER_PROMPT = "onboarding_master_sop_7";
//
//    public ChatService(HarnessAgent.Builder builder, PromptService promptService) {
//        this.builder = builder;
//        this.promptService = promptService;
//    }
//
//    public Mono<ChatResponse> chat(ChatRequest request) {
//        return Mono.defer(() -> {
//            PromptResponse response =
//                    promptService.latest(WORKFLOW, MASTER_PROMPT);
//
//            String systemPrompt = response.content();
//
//            System.out.println("========== AGENT SYSTEM PROMPT ==========");
//            System.out.println("Prompt ID: " + response.promptId());
//            System.out.println("Prompt Ref: " + response.ref());
//            System.out.println("Prompt Length: " +
//                    (systemPrompt == null ? 0 : systemPrompt.length()));
//            System.out.println("=========================================");
//
//            HarnessAgent agent = builder.sysPrompt(systemPrompt).build();
//
//            RuntimeContext context = RuntimeContext.builder()
//                    .userId(request.userId())
//                    .sessionId(request.sessionId())
//                    .build();
//            return agent.call(new UserMessage(request.message()), context)
//                    .map(reply -> new ChatResponse(request.sessionId(), reply.getTextContent()))
//                    .doFinally(signal -> agent.close());
//        });
//    }
//}


import com.example.ai.api.ChatController.ChatRequest;
import com.example.ai.api.ChatController.ChatResponse;
import com.example.ai.dto.PromptResponse;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private static final Logger log =
            LoggerFactory.getLogger(ChatService.class);

    private static final String WORKFLOW = "ddod";
    private static final String MASTER_PROMPT =
            "onboarding_master_sop_7";
    private static final Pattern REFERENCE_ID_PATTERN =
            Pattern.compile(
                    "(?im)^\\s*(?:TRUSTED_REFERENCE_ID|referenceId)\\s*:\\s*[\"']?([^\\s\"'`]+)"
            );

    private final HarnessAgent.Builder builder;
    private final PromptService promptService;

    public ChatService(
            HarnessAgent.Builder builder,
            PromptService promptService) {
        this.builder = builder;
        this.promptService = promptService;
    }

    public Mono<ChatResponse> chat(ChatRequest request) {
        return Mono.defer(() -> {

            // 1. Validate request and trusted context
            if (request == null) {
                return Mono.error(
                        new IllegalArgumentException(
                                "Chat request is required"));
            }

//            String referenceId = request.referenceId().trim();
//
//            if (referenceId == null || referenceId.isBlank()) {
//                return Mono.error(
//                        new IllegalArgumentException(
//                                "referenceId is required"));
//            }

            // 2. Load the latest onboarding SOP
            PromptResponse response =
                    promptService.latest(WORKFLOW, MASTER_PROMPT);

            if (response == null
                    || response.content() == null
                    || response.content().isBlank()) {
                return Mono.error(
                        new IllegalStateException(
                                "Onboarding system prompt is empty"));
            }

            String systemPrompt = response.content();

            Matcher matcher = REFERENCE_ID_PATTERN.matcher(systemPrompt);

            if (!matcher.find()) {
                return Mono.error(new IllegalStateException(
                        "Reference ID missing from loaded onboarding SOP"));
            }

            String referenceId = matcher.group(1).trim();

            log.info("Reference ID extracted from LakeFS prompt: {}", referenceId);

//            log.info("Reference ID loaded from SOP: {}", referenceId);

            String trustedContext = """
                    
                    TRUSTED RUNTIME CONTEXT:
                    referenceId = %s
                    
                    For onboarding status queries, call processWorkflowCommand
                    with operation GET_STATE.
                    
                    The tool requires a top-level body argument:
                    {
                      "body": {
                        "metadata": {
                          "operation": "GET_STATE",
                          "requestId": "<unique-correlation-id>",
                          "referenceId": "%s"
                        },
                        "payload": {}
                      }
                    }
                    
                    Use the exact referenceId supplied in this trusted runtime context.
                    Never use placeholder values from tool descriptions or schema examples.
                    Do not invent or substitute the referenceId.
                    """.formatted(referenceId, referenceId);

            String effectiveSystemPrompt = systemPrompt + "\n\n" + trustedContext;

            log.info("Prompt ID={}, ref={}, extractedReferenceId={}",
                    response.promptId(),
                    response.ref(),
                    referenceId);

            log.info("Trusted context injected={}",
                    effectiveSystemPrompt.contains(
                            "referenceId = " + referenceId));

            HarnessAgent agent = builder
                    .sysPrompt(effectiveSystemPrompt)
                    .build();

//            String systemPrompt = response.content();
//
//            // 3. Add application context to the system prompt
////            String trustedContext = """
////    Trusted application context:
////    referenceId: %s
////
////    For status requests, use this exact referenceId.
////    Never substitute placeholders from tool descriptions.
////    """.formatted(request.referenceId());
////
////            log.info(
////                    "Loaded prompt: id={}, ref={}, length={}",
////                    response.promptId(),
////                    response.ref(),
////                    systemPrompt.length());
////
////            // 4. Build the agent with the prompt
////            HarnessAgent agent = builder
////                    .sysPrompt(systemPrompt)
////                    .build();
//            String trustedContext = """
//
//                    # TRUSTED RUNTIME CONTEXT
//                    The following values are supplied by the application and are authoritative.
//                    referenceId = %s
//
//                    For GET_STATE, use this exact referenceId in body.metadata.referenceId.
//                    Never use example values, placeholder values, or IDs from tool descriptions.
//                    If the tool schema requires body, pass the complete request envelope inside body.
//                    """.formatted(referenceId);
//
////            String effectiveSystemPrompt = systemPrompt + "\n\n" + trustedContext;
//
//            String effectiveSystemPrompt = systemPrompt + "\n\n" + trustedContext;
//
//            log.info("Trusted referenceId={}", referenceId);
//            log.info("Runtime context appended={}",
//                    effectiveSystemPrompt.contains("referenceId = " + referenceId));
//
//            HarnessAgent agent = builder
//                    .sysPrompt(effectiveSystemPrompt)
//                    .build();

            log.info(
                    "Loaded prompt: id={}, ref={}, promptLength={}, runtimeReferenceId={}",
                    response.promptId(),
                    response.ref(),
                    systemPrompt.length(),
                    referenceId
            );

// 4. Build the agent with the effective prompt, not the original prompt
//            HarnessAgent agent = builder
//                    .sysPrompt(effectiveSystemPrompt)
//                    .build();

            // 5. Supply the current user and session context
            RuntimeContext context = RuntimeContext.builder()
                    .userId(request.userId())
                    .sessionId(request.sessionId())
                    .build();

            // 6. Run the agent and return its final text
            return agent.call(
                            new UserMessage(request.message()),
                            context)
                    .map(reply -> new ChatResponse(
                            request.sessionId(),
                            reply.getTextContent()))
                    .doFinally(signal -> agent.close());
        });
    }
}


//            return agent.call(
//                            new UserMessage(request.message()),
//                            context)
//                    .map(reply -> {
//                        String answer = reply.getTextContent();
//
//                        log.info("Agent reply type: {}", reply.getClass().getName());
//                        log.info("Agent reply content: {}", answer);
//
//                        if (answer == null || answer.isBlank()) {
//                            log.warn(
//                                    "Agent returned no final text. sessionId={}, referenceId={}",
//                                    request.sessionId(),
//                                    request.referenceId()
//                            );
//
//                            answer = "I could not retrieve the onboarding status. "
//                                    + "Please check the workflow tool response and try again.";
//                        }
//
//                        return new ChatResponse(request.sessionId(), answer);
//                    })
//                    .doFinally(signal -> agent.close());
//        });
//    }
//}

