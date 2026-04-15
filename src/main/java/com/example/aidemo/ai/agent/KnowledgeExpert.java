package com.example.aidemo.ai.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * 专业知识回答器 - 基于知识库回答专业问题
 */
public interface KnowledgeExpert {
    
    @SystemMessage(fromResource = "system-message.txt")
    Flux<String> answer(@MemoryId String memoryId, @UserMessage String question);
}
