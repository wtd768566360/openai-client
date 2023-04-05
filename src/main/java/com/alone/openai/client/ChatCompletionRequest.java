package com.alone.openai.client;

import cn.hutool.core.util.RandomUtil;
import com.alone.openai.client.api.OpenAIAPI;
import com.alone.openai.client.entity.completion.ChatRequestDto;
import com.alone.openai.client.entity.completion.ChatResponseDto;
import com.alone.openai.client.exception.OpenAIException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;


/**
 * 请求
 */
@Component
public class ChatCompletionRequest {

    public final static String DONE = "[DONE]";

    public final static String BEARER = "Bearer";

    private final WebClient webClient;

    private List<String> propertiesApiKeyList;

    private List<String> databaseApiKeyList;

    public ChatCompletionRequest(WebClient webClient, @Qualifier("properties-api-keys") List<String> propertiesApiKeyList,
                                 @Qualifier("database-api-keys") List<String> databaseApiKeyList) {
        this.webClient = webClient;
        this.propertiesApiKeyList = propertiesApiKeyList;
        this.databaseApiKeyList = databaseApiKeyList;
    }

    /**
     * 只配置关键的参数role和message.其余的都使用接口默认参数
     *
     * @param role
     * @param message
     * @return
     */
    public Flux<ServerSentEvent<String>> sendServerSentEvent(String role, String message) {
        ChatRequestDto request = new ChatRequestDto();
        request.pushMessage(role, message);
        return sendServerSentEvent(request);
    }

    /**
     * 使用user权限发送
     *
     * @param message
     * @return
     */
    public Flux<ServerSentEvent<String>> sendServerSentEventByUser(String message) {
        ChatRequestDto request = new ChatRequestDto();
        request.pushMessage(ChatRequestDto.Role.USER.getName(), message);
        return sendServerSentEvent(request);
    }

    /**
     * 携带上下文发送
     *
     * @param messages
     * @return
     */
    public Flux<ServerSentEvent<String>> sendServerSentEvent(List<ChatRequestDto.Message> messages) {
        ChatRequestDto request = new ChatRequestDto();
        for (ChatRequestDto.Message message : messages) {
            request.pushMessage(message);
        }
        return sendServerSentEvent(request);
    }

    /**
     * 获取接口返回的结果集,已经经过数据解析,
     * 获取到的是解析后的内容数据,并可以通过事件直接发送给前端
     *
     * @param request 要发送请求数据
     * @return
     */
    public Flux<ServerSentEvent<String>> sendServerSentEvent(ChatRequestDto request) {
        String apiKey = "";
        if (databaseApiKeyList != null && databaseApiKeyList.size() > 0) {
            //优先使用数据库的key
            apiKey = RandomUtil.randomEle(databaseApiKeyList);
        } else if (propertiesApiKeyList != null && propertiesApiKeyList.size() > 0) {
            //再次使用配置文件的的key
            apiKey = RandomUtil.randomEle(propertiesApiKeyList);
        } else {
            //都没有就报错
            throw new OpenAIException("没有openai的key,无法启动核心功能");
        }
        return send(request, apiKey)
                .map(string -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    // 将 JSON 字符串转换为 Java 对象
                    String content = DONE;
                    String sseId = DONE;
                    if (!DONE.equals(string)) {
                        ChatResponseDto chatResponseDto = null;
                        try {
                            chatResponseDto = objectMapper.readValue(string, ChatResponseDto.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        content = chatResponseDto.getChoices().get(0).getDelta().getContent();
                        sseId = chatResponseDto.getId();
                    }
                    return ServerSentEvent.builder(content)
                            .id(sseId)
                            .retry(Duration.ofSeconds(5))
                            .build();
                });
    }

    /**
     * 获取接口返回的原始JSON字符串
     *
     * @param request 要发送请求数据
     * @return
     */
    public Flux<String> send(ChatRequestDto request, String apiKey) {
        return webClient.post()
                .uri(OpenAIAPI.CHAT_COMPLETIONS)
                .header(HttpHeaders.AUTHORIZATION, BEARER + " " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .bodyToFlux(String.class);
    }
}
