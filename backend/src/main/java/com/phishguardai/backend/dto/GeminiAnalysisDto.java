package com.phishguardai.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class GeminiAnalysisDto {
  private Verdict verdict;
  private int riskScore;
  private Severity severity;
  private double confidence;
  private String summary;
  private List<RedFlagDto> redFlags = new ArrayList<>();
  private List<String> recommendedActions = new ArrayList<>();

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

  public List<String> getRecommendedActions() {
    return recommendedActions;
  }

  public void setRecommendedActions(List<String> recommendedActions) {
    this.recommendedActions = recommendedActions == null ? new ArrayList<>() : recommendedActions;
  }
}

