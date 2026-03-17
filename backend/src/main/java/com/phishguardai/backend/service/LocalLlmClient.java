package com.phishguardai.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phishguardai.backend.config.LocalLlmProperties;
import com.phishguardai.backend.dto.GeminiAnalysisDto;
import com.phishguardai.backend.util.exception.ExternalServiceException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LocalLlmClient {
  private static final Logger log = LoggerFactory.getLogger(LocalLlmClient.class);

  private final RestClient restClient;
  private final LocalLlmProperties props;
  private final ObjectMapper mapper;

  public LocalLlmClient(RestClient restClient, LocalLlmProperties props, ObjectMapper mapper) {
    this.restClient = restClient;
    this.props = props;
    this.mapper = mapper;
  }

  public boolean isEnabled() {
    return props.isEnabled();
  }

  public GeminiAnalysisDto analyze(
      String textContent, String url, String fileContext, boolean safeBrowsingFlagged, List<String> threatTypes) {
    if (!props.isEnabled()) {
      throw new ExternalServiceException("Local LLM is disabled.");
    }

    Map<String, Object> body =
        Map.of(
            "textContent", textContent == null ? "" : textContent,
            "url", url == null ? "" : url,
            "fileContext", fileContext == null ? "" : fileContext,
            "safeBrowsingFlagged", safeBrowsingFlagged,
            "threatTypes", threatTypes == null ? List.of() : threatTypes);

    try {
      String raw =
          restClient
              .post()
              .uri(props.getBaseUrl() + props.getAnalyzePath())
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(String.class);

      if (raw == null || raw.isBlank()) {
        throw new ExternalServiceException("Local LLM returned an empty response.");
      }
      return mapper.readValue(raw, GeminiAnalysisDto.class);
    } catch (ExternalServiceException e) {
      throw e;
    } catch (Exception e) {
      log.warn("Local LLM request failed: {}", e.toString());
      throw new ExternalServiceException("Local LLM analysis failed.", e);
    }
  }
}
