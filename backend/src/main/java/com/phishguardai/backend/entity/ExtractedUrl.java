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
@Table(name = "extracted_urls")
public class ExtractedUrl {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long incomingEventId;

  @Column(nullable = false, length = 2048)
  private String originalUrl;

  @Column(length = 2048)
  private String normalizedUrl;

  @Column(length = 2048)
  private String finalUrl;

  @Column(nullable = false)
  private boolean validUrl;

  @Column(nullable = false)
  private boolean shortened;

  @Column(nullable = false)
  private boolean redirected;

  @Column(nullable = false)
  private boolean spoofed;

  @Column(nullable = false)
  private boolean suspicious;

  @Column(nullable = false)
  private boolean malicious;

  @Column(nullable = false)
  private boolean safeBrowsingFlagged;

  @Column(nullable = false)
  private int riskScore;

  @Column(nullable = false, length = 32)
  private String statusLabel;

  @Lob
  @Column(columnDefinition = "TEXT")
  private String evidenceJson;

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

  public Long getIncomingEventId() {
    return incomingEventId;
  }

  public void setIncomingEventId(Long incomingEventId) {
    this.incomingEventId = incomingEventId;
  }

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

  public boolean isValidUrl() {
    return validUrl;
  }

  public void setValidUrl(boolean validUrl) {
    this.validUrl = validUrl;
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

  public String getEvidenceJson() {
    return evidenceJson;
  }

  public void setEvidenceJson(String evidenceJson) {
    this.evidenceJson = evidenceJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
