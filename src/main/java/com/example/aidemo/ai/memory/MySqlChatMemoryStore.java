package com.example.aidemo.ai.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aidemo.ai.memory.entity.ChatMessageEntity;
import com.example.aidemo.ai.memory.mapper.ChatMessageMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于MySQL的聊天记忆存储
 * 实现短期持久化，支持消息窗口限制
 */
@Slf4j
@Component
public class MySqlChatMemoryStore implements ChatMemoryStore {

    @Resource
    private ChatMessageMapper chatMessageMapper;

    /**
     * 默认最大消息数（模拟 MessageWindowChatMemory 的行为）
     */
    private static final int DEFAULT_MAX_MESSAGES = 10;

    /**
     * 内存缓存，用于快速访问和窗口管理
     */
    private final Map<String, List<ChatMessage>> memoryCache = new ConcurrentHashMap<>();

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String id = memoryId.toString();
        
        // 先从缓存获取
        if (memoryCache.containsKey(id)) {
            return new ArrayList<>(memoryCache.get(id));
        }

        // 从数据库加载
        List<ChatMessage> messages = loadMessagesFromDb(id);
        memoryCache.put(id, messages);
        
        log.debug("从数据库加载会话消息: memoryId={}, 消息数={}", id, messages.size());
        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String id = memoryId.toString();
        
        // 应用消息窗口限制
        if (messages.size() > DEFAULT_MAX_MESSAGES) {
            messages = messages.subList(messages.size() - DEFAULT_MAX_MESSAGES, messages.size());
        }

        // 更新缓存
        memoryCache.put(id, new ArrayList<>(messages));

        // 保存到数据库
        saveMessagesToDb(id, messages);
        
        log.debug("更新会话消息: memoryId={}, 消息数={}", id, messages.size());
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String id = memoryId.toString();
        
        // 清除缓存
        memoryCache.remove(id);

        // 删除数据库记录
        chatMessageMapper.delete(
            new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getMemoryId, id)
        );
        
        log.debug("删除会话消息: memoryId={}", id);
    }

    /**
     * 从数据库加载消息
     */
    private List<ChatMessage> loadMessagesFromDb(String memoryId) {
        List<ChatMessage> messages = new ArrayList<>();
        
        List<ChatMessageEntity> entities = chatMessageMapper.selectList(
            new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getMemoryId, memoryId)
                .orderByAsc(ChatMessageEntity::getIndex)
        );

        for (ChatMessageEntity entity : entities) {
            ChatMessage message = convertToLangChainMessage(entity);
            if (message != null) {
                messages.add(message);
            }
        }

        return messages;
    }

    /**
     * 保存消息到数据库
     */
    private void saveMessagesToDb(String memoryId, List<ChatMessage> messages) {
        try {
            // 先删除旧数据
            chatMessageMapper.delete(
                new LambdaQueryWrapper<ChatMessageEntity>()
                    .eq(ChatMessageEntity::getMemoryId, memoryId)
            );

            // 批量插入新数据
            List<ChatMessageEntity> entities = new ArrayList<>();
            for (int i = 0; i < messages.size(); i++) {
                ChatMessage message = messages.get(i);
                ChatMessageEntity entity = convertToEntity(memoryId, message, i);
                entities.add(entity);
            }

            // 逐条插入
            for (ChatMessageEntity entity : entities) {
                chatMessageMapper.insert(entity);
            }

            log.debug("保存消息到数据库: memoryId={}, 消息数={}", memoryId, entities.size());
        } catch (Exception e) {
            log.error("保存消息到数据库失败: memoryId={}", memoryId, e);
        }
    }

    /**
     * 将数据库实体转换为 LangChain4j 消息
     */
    private ChatMessage convertToLangChainMessage(ChatMessageEntity entity) {
        return switch (entity.getMessageType()) {
            case "USER" -> new UserMessage(entity.getContent());
            case "ASSISTANT" -> new AiMessage(entity.getContent());
            case "SYSTEM" -> new SystemMessage(entity.getContent());
            default -> {
                log.warn("未知的消息类型: {}", entity.getMessageType());
                yield null;
            }
        };
    }

    /**
     * 将 LangChain4j 消息转换为数据库实体
     */
    private ChatMessageEntity convertToEntity(String memoryId, ChatMessage message, int index) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setMemoryId(memoryId);
        entity.setIndex(index);
        entity.setCreateTime(LocalDateTime.now());

        if (message instanceof UserMessage) {
            entity.setMessageType("USER");
            entity.setContent(((UserMessage) message).singleText());
        } else if (message instanceof AiMessage) {
            entity.setMessageType("ASSISTANT");
            entity.setContent(((AiMessage) message).text());
        } else if (message instanceof SystemMessage) {
            entity.setMessageType("SYSTEM");
            entity.setContent(((SystemMessage) message).text());
        } else {
            // 处理其他消息类型（如 ToolExecutionResultMessage 等）
            entity.setMessageType("OTHER");
            entity.setContent(message.toString());
            log.warn("遇到未处理的消息类型: {}, 使用toString()保存", message.getClass().getSimpleName());
        }

        // 确保 content 不为空
        if (entity.getContent() == null || entity.getContent().isBlank()) {
            entity.setContent("[空消息]");
        }

        return entity;
    }
}
