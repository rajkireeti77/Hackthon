package com.phishguardai.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class SandboxScanResultDto {
  private boolean enabled;
  private boolean attempted;
  private boolean reachable;
  private String finalUrl;
  private int redirectCount;
  private boolean loginFormDetected;
  private boolean downloadAttempted;
  private boolean networkIdleReached;
  private String title;
  private int riskScore;
  private String verdict;
  private String summary;
  private List<String> suspiciousKeywords = new ArrayList<>();
  private List<String> maliciousIndicators = new ArrayList<>();

  public static SandboxScanResultDto disabled() {
    SandboxScanResultDto dto = new SandboxScanResultDto();
    dto.setEnabled(false);
    dto.setAttempted(false);
    dto.setReachable(false);
    dto.setRiskScore(0);
    dto.setVerdict("UNAVAILABLE");
    dto.setSummary("Sandbox scanning is disabled.");
    return dto;
  }

  public static SandboxScanResultDto unavailable(String summary) {
    SandboxScanResultDto dto = new SandboxScanResultDto();
    dto.setEnabled(true);
    dto.setAttempted(true);
    dto.setReachable(false);
    dto.setRiskScore(0);
    dto.setVerdict("UNAVAILABLE");
    dto.setSummary(summary == null || summary.isBlank() ? "Sandbox scan was unavailable." : summary);
    return dto;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isAttempted() {
    return attempted;
  }

  public void setAttempted(boolean attempted) {
    this.attempted = attempted;
  }

  public boolean isReachable() {
    return reachable;
  }

  public void setReachable(boolean reachable) {
    this.reachable = reachable;
  }

  public String getFinalUrl() {
    return finalUrl;
  }

  public void setFinalUrl(String finalUrl) {
    this.finalUrl = finalUrl;
  }

  public int getRedirectCount() {
    return redirectCount;
  }

  public void setRedirectCount(int redirectCount) {
    this.redirectCount = redirectCount;
  }

  public boolean isLoginFormDetected() {
    return loginFormDetected;
  }

  public void setLoginFormDetected(boolean loginFormDetected) {
    this.loginFormDetected = loginFormDetected;
  }

  public boolean isDownloadAttempted() {
    return downloadAttempted;
  }

  public void setDownloadAttempted(boolean downloadAttempted) {
    this.downloadAttempted = downloadAttempted;
  }

  public boolean isNetworkIdleReached() {
    return networkIdleReached;
  }

  public void setNetworkIdleReached(boolean networkIdleReached) {
    this.networkIdleReached = networkIdleReached;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public int getRiskScore() {
    return riskScore;
  }

  public void setRiskScore(int riskScore) {
    this.riskScore = riskScore;
  }

  public String getVerdict() {
    return verdict;
  }

  public void setVerdict(String verdict) {
    this.verdict = verdict;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public List<String> getSuspiciousKeywords() {
    return suspiciousKeywords;
  }

  public void setSuspiciousKeywords(List<String> suspiciousKeywords) {
    this.suspiciousKeywords = suspiciousKeywords == null ? new ArrayList<>() : suspiciousKeywords;
  }

  public List<String> getMaliciousIndicators() {
    return maliciousIndicators;
  }

  public void setMaliciousIndicators(List<String> maliciousIndicators) {
    this.maliciousIndicators = maliciousIndicators == null ? new ArrayList<>() : maliciousIndicators;
  }
}
