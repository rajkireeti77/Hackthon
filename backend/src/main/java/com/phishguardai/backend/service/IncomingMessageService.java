package com.phishguardai.backend.service;

import com.phishguardai.backend.dto.BackgroundIngestRequest;
import com.phishguardai.backend.dto.IncomingScanResultDto;
import com.phishguardai.backend.dto.SecurityAlertDto;
import com.phishguardai.backend.dto.UrlEvidenceDto;
import com.phishguardai.backend.dto.Verdict;
import com.phishguardai.backend.entity.ExtractedUrl;
import com.phishguardai.backend.entity.IncomingEvent;
import com.phishguardai.backend.entity.SecurityAlert;
import com.phishguardai.backend.repository.BlockedSenderRepository;
import com.phishguardai.backend.repository.ExtractedUrlRepository;
import com.phishguardai.backend.repository.IncomingEventRepository;
import com.phishguardai.backend.repository.SecurityAlertRepository;
import com.phishguardai.backend.util.JsonUtil;
import com.phishguardai.backend.util.TextUtil;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class IncomingMessageService {
  private final IncomingEventRepository incomingEventRepository;
  private final ExtractedUrlRepository extractedUrlRepository;
  private final SecurityAlertRepository securityAlertRepository;
  private final BlockedSenderRepository blockedSenderRepository;
  private final IncomingMessageScannerService incomingMessageScannerService;
  private final SecurityAlertMapper securityAlertMapper;
  private final JsonUtil jsonUtil;

  public IncomingMessageService(
      IncomingEventRepository incomingEventRepository,
      ExtractedUrlRepository extractedUrlRepository,
      SecurityAlertRepository securityAlertRepository,
      BlockedSenderRepository blockedSenderRepository,
      IncomingMessageScannerService incomingMessageScannerService,
      SecurityAlertMapper securityAlertMapper,
      JsonUtil jsonUtil) {
    this.incomingEventRepository = incomingEventRepository;
    this.extractedUrlRepository = extractedUrlRepository;
    this.securityAlertRepository = securityAlertRepository;
    this.blockedSenderRepository = blockedSenderRepository;
    this.incomingMessageScannerService = incomingMessageScannerService;
    this.securityAlertMapper = securityAlertMapper;
    this.jsonUtil = jsonUtil;
  }

  @Transactional
  public SecurityAlertDto ingestAndCreateAlert(String userId, BackgroundIngestRequest request) {
    String sourceType = normalizeSourceType(request.getSourceType());
    String sourceMessageId = TextUtil.truncate(TextUtil.nullIfBlank(request.getSourceMessageId()), 255);

    if (sourceMessageId != null) {
      var existing = incomingEventRepository.findByUserIdAndSourceTypeAndSourceMessageId(userId, sourceType, sourceMessageId);
      if (existing.isPresent() && existing.get().getAnalysisId() != null) {
        SecurityAlertDto existingAlert =
            securityAlertRepository
            .findByAnalysisIdAndUserId(existing.get().getAnalysisId(), userId)
            .map(securityAlertMapper::toDto)
            .orElse(null);
        if (existingAlert != null) {
          return existingAlert;
        }
      }
    }

    IncomingEvent event = new IncomingEvent();
    event.setUserId(userId);
    event.setSourceType(sourceType);
    event.setSourceMessageId(sourceMessageId);
    event.setSenderLabel(TextUtil.truncate(TextUtil.nullIfBlank(request.getSenderLabel()), 180));
    event.setSenderAddress(TextUtil.truncate(TextUtil.nullIfBlank(request.getSenderAddress()), 255));
    event.setSubjectLine(TextUtil.truncate(TextUtil.nullIfBlank(request.getSubjectLine()), 240));
    event.setPreviewText(buildPreview(request));
    event.setBodyText(TextUtil.truncate(TextUtil.nullIfBlank(request.getTextContent()), 10000));
    event.setBodyHtml(TextUtil.truncate(TextUtil.nullIfBlank(request.getHtmlContent()), 30000));
    event.setReceivedAt(request.getReceivedAt() == null ? Instant.now() : request.getReceivedAt().toInstant());
    event.setStatus("RECEIVED");
    event = incomingEventRepository.save(event);

    boolean senderBlocked = isSenderBlocked(userId, sourceType, event.getSenderAddress(), event.getSenderLabel());
    IncomingScanResultDto result = incomingMessageScannerService.analyzeAndSave(userId, request, senderBlocked);

    Long incomingEventId = event.getId();
    for (UrlEvidenceDto item : result.getUrlEvidence()) {
      extractedUrlRepository.save(toEntity(incomingEventId, item));
    }

    event.setAnalysisId(result.getAnalysis().getId());
    event.setProcessedAt(Instant.now());
    event.setStatus("ANALYZED");
    incomingEventRepository.save(event);

    SecurityAlert alert = new SecurityAlert();
    alert.setUserId(userId);
    alert.setAnalysisId(result.getAnalysis().getId());
    alert.setIncomingEventId(event.getId());
    alert.setSourceType(sourceType);
    alert.setSenderLabel(event.getSenderLabel());
    alert.setSubjectLine(event.getSubjectLine());
    alert.setPreviewText(event.getPreviewText());
    alert.setFileName(result.getPrimaryAttachment() == null ? null : result.getPrimaryAttachment().getFileName());
    alert.setVerdict(result.getAnalysis().getVerdict().name());
    alert.setRiskScore(result.getAnalysis().getRiskScore());
    alert.setConfidenceScore(result.getAnalysis().getConfidence());
    alert.setSeverity(result.getAnalysis().getSeverity().name());
    alert.setSafeToOpen(result.isSafeToOpen());
    alert.setShortExplanation(TextUtil.truncate(result.getShortExplanation(), 300));
    alert.setUrlStatusSummary(TextUtil.truncate(result.getUrlStatusSummary(), 255));
    alert.setPopupTitle(buildPopupTitle(sourceType, result.getAnalysis().getVerdict(), senderBlocked, result.getPrimaryAttachment() != null));
    alert.setPopupMessage(buildPopupMessage(event.getSenderLabel(), result));
    alert.setSourceMessageId(sourceMessageId);
    alert.setPrimaryTargetUrl(TextUtil.truncate(result.getPrimaryUrl(), 2048));
    alert.setReadStatus(false);
    alert.setOpenedAnyway(false);
    alert.setCancelled(false);
    alert.setBlockedSender(senderBlocked);
    alert.setReportedPhishing(false);

    return securityAlertMapper.toDto(securityAlertRepository.save(alert));
  }

  private ExtractedUrl toEntity(Long incomingEventId, UrlEvidenceDto dto) {
    ExtractedUrl entity = new ExtractedUrl();
    entity.setIncomingEventId(incomingEventId);
    entity.setOriginalUrl(TextUtil.truncate(dto.getOriginalUrl(), 2048));
    entity.setNormalizedUrl(TextUtil.truncate(dto.getNormalizedUrl(), 2048));
    entity.setFinalUrl(TextUtil.truncate(dto.getFinalUrl(), 2048));
    entity.setValidUrl(dto.isValid());
    entity.setShortened(dto.isShortened());
    entity.setRedirected(dto.isRedirected());
    entity.setSpoofed(dto.isSpoofed());
    entity.setSuspicious(dto.isSuspicious());
    entity.setMalicious(dto.isMalicious());
    entity.setSafeBrowsingFlagged(dto.isSafeBrowsingFlagged());
    entity.setRiskScore(dto.getRiskScore());
    entity.setStatusLabel(TextUtil.truncate(dto.getStatusLabel(), 32));
    entity.setEvidenceJson(jsonUtil.toJson(dto.getEvidence()));
    return entity;
  }

  private boolean isSenderBlocked(String userId, String sourceType, String senderAddress, String senderLabel) {
    String senderKey = senderKey(senderAddress, senderLabel);
    return senderKey != null && blockedSenderRepository.existsByUserIdAndSourceTypeAndSenderKey(userId, sourceType, senderKey);
  }

  private String buildPopupTitle(String sourceType, Verdict verdict, boolean senderBlocked, boolean hasAttachment) {
    if (senderBlocked) return "Blocked sender contacted you";
    String source = displaySource(sourceType);
    return switch (verdict) {
      case Malicious -> hasAttachment ? "Unsafe file from " + source : "Unsafe message from " + source;
      case Suspicious -> hasAttachment ? "Suspicious attachment detected" : "Suspicious message detected";
      case LowRisk -> "Low-risk message from " + source;
      case Safe -> hasAttachment ? "Attachment appears safe" : "Message appears safe";
    };
  }

  private String buildPopupMessage(String senderLabel, IncomingScanResultDto result) {
    String actor = TextUtil.nullIfBlank(senderLabel);
    StringBuilder message = new StringBuilder();
    if (actor != null) {
      message.append(actor).append(": ");
    }
    message.append(TextUtil.nullIfBlank(result.getShortExplanation()) == null ? "New message analyzed." : result.getShortExplanation());
    if (TextUtil.nullIfBlank(result.getUrlStatusSummary()) != null) {
      message.append(" URL status: ").append(result.getUrlStatusSummary()).append(".");
    }
    message.append(result.isSafeToOpen() ? " Safe to open with normal caution." : " Do not trust or open it yet.");
    return TextUtil.truncate(message.toString(), 320);
  }

  private String buildPreview(BackgroundIngestRequest request) {
    if (TextUtil.nullIfBlank(request.getTextContent()) != null) return TextUtil.truncate(request.getTextContent(), 260);
    if (TextUtil.nullIfBlank(request.getSubjectLine()) != null) return TextUtil.truncate(request.getSubjectLine(), 260);
    if (TextUtil.nullIfBlank(request.getUrl()) != null) return TextUtil.truncate(request.getUrl(), 260);
    if (TextUtil.nullIfBlank(request.getFileName()) != null) return "Attachment: " + TextUtil.truncate(request.getFileName(), 220);
    return "New incoming content analyzed";
  }

  private String normalizeSourceType(String value) {
    String source = TextUtil.nullIfBlank(value);
    if (source == null) return "UNKNOWN";
    return TextUtil.truncate(source.trim().toUpperCase(Locale.ROOT), 32);
  }

  private String displaySource(String value) {
    return switch (normalizeSourceType(value)) {
      case "WHATSAPP" -> "WhatsApp";
      case "EMAIL" -> "Email";
      case "SMS" -> "SMS";
      case "TELEGRAM" -> "Telegram";
      case "APK" -> "APK";
      default -> "Inbox";
    };
  }

  public String senderKey(String senderAddress, String senderLabel) {
    String key = TextUtil.nullIfBlank(senderAddress);
    if (key == null) key = TextUtil.nullIfBlank(senderLabel);
    return key == null ? null : TextUtil.truncate(key.toLowerCase(Locale.ROOT), 255);
  }
}
