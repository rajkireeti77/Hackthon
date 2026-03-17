package com.phishguardai.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phishguardai.backend.config.SafeBrowsingProperties;
import com.phishguardai.backend.dto.SafeBrowsingResultDto;
import com.phishguardai.backend.util.exception.ExternalServiceException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SafeBrowsingClient {
  private final RestClient restClient;
  private final SafeBrowsingProperties props;
  private final ObjectMapper mapper;

  public SafeBrowsingClient(RestClient restClient, SafeBrowsingProperties props, ObjectMapper mapper) {
    this.restClient = restClient;
    this.props = props;
    this.mapper = mapper;
  }

  public SafeBrowsingResultDto checkUrl(String url) {
    if (props.getApiKey() == null || props.getApiKey().isBlank()) {
      return new SafeBrowsingResultDto(false, List.of());
    }
    try {
      String endpoint =
          props.getBaseUrl()
              + "/v4/threatMatches:find?key="
              + props.getApiKey();

      Map<String, Object> body =
          Map.of(
              "client",
              Map.of("clientId", props.getClientId(), "clientVersion", props.getClientVersion()),
              "threatInfo",
              Map.of(
                  "threatTypes",
                  List.of(
                      "MALWARE",
                      "SOCIAL_ENGINEERING",
                      "UNWANTED_SOFTWARE",
                      "POTENTIALLY_HARMFUL_APPLICATION"),
                  "platformTypes",
                  List.of("ANY_PLATFORM"),
                  "threatEntryTypes",
                  List.of("URL"),
                  "threatEntries",
                  List.of(Map.of("url", url))));

      String json =
          restClient
              .post()
              .uri(endpoint)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(String.class);

      if (json == null || json.isBlank()) {
        return new SafeBrowsingResultDto(false, List.of());
      }

      JsonNode root = mapper.readTree(json);
      JsonNode matches = root.get("matches");
      if (matches == null || !matches.isArray() || matches.isEmpty()) {
        return new SafeBrowsingResultDto(false, List.of());
      }

      List<String> threatTypes = new ArrayList<>();
      for (JsonNode m : matches) {
        JsonNode tt = m.get("threatType");
        if (tt != null && !tt.asText().isBlank() && !threatTypes.contains(tt.asText())) {
          threatTypes.add(tt.asText());
        }
      }
      return new SafeBrowsingResultDto(true, threatTypes);
    } catch (ExternalServiceException e) {
      throw e;
    } catch (Exception e) {
      throw new ExternalServiceException("Safe Browsing check failed.", e);
    }
  }
}

