package com.phishguardai.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sandbox-scanner")
public class SandboxScannerProperties {
  private boolean enabled;
  private String baseUrl = "http://localhost:8002";
  private String analyzePath = "/analyze";

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
}
