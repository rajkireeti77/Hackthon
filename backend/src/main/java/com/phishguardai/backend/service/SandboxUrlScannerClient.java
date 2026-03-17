package com.phishguardai.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phishguardai.backend.config.SandboxScannerProperties;
import com.phishguardai.backend.dto.SandboxScanResultDto;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SandboxUrlScannerClient {
  private static final Logger log = LoggerFactory.getLogger(SandboxUrlScannerClient.class);

  private final RestClient restClient;
  private final SandboxScannerProperties props;
  private final ObjectMapper mapper;

  public SandboxUrlScannerClient(RestClient restClient, SandboxScannerProperties props, ObjectMapper mapper) {
    this.restClient = restClient;
    this.props = props;
    this.mapper = mapper;
  }

  public SandboxScanResultDto scanUrl(String url) {
    if (!props.isEnabled()) {
      return SandboxScanResultDto.disabled();
    }

    try {
      String raw =
          restClient
              .post()
              .uri(props.getBaseUrl() + props.getAnalyzePath())
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .body(Map.of("url", url))
              .retrieve()
              .body(String.class);

      if (raw == null || raw.isBlank()) {
        return SandboxScanResultDto.unavailable("Sandbox scanner returned an empty response.");
      }
      SandboxScanResultDto result = mapper.readValue(raw, SandboxScanResultDto.class);
      result.setEnabled(true);
      result.setAttempted(true);
      return result;
    } catch (Exception e) {
      log.warn("Sandbox URL scan failed for url={}: {}", url, e.toString());
      return SandboxScanResultDto.unavailable("Sandbox scanner could not be reached.");
    }
  }
}
