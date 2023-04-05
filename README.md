# ChatGPT Java Client

OpenAI ChatGPT 的客户端封装。欢迎指教

#### 欢迎加入技术讨论群,QQ群:558133902

# 技术实现

#### spring-boot-starter-webflux

#### 基于SpringBootWebFlux内核,使用Flux/Mono模型,

#### 无缝衔接SpringBootWebFlux项目

# 已实现

|    功能     | 特性  |
|:---------:|:---:|
|   流式请求    | 支持  |
| 多KEY随机轮询  | 支持  |
| 配置文件KEY注入 | 支持  |
| 数据库KEY注入  | 支持  |
|   请求代理    | 支持  |
|  GPT 3.5  | 支持  |

### 简单使用

#### 目前项目还没有打包发布MAVEN,需要下载本地手动打包
```text
pom文件里面没有配置打包成全量包的内容,目前打包是仅仅打包本项目的代码
使用的时候需要将依赖也放进去.
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
    <version>2.7.1</version>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <version>2.7.1</version>
</dependency>

<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
    <version>1.18.24</version>
</dependency>

<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.16</version>
</dependency>
```

#### application.yml配置

```yaml
openai:
  api_key:
    - 你的 API KEY
    - 你的 API KEY
  proxy:
  host: 代理IP地址
  port: 代理端口

```

#### controller 简单发送代码

```java
private ChatCompletionRequest chatCompletionRequest;

public ChatController(ChatCompletionRequest chatCompletionRequest){
        this.chatCompletionRequest=chatCompletionRequest;
        }

@GetMapping("search")
public Flux<ServerSentEvent<String>>search(@RequestParam String message){
        //chatCompletionRequest拥有好几个可用接口,此处只演示一个
        return chatCompletionRequest.sendServerSentEventByUser(message);
        }
```

#### controller 携带上下文发送代码

```java
private ChatCompletionRequest chatCompletionRequest;

public ChatController(ChatCompletionRequest chatCompletionRequest){
        this.chatCompletionRequest=chatCompletionRequest;
        }

@GetMapping("context/search")
public Flux<ServerSentEvent<String>>search(){
        //此处直接写死因为简单粗暴简单易理解;真实使用,需要根据前端传递参数哦.
        //此处messages是本次聊天的所有聊天记录上下文
        List<ChatRequestDto.Message> messages=new ArrayList<>();
        //问
        messages.add(new ChatRequestDto.Message(ChatRequestDto.Role.USER.getName(), "我现在要你扮演一直小猫和我对话"));
        //AI的回答的聊天记录
        messages.add(new ChatRequestDto.Message(ChatRequestDto.Role.SYSTEM.getName(), "好的，我可以扮演一只小猫和你对话。你好啊，主人，我是一只小猫，你想跟我聊些什么呢？"));
        //继续问
        messages.add(new ChatRequestDto.Message(ChatRequestDto.Role.USER.getName(), "你可以干嘛"));
        return chatCompletionRequest.sendServerSentEvent(messages);
}
```

### 读取数据库的KEY

```java

@Configuration
public class DatabaseOpenAIConfig implements OpenAIConfig {
    //你的SpringBoot服务实现接口
    private final ApiKeyService apiKeyService;

    public DatabaseOpenAIConfig(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    public List<String> getDatabaseOpenAIKey() {
        //查询到你的所有的存在KEY
        List<ApiKey> apiKeys = apiKeyService.findAll();
        //将所有的KEY以List<String>模式返回
        return apiKeys.stream()
                .map(ApiKey::getKey)
                .collect(Collectors.toList());
    }
}
```

### 读取数据库的KEY与配置文件的KEY关系

```text
先使用数据库的KEY,如果数据库的没有KEY,则使用配置文件的KEY,如果配置文件也没有KEY,将抛出RuntimeException
```





