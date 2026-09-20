package com.aquanexus.controller;

import com.aquanexus.service.AIAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for the domain-specific AquaNexus AI Assistant.
 * Endpoint: POST /api/ai/chat
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIAssistantService aiAssistantService;

    public AIController(AIAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, String> request) {
        String prompt = request != null ? request.get("prompt") : "";
        Map<String, Object> result = aiAssistantService.processUserPrompt(prompt);
        return ResponseEntity.ok(result);
    }
}
