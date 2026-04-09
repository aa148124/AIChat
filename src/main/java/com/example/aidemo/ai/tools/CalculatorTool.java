package com.example.aidemo.ai.tools;

import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.http.HttpUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
@Slf4j
@Component
public class CalculatorTool {

    @Tool(name = "calculator", value = """
            当用户需要进行数学计算加法时必须调用此工具，绝对不要自己计算，必须调用工具，并返回结果。例如：1 + 2 = 3
            """)
    public int add(int a, int b) {
        log.info("✅ AI 调用了计算器：" + a + " + " + b);
        return a + b;
    }
}
