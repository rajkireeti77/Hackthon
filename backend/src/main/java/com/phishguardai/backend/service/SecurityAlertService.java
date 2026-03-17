package com.phishguardai.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.phishguardai.backend.dto.AlertDetailsDto;
import com.phishguardai.backend.dto.PhishingReportRequest;
import com.phishguardai.backend.dto.SecurityAlertDto;
import com.phishguardai.backend.dto.UrlEvidenceDto;
import com.phishguardai.backend.entity.BlockedSender;
import com.phishguardai.backend.entity.ExtractedUrl;
import com.phishguardai.backend.entity.IncomingEvent;
import com.phishguardai.backend.entity.PhishingReport;
import com.phishguardai.backend.entity.SecurityAlert;
import com.phishguardai.backend.repository.BlockedSenderRepository;
import com.phishguardai.backend.repository.ExtractedUrlRepository;
import com.phishguardai.backend.repository.IncomingEventRepository;
import com.phishguardai.backend.repository.PhishingReportRepository;
import com.phishguardai.backend.repository.SecurityAlertRepository;
import com.phishguardai.backend.util.JsonUtil;
import com.phishguardai.backend.util.TextUtil;
import com.phishguardai.backend.util.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class SecurityAlertService {
  private final SecurityAlertRepository repo;
  private final IncomingEventRepository incomingEventRepository;
  private final ExtractedUrlRepository extractedUrlRepository;
  private final BlockedSenderRepository blockedSenderRepository;
  private final PhishingReportRepository phishingReportRepository;
  private final AnalysisService analysisService;
  private final SecurityAlertMapper mapper;
  private final JsonUtil jsonUtil;

  public SecurityAlertService(
      SecurityAlertRepository repo,
      IncomingEventRepository incomingEventRepository,
      ExtractedUrlRepository extractedUrlRepository,
      BlockedSenderRepository blockedSenderRepository,
      PhishingReportRepository phishingReportRepository,
      AnalysisService analysisService,
      SecurityAlertMapper mapper,
      JsonUtil jsonUtil) {
    this.repo = repo;
    this.incomingEventRepository = incomingEventRepository;
    this.extractedUrlRepository = extractedUrlRepository;
    this.blockedSenderRepository = blockedSenderRepository;
    this.phishingReportRepository = phishingReportRepository;
    this.analysisService = analysisService;
    this.mapper = mapper;
    this.jsonUtil = jsonUtil;
  }

  public List<SecurityAlertDto> recentAlerts(String userId, String sourceType, String verdict, Boolean read) {
    return repo.findTop100ByUserIdOrderByCreatedAtDesc(userId).stream()
        .filter(alert -> matches(alert.getSourceType(), sourceType))
        .filter(alert -> matches(alert.getVerdict(), verdict))
        .filter(alert -> read == null || alert.isReadStatus() == read.booleanValue())
        .map(mapper::toDto)
        .toList();
  }

  public List<SecurityAlertDto> unreadAlerts(String userId) {
    return repo.findTop20ByUserIdAndReadStatusIsFalseOrderByCreatedAtDesc(userId).stream().map(mapper::toDto).toList();
  }

  public AlertDetailsDto getDetails(String userId, Long id) {
    SecurityAlert alert = getAlert(userId, id);
    IncomingEvent event =
        incomingEventRepository.findById(alert.getIncomingEventId()).filter(item -> userId.equals(item.getUserId())).orElse(null);
    var analysis = analysisService.getOne(userId, alert.getAnalysisId());
    List<UrlEvidenceDto> urls =
        extractedUrlRepository.findByIncomingEventIdOrderByRiskScoreDescIdAsc(alert.getIncomingEventId()).stream()
            .map(this::toUrlEvidence)
            .toList();
    return mapper.toDetails(alert, event, analysis, urls);
  }

  @Transactional
  public SecurityAlertDto markRead(String userId, Long id) {
    SecurityAlert alert = getAlert(userId, id);
    alert.setReadStatus(true);
    return mapper.toDto(repo.save(alert));
  }

  @Transactional
  public void markAllRead(String userId) {
    List<SecurityAlert> alerts = repo.findTop20ByUserIdAndReadStatusIsFalseOrderByCreatedAtDesc(userId);
    alerts.forEach(item -> item.setReadStatus(true));
    repo.saveAll(alerts);
  }

  @Transactional
  public SecurityAlertDto openAnyway(String userId, Long id) {
    SecurityAlert alert = getAlert(userId, id);
    alert.setOpenedAnyway(true);
    alert.setCancelled(false);
    alert.setReadStatus(true);
    return mapper.toDto(repo.save(alert));
  }

  @Transactional
  public SecurityAlertDto cancel(String userId, Long id) {
    SecurityAlert alert = getAlert(userId, id);
    alert.setCancelled(true);
    alert.setReadStatus(true);
    return mapper.toDto(repo.save(alert));
  }

  @Transactional
  public SecurityAlertDto blockSender(String userId, Long id) {
    SecurityAlert alert = getAlert(userId, id);
    IncomingEvent event = incomingEventRepository.findById(alert.getIncomingEventId()).orElse(null);
    String senderKey = senderKey(event == null ? null : event.getSenderAddress(), alert.getSenderLabel());
    if (senderKey != null
        && !blockedSenderRepository.existsByUserIdAndSourceTypeAndSenderKey(userId, alert.getSourceType(), senderKey)) {
      BlockedSender blocked = new BlockedSender();
      blocked.setUserId(userId);
      blocked.setSourceType(alert.getSourceType());
      blocked.setSenderKey(senderKey);
      blocked.setReason("Blocked from alert #" + alert.getId());
      blockedSenderRepository.save(blocked);
    }
    alert.setBlockedSender(true);
    alert.setReadStatus(true);
    return mapper.toDto(repo.save(alert));
  }

  @Transactional
  public SecurityAlertDto reportPhishing(String userId, Long id, PhishingReportRequest request) {
    SecurityAlert alert = getAlert(userId, id);
    PhishingReport report = new PhishingReport();
    report.setUserId(userId);
    report.setAlertId(alert.getId());
    report.setAnalysisId(alert.getAnalysisId());
    report.setReportNote(TextUtil.truncate(TextUtil.nullIfBlank(request == null ? null : request.getReportNote()), 500));
    phishingReportRepository.save(report);
    alert.setReportedPhishing(true);
    alert.setReadStatus(true);
    return mapper.toDto(repo.save(alert));
  }

  private SecurityAlert getAlert(String userId, Long id) {
    return repo.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Alert not found."));
  }

  private UrlEvidenceDto toUrlEvidence(ExtractedUrl entity) {
    UrlEvidenceDto dto = new UrlEvidenceDto();
    dto.setOriginalUrl(entity.getOriginalUrl());
    dto.setNormalizedUrl(entity.getNormalizedUrl());
    dto.setFinalUrl(entity.getFinalUrl());
    dto.setValid(entity.isValidUrl());
    dto.setShortened(entity.isShortened());
    dto.setRedirected(entity.isRedirected());
    dto.setSpoofed(entity.isSpoofed());
    dto.setSuspicious(entity.isSuspicious());
    dto.setMalicious(entity.isMalicious());
    dto.setSafeBrowsingFlagged(entity.isSafeBrowsingFlagged());
    dto.setRiskScore(entity.getRiskScore());
    dto.setStatusLabel(entity.getStatusLabel());
    if (entity.getEvidenceJson() != null && !entity.getEvidenceJson().isBlank()) {
      dto.setEvidence(jsonUtil.fromJson(entity.getEvidenceJson(), new TypeReference<List<String>>() {}));
    }
    return dto;
  }

  private boolean matches(String actual, String filter) {
    String expected = TextUtil.nullIfBlank(filter);
    return expected == null || expected.equalsIgnoreCase(actual);
  }

  private String senderKey(String senderAddress, String senderLabel) {
    String key = TextUtil.nullIfBlank(senderAddress);
    if (key == null) key = TextUtil.nullIfBlank(senderLabel);
    return key == null ? null : TextUtil.truncate(key.toLowerCase(Locale.ROOT), 255);
  }
}
