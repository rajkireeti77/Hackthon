package com.phishguardai.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phishguardai.backend.config.GeminiProperties;
import com.phishguardai.backend.dto.GeminiAnalysisDto;
import com.phishguardai.backend.dto.RedFlagDto;
import com.phishguardai.backend.dto.Severity;
import com.phishguardai.backend.dto.Verdict;
import com.phishguardai.backend.util.ScoreUtil;
import com.phishguardai.backend.util.TextUtil;
import com.phishguardai.backend.util.exception.ExternalServiceException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GeminiClient {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GeminiClient.class);
  private final RestClient restClient;
  private final GeminiProperties props;
  private final LocalLlmClient localLlmClient;
  private final ObjectMapper mapper;

  public GeminiClient(RestClient restClient, GeminiProperties props, LocalLlmClient localLlmClient, ObjectMapper mapper) {
    this.restClient = restClient;
    this.props = props;
    this.localLlmClient = localLlmClient;
    this.mapper = mapper;
  }

  public GeminiAnalysisDto analyze(
      String textContent, String url, String fileContext, boolean safeBrowsingFlagged, List<String> threatTypes) {
    if (localLlmClient.isEnabled()) {
      try {
        return localLlmClient.analyze(textContent, url, fileContext, safeBrowsingFlagged, threatTypes);
      } catch (ExternalServiceException e) {
        log.warn("Falling back from local LLM to Gemini/heuristics: {}", e.getMessage());
      }
    }

    if (props.getApiKey() == null || props.getApiKey().isBlank()) {
      return heuristicAnalysis(textContent, url, fileContext, safeBrowsingFlagged, threatTypes);
    }

    String prompt = buildPrompt(textContent, url, fileContext, safeBrowsingFlagged, threatTypes);
    try {
      String endpoint =
          props.getBaseUrl()
              + "/v1beta/models/"
              + props.getModel()
              + ":generateContent?key="
              + props.getApiKey();

      Map<String, Object> body =
          Map.of(
              "contents",
              List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
              "generationConfig",
              Map.of(
                  "temperature",
                  0.2,
                  "maxOutputTokens",
                  800,
                  "responseMimeType",
                  "application/json"));

      String json =
          restClient
              .post()
              .uri(endpoint)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(String.class);

      if (json == null || json.isBlank()) {
        throw new ExternalServiceException("Gemini returned an empty response.");
      }

      String extracted = extractModelTextAsJson(json);
      return parseStructuredAnalysis(extracted);
    } catch (ExternalServiceException e) {
      throw e;
    } catch (Exception e) {
      throw new ExternalServiceException("Gemini analysis failed.", e);
    }
  }

  private GeminiAnalysisDto heuristicAnalysis(
      String textContent, String url, String fileContext, boolean safeBrowsingFlagged, List<String> threatTypes) {
    String t = (textContent == null) ? "" : textContent.toLowerCase();
    String u = (url == null) ? "" : url.toLowerCase();
    String f = (fileContext == null) ? "" : fileContext.toLowerCase();

    int score = 10;
    List<RedFlagDto> flags = new ArrayList<>();

    if (safeBrowsingFlagged) {
      score += 60;
      flags.add(
          new RedFlagDto(
              "URL Reputation",
              "Critical",
              "The URL is flagged by Safe Browsing ("
                  + ((threatTypes == null || threatTypes.isEmpty()) ? "Unknown threat" : String.join(", ", threatTypes))
                  + ")."));
    }

    // Don't penalize just because the user provided a URL field.
    boolean hasLinkInText = t.contains("http://") || t.contains("https://");
    if (hasLinkInText) score += 10;

    String[] urgent = {"urgent", "immediately", "asap", "right now", "last chance", "limited time", "act now"};
    String[] creds = {"password", "otp", "one time password", "pin", "ssn", "aadhar", "pan", "cvv", "bank", "card"};
    String[] threat = {"account will be", "suspended", "locked", "blocked", "legal action", "fine", "arrest"};
    String[] lure = {"verify", "confirm", "update", "reset", "login", "sign in", "click", "tap"};

    score += bumpForMatches(t, urgent, flags, "Urgency", "High", "Uses urgency/pressure language.");
    score += bumpForMatches(t, threat, flags, "Threat/Fear", "High", "Uses fear/threats to pressure action.");
    score += bumpForMatches(t, creds, flags, "Credential/Payment Theft", "Critical", "Asks for sensitive credentials or payment info.");
    score += bumpForMatches(t, lure, flags, "Call To Action", "Medium", "Pushes you to click/verify/update quickly.");
    score += bumpForMatches(
        f,
        new String[] {".exe", ".dll", ".scr", ".bat", ".cmd", ".ps1", ".vbs", ".jar", ".apk", ".msi", ".lnk"},
        flags,
        "Malicious File Type",
        "Critical",
        "The uploaded file has an executable or scriptable format often abused by malware.");
    score += bumpForMatches(
        f,
        new String[] {"doubleextension=true", "macroextension=true", "samplehasmz=true", "samplehaself=true"},
        flags,
        "Suspicious File Signal",
        "High",
        "The uploaded file exposes static malware indicators.");

    score = ScoreUtil.clampScore(score);

    String summary =
        score >= 70
            ? "This looks potentially malicious and may be a phishing attempt."
            : (score >= 40
                ? "This looks suspicious. Be careful before clicking links, opening files, or sharing information."
                : "This looks mostly safe, but stay cautious with links, attachments, and requests for sensitive data.");
    if (!f.isBlank() && score >= 60) {
      summary = "The attached file shows malware-style indicators and should be treated as unsafe until verified.";
    }

    GeminiAnalysisDto dto = new GeminiAnalysisDto();
    dto.setRiskScore(score);
    dto.setVerdict(ScoreUtil.verdictForScore(score));
    dto.setSeverity(ScoreUtil.severityForScore(score));
    dto.setConfidence(0.55);
    dto.setSummary(summary);
    dto.setRedFlags(flags);
    dto.setRecommendedActions(
        List.of(
            "Do not click unknown links",
            "Verify the sender via official channels",
            "Never share OTPs/passwords",
            "Scan suspicious files before opening them",
            "Report or delete suspicious messages"));
    return dto;
  }

  private static int bumpForMatches(
      String haystack,
      String[] needles,
      List<RedFlagDto> flags,
      String type,
      String severity,
      String description) {
    for (String n : needles) {
      if (haystack.contains(n)) {
        flags.add(new RedFlagDto(type, severity, description));
        return 15;
      }
    }
    return 0;
  }

  private String buildPrompt(
      String textContent, String url, String fileContext, boolean safeBrowsingFlagged, List<String> threatTypes) {
    String safeText = TextUtil.nullIfBlank(textContent);
    String safeUrl = TextUtil.nullIfBlank(url);
    String safeFile = TextUtil.nullIfBlank(fileContext);
    String threatList = (threatTypes == null || threatTypes.isEmpty()) ? "[]" : threatTypes.toString();

    return """
You are PhishGuard AI, a cybersecurity assistant specialized in phishing and social engineering detection.

Analyze the following user-provided content and produce ONLY a strict JSON object (no markdown, no code fences, no extra text).

Inputs:
- textContent: %s
- url: %s
- fileContext: %s
- safeBrowsingFlagged: %s
- safeBrowsingThreatTypes: %s

Task:
1) Detect phishing/social engineering tactics and file-malware indicators: urgency, fear, authority impersonation, credential theft intent, suspicious tone, scam indicators, spoofing, dangerous attachments, double extensions, macro/executable payloads.
2) Decide a final verdict: Safe | LowRisk | Suspicious | Malicious
3) Provide an explainable summary in simple English (max 2 sentences).
4) Provide redFlags: array of objects { type, severity, description } where severity is Low|Medium|High|Critical.
5) Provide recommendedActions: 3-6 short bullet-like strings.
6) Provide riskScore: integer 0-100.
7) Provide confidence: number 0.0-1.0.
8) Provide severity: Low | Medium | High | Critical.

Required JSON schema:
{
  "verdict": "Safe|LowRisk|Suspicious|Malicious",
  "riskScore": 0,
  "severity": "Low|Medium|High|Critical",
  "confidence": 0.0,
  "summary": "string",
  "redFlags": [{"type":"string","severity":"Low|Medium|High|Critical","description":"string"}],
  "recommendedActions": ["string"]
}
""".formatted(
        safeText == null ? "null" : mapperSafeQuote(safeText),
        safeUrl == null ? "null" : mapperSafeQuote(safeUrl),
        safeFile == null ? "null" : mapperSafeQuote(safeFile),
        safeBrowsingFlagged,
        threatList);
  }

  private String mapperSafeQuote(String s) {
    String t = TextUtil.truncate(s, 4000);
    try {
      return mapper.writeValueAsString(t);
    } catch (Exception e) {
      return "\"" + t.replace("\"", "\\\"") + "\"";
    }
  }

  private String extractModelTextAsJson(String geminiApiResponseJson) throws Exception {
    JsonNode root = mapper.readTree(geminiApiResponseJson);
    JsonNode candidates = root.get("candidates");
    if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
      throw new ExternalServiceException("Gemini returned no candidates.");
    }
    JsonNode content = candidates.get(0).path("content");
    JsonNode parts = content.path("parts");
    if (!parts.isArray() || parts.isEmpty()) {
      throw new ExternalServiceException("Gemini returned no content parts.");
    }
    String text = parts.get(0).path("text").asText(null);
    if (text == null || text.isBlank()) {
      throw new ExternalServiceException("Gemini returned empty content text.");
    }
    String trimmed = text.trim();
    if (trimmed.startsWith("```")) {
      // Defensive: strip possible markdown fences
      trimmed = trimmed.replaceAll("^```[a-zA-Z]*\\s*", "");
      trimmed = trimmed.replaceAll("\\s*```$", "");
      trimmed = trimmed.trim();
    }
    return trimmed;
  }

  private GeminiAnalysisDto parseStructuredAnalysis(String structuredJson) throws Exception {
    JsonNode root = mapper.readTree(structuredJson);

    GeminiAnalysisDto dto = new GeminiAnalysisDto();
    dto.setVerdict(parseEnum(root.path("verdict").asText(null), Verdict.class, Verdict.Suspicious));
    dto.setRiskScore(ScoreUtil.clampScore(root.path("riskScore").asInt(50)));
    dto.setSeverity(parseEnum(root.path("severity").asText(null), Severity.class, ScoreUtil.severityForScore(dto.getRiskScore())));
    dto.setConfidence(clampDouble(root.path("confidence").asDouble(0.6), 0.0, 1.0));
    dto.setSummary(TextUtil.truncate(root.path("summary").asText(""), 800));

    List<RedFlagDto> redFlags = new ArrayList<>();
    JsonNode flags = root.get("redFlags");
    if (flags != null && flags.isArray()) {
      for (JsonNode f : flags) {
        String type = TextUtil.truncate(f.path("type").asText("Red Flag"), 120);
        String sev = TextUtil.truncate(f.path("severity").asText("Medium"), 16);
        String desc = TextUtil.truncate(f.path("description").asText(""), 300);
        if (!type.isBlank() && !desc.isBlank()) {
          redFlags.add(new RedFlagDto(type, sev, desc));
        }
      }
    }
    dto.setRedFlags(redFlags);

    List<String> actions = new ArrayList<>();
    JsonNode recs = root.get("recommendedActions");
    if (recs != null && recs.isArray()) {
      for (JsonNode a : recs) {
        String v = TextUtil.truncate(a.asText(""), 180);
        if (!v.isBlank()) actions.add(v);
      }
    }
    dto.setRecommendedActions(actions);

    return dto;
  }

  private static double clampDouble(double v, double min, double max) {
    if (v < min) return min;
    return Math.min(v, max);
  }

  private static <T extends Enum<T>> T parseEnum(String value, Class<T> type, T fallback) {
    if (value == null) return fallback;
    String normalized = value.trim().replace(" ", "").replace("_", "");
    for (T e : type.getEnumConstants()) {
      if (e.name().equalsIgnoreCase(normalized)) return e;
    }
    return fallback;
  }
}

