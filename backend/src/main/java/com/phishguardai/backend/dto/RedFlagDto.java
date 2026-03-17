package com.phishguardai.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class RedFlagDto {
  @NotBlank private String type;
  @NotBlank private String severity;
  @NotBlank private String description;

  public RedFlagDto() {}

  public RedFlagDto(String type, String severity, String description) {
    this.type = type;
    this.severity = severity;
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}

