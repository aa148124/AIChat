package com.example.aidemo.ai.agent;

import com.example.aidemo.ai.memory.MySqlChatMemory;
import com.example.aidemo.ai.memory.MySqlChatMemoryStore;
import com.example.aidemo.ai.tools.CalculatorTool;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 智能体工厂 - 创建多个独立的 ServiceAI 实例
 */
@Configuration
public class AgentFactory {

    @Resource
    private ChatModel qwenChatModel;
    
    @Resource
    private StreamingChatModel qwenStreamingChatModel;
    
    @Resource
    private ContentRetriever contentRetriever;
    
    @Resource
    private MySqlChatMemoryStore mySqlChatMemoryStore;

    @Bean
    public IntentAnalyzer intentAnalyzer() {
        return AiServices.builder(IntentAnalyzer.class)
                .chatModel(qwenChatModel)
                .build();
    }

    @Bean
    public KnowledgeExpert knowledgeExpert() {
        ChatMemoryProvider chatMemoryProvider = memoryId -> 
                MySqlChatMemory.create(memoryId, mySqlChatMemoryStore);
                
        return AiServices.builder(KnowledgeExpert.class)
                .chatModel(qwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .contentRetriever(contentRetriever)
                .build();
    }

    @Bean
    public GeneralAssistant generalAssistant() {
        ChatMemoryProvider chatMemoryProvider = memoryId -> 
                MySqlChatMemory.create(memoryId, mySqlChatMemoryStore);
                
        return AiServices.builder(GeneralAssistant.class)
                .chatModel(qwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(new CalculatorTool())
                .build();
    }
}
