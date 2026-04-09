package com.example.aidemo.ai.controller;

import com.example.aidemo.ai.AiCodeHelperService;
import com.example.aidemo.ai.agent.ChainedAgent;
import jakarta.annotation.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    @Resource
    private AiCodeHelperService aiCodeHelperService;
    
    @Resource
    private ChainedAgent chainedAgent;

    @GetMapping("/chat")
    public Flux<ServerSentEvent<String>> chat(@RequestParam("memoryId") String memoryId, @RequestParam("message") String message) {
        return aiCodeHelperService.chatStream(memoryId, message)
                .map(text -> ServerSentEvent.<String>builder()
                        .data(text)
                        .build());
    }

    /**
     * 链式智能体接口（支持多AI协同）
     */
    @GetMapping("/agent/chat")
    public Flux<ServerSentEvent<String>> agentChat(@RequestParam("userId") String userId, @RequestParam("message") String message) {
        return chainedAgent.executeStream(userId, message)
                .map(text -> ServerSentEvent.<String>builder()
                        .data(text)
                        .build());
    }
}
