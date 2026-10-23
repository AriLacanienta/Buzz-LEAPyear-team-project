package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.dto.ChatRequest;
import com.buzzleapyear.trading_api.dto.ChatResponse;
import com.buzzleapyear.trading_api.service.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
        @Valid @RequestBody ChatRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            chatbotService.respond(request.message(), authentication.getName())
        );
    }
}