package com.copilotbackend.askapi.controller;

import com.copilotbackend.askapi.dto.AskResponse;
import com.copilotbackend.askapi.service.AskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AskController {

    private static final Logger logger = LoggerFactory.getLogger(AskController.class);

    private final AskService askService;

    public AskController(AskService askService) {
        this.askService = askService;
    }

    @GetMapping("/ask")
    public ResponseEntity<AskResponse> ask(@RequestParam(required = false) String question) {
        logger.info("Received GET /api/ask request");
        return ResponseEntity.ok(askService.ask(question));
    }
}
