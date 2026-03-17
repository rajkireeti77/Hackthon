package com.phishguardai.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.phishguardai.backend.dto.AnalysisResponse;
import com.phishguardai.backend.dto.AnalysisSummaryDto;
import com.phishguardai.backend.dto.RedFlagDto;
import com.phishguardai.backend.dto.SafeBrowsingResultDto;
import com.phishguardai.backend.dto.SandboxScanResultDto;
import com.phishguardai.backend.dto.Severity;
import com.phishguardai.backend.dto.Verdict;
import com.phishguardai.backend.entity.AnalysisRecord;
import com.phishguardai.backend.util.JsonUtil;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AnalysisMapper {
  private final JsonUtil json;

  public AnalysisMapper(JsonUtil json) {
    this.json = json;
  }

  public AnalysisSummaryDto toSummary(AnalysisRecord r) {
    AnalysisSummaryDto dto = new AnalysisSummaryDto();
    dto.setId(r.getId());
    dto.setVerdict(Verdict.valueOf(r.getVerdict()));
    dto.setRiskScore(r.getRiskScore());
    dto.setSeverity(Severity.valueOf(r.getSeverity()));
    dto.setCreatedAt(OffsetDateTime.ofInstant(r.getCreatedAt(), ZoneOffset.UTC));
    dto.setInputType(inputType(r.getInputText(), r.getInputUrl(), r.getInputFileName()));
    return dto;
  }

  public AnalysisResponse toResponse(AnalysisRecord r) {
    AnalysisResponse dto = new AnalysisResponse();
    dto.setId(r.getId());
    dto.setVerdict(Verdict.valueOf(r.getVerdict()));
    dto.setRiskScore(r.getRiskScore());
    dto.setSeverity(Severity.valueOf(r.getSeverity()));
    dto.setConfidence(r.getConfidence());
    dto.setSummary(r.getAiSummary());

    List<RedFlagDto> redFlags = new ArrayList<>();
    if (r.getRedFlagsJson() != null && !r.getRedFlagsJson().isBlank()) {
      redFlags =
          json.fromJson(
              r.getRedFlagsJson(), new TypeReference<List<RedFlagDto>>() {});
    }
    dto.setRedFlags(redFlags);

    SafeBrowsingResultDto sb = new SafeBrowsingResultDto();
    sb.setFlagged(r.isSafeBrowsingFlagged());
    sb.setThreatTypes(parseThreatTypes(r.getThreatTypes()));
    dto.setSafeBrowsingResult(sb);

    SandboxScanResultDto sandbox = SandboxScanResultDto.disabled();
    if (r.getSandboxScanJson() != null && !r.getSandboxScanJson().isBlank()) {
      sandbox = json.fromJson(r.getSandboxScanJson(), SandboxScanResultDto.class);
    }
    dto.setSandboxScanResult(sandbox);

    List<String> actions = new ArrayList<>();
    if (r.getActionsJson() != null && !r.getActionsJson().isBlank()) {
      actions = json.fromJson(r.getActionsJson(), new TypeReference<List<String>>() {});
    }
    dto.setRecommendedActions(actions);
    dto.setFileName(r.getInputFileName());
    dto.setFileContentType(r.getInputFileType());
    dto.setFileSizeBytes(r.getInputFileSizeBytes());
    dto.setCreatedAt(OffsetDateTime.ofInstant(r.getCreatedAt(), ZoneOffset.UTC));

    return dto;
  }

  private static String inputType(String text, String url, String fileName) {
    boolean hasText = text != null && !text.isBlank();
    boolean hasUrl = url != null && !url.isBlank();
    boolean hasFile = fileName != null && !fileName.isBlank();
    if (hasText && hasUrl && hasFile) return "TEXT_URL_FILE";
    if (hasText && hasUrl) return "TEXT_AND_URL";
    if (hasText && hasFile) return "TEXT_AND_FILE";
    if (hasUrl && hasFile) return "URL_AND_FILE";
    if (hasText) return "TEXT_ONLY";
    if (hasUrl) return "URL_ONLY";
    if (hasFile) return "FILE_ONLY";
    return "UNKNOWN";
  }

  private static List<String> parseThreatTypes(String threatTypes) {
    if (threatTypes == null || threatTypes.isBlank()) return List.of();
    String[] parts = threatTypes.split(",");
    List<String> out = new ArrayList<>();
    for (String p : parts) {
      String t = p.trim();
      if (!t.isBlank()) out.add(t);
    }
    return out;
  }
}

