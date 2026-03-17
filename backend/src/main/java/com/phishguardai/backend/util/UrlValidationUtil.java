package com.phishguardai.backend.util;

import java.net.IDN;
import java.net.URI;

public final class UrlValidationUtil {
  private UrlValidationUtil() {}

  public static String normalizeAndValidateHttpUrl(String input) {
    if (input == null) return null;
    String trimmed = input.trim();
    if (trimmed.isBlank()) return null;

    URI uri;
    try {
      uri = URI.create(trimmed);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid URL format.");
    }

    String scheme = uri.getScheme();
    if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
      throw new IllegalArgumentException("URL must start with http:// or https://");
    }

    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("URL host is missing.");
    }

    try {
      IDN.toASCII(host);
    } catch (Exception e) {
      throw new IllegalArgumentException("URL host is invalid.");
    }

    return uri.toString();
  }
}

