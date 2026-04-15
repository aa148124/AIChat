package com.example.aidemo.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface OllamaChatService {

    @SystemMessage("你是一个基于Ollama本地部署的智能助手，使用qwen3-vl模型。你擅长理解和回答各类问题，请用中文回答。")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);

    @SystemMessage("你是一个基于Ollama本地部署的智能助手，使用qwen3-vl模型。你擅长理解和回答各类问题，请用中文回答。")
    Flux<String> chatStream(@MemoryId String memoryId, @UserMessage String userMessage);
}
