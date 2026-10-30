package com.example.ai.api;

import com.example.ai.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
 private final ChatService service;
 public ChatController(ChatService service) { this.service = service; }
 @PostMapping
 public Mono<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
  return service.chat(request);
 }
 public record ChatRequest(@NotBlank String userId,
                           @NotBlank String sessionId,
                           String referenceId,
                           @NotBlank String message) {}
 public record ChatResponse(String sessionId, String answer) {}
}
