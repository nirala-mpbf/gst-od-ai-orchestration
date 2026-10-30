package com.example.ai.api;

import com.example.ai.dto.PromptLookupRequest;
import com.example.ai.dto.PromptResponse;
import com.example.ai.dto.SavePromptResponse;
import com.example.ai.exception.LakefsException;
import com.example.ai.service.PromptService;
import io.lakefs.clients.sdk.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/prompts")
public class PromptController {

    private final PromptService prompts;

    public PromptController(PromptService prompts) {
        this.prompts = prompts;
    }

    @PostMapping(headers = "X-Action=upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SavePromptResponse uploadFile(@RequestPart("file") MultipartFile file, @Valid @RequestPart("metadata") PromptLookupRequest request) throws ApiException {

        if (file.isEmpty()) {
            throw new LakefsException(HttpStatus.BAD_REQUEST, "Markdown file must not be empty");
        }

        String filename = file.getOriginalFilename();

        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".md")) {
            throw new LakefsException(HttpStatus.BAD_REQUEST, "Only .md files are accepted");
        }

        return prompts.save(request.workflow(), request.kind(), file.getResource());
    }

    @PostMapping(headers = "X-Action=latest", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PromptResponse latest(@Valid @RequestBody PromptLookupRequest request) {

        return prompts.latest(request.workflow(), request.kind());
    }


}
