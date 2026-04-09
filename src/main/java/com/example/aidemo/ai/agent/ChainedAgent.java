package com.example.aidemo.ai.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 链式智能体 - 编排多个 ServiceAI 协同工作
 */
@Service
public class ChainedAgent {

    private static final Logger log = LoggerFactory.getLogger(ChainedAgent.class);
    
    @Resource
    private IntentAnalyzer intentAnalyzer;
    
    @Resource
    private KnowledgeExpert knowledgeExpert;
    
    @Resource
    private GeneralAssistant generalAssistant;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 非流式链式调用
     * 
     * @param userId 用户ID
     * @param userMessage 用户消息
     * @return 最终响应
     */
    public String execute(String userId, String userMessage) {
        // 第一步：意图分析
        log.info("步骤1: 分析用户意图 - {}", userMessage);
        String analysisResult = intentAnalyzer.analyze(userMessage);
        log.info("意图分析结果: {}", analysisResult);

        // 解析意图
        String intent = extractIntent(analysisResult);
        
        // 第二步：根据意图路由到对应处理器
        log.info("步骤2: 路由到处理器 - intent={}", intent);
        return switch (intent) {
            case "knowledge" -> {
                // 知识库问题，同步调用
                yield knowledgeExpert.answer(userId, userMessage)
                        .collectList()
                        .block()
                        .stream()
                        .reduce("", String::concat);
            }
            case "calculator" -> {
                // 计算问题
                yield generalAssistant.chat(userId, "请帮我计算: " + userMessage)
                        .collectList()
                        .block()
                        .stream()
                        .reduce("", String::concat);
            }
            default -> {
                // 一般问题
                yield generalAssistant.chat(userId, userMessage)
                        .collectList()
                        .block()
                        .stream()
                        .reduce("", String::concat);
            }
        };
    }

    /**
     * 流式链式调用
     * 
     * @param userId 用户ID
     * @param userMessage 用户消息
     * @return 流式响应
     */
    public Flux<String> executeStream(String userId, String userMessage) {
        // 第一步：意图分析（非流式，快速获取意图）
        log.info("步骤1: 分析用户意图 - {}", userMessage);
        String analysisResult = intentAnalyzer.analyze(userMessage);
        String intent = extractIntent(analysisResult);
        log.info("步骤2: 路由到处理器 - intent={}", intent);

        // 第二步：根据意图路由到对应处理器（流式）
        return switch (intent) {
            case "knowledge" -> knowledgeExpert.answer(userId, userMessage);
            case "calculator" -> generalAssistant.chat(userId, "请帮我计算: " + userMessage);
            default -> generalAssistant.chat(userId, userMessage);
        };
    }

    /**
     * 从分析结果中提取意图
     */
    private String extractIntent(String analysisResult) {
        try {
            JsonNode json = objectMapper.readTree(analysisResult);
            return json.has("intent") ? json.get("intent").asText() : "general";
        } catch (JsonProcessingException e) {
            log.warn("解析意图失败，使用默认处理器: {}", e.getMessage());
            return "general";
        }
    }
}
