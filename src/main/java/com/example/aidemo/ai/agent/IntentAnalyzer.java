package com.example.aidemo.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 意图分析器 - 负责分析用户意图并决定下一步处理策略
 */
public interface IntentAnalyzer {
    
    @SystemMessage("""
        你是一个意图分析专家。分析用户的问题，判断需要什么类型的帮助。
        请返回以下 JSON 格式：
        {
          "intent": "意图类型（calculator/search/knowledge/general）",
          "summary": "用户需求摘要",
          "nextAction": "下一步处理建议"
        }
        """)
    String analyze(@UserMessage String userMessage);
}
