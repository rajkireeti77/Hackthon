package com.phishguardai.backend.service;

import com.phishguardai.backend.dto.AlertDetailsDto;
import com.phishguardai.backend.dto.AnalysisResponse;
import com.phishguardai.backend.dto.SecurityAlertDto;
import com.phishguardai.backend.dto.Severity;
import com.phishguardai.backend.dto.UrlEvidenceDto;
import com.phishguardai.backend.dto.Verdict;
import com.phishguardai.backend.entity.IncomingEvent;
import com.phishguardai.backend.entity.SecurityAlert;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SecurityAlertMapper {
  public SecurityAlertDto toDto(SecurityAlert alert) {
    SecurityAlertDto dto = new SecurityAlertDto();
    dto.setId(alert.getId());
    dto.setAnalysisId(alert.getAnalysisId());
    dto.setIncomingEventId(alert.getIncomingEventId());
    dto.setSourceType(alert.getSourceType());
    dto.setSenderLabel(alert.getSenderLabel());
    dto.setSubjectLine(alert.getSubjectLine());
    dto.setPreviewText(alert.getPreviewText());
    dto.setFileName(alert.getFileName());
    dto.setVerdict(Verdict.valueOf(alert.getVerdict()));
    dto.setRiskScore(alert.getRiskScore());
    dto.setConfidenceScore(alert.getConfidenceScore());
    dto.setSeverity(Severity.valueOf(alert.getSeverity()));
    dto.setSafeToOpen(alert.isSafeToOpen());
    dto.setShortExplanation(alert.getShortExplanation());
    dto.setUrlStatusSummary(alert.getUrlStatusSummary());
    dto.setPopupTitle(alert.getPopupTitle());
    dto.setPopupMessage(alert.getPopupMessage());
    dto.setSourceMessageId(alert.getSourceMessageId());
    dto.setPrimaryTargetUrl(alert.getPrimaryTargetUrl());
    dto.setRead(alert.isReadStatus());
    dto.setOpenedAnyway(alert.isOpenedAnyway());
    dto.setCancelled(alert.isCancelled());
    dto.setBlockedSender(alert.isBlockedSender());
    dto.setReportedPhishing(alert.isReportedPhishing());
    dto.setCreatedAt(OffsetDateTime.ofInstant(alert.getCreatedAt(), ZoneOffset.UTC));
    return dto;
  }

  public AlertDetailsDto toDetails(SecurityAlert alert, IncomingEvent event, AnalysisResponse analysis, List<UrlEvidenceDto> urls) {
    AlertDetailsDto dto = new AlertDetailsDto();
    dto.setId(alert.getId());
    dto.setAnalysisId(alert.getAnalysisId());
    dto.setIncomingEventId(alert.getIncomingEventId());
    dto.setSourceType(alert.getSourceType());
    dto.setSourceMessageId(alert.getSourceMessageId());
    dto.setSenderLabel(alert.getSenderLabel());
    dto.setSenderAddress(event == null ? null : event.getSenderAddress());
    dto.setSubjectLine(alert.getSubjectLine());
    dto.setPreviewText(alert.getPreviewText());
    dto.setFileName(alert.getFileName());
    dto.setVerdict(Verdict.valueOf(alert.getVerdict()));
    dto.setRiskScore(alert.getRiskScore());
    dto.setConfidenceScore(alert.getConfidenceScore());
    dto.setSeverity(Severity.valueOf(alert.getSeverity()));
    dto.setSafeToOpen(alert.isSafeToOpen());
    dto.setShortExplanation(alert.getShortExplanation());
    dto.setUrlStatusSummary(alert.getUrlStatusSummary());
    dto.setPopupTitle(alert.getPopupTitle());
    dto.setPopupMessage(alert.getPopupMessage());
    dto.setPrimaryTargetUrl(alert.getPrimaryTargetUrl());
    dto.setRead(alert.isReadStatus());
    dto.setOpenedAnyway(alert.isOpenedAnyway());
    dto.setCancelled(alert.isCancelled());
    dto.setBlockedSender(alert.isBlockedSender());
    dto.setReportedPhishing(alert.isReportedPhishing());
    dto.setCreatedAt(OffsetDateTime.ofInstant(alert.getCreatedAt(), ZoneOffset.UTC));
    if (event != null && event.getReceivedAt() != null) {
      dto.setReceivedAt(OffsetDateTime.ofInstant(event.getReceivedAt(), ZoneOffset.UTC));
    }
    dto.setUrls(urls);
    dto.setAnalysis(analysis);
    return dto;
  }
}
