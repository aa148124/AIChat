package com.example.aidemo.ai.rag;

import com.example.aidemo.ai.AiCodeHelperService;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class RagConfigTest {

    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @Test
    public void contentRetriever() {
        String chat = aiCodeHelperService.chat("kill是什么意思");
        System.out.println(chat);
    }
}