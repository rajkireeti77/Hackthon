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
@Table(name = "phishing_reports")
public class PhishingReport {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 128)
  private String userId;

  @Column(nullable = false)
  private Long alertId;

  @Column(nullable = false)
  private Long analysisId;

  @Column(columnDefinition = "TEXT")
  private String reportNote;

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

  public Long getAlertId() {
    return alertId;
  }

  public void setAlertId(Long alertId) {
    this.alertId = alertId;
  }

  public Long getAnalysisId() {
    return analysisId;
  }

  public void setAnalysisId(Long analysisId) {
    this.analysisId = analysisId;
  }

  public String getReportNote() {
    return reportNote;
  }

  public void setReportNote(String reportNote) {
    this.reportNote = reportNote;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
