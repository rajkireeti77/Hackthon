package com.phishguardai.backend.service;

import com.phishguardai.backend.dto.AnalyzeRequest;
import com.phishguardai.backend.dto.AnalysisResponse;
import com.phishguardai.backend.dto.GeminiAnalysisDto;
import com.phishguardai.backend.dto.RedFlagDto;
import com.phishguardai.backend.dto.SafeBrowsingResultDto;
import com.phishguardai.backend.dto.SandboxScanResultDto;
import com.phishguardai.backend.entity.AnalysisRecord;
import com.phishguardai.backend.repository.AnalysisRecordRepository;
import com.phishguardai.backend.util.JsonUtil;
import com.phishguardai.backend.util.ScoreUtil;
import com.phishguardai.backend.util.TextUtil;
import com.phishguardai.backend.util.UrlValidationUtil;
import com.phishguardai.backend.util.exception.BadRequestException;
import com.phishguardai.backend.util.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {
  private final SafeBrowsingClient safeBrowsingClient;
  private final GeminiClient geminiClient;
  private final RiskScoringService scoring;
  private final UrlRiskModel urlRiskModel;
  private final TextRiskModel textRiskModel;
  private final FileRiskModel fileRiskModel;
  private final SandboxUrlScannerClient sandboxUrlScannerClient;
  private final AnalysisRecordRepository repo;
  private final AnalysisMapper mapper;
  private final JsonUtil json;

  public AnalysisService(
      SafeBrowsingClient safeBrowsingClient,
      GeminiClient geminiClient,
      RiskScoringService scoring,
      UrlRiskModel urlRiskModel,
      TextRiskModel textRiskModel,
      FileRiskModel fileRiskModel,
      SandboxUrlScannerClient sandboxUrlScannerClient,
      AnalysisRecordRepository repo,
      AnalysisMapper mapper,
      JsonUtil json) {
    this.safeBrowsingClient = safeBrowsingClient;
    this.geminiClient = geminiClient;
    this.scoring = scoring;
    this.urlRiskModel = urlRiskModel;
    this.textRiskModel = textRiskModel;
    this.fileRiskModel = fileRiskModel;
    this.sandboxUrlScannerClient = sandboxUrlScannerClient;
    this.repo = repo;
    this.mapper = mapper;
    this.json = json;
  }

  @Transactional
  public AnalysisResponse analyzeAndSave(String userId, AnalyzeRequest req) {
    String text = TextUtil.nullIfBlank(req.getTextContent());
    String urlRaw = TextUtil.nullIfBlank(req.getUrl());
    String fileName = TextUtil.nullIfBlank(req.getFileName());
    String fileContentType = TextUtil.nullIfBlank(req.getFileContentType());
    Long fileSizeBytes = req.getFileSizeBytes();
    String fileSha256 = normalizeSha256(req.getFileSha256());
    byte[] fileSample = decodeBase64(req.getFileSampleBase64());
    boolean hasFile = hasFileInput(fileName, fileContentType, fileSizeBytes, fileSha256, fileSample);
    if (text == null && urlRaw == null && !hasFile) {
      throw new BadRequestException("Please provide text content, a URL, and/or file metadata to analyze.");
    }

    String url = null;
    if (urlRaw != null) {
      url = UrlValidationUtil.normalizeAndValidateHttpUrl(urlRaw);
    }

    SafeBrowsingResultDto safeBrowsing = new SafeBrowsingResultDto(false, List.of());
    if (url != null) {
      safeBrowsing = safeBrowsingClient.checkUrl(url);
    }

    SandboxScanResultDto sandboxScan = SandboxScanResultDto.disabled();
    if (url != null) {
      sandboxScan = sandboxUrlScannerClient.scanUrl(url);
    }

    int urlRiskScore = 0;
    if (url != null) {
      urlRiskScore = urlRiskModel.score(url);
    }

    int textModelScore = 0;
    if (text != null) {
      textModelScore = textRiskModel.score(text);
    }

    int fileRiskScore = 0;
    if (hasFile) {
      fileRiskScore = fileRiskModel.score(fileName, fileContentType, fileSizeBytes, fileSha256, fileSample);
    }

    GeminiAnalysisDto ai =
        geminiClient.analyze(
            Objects.toString(text, ""),
            Objects.toString(url, ""),
            hasFile ? fileRiskModel.describeForPrompt(fileName, fileContentType, fileSizeBytes, fileSha256, fileSample) : "",
            safeBrowsing.isFlagged(),
            safeBrowsing.getThreatTypes());

    int combinedScore =
        scoring.combineScore(
            ai.getRiskScore(), text, safeBrowsing, sandboxScan, ai.getRedFlags(), urlRiskScore, textModelScore, fileRiskScore);
    var finalVerdict = ScoreUtil.verdictForScore(combinedScore);
    var finalSeverity = ScoreUtil.severityForScore(combinedScore);

    List<RedFlagDto> mergedFlags = ai.getRedFlags();
    if (safeBrowsing.isFlagged()) {
      mergedFlags =
          mergeSafeBrowsingFlag(
              mergedFlags,
              safeBrowsing.getThreatTypes() == null ? List.of() : safeBrowsing.getThreatTypes());
    }
    if (url != null && urlRiskScore >= 70) {
      mergedFlags =
          mergeUrlModelFlag(
              mergedFlags, urlRiskScore, urlRiskModel.explain(url));
    }
    if (sandboxScan.isReachable() && sandboxScan.getRiskScore() >= 55) {
      mergedFlags = mergeSandboxFlag(mergedFlags, sandboxScan);
    }
    if (hasFile && fileRiskScore >= 65) {
      mergedFlags =
          mergeFileModelFlag(
              mergedFlags, fileRiskScore, fileRiskModel.explain(fileName, fileContentType, fileSizeBytes, fileSha256, fileSample));
    }

    List<String> actions = ai.getRecommendedActions();
    if (actions == null || actions.isEmpty()) {
      actions =
          List.of(
              "Do not click suspicious links",
              "Do not share OTP or passwords",
              "Verify the sender via official channels",
              "Report the message as phishing if unsure");
    }
    if (hasFile && fileRiskScore >= 65) {
      actions =
          mergeAction(
              actions,
              "Do not open the file until it has been scanned with antivirus or sandbox tools");
    }
    if (sandboxScan.isReachable() && sandboxScan.getRiskScore() >= 55) {
      actions = mergeAction(actions, "Do not visit the URL outside a sandbox until it has been verified");
    }

    String summary = ai.getSummary();
    if (sandboxScan.isReachable() && sandboxScan.getRiskScore() >= 70) {
      summary = "The URL behaved suspiciously in an isolated browser environment and should be treated as unsafe.";
    }

    AnalysisRecord record = new AnalysisRecord();
    record.setUserId(userId);
    record.setInputText(text);
    record.setInputUrl(url);
    record.setInputFileName(fileName);
    record.setInputFileType(fileContentType);
    record.setInputFileSizeBytes(fileSizeBytes);
    record.setVerdict(finalVerdict.name());
    record.setRiskScore(combinedScore);
    record.setSeverity(finalSeverity.name());
    record.setConfidence(ai.getConfidence());
    record.setAiSummary(summary);
    record.setSafeBrowsingFlagged(safeBrowsing.isFlagged());
    record.setThreatTypes(
        safeBrowsing.getThreatTypes() == null ? "" : String.join(",", safeBrowsing.getThreatTypes()));
    record.setRedFlagsJson(json.toJson(mergedFlags));
    record.setActionsJson(json.toJson(actions));
    record.setSandboxScanJson(json.toJson(sandboxScan));

    AnalysisRecord saved = repo.save(record);
    return mapper.toResponse(saved);
  }

  public List<com.phishguardai.backend.dto.AnalysisSummaryDto> recentAnalyses(String userId) {
    return repo.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream().map(mapper::toSummary).toList();
  }

  public AnalysisResponse getOne(String userId, Long id) {
    AnalysisRecord r =
        repo.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Scan not found."));
    return mapper.toResponse(r);
  }

  @Transactional
  public void deleteOne(String userId, Long id) {
    AnalysisRecord r =
        repo.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Scan not found."));
    repo.delete(r);
  }

  private List<RedFlagDto> mergeSafeBrowsingFlag(List<RedFlagDto> current, List<String> threatTypes) {
    List<RedFlagDto> out = (current == null) ? new java.util.ArrayList<>() : new java.util.ArrayList<>(current);
    String types = threatTypes == null || threatTypes.isEmpty() ? "Unsafe URL" : String.join(", ", threatTypes);
    out.add(
        new RedFlagDto(
            "Suspicious URL",
            "Critical",
            "The provided URL is flagged by Google Safe Browsing (" + types + ")."));
    return out;
  }

  private List<RedFlagDto> mergeUrlModelFlag(List<RedFlagDto> current, int urlScore, String reason) {
    List<RedFlagDto> out = (current == null) ? new java.util.ArrayList<>() : new java.util.ArrayList<>(current);
    String sev = urlScore >= 85 ? "Critical" : (urlScore >= 70 ? "High" : "Medium");
    out.add(new RedFlagDto("Suspicious URL", sev, "URL risk model score " + urlScore + "/100 (" + reason + ")."));
    return out;
  }

  private List<RedFlagDto> mergeFileModelFlag(List<RedFlagDto> current, int fileScore, String reason) {
    List<RedFlagDto> out = (current == null) ? new java.util.ArrayList<>() : new java.util.ArrayList<>(current);
    String sev = fileScore >= 90 ? "Critical" : (fileScore >= 75 ? "High" : "Medium");
    out.add(new RedFlagDto("Suspicious File", sev, "File risk model score " + fileScore + "/100 (" + reason + ")."));
    return out;
  }

  private List<String> mergeAction(List<String> current, String action) {
    List<String> out = (current == null) ? new java.util.ArrayList<>() : new java.util.ArrayList<>(current);
    if (!out.contains(action)) out.add(action);
    return out;
  }

  private static boolean hasFileInput(
      String fileName, String fileContentType, Long fileSizeBytes, String fileSha256, byte[] fileSample) {
    return fileName != null
        || fileContentType != null
        || fileSizeBytes != null
        || fileSha256 != null
        || (fileSample != null && fileSample.length > 0);
  }

  private static String normalizeSha256(String raw) {
    String value = TextUtil.nullIfBlank(raw);
    if (value == null) return null;
    String normalized = value.trim().toLowerCase();
    return normalized.matches("[0-9a-f]{64}") ? normalized : null;
  }

  private static byte[] decodeBase64(String raw) {
    String value = TextUtil.nullIfBlank(raw);
    if (value == null) return new byte[0];
    try {
      return Base64.getDecoder().decode(value);
    } catch (IllegalArgumentException e) {
      return new byte[0];
    }
  }

  private List<RedFlagDto> mergeSandboxFlag(List<RedFlagDto> current, SandboxScanResultDto sandboxScan) {
    List<RedFlagDto> out = (current == null) ? new java.util.ArrayList<>() : new java.util.ArrayList<>(current);
    StringBuilder description = new StringBuilder();
    description.append("Sandbox URL scan score ").append(sandboxScan.getRiskScore()).append("/100");
    if (sandboxScan.getRedirectCount() > 0) {
      description.append("; redirects=").append(sandboxScan.getRedirectCount());
    }
    if (sandboxScan.isLoginFormDetected()) {
      description.append("; login form detected");
    }
    if (sandboxScan.isDownloadAttempted()) {
      description.append("; download attempt detected");
    }
    if (sandboxScan.getMaliciousIndicators() != null && !sandboxScan.getMaliciousIndicators().isEmpty()) {
      description.append("; indicators=").append(String.join(", ", sandboxScan.getMaliciousIndicators()));
    }
    String severity = sandboxScan.getRiskScore() >= 85 ? "Critical" : (sandboxScan.getRiskScore() >= 70 ? "High" : "Medium");
    out.add(new RedFlagDto("Sandbox URL Behavior", severity, description.toString() + "."));
    return out;
  }
}

