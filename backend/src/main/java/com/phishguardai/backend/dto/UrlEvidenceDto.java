package com.phishguardai.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class UrlEvidenceDto {
  private String originalUrl;
  private String normalizedUrl;
  private String finalUrl;
  private boolean valid;
  private boolean shortened;
  private boolean redirected;
  private boolean spoofed;
  private boolean suspicious;
  private boolean malicious;
  private boolean safeBrowsingFlagged;
  private int riskScore;
  private String statusLabel;
  private List<String> evidence = new ArrayList<>();

  public String getOriginalUrl() {
    return originalUrl;
  }

  public void setOriginalUrl(String originalUrl) {
    this.originalUrl = originalUrl;
  }

  public String getNormalizedUrl() {
    return normalizedUrl;
  }

  public void setNormalizedUrl(String normalizedUrl) {
    this.normalizedUrl = normalizedUrl;
  }

  public String getFinalUrl() {
    return finalUrl;
  }

  public void setFinalUrl(String finalUrl) {
    this.finalUrl = finalUrl;
  }

  public boolean isValid() {
    return valid;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public boolean isShortened() {
    return shortened;
  }

  public void setShortened(boolean shortened) {
    this.shortened = shortened;
  }

  public boolean isRedirected() {
    return redirected;
  }

  public void setRedirected(boolean redirected) {
    this.redirected = redirected;
  }

  public boolean isSpoofed() {
    return spoofed;
  }

  public void setSpoofed(boolean spoofed) {
    this.spoofed = spoofed;
  }

  public boolean isSuspicious() {
    return suspicious;
  }

  public void setSuspicious(boolean suspicious) {
    this.suspicious = suspicious;
  }

  public boolean isMalicious() {
    return malicious;
  }

  public void setMalicious(boolean malicious) {
    this.malicious = malicious;
  }

  public boolean isSafeBrowsingFlagged() {
    return safeBrowsingFlagged;
  }

  public void setSafeBrowsingFlagged(boolean safeBrowsingFlagged) {
    this.safeBrowsingFlagged = safeBrowsingFlagged;
  }

  public int getRiskScore() {
    return riskScore;
  }

  public void setRiskScore(int riskScore) {
    this.riskScore = riskScore;
  }

  public String getStatusLabel() {
    return statusLabel;
  }

  public void setStatusLabel(String statusLabel) {
    this.statusLabel = statusLabel;
  }

  public List<String> getEvidence() {
    return evidence;
  }

  public void setEvidence(List<String> evidence) {
    this.evidence = evidence == null ? new ArrayList<>() : evidence;
  }
}
