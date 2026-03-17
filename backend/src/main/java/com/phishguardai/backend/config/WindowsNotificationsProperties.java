package com.phishguardai.backend.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.incoming.windows-notifications")
public class WindowsNotificationsProperties {
  private boolean enabled;
  private String userId = "demo";
  private String dbPath;
  private long pollIntervalMs = 5000;
  private List<String> allowedApps =
      new ArrayList<>(List.of("5319275A.WhatsAppDesktop_cv1g1gvanyjgm!App", "WhatsApp", "whatsapp"));
  private List<String> ignoredPayloadTypes = new ArrayList<>(List.of("badge", "tile"));

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getDbPath() {
    return dbPath;
  }

  public void setDbPath(String dbPath) {
    this.dbPath = dbPath;
  }

  public long getPollIntervalMs() {
    return pollIntervalMs;
  }

  public void setPollIntervalMs(long pollIntervalMs) {
    this.pollIntervalMs = pollIntervalMs;
  }

  public List<String> getAllowedApps() {
    return allowedApps;
  }

  public void setAllowedApps(List<String> allowedApps) {
    this.allowedApps = allowedApps == null ? new ArrayList<>() : allowedApps;
  }

  public List<String> getIgnoredPayloadTypes() {
    return ignoredPayloadTypes;
  }

  public void setIgnoredPayloadTypes(List<String> ignoredPayloadTypes) {
    this.ignoredPayloadTypes = ignoredPayloadTypes == null ? new ArrayList<>() : ignoredPayloadTypes;
  }
}
