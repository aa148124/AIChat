package com.example.aidemo.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.guardrail.InputGuardrailResult;

public class MyInputGuardrail implements InputGuardrail {
    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        if (userMessage.singleText().contains("kill")) {
            return failure("You are not allowed to use the word 'kill'.");
        }
        return success();
    }

}
