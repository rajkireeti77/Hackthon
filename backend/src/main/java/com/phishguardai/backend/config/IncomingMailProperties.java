package com.phishguardai.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.incoming.mail")
public class IncomingMailProperties {
  private boolean enabled;
  private String userId = "demo";
  private String host;
  private int port = 993;
  private String protocol = "imaps";
  private String username;
  private String password;
  private String folder = "INBOX";
  private long pollIntervalMs = 30000;
  private int maxMessagesPerPoll = 10;
  private boolean markSeenOnProcess = true;

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

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFolder() {
    return folder;
  }

  public void setFolder(String folder) {
    this.folder = folder;
  }

  public long getPollIntervalMs() {
    return pollIntervalMs;
  }

  public void setPollIntervalMs(long pollIntervalMs) {
    this.pollIntervalMs = pollIntervalMs;
  }

  public int getMaxMessagesPerPoll() {
    return maxMessagesPerPoll;
  }

  public void setMaxMessagesPerPoll(int maxMessagesPerPoll) {
    this.maxMessagesPerPoll = maxMessagesPerPoll;
  }

  public boolean isMarkSeenOnProcess() {
    return markSeenOnProcess;
  }

  public void setMarkSeenOnProcess(boolean markSeenOnProcess) {
    this.markSeenOnProcess = markSeenOnProcess;
  }
}
