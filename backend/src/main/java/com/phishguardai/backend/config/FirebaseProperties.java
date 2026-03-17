package com.phishguardai.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.firebase")
public class FirebaseProperties {
  private String projectId;
  private String serviceAccountPath;

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getServiceAccountPath() {
    return serviceAccountPath;
  }

  public void setServiceAccountPath(String serviceAccountPath) {
    this.serviceAccountPath = serviceAccountPath;
  }
}

