package com.copilotbackend.askapi.dto;

import java.util.List;

public record AskResponse(
        String question,
        String answer,
        List<String> sources
) {
}
