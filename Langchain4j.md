# 一、导入依赖

```yaml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai-spring-boot4-starter</artifactId>
    <version>1.18.1-beta28</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-spring-boot4-starter</artifactId>
    <version>1.18.1-beta28</version>
</dependency>

<!-- 流式输出 -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-reactor</artifactId>
    <version>1.18.1-beta28</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-community-redis-spring-boot-starter</artifactId>
    <version>1.18.0-beta28</version>
    <!-- 高版本对redis有要求，需要Redis Stack  -->
    <!-- <version>1.1.0-beta7</version> -->
</dependency>
```

# 二、配置文件
> ```yaml
> langchain4j:
>   open-ai:
>     chat-model:
>       base-url: https://api.deepseek.com
>       api-key: <Your API Key>
>       model-name: deepseek-chat
>       log-requests: true
>       log-responses: true
> ```
>

# 三 、AI Services
```java
public interface AiCodeGeneratorService {
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    String generateHtmlCode(String userMessage);
}
```
```java
@Configuration
public class AiCodeGeneratorServiceFactory {
    @Resource
    private ChatModel chatModel;
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return AiServices.create(AiCodeGeneratorService.class, chatModel);
    }
}
```

# 四、结构化输出
1. 在提示词中明确要求返回json
2. 模型官网建议：max-token: 8192
3. Json Schema
4. 添加文字描述
```yaml
langchain4j:
  open-ai:
    chat-model:
      max-token: 8192
      strict-json-schema: true
      response-format: json_object
```
```java
@Description("生成 HTML 代码文件的结果")
@Data
public class HtmlCodeResult {
    @Description("HTML代码")
    private String htmlCode;

    @Description("生成代码的描述")
    private String description;
}
```
```java
@SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
HtmlCodeResult generateHtmlCode(String userMessage);
```

# 五、流式输出
```yaml
langchain4j:
  open-ai:
    streaming-chat-model:
      base-url: https://api.deepseek.com
      api-key: <Your API Key>
      model-name: deepseek-chat
      max-tokens: 8192
      log-requests: true
      log-responses: true

```

```java
public interface AiCodeGeneratorService {
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);
}
```

```java
@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private StreamingChatModel streamingChatModel;

    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return AiServices.builder(AiCodeGeneratorService.class)
                .streamingChatModel(streamingChatModel)
                .build();
    }
}
```

**处理流式返回结果**
```java
private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
    Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
    // 当流式返回生成代码完成后，再保存代码
    StringBuilder codeBuilder = new StringBuilder();
    return result
            .doOnNext(chunk -> {
                // 实时收集代码片段
                codeBuilder.append(chunk);
            })
            .doOnComplete(() -> {
                // 流式返回完成后保存代码
                try {
                    String completeHtmlCode = codeBuilder.toString();
                    HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(completeHtmlCode);
                    File savedDir = CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
                    log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
                } catch (Exception e) {
                    log.error("保存失败: {}", e.getMessage());
                }
            });
}
```

```java
@GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                   @RequestParam String message,
                                                   HttpServletRequest request) {
    // 调用服务生成代码（流式）
    Flux<String> contentFlux = generateAndSaveHtmlCodeStream(message);
    // 转换为 ServerSentEvent 格式
    return contentFlux
            .map(chunk -> {
                // 将内容包装成JSON对象
                Map<String, String> wrapper = Map.of("d", chunk);
                String jsonData = JSONUtil.toJsonStr(wrapper);
                return ServerSentEvent.<String>builder()
                        .data(jsonData)
                        .build();
            })
            .concatWith(Mono.just(
                    // 发送结束事件
                    ServerSentEvent.<String>builder()
                            .event("done")
                            .data("")
                            .build()
            ));
}

```

# 六、对话记忆
内置隔离实现
```yaml
spring:
  # redis
  data:
    redis:
      host: localhost
      port: 6379
      password: 
      ttl: 3600
```
```java
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryStoreConfig {

    private String host;

    private int port;

    private String password;

    private long ttl;

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        return RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .password(password)
                .ttl(ttl)
                .user("default")
                .build();
    }
}

// 启动类上添加下面代码排除embedding自动装配：
//@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
```
```java
// AI Services中接口
@SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
Flux<String> generateHtmlCodeStream(@MemoryId int memoryId, @UserMessage String userMessage);

//工厂创建AI Services
@Resource
private RedisChatMemoryStore redisChatMemoryStore;

@Bean
public AiCodeGeneratorService aiCodeGeneratorService() {
    return AiServices.builder(AiCodeGeneratorService.class)
            .chatModel(chatModel)
            .streamingChatModel(streamingChatModel)
            // 根据 id 构建独立的对话记忆
            .chatMemoryProvider(memoryId -> {
                Long appId = Long.valueOf(memoryId.toString());
                MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                        .builder()
                        .id(appId)
                        .chatMemoryStore(redisChatMemoryStore)
                        .maxMessages(20)
                        .build();
                chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
                return chatMemory;
            })
            .build();
}
```
创建多个AI Services来实现隔离
```java
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;
    @Resource
    private StreamingChatModel openAiStreamingChatModel;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<Long, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 创建新的 AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库中加载对话历史在记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(openAiStreamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * 根据 appId 获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return serviceCache.get(appId, this::createAiCodeGeneratorService);
    }
}
```
# 七、工具调用
```java
@Slf4j
public class FileWriteTool {

    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要写入文件的内容")
            String content,
            @ToolMemoryId Long appId
    ) {
        // 具体实现    
    }
}
```

```java
 private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
    // 根据 appId 构建独立的对话记忆
    MessageWindowChatMemory chatMemory = MessageWindowChatMemory
            .builder()
            .id(appId)
            .chatMemoryStore(redisChatMemoryStore)
            .maxMessages(20)
            .build();
    // 从数据库加载历史对话到记忆中
    chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
    return AiServices.builder(AiCodeGeneratorService.class)
            .streamingChatModel(reasoningStreamingChatModel)
            .chatMemoryProvider(memoryId -> chatMemory)
            .tools(new FileWriteTool())
            .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                    toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
            ))
            .build();
}
```

```java
@SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
Flux<String> generateVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);
```
