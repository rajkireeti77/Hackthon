package com.phishguardai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "analysis_records")
public class AnalysisRecord {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 128)
  private String userId;

  @Column(columnDefinition = "TEXT")
  private String inputText;

  @Column(length = 2048)
  private String inputUrl;

  @Column(length = 255)
  private String inputFileName;

  @Column(length = 255)
  private String inputFileType;

  private Long inputFileSizeBytes;

  @Column(nullable = false, length = 16)
  private String verdict;

  @Column(nullable = false)
  private int riskScore;

  @Column(nullable = false, length = 16)
  private String severity;

  @Column(nullable = false)
  private double confidence;

  @Column(columnDefinition = "TEXT")
  private String aiSummary;

  @Column(nullable = false)
  private boolean safeBrowsingFlagged;

  @Column(columnDefinition = "TEXT")
  private String threatTypes;

  @Lob
  @Column(columnDefinition = "TEXT")
  private String redFlagsJson;

  @Lob
  @Column(columnDefinition = "TEXT")
  private String actionsJson;

  @Lob
  @Column(columnDefinition = "TEXT")
  private String sandboxScanJson;

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

  public String getInputText() {
    return inputText;
  }

  public void setInputText(String inputText) {
    this.inputText = inputText;
  }

  public String getInputUrl() {
    return inputUrl;
  }

  public void setInputUrl(String inputUrl) {
    this.inputUrl = inputUrl;
  }

  public String getInputFileName() {
    return inputFileName;
  }

  public void setInputFileName(String inputFileName) {
    this.inputFileName = inputFileName;
  }

  public String getInputFileType() {
    return inputFileType;
  }

  public void setInputFileType(String inputFileType) {
    this.inputFileType = inputFileType;
  }

  public Long getInputFileSizeBytes() {
    return inputFileSizeBytes;
  }

  public void setInputFileSizeBytes(Long inputFileSizeBytes) {
    this.inputFileSizeBytes = inputFileSizeBytes;
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

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public double getConfidence() {
    return confidence;
  }

  public void setConfidence(double confidence) {
    this.confidence = confidence;
  }

  public String getAiSummary() {
    return aiSummary;
  }

  public void setAiSummary(String aiSummary) {
    this.aiSummary = aiSummary;
  }

  public boolean isSafeBrowsingFlagged() {
    return safeBrowsingFlagged;
  }

  public void setSafeBrowsingFlagged(boolean safeBrowsingFlagged) {
    this.safeBrowsingFlagged = safeBrowsingFlagged;
  }

  public String getThreatTypes() {
    return threatTypes;
  }

  public void setThreatTypes(String threatTypes) {
    this.threatTypes = threatTypes;
  }

  public String getRedFlagsJson() {
    return redFlagsJson;
  }

  public void setRedFlagsJson(String redFlagsJson) {
    this.redFlagsJson = redFlagsJson;
  }

  public String getActionsJson() {
    return actionsJson;
  }

  public void setActionsJson(String actionsJson) {
    this.actionsJson = actionsJson;
  }

  public String getSandboxScanJson() {
    return sandboxScanJson;
  }

  public void setSandboxScanJson(String sandboxScanJson) {
    this.sandboxScanJson = sandboxScanJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}

