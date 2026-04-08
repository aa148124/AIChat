package com.example.aidemo.ai;

import com.example.aidemo.ai.guardrail.MyInputGuardrail;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import reactor.core.publisher.Flux;

@InputGuardrails({MyInputGuardrail.class})
public interface AiCodeHelperService {

    @SystemMessage(fromResource = "system-message.txt")
    String chat(String userMessage);

    @SystemMessage(fromResource = "system-message.txt")
    Flux<String> chatStream(@MemoryId String memoryId, @UserMessage String userMessage);
}
