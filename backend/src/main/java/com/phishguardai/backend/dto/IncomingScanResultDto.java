package com.phishguardai.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class IncomingScanResultDto {
  private AnalysisResponse analysis;
  private List<UrlEvidenceDto> urlEvidence = new ArrayList<>();
  private IncomingAttachmentDto primaryAttachment;
  private String primaryUrl;
  private String shortExplanation;
  private String urlStatusSummary;
  private boolean safeToOpen;

  public AnalysisResponse getAnalysis() {
    return analysis;
  }

  public void setAnalysis(AnalysisResponse analysis) {
    this.analysis = analysis;
  }

  public List<UrlEvidenceDto> getUrlEvidence() {
    return urlEvidence;
  }

  public void setUrlEvidence(List<UrlEvidenceDto> urlEvidence) {
    this.urlEvidence = urlEvidence == null ? new ArrayList<>() : urlEvidence;
  }

  public IncomingAttachmentDto getPrimaryAttachment() {
    return primaryAttachment;
  }

  public void setPrimaryAttachment(IncomingAttachmentDto primaryAttachment) {
    this.primaryAttachment = primaryAttachment;
  }

  public String getPrimaryUrl() {
    return primaryUrl;
  }

  public void setPrimaryUrl(String primaryUrl) {
    this.primaryUrl = primaryUrl;
  }

  public String getShortExplanation() {
    return shortExplanation;
  }

  public void setShortExplanation(String shortExplanation) {
    this.shortExplanation = shortExplanation;
  }

  public String getUrlStatusSummary() {
    return urlStatusSummary;
  }

  public void setUrlStatusSummary(String urlStatusSummary) {
    this.urlStatusSummary = urlStatusSummary;
  }

  public boolean isSafeToOpen() {
    return safeToOpen;
  }

  public void setSafeToOpen(boolean safeToOpen) {
    this.safeToOpen = safeToOpen;
  }
}
