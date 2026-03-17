package com.phishguardai.backend.util;

import jakarta.servlet.http.HttpServletRequest;

public final class BearerTokenUtil {
  private BearerTokenUtil() {}

  public static String extractBearerToken(HttpServletRequest request) {
    var header = request.getHeader("Authorization");
    if (header == null) return null;
    if (!header.startsWith("Bearer ")) return null;
    var token = header.substring("Bearer ".length()).trim();
    return token.isBlank() ? null : token;
  }
}

