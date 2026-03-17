package com.phishguardai.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phishguardai.backend.config.UrlModelProperties;
import java.io.File;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UrlRiskModel {
  private static final Logger log = LoggerFactory.getLogger(UrlRiskModel.class);

  private static final Set<String> RISKY_TLDS =
      Set.of(
          "zip", "mov", "click", "top", "xyz", "gq", "tk", "ml", "cf", "work", "life", "live", "rest",
          "country", "stream", "download", "kim", "men", "quest", "link");

  private static final String[] SUSPICIOUS_KEYWORDS =
      new String[] {"login", "verify", "secure", "update", "account", "bank", "confirm", "password", "otp"};

  private final ObjectMapper mapper;
  private final UrlModelProperties props;

  // weights for a tiny logistic regression: score = sigmoid(b + sum(w_i * x_i)) * 100
  private volatile Weights weights = Weights.defaults();

  public UrlRiskModel(ObjectMapper mapper, UrlModelProperties props) {
    this.mapper = mapper;
    this.props = props;
    loadWeightsIfPresent();
  }

  public int score(String url) {
    if (url == null || url.isBlank()) return 0;
    Features f = Features.fromUrl(url);
    double z =
        weights.bias
            + weights.wLength * f.length
            + weights.wDots * f.dots
            + weights.wHyphens * f.hyphens
            + weights.wHasAt * (f.hasAt ? 1 : 0)
            + weights.wHasIpHost * (f.hasIpHost ? 1 : 0)
            + weights.wHasPunycode * (f.hasPunycode ? 1 : 0)
            + weights.wDigitsInHost * f.digitsInHostRatio
            + weights.wRiskyTld * (f.riskyTld ? 1 : 0)
            + weights.wKeywordHits * f.keywordHits
            + weights.wHttps * (f.https ? 1 : 0);

    double p = sigmoid(z);
    int out = (int) Math.round(p * 100.0);

    // Safety overrides for patterns that are strongly correlated with phishing but may be rare in training data.
    if (f.hasAt) out = Math.max(out, 80);
    if (f.hasIpHost) out = Math.max(out, 85);
    if (f.hasPunycode) out = Math.max(out, 95);
    if (!f.https && (f.keywordHits >= 0.34 || f.riskyTld)) out = Math.max(out, 60);

    if (out < 0) return 0;
    if (out > 100) return 100;
    return out;
  }

  public String explain(String url) {
    Features f = Features.fromUrl(url);
    StringBuilder sb = new StringBuilder();
    if (f.hasIpHost) sb.append("IP host; ");
    if (f.hasPunycode) sb.append("punycode; ");
    if (f.hasAt) sb.append("@ in URL; ");
    if (f.riskyTld) sb.append("risky TLD; ");
    if (f.keywordHits > 0) sb.append("suspicious keywords; ");
    if (!f.https) sb.append("not https; ");
    if (sb.length() == 0) sb.append("no strong URL red flags");
    return sb.toString().trim();
  }

  private void loadWeightsIfPresent() {
    try {
      String path = (props.getWeightsPath() == null) ? "" : props.getWeightsPath().trim();
      if (path.isBlank()) return;

      File file = new File(path);
      if (!file.exists() || !file.isFile()) {
        log.warn("URL model weights not found at path={}", path);
        return;
      }

      Weights w = mapper.readValue(file, Weights.class);
      if (w == null) return;
      this.weights = w;
      log.info("Loaded URL model weights from path={}", path);
    } catch (Exception e) {
      log.warn("Failed to load URL model weights; using defaults. err={}", e.toString());
    }
  }

  private static double sigmoid(double z) {
    if (z >= 0) {
      double ez = Math.exp(-z);
      return 1.0 / (1.0 + ez);
    }
    double ez = Math.exp(z);
    return ez / (1.0 + ez);
  }

  private static final class Features {
    final double length;
    final double dots;
    final double hyphens;
    final boolean hasAt;
    final boolean hasIpHost;
    final boolean hasPunycode;
    final double digitsInHostRatio;
    final boolean riskyTld;
    final double keywordHits;
    final boolean https;

    private Features(
        double length,
        double dots,
        double hyphens,
        boolean hasAt,
        boolean hasIpHost,
        boolean hasPunycode,
        double digitsInHostRatio,
        boolean riskyTld,
        double keywordHits,
        boolean https) {
      this.length = length;
      this.dots = dots;
      this.hyphens = hyphens;
      this.hasAt = hasAt;
      this.hasIpHost = hasIpHost;
      this.hasPunycode = hasPunycode;
      this.digitsInHostRatio = digitsInHostRatio;
      this.riskyTld = riskyTld;
      this.keywordHits = keywordHits;
      this.https = https;
    }

    static Features fromUrl(String url) {
      String u = url.trim();
      URI uri;
      try {
        uri = URI.create(u);
      } catch (Exception e) {
        // treat unparseable as risky-ish
        return new Features(u.length(), 0, 0, u.contains("@"), false, u.contains("xn--"), 0.0, false, 0.0, false);
      }

      String host = uri.getHost();
      if (host == null) host = "";
      String hostLower = host.toLowerCase(Locale.ROOT);
      String fullLower = u.toLowerCase(Locale.ROOT);

      boolean https = "https".equalsIgnoreCase(uri.getScheme());
      boolean hasAt = u.contains("@");
      boolean hasPunycode = hostLower.contains("xn--");
      boolean hasIpHost = looksLikeIpv4(hostLower);

      int dotCount = countChar(hostLower, '.');
      int hyphenCount = countChar(hostLower, '-');
      double digitsRatio = ratioDigits(hostLower);
      boolean riskyTld = isRiskyTld(hostLower);
      double keywordHits = countKeywordHits(fullLower);

      // Normalize scales a bit so weights are stable
      double length = Math.min(u.length(), 200) / 200.0; // 0..1
      double dots = Math.min(dotCount, 10) / 10.0; // 0..1
      double hyphens = Math.min(hyphenCount, 10) / 10.0; // 0..1
      double keywords = Math.min(keywordHits, 6) / 6.0; // 0..1

      return new Features(length, dots, hyphens, hasAt, hasIpHost, hasPunycode, digitsRatio, riskyTld, keywords, https);
    }

    private static int countChar(String s, char c) {
      int n = 0;
      for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
      return n;
    }

    private static double ratioDigits(String s) {
      if (s.isBlank()) return 0.0;
      int digits = 0;
      for (int i = 0; i < s.length(); i++) {
        if (Character.isDigit(s.charAt(i))) digits++;
      }
      return (double) digits / (double) s.length();
    }

    private static boolean looksLikeIpv4(String host) {
      // simple + fast: a.b.c.d where each is 0-255
      String[] parts = host.split("\\.");
      if (parts.length != 4) return false;
      for (String p : parts) {
        if (p.isEmpty() || p.length() > 3) return false;
        for (int i = 0; i < p.length(); i++) if (!Character.isDigit(p.charAt(i))) return false;
        int v = Integer.parseInt(p);
        if (v < 0 || v > 255) return false;
      }
      return true;
    }

    private static boolean isRiskyTld(String hostLower) {
      int lastDot = hostLower.lastIndexOf('.');
      if (lastDot < 0 || lastDot == hostLower.length() - 1) return false;
      String tld = hostLower.substring(lastDot + 1);
      return RISKY_TLDS.contains(tld);
    }

    private static int countKeywordHits(String urlLower) {
      int hits = 0;
      for (String k : SUSPICIOUS_KEYWORDS) {
        if (urlLower.contains(k)) hits++;
      }
      return hits;
    }
  }

  public static final class Weights {
    public double bias;
    public double wLength;
    public double wDots;
    public double wHyphens;
    public double wHasAt;
    public double wHasIpHost;
    public double wHasPunycode;
    public double wDigitsInHost;
    public double wRiskyTld;
    public double wKeywordHits;
    public double wHttps;

    public Weights() {}

    static Weights defaults() {
      // Calibrated to be conservative: only strong URL signals push very high.
      Weights w = new Weights();
      w.bias = -1.25;
      w.wLength = 1.2;
      w.wDots = 0.7;
      w.wHyphens = 0.9;
      w.wHasAt = 2.2;
      w.wHasIpHost = 2.4;
      w.wHasPunycode = 2.0;
      w.wDigitsInHost = 1.6;
      w.wRiskyTld = 1.6;
      w.wKeywordHits = 1.4;
      w.wHttps = -0.6;
      return w;
    }

    public Map<String, Double> asMap() {
      return Map.ofEntries(
          Map.entry("bias", bias),
          Map.entry("wLength", wLength),
          Map.entry("wDots", wDots),
          Map.entry("wHyphens", wHyphens),
          Map.entry("wHasAt", wHasAt),
          Map.entry("wHasIpHost", wHasIpHost),
          Map.entry("wHasPunycode", wHasPunycode),
          Map.entry("wDigitsInHost", wDigitsInHost),
          Map.entry("wRiskyTld", wRiskyTld),
          Map.entry("wKeywordHits", wKeywordHits),
          Map.entry("wHttps", wHttps));
    }
  }
}

