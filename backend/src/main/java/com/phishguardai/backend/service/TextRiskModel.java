package com.phishguardai.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phishguardai.backend.config.TextModelProperties;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TextRiskModel {
  private static final Logger log = LoggerFactory.getLogger(TextRiskModel.class);
  private static final Pattern TOKEN_RE = Pattern.compile("[a-zA-Z0-9]+");

  private final ObjectMapper mapper;
  private final TextModelProperties props;
  private volatile Weights weights = Weights.defaults();

  public TextRiskModel(ObjectMapper mapper, TextModelProperties props) {
    this.mapper = mapper;
    this.props = props;
    loadWeightsIfPresent();
  }

  public int score(String text) {
    if (text == null || text.isBlank()) return 0;
    String t = text.toLowerCase(Locale.ROOT);
    float[] x = featurize(t, weights.nFeatures);
    double z = weights.bias;
    for (int i = 0; i < x.length; i++) {
      z += weights.w[i] * x[i];
    }
    double p = sigmoid(z);
    int out = (int) Math.round(p * 100.0);
    if (out < 0) return 0;
    if (out > 100) return 100;
    return out;
  }

  private void loadWeightsIfPresent() {
    try {
      String path = (props.getWeightsPath() == null) ? "" : props.getWeightsPath().trim();
      if (path.isBlank()) return;
      File file = new File(path);
      if (!file.exists() || !file.isFile()) {
        log.warn("Text model weights not found at path={}", path);
        return;
      }
      WeightsJson wj = mapper.readValue(file, WeightsJson.class);
      if (wj == null || wj.weights == null || wj.weights.length == 0) return;
      this.weights = Weights.fromJson(wj);
      log.info("Loaded text model weights from path={} nFeatures={}", path, this.weights.nFeatures);
    } catch (Exception e) {
      log.warn("Failed to load text model weights; using defaults. err={}", e.toString());
    }
  }

  private static float[] featurize(String textLower, int nFeatures) {
    float[] v = new float[nFeatures];
    var m = TOKEN_RE.matcher(textLower);
    while (m.find()) {
      String tok = m.group();
      int idx = (int) (fnv1a32(tok) % (long) nFeatures);
      v[idx] += 1.0f;
    }
    // L2 normalize
    double norm = 0.0;
    for (float f : v) norm += (double) f * (double) f;
    norm = Math.sqrt(norm);
    if (norm > 0.0) {
      float inv = (float) (1.0 / norm);
      for (int i = 0; i < v.length; i++) v[i] *= inv;
    }
    return v;
  }

  private static long fnv1a32(String s) {
    long h = 2166136261L;
    byte[] b = s.getBytes(StandardCharsets.UTF_8);
    for (byte value : b) {
      h ^= (value & 0xffL);
      h = (h * 16777619L) & 0xffffffffL;
    }
    return h & 0xffffffffL;
  }

  private static double sigmoid(double z) {
    if (z >= 0) {
      double ez = Math.exp(-z);
      return 1.0 / (1.0 + ez);
    }
    double ez = Math.exp(z);
    return ez / (1.0 + ez);
  }

  private static final class Weights {
    final double bias;
    final int nFeatures;
    final float[] w;

    private Weights(double bias, int nFeatures, float[] w) {
      this.bias = bias;
      this.nFeatures = nFeatures;
      this.w = w;
    }

    static Weights fromJson(WeightsJson j) {
      float[] w = new float[j.weights.length];
      for (int i = 0; i < j.weights.length; i++) w[i] = (float) j.weights[i];
      return new Weights(j.bias, j.nFeatures, w);
    }

    static Weights defaults() {
      // Fallback: no ML weights; keep near-neutral.
      return new Weights(-2.0, 1024, new float[1024]);
    }
  }

  // JSON shape produced by backend/ml/train_text_model.py
  private static final class WeightsJson {
    public double bias;
    public int nFeatures;
    public double[] weights;
  }
}

