package com.phishguardai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "security_alerts")
public class SecurityAlert {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 128)
  private String userId;

  @Column(nullable = false)
  private Long analysisId;

  @Column(nullable = false)
  private Long incomingEventId;

  @Column(nullable = false, length = 32)
  private String sourceType;

  @Column(length = 180)
  private String senderLabel;

  @Column(length = 240)
  private String subjectLine;

  @Column(columnDefinition = "TEXT")
  private String previewText;

  @Column(length = 255)
  private String fileName;

  @Column(nullable = false, length = 16)
  private String verdict;

  @Column(nullable = false)
  private int riskScore;

  @Column(nullable = false)
  private double confidenceScore;

  @Column(nullable = false, length = 16)
  private String severity;

  @Column(nullable = false)
  private boolean safeToOpen;

  @Column(columnDefinition = "TEXT")
  private String shortExplanation;

  @Column(length = 255)
  private String urlStatusSummary;

  @Column(nullable = false, length = 160)
  private String popupTitle;

  @Column(columnDefinition = "TEXT")
  private String popupMessage;

  @Column(length = 255)
  private String sourceMessageId;

  @Column(length = 2048)
  private String primaryTargetUrl;

  @Column(nullable = false)
  private boolean readStatus;

  @Column(nullable = false)
  private boolean openedAnyway;

  @Column(nullable = false)
  private boolean cancelled;

  @Column(nullable = false)
  private boolean blockedSender;

  @Column(nullable = false)
  private boolean reportedPhishing;

  @Column(nullable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    if (createdAt == null) createdAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
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

  public String getSenderLabel() {
    return senderLabel;
  }

  public void setSenderLabel(String senderLabel) {
    this.senderLabel = senderLabel;
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

  public String getVerdict() {
    return verdict;
  }

  public void setVerdict(String verdict) {
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

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
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

  public String getSourceMessageId() {
    return sourceMessageId;
  }

  public void setSourceMessageId(String sourceMessageId) {
    this.sourceMessageId = sourceMessageId;
  }

  public String getPrimaryTargetUrl() {
    return primaryTargetUrl;
  }

  public void setPrimaryTargetUrl(String primaryTargetUrl) {
    this.primaryTargetUrl = primaryTargetUrl;
  }

  public boolean isReadStatus() {
    return readStatus;
  }

  public void setReadStatus(boolean readStatus) {
    this.readStatus = readStatus;
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

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
