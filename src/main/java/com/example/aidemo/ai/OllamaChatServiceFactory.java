package com.example.aidemo.ai;

import com.example.aidemo.ai.memory.MySqlChatMemory;
import com.example.aidemo.ai.memory.MySqlChatMemoryStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaChatServiceFactory {

    @Resource
    @Qualifier("ollamaChatModel")
    private ChatModel ollamaChatModel;

    @Resource
    @Qualifier("ollamaStreamingChatModel")
    private StreamingChatModel ollamaStreamingChatModel;

    @Resource
    private MySqlChatMemoryStore mySqlChatMemoryStore;

    @Bean
    public OllamaChatService ollamaChatService() {
        ChatMemoryProvider chatMemoryProvider = memoryId ->
                MySqlChatMemory.create(memoryId, mySqlChatMemoryStore);

        return AiServices.builder(OllamaChatService.class)
                .chatModel(ollamaChatModel)
                .streamingChatModel(ollamaStreamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }
}
