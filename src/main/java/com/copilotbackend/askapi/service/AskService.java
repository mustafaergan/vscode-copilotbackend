package com.copilotbackend.askapi.service;

import com.copilotbackend.askapi.dto.AskResponse;
public interface AskService {

    AskResponse ask(String question);
}
