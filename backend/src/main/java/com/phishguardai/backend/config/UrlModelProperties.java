package com.phishguardai.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.url-model")
public class UrlModelProperties {
  /**
   * Optional path to a JSON file containing trained weights.
   * If absent/blank/unreadable, the app uses built-in default weights.
   */
  private String weightsPath;

  public String getWeightsPath() {
    return weightsPath;
  }

  public void setWeightsPath(String weightsPath) {
    this.weightsPath = weightsPath;
  }
}

