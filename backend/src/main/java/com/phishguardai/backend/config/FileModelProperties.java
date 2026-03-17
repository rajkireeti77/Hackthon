package com.phishguardai.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.file-model")
public class FileModelProperties {
  private String weightsPath;

  public String getWeightsPath() {
    return weightsPath;
  }

  public void setWeightsPath(String weightsPath) {
    this.weightsPath = weightsPath;
  }
}
