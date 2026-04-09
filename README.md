# AIChat - 基于 LangChain4j 的智能对话系统

基于 Spring Boot 和 LangChain4j 构建的多智能体 AI 对话系统，支持流式响应、知识库检索、工具调用和意图路由。

## ✨ 核心特性

- 🤖 **多智能体架构** - 链式智能体编排，意图分析 + 专业处理器协同工作
- 📡 **流式响应** - 基于 SSE (Server-Sent Events) 的实时输出
- 🧠 **知识库检索 (RAG)** - 支持本地文档检索增强生成
- 🔧 **工具调用** - 集成计算器工具和 MCP (Model Context Protocol)
- 💬 **多会话管理** - 支持多用户并发对话，独立记忆上下文
- 🛡️ **输入防护** - 自定义 Guardrail 机制保障输入安全

## 🏗️ 架构设计

### 链式智能体工作流

```
用户输入 → IntentAnalyzer (意图分析) → 路由决策
    ├─ knowledge → KnowledgeExpert (RAG 知识库)
    ├─ calculator → GeneralAssistant (计算工具)
    └─ general → GeneralAssistant (通用回答)
```

### 技术栈

| 组件 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.5 |
| AI 框架 | LangChain4j |
| AI 模型 | 通义千问 (DashScope) |
| 流式响应 | Reactor Flux + SSE |
| 向量检索 | Embedding + ContentRetriever |
| 工具协议 | MCP (Model Context Protocol) |

## 🚀 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- 通义千问 API Key

### 配置

1. 复制配置文件：
```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

2. 编辑 `application-local.yml`，配置你的 API Key：
```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: your-api-key
      model-name: qwen-max
```

### 启动

```bash
# 使用 Maven Wrapper
./mvnw spring-boot:run

# 或直接运行
mvn spring-boot:run
```

## 📡 API 接口

### 1. 标准对话接口

```http
GET /ai/chat?memoryId=user1&message=你好
```

**响应示例 (SSE 流)：**
```
event: message
data: 你

event: message
data: 好

event: message
data: ！
```

### 2. 链式智能体接口

```http
GET /ai/agent/chat?userId=user1&message=如何安装MySQL？
```

智能体自动分析意图并路由到对应处理器。

## 📁 项目结构

```
AIDemo/
├── src/main/java/com/example/aidemo/
│   ├── ai/
│   │   ├── agent/              # 多智能体模块
│   │   │   ├── AgentFactory.java           # 智能体工厂
│   │   │   ├── ChainedAgent.java           # 链式编排器
│   │   │   ├── IntentAnalyzer.java         # 意图分析器
│   │   │   ├── KnowledgeExpert.java        # 知识库专家
│   │   │   └── GeneralAssistant.java       # 通用助手
│   │   ├── config/             # 配置模块
│   │   │   ├── mcp/            # MCP 配置
│   │   │   └── rag/            # RAG 配置
│   │   ├── controller/         # 控制器
│   │   │   └── AiChatController.java
│   │   ├── guardrail/          # 输入防护
│   │   │   └── MyInputGuardrail.java
│   │   ├── tools/              # 自定义工具
│   │   │   └── CalculatorTool.java
│   │   ├── AiCodeHelperService.java
│   │   └── AiCodeHelperServiceFactory.java
│   └── AiDemoApplication.java
├── src/main/resources/
│   ├── doc/                    # 知识库文档
│   ├── application.yml
│   ├── application-local.yml
│   └── system-message.txt
└── pom.xml
```

## 🧪 测试

运行单元测试：

```bash
./mvnw test
```

测试链式智能体：

```bash
./mvnw test -Dtest=ChainedAgentTest
```

## 🔧 扩展开发

### 添加新的智能体

1. 定义 ServiceAI 接口：
```java
public interface MySpecialist {
    @SystemMessage("你是专业助手...")
    Flux<String> handle(@MemoryId String id, @UserMessage String message);
}
```

2. 在 `AgentFactory` 中注册：
```java
@Bean
public MySpecialist mySpecialist() {
    return AiServices.builder(MySpecialist.class)
            .chatModel(qwenChatModel)
            .streamingChatModel(qwenStreamingChatModel)
            .build();
}
```

3. 在 `ChainedAgent` 中添加路由逻辑。

### 添加自定义工具

```java
@Tool("计算两个数的和")
public double add(double a, double b) {
    return a + b;
}
```

## 📝 配置文件说明

- `application.yml` - 主配置（提交到 Git）
- `application-local.yml` - 本地配置（包含 API Key，已忽略提交）

## ⚠️ 注意事项

- `application-local.yml` 包含敏感信息，不会提交到 Git
- 首次使用需要配置通义千问 API Key
- 流式响应需要使用支持 SSE 的客户端

## 📄 License

MIT License
