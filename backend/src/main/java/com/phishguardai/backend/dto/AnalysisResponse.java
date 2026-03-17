package com.phishguardai.backend.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class AnalysisResponse {
  private Long id;
  private Verdict verdict;
  private int riskScore;
  private Severity severity;
  private double confidence;
  private String summary;
  private List<RedFlagDto> redFlags = new ArrayList<>();
  private SafeBrowsingResultDto safeBrowsingResult;
  private SandboxScanResultDto sandboxScanResult;
  private List<String> recommendedActions = new ArrayList<>();
  private String fileName;
  private String fileContentType;
  private Long fileSizeBytes;
  private OffsetDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public Severity getSeverity() {
    return severity;
  }

  public void setSeverity(Severity severity) {
    this.severity = severity;
  }

  public double getConfidence() {
    return confidence;
  }

  public void setConfidence(double confidence) {
    this.confidence = confidence;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public List<RedFlagDto> getRedFlags() {
    return redFlags;
  }

  public void setRedFlags(List<RedFlagDto> redFlags) {
    this.redFlags = redFlags == null ? new ArrayList<>() : redFlags;
  }

  public SafeBrowsingResultDto getSafeBrowsingResult() {
    return safeBrowsingResult;
  }

  public void setSafeBrowsingResult(SafeBrowsingResultDto safeBrowsingResult) {
    this.safeBrowsingResult = safeBrowsingResult;
  }

  public SandboxScanResultDto getSandboxScanResult() {
    return sandboxScanResult;
  }

  public void setSandboxScanResult(SandboxScanResultDto sandboxScanResult) {
    this.sandboxScanResult = sandboxScanResult;
  }

  public List<String> getRecommendedActions() {
    return recommendedActions;
  }

  public void setRecommendedActions(List<String> recommendedActions) {
    this.recommendedActions = recommendedActions == null ? new ArrayList<>() : recommendedActions;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFileContentType() {
    return fileContentType;
  }

  public void setFileContentType(String fileContentType) {
    this.fileContentType = fileContentType;
  }

  public Long getFileSizeBytes() {
    return fileSizeBytes;
  }

  public void setFileSizeBytes(Long fileSizeBytes) {
    this.fileSizeBytes = fileSizeBytes;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}

