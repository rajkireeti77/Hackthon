package com.phishguardai.backend.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {
  @Bean
  public RestClient restClient() {
    var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    var requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(20));
    return RestClient.builder().requestFactory(requestFactory).build();
  }
}

