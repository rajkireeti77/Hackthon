package com.phishguardai.backend.service;

import com.phishguardai.backend.config.WindowsNotificationsProperties;
import com.phishguardai.backend.dto.BackgroundIngestRequest;
import com.phishguardai.backend.util.TextUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Service
public class WindowsNotificationIngestService {
  private static final Logger log = LoggerFactory.getLogger(WindowsNotificationIngestService.class);

  private final WindowsNotificationsProperties props;
  private final IncomingMessageService incomingMessageService;
  private final ObjectMapper objectMapper;
  private volatile long lastSeenOrder = 0;
  private final Set<String> seenToastIds = new HashSet<>();

  public WindowsNotificationIngestService(
      WindowsNotificationsProperties props, IncomingMessageService incomingMessageService, ObjectMapper objectMapper) {
    this.props = props;
    this.incomingMessageService = incomingMessageService;
    this.objectMapper = objectMapper;
  }

  @Scheduled(fixedDelayString = "${app.incoming.windows-notifications.poll-interval-ms:5000}")
  public void pollWindowsNotifications() {
    if (!props.isEnabled()) return;
    pollActiveToasts();
    Path dbPath = resolveDbPath();
    if (dbPath == null || !Files.exists(dbPath)) return;

    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toString())) {
      long latest = seedLastSeenOrder(connection);
      try (PreparedStatement statement =
          connection.prepareStatement(
              """
              SELECT n.[Order], n.Id, n.Type, n.PayloadType, CAST(n.Payload AS TEXT) AS PayloadXml, n.ArrivalTime,
                     h.PrimaryId
              FROM Notification n
              JOIN NotificationHandler h ON h.RecordId = n.HandlerId
              WHERE n.[Order] > ?
              ORDER BY n.[Order] ASC
              """)) {
        statement.setLong(1, latest);
        try (ResultSet rs = statement.executeQuery()) {
          long maxSeen = latest;
          while (rs.next()) {
            long order = rs.getLong("Order");
            maxSeen = Math.max(maxSeen, order);
            processRow(rs);
          }
          lastSeenOrder = Math.max(lastSeenOrder, maxSeen);
        }
      }
    } catch (Exception e) {
      log.warn("Windows notification polling failed: {}", e.toString());
    }
  }

  private void pollActiveToasts() {
    Path scriptPath = Path.of("scripts", "read_windows_toasts.ps1").toAbsolutePath();
    if (!Files.exists(scriptPath)) {
      scriptPath = Path.of("backend", "scripts", "read_windows_toasts.ps1").toAbsolutePath();
    }
    if (!Files.exists(scriptPath)) return;
    ProcessBuilder builder =
        new ProcessBuilder(
            "powershell",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            scriptPath.toString());
    builder.redirectErrorStream(true);
    try {
      Process process = builder.start();
      String output;
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        output = reader.lines().reduce("", (a, b) -> a.isEmpty() ? b : a + b);
      }
      int exit = process.waitFor();
      if (exit != 0 || TextUtil.nullIfBlank(output) == null || !output.trim().startsWith("[")) return;
      List<ToastSnapshot> toasts = objectMapper.readValue(output, new TypeReference<List<ToastSnapshot>>() {});
      for (ToastSnapshot toast : toasts) {
        if (toast == null || TextUtil.nullIfBlank(toast.id) == null || seenToastIds.contains(toast.id)) continue;
        if (!isAllowedToastApp(toast.appDisplayName, toast.appId)) continue;
        if (toast.texts == null || toast.texts.isEmpty()) continue;
        if (looksLikePhishGuardNotification(toast.texts)) continue;

        BackgroundIngestRequest request = new BackgroundIngestRequest();
        request.setSourceType(resolveSourceType(safeString(toast.appDisplayName, toast.appId)));
        request.setSourceMessageId("toast-" + toast.id);
        request.setSenderLabel(TextUtil.truncate(TextUtil.nullIfBlank(toast.texts.get(0)), 180));
        request.setSenderAddress(TextUtil.truncate(safeString(toast.appDisplayName, toast.appId), 255));
        request.setTextContent(TextUtil.truncate(buildToastBody(toast.texts), 10000));
        incomingMessageService.ingestAndCreateAlert(props.getUserId(), request);
        seenToastIds.add(toast.id);
      }
    } catch (Exception e) {
      log.debug("Active Windows toast polling failed: {}", e.toString());
    }
  }

  private void processRow(ResultSet rs) throws Exception {
    String appId = rs.getString("PrimaryId");
    String type = safeLower(rs.getString("Type"));
    if (!isAllowedApp(appId) || isIgnoredPayloadType(type)) return;

    String payloadXml = TextUtil.nullIfBlank(rs.getString("PayloadXml"));
    if (payloadXml == null || !payloadXml.contains("<toast")) return;

    List<String> texts = extractTextElements(payloadXml);
    if (texts.isEmpty()) return;
    if (looksLikePhishGuardNotification(texts)) return;

    String sender = TextUtil.truncate(TextUtil.nullIfBlank(texts.get(0)), 180);
    String body = texts.size() > 1 ? TextUtil.truncate(TextUtil.nullIfBlank(String.join(" ", texts.subList(1, texts.size()))), 10000) : null;
    String combinedText = body == null ? sender : body;
    if (TextUtil.nullIfBlank(combinedText) == null) return;

    BackgroundIngestRequest request = new BackgroundIngestRequest();
    request.setSourceType(resolveSourceType(appId));
    request.setSourceMessageId("win-" + rs.getLong("Id"));
    request.setSenderLabel(sender == null ? displayAppName(appId) : sender);
    request.setSenderAddress(displayAppName(appId));
    request.setTextContent(combinedText);
    request.setReceivedAt(fromWindowsFileTime(rs.getLong("ArrivalTime")));

    incomingMessageService.ingestAndCreateAlert(props.getUserId(), request);
  }

  private long seedLastSeenOrder(Connection connection) throws Exception {
    if (lastSeenOrder > 0) return lastSeenOrder;
    try (PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(MAX([Order]), 0) FROM Notification");
        ResultSet rs = statement.executeQuery()) {
      if (rs.next()) {
        lastSeenOrder = rs.getLong(1);
      }
    }
    return lastSeenOrder;
  }

  private Path resolveDbPath() {
    String configured = TextUtil.nullIfBlank(props.getDbPath());
    if (configured != null) return Path.of(configured);
    String localAppData = System.getenv("LOCALAPPDATA");
    if (TextUtil.nullIfBlank(localAppData) == null) return null;
    return Path.of(localAppData, "Microsoft", "Windows", "Notifications", "wpndatabase.db");
  }

  private boolean isAllowedApp(String appId) {
    String value = safeLower(appId);
    if (value == null) return false;
    if (props.getAllowedApps() == null || props.getAllowedApps().isEmpty()) return true;
    return props.getAllowedApps().stream()
        .map(this::safeLower)
        .filter(item -> item != null)
        .anyMatch(item -> value.contains(item) || item.contains(value) || relaxedAppMatch(value, item));
  }

  private boolean isAllowedToastApp(String appDisplayName, String appId) {
    if (props.getAllowedApps() == null || props.getAllowedApps().isEmpty()) return true;
    String display = safeLower(appDisplayName);
    String id = safeLower(appId);
    for (String allowed : props.getAllowedApps()) {
      String expected = safeLower(allowed);
      if (expected == null) continue;
      if (display != null
          && (display.contains(expected) || expected.contains(display) || relaxedAppMatch(display, expected))) {
        return true;
      }
      if (id != null && (id.contains(expected) || expected.contains(id) || relaxedAppMatch(id, expected))) {
        return true;
      }
    }
    return false;
  }

  private boolean isIgnoredPayloadType(String type) {
    if (type == null) return false;
    return props.getIgnoredPayloadTypes() != null
        && props.getIgnoredPayloadTypes().stream().anyMatch(item -> type.equalsIgnoreCase(item));
  }

  private List<String> extractTextElements(String payloadXml) {
    List<String> values = new ArrayList<>();
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(payloadXml)));
      NodeList textNodes = document.getElementsByTagName("text");
      for (int i = 0; i < textNodes.getLength(); i++) {
        String value = TextUtil.nullIfBlank(textNodes.item(i).getTextContent());
        if (value != null) values.add(value);
      }
    } catch (Exception e) {
      log.debug("Could not parse notification XML: {}", e.toString());
    }
    return values;
  }

  private OffsetDateTime fromWindowsFileTime(long fileTime) {
    long unixMillis = (fileTime / 10_000L) - 11_644_473_600_000L;
    return OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(Math.max(unixMillis, 0L)), ZoneOffset.UTC);
  }

  private String resolveSourceType(String appId) {
    String value = safeLower(appId);
    if (value != null && value.contains("whatsapp")) return "WHATSAPP";
    if (value != null && value.contains("outlook")) return "EMAIL";
    if (value != null && value.contains("telegram")) return "TELEGRAM";
    return "WINDOWS_NOTIFICATION";
  }

  private String displayAppName(String appId) {
    String value = TextUtil.nullIfBlank(appId);
    if (value == null) return "Windows Notification";
    if (value.toLowerCase(Locale.ROOT).contains("whatsapp")) return "WhatsApp Desktop";
    if (value.toLowerCase(Locale.ROOT).contains("outlook")) return "Outlook";
    return value;
  }

  private String buildToastBody(List<String> texts) {
    if (texts == null || texts.isEmpty()) return null;
    if (texts.size() == 1) return texts.get(0);
    return String.join(" ", texts.subList(1, texts.size()));
  }

  private String safeString(String... values) {
    for (String value : values) {
      if (TextUtil.nullIfBlank(value) != null) return value;
    }
    return null;
  }

  private String safeLower(String value) {
    return value == null ? null : value.toLowerCase(Locale.ROOT);
  }

  private boolean relaxedAppMatch(String actual, String allowed) {
    if (actual == null || allowed == null) return false;
    if (actual.contains("whatsapp") && allowed.contains("whatsapp")) return true;
    if (actual.contains("outlook") && allowed.contains("outlook")) return true;
    if (actual.contains("telegram") && allowed.contains("telegram")) return true;
    return false;
  }

  static boolean looksLikePhishGuardNotification(List<String> texts) {
    if (texts == null || texts.isEmpty()) return false;
    for (String text : texts) {
      if (looksLikePhishGuardNotification(text)) return true;
    }
    String joined = String.join(" ", texts);
    return looksLikePhishGuardNotification(joined);
  }

  static boolean looksLikePhishGuardNotification(String text) {
    String value = safeLowerStatic(text);
    if (value == null) return false;
    return value.contains("phishguard ai")
        || value.contains("unsafe message from")
        || value.contains("unsafe file from")
        || value.contains("suspicious message detected")
        || value.contains("low-risk message from")
        || value.contains("message appears safe")
        || value.contains("attachment appears safe")
        || value.contains("url status:")
        || value.contains("safe to open with normal caution")
        || value.contains("do not trust or open it yet");
  }

  private static String safeLowerStatic(String value) {
    return value == null ? null : value.toLowerCase(Locale.ROOT);
  }

  private static class ToastSnapshot {
    public String id;
    public String appDisplayName;
    public String appId;
    public String createdAt;
    public List<String> texts;
  }
}
