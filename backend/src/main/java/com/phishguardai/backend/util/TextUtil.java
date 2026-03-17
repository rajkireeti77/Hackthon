package com.phishguardai.backend.util;

public final class TextUtil {
  private TextUtil() {}

  public static String nullIfBlank(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isBlank() ? null : t;
  }

  public static String truncate(String s, int maxLen) {
    if (s == null) return null;
    if (s.length() <= maxLen) return s;
    return s.substring(0, maxLen);
  }
}

