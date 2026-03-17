package com.phishguardai.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phishguardai.backend.config.FileModelProperties;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FileRiskModel {
  private static final Logger log = LoggerFactory.getLogger(FileRiskModel.class);

  private static final Set<String> EXECUTABLE_EXTENSIONS =
      Set.of("exe", "dll", "scr", "bat", "cmd", "ps1", "js", "vbs", "vbe", "hta", "jar", "apk", "msi", "com", "reg", "cpl", "lnk");
  private static final Set<String> ARCHIVE_EXTENSIONS = Set.of("zip", "rar", "7z", "iso", "img");
  private static final Set<String> MACRO_EXTENSIONS = Set.of("docm", "xlsm", "pptm");
  private static final Set<String> BENIGN_EXTENSIONS =
      Set.of("pdf", "png", "jpg", "jpeg", "gif", "txt", "csv", "docx", "xlsx", "pptx", "mp3", "mp4", "json", "html", "py", "java");
  private static final Set<String> SCRIPT_CONTENT_TYPES =
      Set.of("application/javascript", "text/javascript", "text/plain", "application/x-bat", "application/hta");
  private static final Set<String> ARCHIVE_CONTENT_TYPES =
      Set.of("application/zip", "application/x-zip-compressed", "application/x-iso9660-image", "application/x-rar-compressed");
  private static final String[] SUSPICIOUS_STRINGS =
      new String[] {"powershell", "wscript.shell", "cmd.exe", "createobject", "invoke-webrequest", "downloadstring", "rundll32"};

  private final ObjectMapper mapper;
  private final FileModelProperties props;
  private volatile Weights weights = Weights.defaults();

  public FileRiskModel(ObjectMapper mapper, FileModelProperties props) {
    this.mapper = mapper;
    this.props = props;
    loadWeightsIfPresent();
  }

  public int score(String fileName, String contentType, Long fileSizeBytes, String fileSha256, byte[] sampleBytes) {
    Features f = Features.from(fileName, contentType, fileSizeBytes, sampleBytes);
    double z =
        weights.bias
            + weights.wExecutableExt * (f.executableExt ? 1 : 0)
            + weights.wArchiveExt * (f.archiveExt ? 1 : 0)
            + weights.wMacroExt * (f.macroExt ? 1 : 0)
            + weights.wBenignExt * (f.benignExt ? 1 : 0)
            + weights.wDoubleExtension * (f.doubleExtension ? 1 : 0)
            + weights.wScriptContentType * (f.scriptContentType ? 1 : 0)
            + weights.wArchiveContentType * (f.archiveContentType ? 1 : 0)
            + weights.wEmptyContentType * (f.emptyContentType ? 1 : 0)
            + weights.wSize * f.size;

    int out = (int) Math.round(sigmoid(z) * 100.0);
    if (f.executableExt) out = Math.max(out, 82);
    if (f.macroExt) out = Math.max(out, 70);
    if (f.doubleExtension && (f.executableExt || f.macroExt)) out = Math.max(out, 92);
    if (f.peHeader || f.elfHeader) out = Math.max(out, 95);
    if (f.scriptMarkerHits > 0 && (f.executableExt || f.scriptContentType)) out = Math.max(out, 88);
    if (f.archiveExt && f.doubleExtension) out = Math.max(out, 78);
    if (fileSha256 != null && fileSha256.length() == 64 && f.executableExt) out = Math.max(out, 84);
    if (out < 0) return 0;
    if (out > 100) return 100;
    return out;
  }

  public String explain(String fileName, String contentType, Long fileSizeBytes, String fileSha256, byte[] sampleBytes) {
    Features f = Features.from(fileName, contentType, fileSizeBytes, sampleBytes);
    StringBuilder sb = new StringBuilder();
    if (f.doubleExtension) sb.append("double extension; ");
    if (f.executableExt) sb.append("executable or script extension; ");
    if (f.macroExt) sb.append("macro-enabled document; ");
    if (f.archiveExt) sb.append("archive/image delivery format; ");
    if (f.scriptContentType) sb.append("script-like content type; ");
    if (f.peHeader) sb.append("PE header detected; ");
    if (f.elfHeader) sb.append("ELF header detected; ");
    if (f.scriptMarkerHits > 0) sb.append("suspicious script markers; ");
    if (sb.length() == 0 && fileSha256 != null && !fileSha256.isBlank()) sb.append("static file metadata analyzed; ");
    if (sb.length() == 0) sb.append("no strong file red flags");
    return sb.toString().trim();
  }

  public String describeForPrompt(String fileName, String contentType, Long fileSizeBytes, String fileSha256, byte[] sampleBytes) {
    Features f = Features.from(fileName, contentType, fileSizeBytes, sampleBytes);
    return "fileName="
        + safeValue(fileName)
        + ", contentType="
        + safeValue(contentType)
        + ", sizeBytes="
        + (fileSizeBytes == null ? "null" : fileSizeBytes)
        + ", sha256Present="
        + (fileSha256 != null && !fileSha256.isBlank())
        + ", doubleExtension="
        + f.doubleExtension
        + ", executableExtension="
        + f.executableExt
        + ", macroExtension="
        + f.macroExt
        + ", archiveExtension="
        + f.archiveExt
        + ", sampleHasMz="
        + f.peHeader
        + ", sampleHasElf="
        + f.elfHeader
        + ", suspiciousScriptMarkers="
        + f.scriptMarkerHits;
  }

  private void loadWeightsIfPresent() {
    try {
      String path = props.getWeightsPath() == null ? "" : props.getWeightsPath().trim();
      if (path.isBlank()) return;
      File file = new File(path);
      if (!file.exists() || !file.isFile()) {
        log.warn("File model weights not found at path={}", path);
        return;
      }
      Weights w = mapper.readValue(file, Weights.class);
      if (w == null) return;
      this.weights = w;
      log.info("Loaded file model weights from path={}", path);
    } catch (Exception e) {
      log.warn("Failed to load file model weights; using defaults. err={}", e.toString());
    }
  }

  private static String safeValue(String value) {
    return value == null || value.isBlank() ? "null" : value;
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
    final boolean executableExt;
    final boolean archiveExt;
    final boolean macroExt;
    final boolean benignExt;
    final boolean doubleExtension;
    final boolean scriptContentType;
    final boolean archiveContentType;
    final boolean emptyContentType;
    final double size;
    final boolean peHeader;
    final boolean elfHeader;
    final int scriptMarkerHits;

    private Features(
        boolean executableExt,
        boolean archiveExt,
        boolean macroExt,
        boolean benignExt,
        boolean doubleExtension,
        boolean scriptContentType,
        boolean archiveContentType,
        boolean emptyContentType,
        double size,
        boolean peHeader,
        boolean elfHeader,
        int scriptMarkerHits) {
      this.executableExt = executableExt;
      this.archiveExt = archiveExt;
      this.macroExt = macroExt;
      this.benignExt = benignExt;
      this.doubleExtension = doubleExtension;
      this.scriptContentType = scriptContentType;
      this.archiveContentType = archiveContentType;
      this.emptyContentType = emptyContentType;
      this.size = size;
      this.peHeader = peHeader;
      this.elfHeader = elfHeader;
      this.scriptMarkerHits = scriptMarkerHits;
    }

    static Features from(String fileName, String contentType, Long fileSizeBytes, byte[] sampleBytes) {
      String normalizedName = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
      String normalizedType = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
      String ext = extensionOf(normalizedName);
      byte[] sample = sampleBytes == null ? new byte[0] : sampleBytes;
      return new Features(
          EXECUTABLE_EXTENSIONS.contains(ext),
          ARCHIVE_EXTENSIONS.contains(ext),
          MACRO_EXTENSIONS.contains(ext),
          BENIGN_EXTENSIONS.contains(ext),
          hasDoubleExtension(normalizedName),
          SCRIPT_CONTENT_TYPES.contains(normalizedType),
          ARCHIVE_CONTENT_TYPES.contains(normalizedType),
          normalizedType.isBlank(),
          normalizedSize(fileSizeBytes),
          hasPeHeader(sample),
          hasElfHeader(sample),
          countScriptMarkers(sample));
    }

    private static String extensionOf(String fileName) {
      if (fileName == null || fileName.isBlank()) return "";
      int dot = fileName.lastIndexOf('.');
      if (dot < 0 || dot == fileName.length() - 1) return "";
      return fileName.substring(dot + 1);
    }

    private static boolean hasDoubleExtension(String fileName) {
      if (fileName == null || fileName.isBlank()) return false;
      String[] parts = fileName.split("\\.");
      int count = 0;
      for (String part : parts) {
        if (!part.isBlank()) count++;
      }
      return count >= 3;
    }

    private static double normalizedSize(Long fileSizeBytes) {
      long safe = Math.max(fileSizeBytes == null ? 0L : fileSizeBytes, 0L);
      return Math.min(Math.log10(safe + 1.0) / 9.0, 1.0);
    }

    private static boolean hasPeHeader(byte[] sample) {
      return sample.length >= 2 && sample[0] == 'M' && sample[1] == 'Z';
    }

    private static boolean hasElfHeader(byte[] sample) {
      return sample.length >= 4 && sample[0] == 0x7f && sample[1] == 'E' && sample[2] == 'L' && sample[3] == 'F';
    }

    private static int countScriptMarkers(byte[] sample) {
      if (sample.length == 0) return 0;
      String text = new String(sample, StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
      int hits = 0;
      for (String marker : SUSPICIOUS_STRINGS) {
        if (text.contains(marker)) hits++;
      }
      return hits;
    }
  }

  public static final class Weights {
    public double bias;
    public double wExecutableExt;
    public double wArchiveExt;
    public double wMacroExt;
    public double wBenignExt;
    public double wDoubleExtension;
    public double wScriptContentType;
    public double wArchiveContentType;
    public double wEmptyContentType;
    public double wSize;

    public Weights() {}

    static Weights defaults() {
      Weights w = new Weights();
      w.bias = -1.8;
      w.wExecutableExt = 3.0;
      w.wArchiveExt = 1.2;
      w.wMacroExt = 2.2;
      w.wBenignExt = -2.4;
      w.wDoubleExtension = 2.6;
      w.wScriptContentType = 1.4;
      w.wArchiveContentType = 0.8;
      w.wEmptyContentType = 0.2;
      w.wSize = 0.5;
      return w;
    }

    public Map<String, Double> asMap() {
      return Map.ofEntries(
          Map.entry("bias", bias),
          Map.entry("wExecutableExt", wExecutableExt),
          Map.entry("wArchiveExt", wArchiveExt),
          Map.entry("wMacroExt", wMacroExt),
          Map.entry("wBenignExt", wBenignExt),
          Map.entry("wDoubleExtension", wDoubleExtension),
          Map.entry("wScriptContentType", wScriptContentType),
          Map.entry("wArchiveContentType", wArchiveContentType),
          Map.entry("wEmptyContentType", wEmptyContentType),
          Map.entry("wSize", wSize));
    }
  }
}
