package com.alone.openai.client.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "openai")
public class OpenAIProperties {

    private List<String> apikey;

    private Proxy proxy;

    @Data
    public static class Proxy {
        //代理IP
        private String host;
        //代理端口
        private Integer port;
    }
}
