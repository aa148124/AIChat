package com.example.aidemo.ai.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * 通用助手 - 处理一般性问题
 */
public interface GeneralAssistant {
    
    @SystemMessage("你是一个友好、专业的通用AI助手，能够帮助用户解答各种问题。请用简洁清晰的语言回答。")
    Flux<String> chat(@MemoryId String memoryId, @UserMessage String userMessage);
}
