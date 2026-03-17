package com.phishguardai.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.local-llm")
public class LocalLlmProperties {
  private boolean enabled;
  private String baseUrl = "http://localhost:8001";
  private String analyzePath = "/analyze";
  private int timeoutMs = 60000;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getAnalyzePath() {
    return analyzePath;
  }

  public void setAnalyzePath(String analyzePath) {
    this.analyzePath = analyzePath;
  }

  public int getTimeoutMs() {
    return timeoutMs;
  }

  public void setTimeoutMs(int timeoutMs) {
    this.timeoutMs = timeoutMs;
  }
}
