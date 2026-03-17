package com.phishguardai.backend.service;

import com.phishguardai.backend.util.TextUtil;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class UrlExtractionService {
  private static final Pattern URL_PATTERN = Pattern.compile("(https?://[^\\s\"'<>]+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern HREF_PATTERN =
      Pattern.compile("href\\s*=\\s*[\"'](https?://[^\"'>\\s]+)[\"']", Pattern.CASE_INSENSITIVE);

  public List<String> extractAll(String textContent, String htmlContent, String explicitUrl) {
    Set<String> urls = new LinkedHashSet<>();
    addCandidate(urls, explicitUrl);
    collect(urls, textContent, URL_PATTERN);
    collect(urls, htmlContent, URL_PATTERN);
    collect(urls, htmlContent, HREF_PATTERN);
    return new ArrayList<>(urls);
  }

  private void collect(Set<String> urls, String source, Pattern pattern) {
    String text = TextUtil.nullIfBlank(source);
    if (text == null) return;
    Matcher matcher = pattern.matcher(text);
    while (matcher.find()) {
      addCandidate(urls, matcher.group(1));
    }
  }

  private void addCandidate(Set<String> urls, String raw) {
    String cleaned = cleanCandidate(raw);
    if (cleaned != null) {
      urls.add(cleaned);
    }
  }

  private String cleanCandidate(String raw) {
    String value = TextUtil.nullIfBlank(raw);
    if (value == null) return null;
    String cleaned = value.replace("&amp;", "&").trim();
    while (!cleaned.isEmpty() && ",.;)]}>".indexOf(cleaned.charAt(cleaned.length() - 1)) >= 0) {
      cleaned = cleaned.substring(0, cleaned.length() - 1);
    }
    return TextUtil.nullIfBlank(cleaned);
  }
}
