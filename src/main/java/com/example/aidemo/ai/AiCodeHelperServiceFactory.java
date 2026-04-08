package com.example.aidemo.ai;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AiCodeHelperServiceFactory {

    @Resource
    private ChatModel qwenChatModel;
    @Resource
    private StreamingChatModel qwenStreamingChatModel;
    @Resource
    private ContentRetriever contentRetriever;
    @Resource
    private McpToolProvider mcpToolProvider;

    @Bean
    public AiCodeHelperService aiCodeHelperService() {
        //会话记忆 - 使用 ChatMemoryProvider 支持多会话
        ChatMemoryProvider chatMemoryProvider = memoryId -> 
                MessageWindowChatMemory.withMaxMessages(10);

        return AiServices.builder(AiCodeHelperService.class)
                //.chatModel(qwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .contentRetriever(contentRetriever)
                .toolProvider(mcpToolProvider)
                .build();
    }
}
