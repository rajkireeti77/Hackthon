package com.phishguardai.backend.service;

import com.phishguardai.backend.dto.RedFlagDto;
import com.phishguardai.backend.dto.SafeBrowsingResultDto;
import com.phishguardai.backend.dto.SandboxScanResultDto;
import com.phishguardai.backend.util.ScoreUtil;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class RiskScoringService {

  public int combineScore(
      int aiScore,
      String textContent,
      SafeBrowsingResultDto safeBrowsing,
      SandboxScanResultDto sandboxScan,
      List<RedFlagDto> aiRedFlags,
      int urlRiskScore,
      int textModelScore,
      int fileRiskScore) {
    int score = ScoreUtil.clampScore(aiScore);

    score += keywordHeuristics(textContent);

    if (textModelScore > 0) {
      int excess = Math.max(0, textModelScore - 25);
      score += Math.round(excess * 0.55f);
    }

    if (urlRiskScore > 0) {
      // Only let URL risk influence the outcome when it is meaningfully suspicious.
      int excess = Math.max(0, urlRiskScore - 30);
      score += Math.round(excess * 0.8f);
    }

    if (fileRiskScore > 0) {
      int excess = Math.max(0, fileRiskScore - 25);
      score += Math.round(excess * 0.85f);
    }

    if (safeBrowsing != null && safeBrowsing.isFlagged()) {
      score += 25;
      if (safeBrowsing.getThreatTypes() != null) {
        for (String t : safeBrowsing.getThreatTypes()) {
          if (t == null) continue;
          switch (t.toUpperCase(Locale.ROOT)) {
            case "SOCIAL_ENGINEERING" -> score += 10;
            case "MALWARE" -> score += 15;
            case "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION" -> score += 8;
            default -> score += 5;
          }
        }
      }
    }

    if (sandboxScan != null && sandboxScan.isReachable()) {
      int excess = Math.max(0, sandboxScan.getRiskScore() - 20);
      score += Math.round(excess * 0.85f);
      if (sandboxScan.isDownloadAttempted()) score += 12;
      if (sandboxScan.isLoginFormDetected()) score += 10;
      if (sandboxScan.getRedirectCount() >= 3) score += 6;
    }

    if (aiRedFlags != null) {
      for (RedFlagDto f : aiRedFlags) {
        if (f == null || f.getType() == null) continue;
        String type = f.getType().toLowerCase(Locale.ROOT);
        if (type.contains("credential") || type.contains("otp") || type.contains("password")) score += 10;
        if (type.contains("impersonation") || type.contains("authority")) score += 8;
        if (type.contains("urgency") || type.contains("urgent") || type.contains("immediately")) score += 6;
        if (type.contains("suspicious url") || type.contains("link")) score += 10;
      }
    }

    return ScoreUtil.clampScore(score);
  }

  private int keywordHeuristics(String text) {
    if (text == null || text.isBlank()) return 0;
    String t = text.toLowerCase(Locale.ROOT);
    int add = 0;

    if (containsAny(t, "urgent", "immediately", "act now", "within 24 hours", "final notice", "limited time")) add += 8;
    if (containsAny(t, "account will be blocked", "suspended", "locked", "verify now", "security alert")) add += 10;
    if (containsAny(t, "password", "otp", "opt", "one-time", "one time password", "pin", "cvv", "bank login", "ssn", "social security")) add += 14;
    if (containsAny(t, "click here", "tap here", "login here", "open the link", "download")) add += 6;
    if (containsAny(t, "confirm your identity", "update your information", "payment failed", "refund")) add += 6;
    if (containsAny(t, "bank", "bank account", "debit card", "credit card", "upi", "net banking", "kyc")) add += 10;
    if (containsAny(t, "won", "winner", "congratulations", "prize", "lottery", "reward", "gift", "cashback", "free iphone")) add += 12;

    if (containsAny(t, "otp", "opt", "one time password", "pin") && containsAny(t, "share", "tell", "send", "give", "forward")) {
      add += 28;
    }
    if (containsAny(t, "bank", "account", "card", "upi", "kyc")
        && containsAny(t, "verify", "update", "unlock", "blocked", "suspended", "login", "confirm")) {
      add += 22;
    }
    if (containsAny(t, "won", "winner", "prize", "lottery", "gift", "reward")
        && containsAny(t, "claim", "click", "tap", "pay fee", "processing fee", "send")) {
      add += 20;
    }

    return add;
  }

  private boolean containsAny(String haystack, String... needles) {
    for (String n : needles) {
      if (haystack.contains(n)) return true;
    }
    return false;
  }
}

