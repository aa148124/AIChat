package com.example.aidemo.ai.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * 专业知识回答器 - 基于知识库回答专业问题
 */
public interface KnowledgeExpert {
    
    @SystemMessage("你是一个专业的技术知识库助手，擅长根据检索到的文档内容回答用户问题。回答时请引用相关知识来源。")
    Flux<String> answer(@MemoryId String memoryId, @UserMessage String question);
}
