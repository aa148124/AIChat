package com.example.aidemo.ai.memory;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于MySQL的聊天记忆实现
 * 使用 MessageWindowChatMemory 包装 MySqlChatMemoryStore
 */
@Slf4j
public class MySqlChatMemory {

    /**
     * 创建基于MySQL存储的聊天记忆
     * 
     * @param memoryId 会话ID
     * @param store MySQL聊天记忆存储
     * @return MessageWindowChatMemory实例
     */
    public static MessageWindowChatMemory create(Object memoryId, ChatMemoryStore store) {
        return MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(store)
                .build();
    }
}
