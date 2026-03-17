package com.phishguardai.backend.service;

import com.phishguardai.backend.dto.SafeBrowsingResultDto;
import com.phishguardai.backend.dto.UrlEvidenceDto;
import com.phishguardai.backend.util.ScoreUtil;
import com.phishguardai.backend.util.UrlValidationUtil;
import com.phishguardai.backend.util.exception.ExternalServiceException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UrlReputationService {
  private static final Set<String> SHORTENER_HOSTS =
      Set.of("bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "buff.ly", "rebrand.ly", "is.gd", "cutt.ly");

  private final SafeBrowsingClient safeBrowsingClient;
  private final UrlRiskModel urlRiskModel;
  private final HttpClient httpClient =
      HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).connectTimeout(Duration.ofSeconds(4)).build();

  public UrlReputationService(SafeBrowsingClient safeBrowsingClient, UrlRiskModel urlRiskModel) {
    this.safeBrowsingClient = safeBrowsingClient;
    this.urlRiskModel = urlRiskModel;
  }

  public List<UrlEvidenceDto> analyzeAll(List<String> rawUrls) {
    List<UrlEvidenceDto> results = new ArrayList<>();
    if (rawUrls == null) return results;
    Set<String> seen = new HashSet<>();
    for (String raw : rawUrls) {
      if (raw == null || raw.isBlank()) continue;
      if (!seen.add(raw)) continue;
      results.add(analyzeOne(raw));
    }
    return results;
  }

  public UrlEvidenceDto analyzeOne(String rawUrl) {
    UrlEvidenceDto dto = new UrlEvidenceDto();
    dto.setOriginalUrl(rawUrl);
    List<String> evidence = new ArrayList<>();

    String normalized;
    try {
      normalized = UrlValidationUtil.normalizeAndValidateHttpUrl(rawUrl);
      dto.setNormalizedUrl(normalized);
      dto.setValid(true);
    } catch (Exception e) {
      dto.setValid(false);
      dto.setRiskScore(20);
      dto.setStatusLabel("INVALID");
      evidence.add("URL format is invalid.");
      dto.setEvidence(evidence);
      return dto;
    }

    int risk = urlRiskModel.score(normalized);
    URI uri = URI.create(normalized);
    String host = safeLower(uri.getHost());

    if (isShortener(host)) {
      dto.setShortened(true);
      risk += 12;
      evidence.add("Uses a shortened URL host.");
    }

    if (isSpoofLike(host)) {
      dto.setSpoofed(true);
      risk += 15;
      evidence.add("Host shows spoofing or lookalike characteristics.");
    }

    SafeBrowsingResultDto sb = safeBrowsing(normalized);
    if (sb.isFlagged()) {
      dto.setSafeBrowsingFlagged(true);
      dto.setMalicious(true);
      risk = Math.max(risk, 92);
      evidence.add("Flagged by Google Safe Browsing.");
    }

    String finalUrl = resolveFinalUrl(normalized);
    if (finalUrl != null && !finalUrl.equals(normalized)) {
      dto.setRedirected(true);
      dto.setFinalUrl(finalUrl);
      risk += dto.isShortened() ? 10 : 5;
      evidence.add("Redirects to another destination.");
      try {
        String finalHost = safeLower(URI.create(finalUrl).getHost());
        if (finalHost != null && host != null && !finalHost.equals(host)) {
          evidence.add("Final destination host differs from the visible host.");
        }
        if (isSpoofLike(finalHost)) {
          dto.setSpoofed(true);
          risk += 10;
        }
        SafeBrowsingResultDto finalSb = safeBrowsing(finalUrl);
        if (finalSb.isFlagged()) {
          dto.setSafeBrowsingFlagged(true);
          dto.setMalicious(true);
          risk = Math.max(risk, 95);
          evidence.add("Final redirected URL is flagged by Google Safe Browsing.");
        }
      } catch (Exception ignored) {
      }
    } else {
      dto.setFinalUrl(normalized);
    }

    dto.setSuspicious(risk >= 45 || dto.isShortened() || dto.isSpoofed() || dto.isRedirected());
    dto.setMalicious(dto.isMalicious() || risk >= 85);
    dto.setRiskScore(ScoreUtil.clampScore(risk));
    dto.setStatusLabel(resolveStatus(dto));
    if (dto.getEvidence().isEmpty() && evidence.isEmpty()) {
      evidence.add("URL format is valid and no high-confidence threat source flagged it.");
    }
    dto.setEvidence(evidence);
    return dto;
  }

  private String resolveFinalUrl(String normalized) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(normalized))
              .timeout(Duration.ofSeconds(6))
              .header("User-Agent", "PhishGuardAI/1.0")
              .GET()
              .build();
      HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      return response.uri() == null ? normalized : response.uri().toString();
    } catch (Exception e) {
      return normalized;
    }
  }

  private SafeBrowsingResultDto safeBrowsing(String normalized) {
    try {
      return safeBrowsingClient.checkUrl(normalized);
    } catch (ExternalServiceException e) {
      return new SafeBrowsingResultDto(false, List.of());
    }
  }

  private boolean isShortener(String host) {
    return host != null && SHORTENER_HOSTS.contains(host);
  }

  private boolean isSpoofLike(String host) {
    if (host == null || host.isBlank()) return false;
    String lower = host.toLowerCase(Locale.ROOT);
    if (lower.contains("xn--")) return true;
    if (lower.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) return true;
    if (lower.chars().filter(ch -> ch == '-').count() >= 3) return true;
    return lower.contains("login")
        || lower.contains("verify")
        || lower.contains("secure")
        || lower.contains("account")
        || lower.contains("update");
  }

  private String resolveStatus(UrlEvidenceDto dto) {
    if (!dto.isValid()) return "INVALID";
    if (dto.isMalicious()) return "MALICIOUS";
    if (dto.isSpoofed()) return "SPOOFED";
    if (dto.isShortened()) return "SHORTENED";
    if (dto.isRedirected()) return "REDIRECTED";
    if (dto.isSuspicious()) return "SUSPICIOUS";
    return "VALID";
  }

  private String safeLower(String value) {
    return value == null ? null : value.toLowerCase(Locale.ROOT);
  }
}
