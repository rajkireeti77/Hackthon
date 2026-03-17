package com.phishguardai.backend.util;

import com.phishguardai.backend.dto.Severity;
import com.phishguardai.backend.dto.Verdict;

public final class ScoreUtil {
  private ScoreUtil() {}

  public static int clampScore(int score) {
    if (score < 0) return 0;
    return Math.min(score, 100);
  }

  public static Severity severityForScore(int score) {
    int s = clampScore(score);
    if (s <= 24) return Severity.Low;
    if (s <= 49) return Severity.Medium;
    if (s <= 74) return Severity.High;
    return Severity.Critical;
  }

  public static Verdict verdictForScore(int score) {
    int s = clampScore(score);
    if (s <= 19) return Verdict.Safe;
    if (s <= 39) return Verdict.LowRisk;
    if (s <= 69) return Verdict.Suspicious;
    return Verdict.Malicious;
  }
}

