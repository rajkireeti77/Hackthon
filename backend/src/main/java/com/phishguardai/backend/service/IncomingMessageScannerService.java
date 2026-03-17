package com.phishguardai.backend.service;

import com.phishguardai.backend.dto.AnalysisResponse;
import com.phishguardai.backend.dto.GeminiAnalysisDto;
import com.phishguardai.backend.dto.IncomingAttachmentDto;
import com.phishguardai.backend.dto.IncomingScanResultDto;
import com.phishguardai.backend.dto.RedFlagDto;
import com.phishguardai.backend.dto.SafeBrowsingResultDto;
import com.phishguardai.backend.dto.SandboxScanResultDto;
import com.phishguardai.backend.dto.Severity;
import com.phishguardai.backend.dto.UrlEvidenceDto;
import com.phishguardai.backend.entity.AnalysisRecord;
import com.phishguardai.backend.repository.AnalysisRecordRepository;
import com.phishguardai.backend.util.JsonUtil;
import com.phishguardai.backend.util.ScoreUtil;
import com.phishguardai.backend.util.TextUtil;
import com.phishguardai.backend.util.exception.BadRequestException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class IncomingMessageScannerService {
  private final UrlExtractionService urlExtractionService;
  private final UrlReputationService urlReputationService;
  private final GeminiClient geminiClient;
  private final RiskScoringService riskScoringService;
  private final TextRiskModel textRiskModel;
  private final FileRiskModel fileRiskModel;
  private final SandboxUrlScannerClient sandboxUrlScannerClient;
  private final AnalysisRecordRepository analysisRecordRepository;
  private final AnalysisMapper analysisMapper;
  private final JsonUtil jsonUtil;

  public IncomingMessageScannerService(
      UrlExtractionService urlExtractionService,
      UrlReputationService urlReputationService,
      GeminiClient geminiClient,
      RiskScoringService riskScoringService,
      TextRiskModel textRiskModel,
      FileRiskModel fileRiskModel,
      SandboxUrlScannerClient sandboxUrlScannerClient,
      AnalysisRecordRepository analysisRecordRepository,
      AnalysisMapper analysisMapper,
      JsonUtil jsonUtil) {
    this.urlExtractionService = urlExtractionService;
    this.urlReputationService = urlReputationService;
    this.geminiClient = geminiClient;
    this.riskScoringService = riskScoringService;
    this.textRiskModel = textRiskModel;
    this.fileRiskModel = fileRiskModel;
    this.sandboxUrlScannerClient = sandboxUrlScannerClient;
    this.analysisRecordRepository = analysisRecordRepository;
    this.analysisMapper = analysisMapper;
    this.jsonUtil = jsonUtil;
  }

  public IncomingScanResultDto analyzeAndSave(String userId, com.phishguardai.backend.dto.BackgroundIngestRequest request, boolean senderBlocked) {
    String analysisText = buildAnalysisText(request);
    List<String> extractedUrls = urlExtractionService.extractAll(request.getTextContent(), request.getHtmlContent(), request.getUrl());
    List<UrlEvidenceDto> urlEvidence = urlReputationService.analyzeAll(extractedUrls);
    UrlEvidenceDto primaryUrl = urlEvidence.stream().max(Comparator.comparingInt(UrlEvidenceDto::getRiskScore)).orElse(null);

    AttachmentAssessment attachment = pickPrimaryAttachment(request);
    if (analysisText == null && primaryUrl == null && attachment == null) {
      throw new BadRequestException("Incoming content must include message text, at least one URL, and/or attachment metadata.");
    }
    String fileContext =
        attachment == null
            ? ""
            : fileRiskModel.describeForPrompt(
                attachment.attachment.getFileName(),
                attachment.attachment.getContentType(),
                attachment.attachment.getSizeBytes(),
                attachment.sha256,
                attachment.sample);

    boolean safeBrowsingFlagged = urlEvidence.stream().anyMatch(UrlEvidenceDto::isSafeBrowsingFlagged);
    List<String> threatTypes = safeBrowsingFlagged ? List.of("MULTI_URL_FLAGGED") : List.of();

    GeminiAnalysisDto ai =
        geminiClient.analyze(
            analysisText == null ? "" : analysisText,
            primaryUrl == null ? "" : safeString(primaryUrl.getNormalizedUrl(), primaryUrl.getOriginalUrl()),
            fileContext,
            safeBrowsingFlagged,
            threatTypes);

    SandboxScanResultDto sandbox =
        primaryUrl == null ? SandboxScanResultDto.disabled() : sandboxUrlScannerClient.scanUrl(safeString(primaryUrl.getNormalizedUrl(), primaryUrl.getOriginalUrl()));

    int textScore = analysisText == null ? 0 : textRiskModel.score(analysisText);
    int maxUrlScore = primaryUrl == null ? 0 : primaryUrl.getRiskScore();
    int fileRiskScore = attachment == null ? 0 : attachment.riskScore;

    int combined =
        riskScoringService.combineScore(
            ai.getRiskScore(),
            analysisText,
            new SafeBrowsingResultDto(safeBrowsingFlagged, threatTypes),
            sandbox,
            ai.getRedFlags(),
            maxUrlScore,
            textScore,
            fileRiskScore);

    combined += aggregateUrlBonus(urlEvidence);
    if (senderBlocked) combined += 12;
    if (attachment != null && countAttachments(request) > 1) combined += 4;
    combined = ScoreUtil.clampScore(combined);

    var verdict = ScoreUtil.verdictForScore(combined);
    var severity = ScoreUtil.severityForScore(combined);
    double confidence = adjustConfidence(ai.getConfidence(), urlEvidence, sandbox, senderBlocked, fileRiskScore);

    List<RedFlagDto> mergedFlags = new ArrayList<>(ai.getRedFlags() == null ? List.of() : ai.getRedFlags());
    mergeUrlFlags(mergedFlags, urlEvidence);
    if (attachment != null && fileRiskScore >= 60) {
      mergedFlags.add(
          new RedFlagDto(
              "Suspicious Attachment",
              fileRiskScore >= 85 ? "Critical" : "High",
              "Attachment metadata scored " + fileRiskScore + "/100 for malware-style indicators."));
    }
    if (senderBlocked) {
      mergedFlags.add(
          new RedFlagDto("Blocked Sender", "High", "This sender was previously blocked but contacted you again."));
    }

    List<String> actions = new ArrayList<>(ai.getRecommendedActions() == null ? List.of() : ai.getRecommendedActions());
    addAction(actions, "View the full scan details before trusting the message.");
    if (!urlEvidence.isEmpty()) addAction(actions, "Avoid opening links until each extracted URL is verified.");
    if (attachment != null && fileRiskScore >= 60) addAction(actions, "Do not open the attachment outside a controlled environment.");
    if (senderBlocked) addAction(actions, "Keep the sender blocked and report the repeated contact.");

    String summary = buildSummary(ai.getSummary(), urlEvidence, sandbox, senderBlocked, verdict, attachment != null);
    String shortExplanation = buildShortExplanation(analysisText, urlEvidence, senderBlocked, attachment, sandbox, summary);
    String urlStatusSummary = buildUrlStatusSummary(urlEvidence);
    boolean safeToOpen = isSafeToOpen(verdict, combined, analysisText, urlEvidence, attachment == null ? 0 : attachment.riskScore);

    AnalysisRecord record = new AnalysisRecord();
    record.setUserId(userId);
    record.setInputText(analysisText);
    record.setInputUrl(primaryUrl == null ? null : safeString(primaryUrl.getNormalizedUrl(), primaryUrl.getOriginalUrl()));
    record.setInputFileName(attachment == null ? null : attachment.attachment.getFileName());
    record.setInputFileType(attachment == null ? null : attachment.attachment.getContentType());
    record.setInputFileSizeBytes(attachment == null ? null : attachment.attachment.getSizeBytes());
    record.setVerdict(verdict.name());
    record.setRiskScore(combined);
    record.setSeverity(severity.name());
    record.setConfidence(confidence);
    record.setAiSummary(summary);
    record.setSafeBrowsingFlagged(safeBrowsingFlagged);
    record.setThreatTypes(String.join(",", threatTypes));
    record.setRedFlagsJson(jsonUtil.toJson(mergedFlags));
    record.setActionsJson(jsonUtil.toJson(actions));
    record.setSandboxScanJson(jsonUtil.toJson(sandbox));

    AnalysisResponse response = analysisMapper.toResponse(analysisRecordRepository.save(record));

    IncomingScanResultDto result = new IncomingScanResultDto();
    result.setAnalysis(response);
    result.setUrlEvidence(urlEvidence);
    result.setPrimaryAttachment(attachment == null ? null : attachment.attachment);
    result.setPrimaryUrl(primaryUrl == null ? null : safeString(primaryUrl.getNormalizedUrl(), primaryUrl.getOriginalUrl()));
    result.setShortExplanation(shortExplanation);
    result.setUrlStatusSummary(urlStatusSummary);
    result.setSafeToOpen(safeToOpen);
    return result;
  }

  private String buildAnalysisText(com.phishguardai.backend.dto.BackgroundIngestRequest request) {
    List<String> parts = new ArrayList<>();
    if (TextUtil.nullIfBlank(request.getSenderLabel()) != null) parts.add("Sender: " + request.getSenderLabel().trim());
    if (TextUtil.nullIfBlank(request.getSenderAddress()) != null) parts.add("Sender address: " + request.getSenderAddress().trim());
    if (TextUtil.nullIfBlank(request.getSubjectLine()) != null) parts.add("Subject: " + request.getSubjectLine().trim());
    if (TextUtil.nullIfBlank(request.getTextContent()) != null) parts.add(request.getTextContent().trim());
    if (TextUtil.nullIfBlank(request.getHtmlContent()) != null) parts.add("HTML snippet: " + TextUtil.truncate(request.getHtmlContent().replaceAll("\\s+", " "), 1200));
    if (parts.isEmpty()) return null;
    return TextUtil.truncate(String.join("\n", parts), 10000);
  }

  private void mergeUrlFlags(List<RedFlagDto> mergedFlags, List<UrlEvidenceDto> urlEvidence) {
    urlEvidence.stream()
        .filter(item -> !"VALID".equals(item.getStatusLabel()))
        .sorted(Comparator.comparingInt(UrlEvidenceDto::getRiskScore).reversed())
        .limit(4)
        .forEach(
            item ->
                mergedFlags.add(
                    new RedFlagDto(
                        "URL " + item.getStatusLabel(),
                        item.getRiskScore() >= 85 ? "Critical" : item.getRiskScore() >= 60 ? "High" : "Medium",
                        TextUtil.truncate(
                            safeString(item.getFinalUrl(), item.getNormalizedUrl(), item.getOriginalUrl())
                                + " scored "
                                + item.getRiskScore()
                                + "/100. "
                                + String.join(" ", item.getEvidence()),
                            300))));
  }

  private int aggregateUrlBonus(List<UrlEvidenceDto> urlEvidence) {
    int malicious = 0;
    int suspicious = 0;
    int redirected = 0;
    for (UrlEvidenceDto item : urlEvidence) {
      if (item.isMalicious()) malicious++;
      else if (item.isSuspicious()) suspicious++;
      if (item.isRedirected()) redirected++;
    }
    return malicious * 8 + suspicious * 3 + Math.max(0, redirected - 1) * 2;
  }

  private double adjustConfidence(
      double aiConfidence, List<UrlEvidenceDto> urlEvidence, SandboxScanResultDto sandbox, boolean senderBlocked, int fileRiskScore) {
    double confidence = Math.max(0.45, aiConfidence);
    if (urlEvidence.stream().anyMatch(UrlEvidenceDto::isSafeBrowsingFlagged)) confidence = Math.max(confidence, 0.94);
    if (urlEvidence.stream().anyMatch(UrlEvidenceDto::isMalicious)) confidence = Math.max(confidence, 0.9);
    if (sandbox.isReachable() && sandbox.getRiskScore() >= 65) confidence = Math.max(confidence, 0.9);
    if (senderBlocked) confidence = Math.max(confidence, 0.88);
    if (fileRiskScore >= 80) confidence = Math.max(confidence, 0.9);
    return Math.min(confidence, 0.99);
  }

  private String buildSummary(
      String aiSummary, List<UrlEvidenceDto> urlEvidence, SandboxScanResultDto sandbox, boolean senderBlocked, com.phishguardai.backend.dto.Verdict verdict,
      boolean hasAttachment) {
    if (sandbox.isReachable() && sandbox.getRiskScore() >= 75) {
      return "The highest-risk URL behaved suspiciously in the isolated browser scan and should not be trusted.";
    }
    if (urlEvidence.stream().anyMatch(UrlEvidenceDto::isMalicious)) {
      return "One or more extracted URLs were classified as malicious or flagged by reputation checks.";
    }
    if (senderBlocked) {
      return "This message came from a sender you already blocked, which increases the likelihood of a repeat phishing attempt.";
    }
    if (hasAttachment && verdict == com.phishguardai.backend.dto.Verdict.Malicious) {
      return "The message includes a risky attachment and should not be opened until it is verified in a secure environment.";
    }
    return TextUtil.truncate(TextUtil.nullIfBlank(aiSummary), 800);
  }

  private String buildShortExplanation(
      String analysisText,
      List<UrlEvidenceDto> urlEvidence,
      boolean senderBlocked,
      AttachmentAssessment attachment,
      SandboxScanResultDto sandbox,
      String summary) {
    if (urlEvidence.stream().anyMatch(UrlEvidenceDto::isMalicious)) {
      return "Malicious or spoofed link behavior was detected in the message.";
    }
    if (sandbox.isReachable() && sandbox.getRiskScore() >= 65) {
      return "The main link behaved suspiciously in the sandbox scan.";
    }
    if (containsSensitiveRequest(analysisText)) {
      return "The message asks for OTP, banking, or prize-claim details, which is a common phishing tactic.";
    }
    if (attachment != null && attachment.riskScore >= 70) {
      return "The attachment metadata matches common malware delivery patterns.";
    }
    if (senderBlocked) {
      return "The sender matches a blocked sender and should not be trusted automatically.";
    }
    return TextUtil.truncate(TextUtil.nullIfBlank(summary), 200);
  }

  private String buildUrlStatusSummary(List<UrlEvidenceDto> urlEvidence) {
    if (urlEvidence == null || urlEvidence.isEmpty()) return "No URLs found";
    int malicious = 0;
    int suspicious = 0;
    int redirected = 0;
    int valid = 0;
    for (UrlEvidenceDto item : urlEvidence) {
      if (item.isMalicious()) malicious++;
      else if (item.isSuspicious()) suspicious++;
      else valid++;
      if (item.isRedirected()) redirected++;
    }
    List<String> parts = new ArrayList<>();
    if (malicious > 0) parts.add(malicious + " malicious");
    if (suspicious > 0) parts.add(suspicious + " suspicious");
    if (redirected > 0) parts.add(redirected + " redirected");
    if (valid > 0) parts.add(valid + " valid");
    return String.join(", ", parts);
  }

  private boolean isSafeToOpen(
      com.phishguardai.backend.dto.Verdict verdict,
      int combinedScore,
      String analysisText,
      List<UrlEvidenceDto> urlEvidence,
      int fileRiskScore) {
    if (verdict == com.phishguardai.backend.dto.Verdict.Suspicious || verdict == com.phishguardai.backend.dto.Verdict.Malicious) return false;
    if (urlEvidence.stream().anyMatch(item -> item.isMalicious() || item.isSpoofed())) return false;
    if (combinedScore >= 35) return false;
    if (containsSensitiveRequest(analysisText)) return false;
    return fileRiskScore < 60;
  }

  private boolean containsSensitiveRequest(String text) {
    if (text == null || text.isBlank()) return false;
    String t = text.toLowerCase(Locale.ROOT);
    boolean asksForOtp =
        containsAny(t, "otp", "opt", "one time password", "pin")
            && containsAny(t, "share", "tell", "send", "give", "forward");
    boolean bankTheft =
        containsAny(t, "bank", "bank account", "account", "card", "upi", "net banking", "kyc")
            && containsAny(t, "verify", "update", "unlock", "blocked", "suspended", "confirm", "login");
    boolean prizeScam =
        containsAny(t, "won", "winner", "congratulations", "prize", "lottery", "reward", "gift")
            && containsAny(t, "claim", "click", "tap", "pay fee", "send");
    return asksForOtp || bankTheft || prizeScam;
  }

  private AttachmentAssessment pickPrimaryAttachment(com.phishguardai.backend.dto.BackgroundIngestRequest request) {
    List<IncomingAttachmentDto> attachments = new ArrayList<>();
    if (TextUtil.nullIfBlank(request.getFileName()) != null
        || TextUtil.nullIfBlank(request.getFileContentType()) != null
        || request.getFileSizeBytes() != null
        || TextUtil.nullIfBlank(request.getFileSha256()) != null
        || TextUtil.nullIfBlank(request.getFileSampleBase64()) != null) {
      IncomingAttachmentDto legacy = new IncomingAttachmentDto();
      legacy.setFileName(request.getFileName());
      legacy.setContentType(request.getFileContentType());
      legacy.setSizeBytes(request.getFileSizeBytes());
      legacy.setSha256(request.getFileSha256());
      legacy.setSampleBase64(request.getFileSampleBase64());
      attachments.add(legacy);
    }
    if (request.getAttachments() != null) {
      attachments.addAll(request.getAttachments());
    }
    AttachmentAssessment best = null;
    for (IncomingAttachmentDto attachment : attachments) {
      String sha = normalizeSha256(attachment.getSha256());
      byte[] sample = decodeBase64(attachment.getSampleBase64());
      int score =
          fileRiskModel.score(
              attachment.getFileName(), attachment.getContentType(), attachment.getSizeBytes(), sha, sample);
      if (best == null || score > best.riskScore) {
        best = new AttachmentAssessment(attachment, score, sha, sample);
      }
    }
    return best;
  }

  private int countAttachments(com.phishguardai.backend.dto.BackgroundIngestRequest request) {
    int total = request.getAttachments() == null ? 0 : request.getAttachments().size();
    if (TextUtil.nullIfBlank(request.getFileName()) != null) total++;
    return total;
  }

  private String normalizeSha256(String raw) {
    String value = TextUtil.nullIfBlank(raw);
    if (value == null) return null;
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    return normalized.matches("[0-9a-f]{64}") ? normalized : null;
  }

  private byte[] decodeBase64(String raw) {
    String value = TextUtil.nullIfBlank(raw);
    if (value == null) return new byte[0];
    try {
      return Base64.getDecoder().decode(value);
    } catch (IllegalArgumentException e) {
      return new byte[0];
    }
  }

  private void addAction(List<String> actions, String action) {
    if (action == null || action.isBlank() || actions.contains(action)) return;
    actions.add(action);
  }

  private boolean containsAny(String haystack, String... needles) {
    for (String needle : needles) {
      if (haystack.contains(needle)) return true;
    }
    return false;
  }

  private String safeString(String... candidates) {
    for (String value : candidates) {
      if (TextUtil.nullIfBlank(value) != null) return value;
    }
    return null;
  }

  private static class AttachmentAssessment {
    private final IncomingAttachmentDto attachment;
    private final int riskScore;
    private final String sha256;
    private final byte[] sample;

    private AttachmentAssessment(IncomingAttachmentDto attachment, int riskScore, String sha256, byte[] sample) {
      this.attachment = attachment;
      this.riskScore = riskScore;
      this.sha256 = sha256;
      this.sample = sample;
    }
  }
}
