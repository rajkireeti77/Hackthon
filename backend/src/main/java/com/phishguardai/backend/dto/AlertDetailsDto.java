package com.phishguardai.backend.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class AlertDetailsDto {
  private Long id;
  private Long analysisId;
  private Long incomingEventId;
  private String sourceType;
  private String sourceMessageId;
  private String senderLabel;
  private String senderAddress;
  private String subjectLine;
  private String previewText;
  private String fileName;
  private Verdict verdict;
  private int riskScore;
  private double confidenceScore;
  private Severity severity;
  private boolean safeToOpen;
  private String shortExplanation;
  private String urlStatusSummary;
  private String popupTitle;
  private String popupMessage;
  private String primaryTargetUrl;
  private boolean read;
  private boolean openedAnyway;
  private boolean cancelled;
  private boolean blockedSender;
  private boolean reportedPhishing;
  private OffsetDateTime createdAt;
  private OffsetDateTime receivedAt;
  private List<UrlEvidenceDto> urls = new ArrayList<>();
  private AnalysisResponse analysis;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getAnalysisId() {
    return analysisId;
  }

  public void setAnalysisId(Long analysisId) {
    this.analysisId = analysisId;
  }

  public Long getIncomingEventId() {
    return incomingEventId;
  }

  public void setIncomingEventId(Long incomingEventId) {
    this.incomingEventId = incomingEventId;
  }

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  public String getSourceMessageId() {
    return sourceMessageId;
  }

  public void setSourceMessageId(String sourceMessageId) {
    this.sourceMessageId = sourceMessageId;
  }

  public String getSenderLabel() {
    return senderLabel;
  }

  public void setSenderLabel(String senderLabel) {
    this.senderLabel = senderLabel;
  }

  public String getSenderAddress() {
    return senderAddress;
  }

  public void setSenderAddress(String senderAddress) {
    this.senderAddress = senderAddress;
  }

  public String getSubjectLine() {
    return subjectLine;
  }

  public void setSubjectLine(String subjectLine) {
    this.subjectLine = subjectLine;
  }

  public String getPreviewText() {
    return previewText;
  }

  public void setPreviewText(String previewText) {
    this.previewText = previewText;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public Verdict getVerdict() {
    return verdict;
  }

  public void setVerdict(Verdict verdict) {
    this.verdict = verdict;
  }

  public int getRiskScore() {
    return riskScore;
  }

  public void setRiskScore(int riskScore) {
    this.riskScore = riskScore;
  }

  public double getConfidenceScore() {
    return confidenceScore;
  }

  public void setConfidenceScore(double confidenceScore) {
    this.confidenceScore = confidenceScore;
  }

  public Severity getSeverity() {
    return severity;
  }

  public void setSeverity(Severity severity) {
    this.severity = severity;
  }

  public boolean isSafeToOpen() {
    return safeToOpen;
  }

  public void setSafeToOpen(boolean safeToOpen) {
    this.safeToOpen = safeToOpen;
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

  public String getPopupTitle() {
    return popupTitle;
  }

  public void setPopupTitle(String popupTitle) {
    this.popupTitle = popupTitle;
  }

  public String getPopupMessage() {
    return popupMessage;
  }

  public void setPopupMessage(String popupMessage) {
    this.popupMessage = popupMessage;
  }

  public String getPrimaryTargetUrl() {
    return primaryTargetUrl;
  }

  public void setPrimaryTargetUrl(String primaryTargetUrl) {
    this.primaryTargetUrl = primaryTargetUrl;
  }

  public boolean isRead() {
    return read;
  }

  public void setRead(boolean read) {
    this.read = read;
  }

  public boolean isOpenedAnyway() {
    return openedAnyway;
  }

  public void setOpenedAnyway(boolean openedAnyway) {
    this.openedAnyway = openedAnyway;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

  public boolean isBlockedSender() {
    return blockedSender;
  }

  public void setBlockedSender(boolean blockedSender) {
    this.blockedSender = blockedSender;
  }

  public boolean isReportedPhishing() {
    return reportedPhishing;
  }

  public void setReportedPhishing(boolean reportedPhishing) {
    this.reportedPhishing = reportedPhishing;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getReceivedAt() {
    return receivedAt;
  }

  public void setReceivedAt(OffsetDateTime receivedAt) {
    this.receivedAt = receivedAt;
  }

  public List<UrlEvidenceDto> getUrls() {
    return urls;
  }

  public void setUrls(List<UrlEvidenceDto> urls) {
    this.urls = urls == null ? new ArrayList<>() : urls;
  }

  public AnalysisResponse getAnalysis() {
    return analysis;
  }

  public void setAnalysis(AnalysisResponse analysis) {
    this.analysis = analysis;
  }
}
