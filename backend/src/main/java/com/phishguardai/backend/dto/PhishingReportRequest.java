package com.phishguardai.backend.dto;

import jakarta.validation.constraints.Size;

public class PhishingReportRequest {
  @Size(max = 500, message = "reportNote is too long (max 500 chars)")
  private String reportNote;

  public String getReportNote() {
    return reportNote;
  }

  public void setReportNote(String reportNote) {
    this.reportNote = reportNote;
  }
}
