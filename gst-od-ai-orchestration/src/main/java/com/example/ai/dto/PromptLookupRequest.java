package com.example.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record PromptLookupRequest(
        @NotBlank String workflow,
        @NotBlank String kind
) {}