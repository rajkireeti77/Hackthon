package com.phishguardai.backend.service;

import com.phishguardai.backend.config.IncomingMailProperties;
import com.phishguardai.backend.dto.BackgroundIngestRequest;
import com.phishguardai.backend.dto.IncomingAttachmentDto;
import com.phishguardai.backend.util.TextUtil;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.FlagTerm;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class IncomingMailScannerService {
  private static final Logger log = LoggerFactory.getLogger(IncomingMailScannerService.class);

  private final IncomingMailProperties props;
  private final IncomingMessageService incomingMessageService;

  public IncomingMailScannerService(IncomingMailProperties props, IncomingMessageService incomingMessageService) {
    this.props = props;
    this.incomingMessageService = incomingMessageService;
  }

  @Scheduled(fixedDelayString = "${app.incoming.mail.poll-interval-ms:30000}")
  public void pollMailbox() {
    if (!props.isEnabled()) return;
    if (TextUtil.nullIfBlank(props.getHost()) == null || TextUtil.nullIfBlank(props.getUsername()) == null) return;

    Properties mailProps = new Properties();
    mailProps.put("mail.store.protocol", props.getProtocol());
    mailProps.put("mail." + props.getProtocol() + ".host", props.getHost());
    mailProps.put("mail." + props.getProtocol() + ".port", String.valueOf(props.getPort()));
    mailProps.put("mail." + props.getProtocol() + ".ssl.enable", "true");
    Session session = Session.getInstance(mailProps);

    try (Store store = session.getStore(props.getProtocol())) {
      store.connect(props.getHost(), props.getPort(), props.getUsername(), props.getPassword());
      Folder folder = store.getFolder(props.getFolder());
      folder.open(Folder.READ_WRITE);
      try {
        Message[] unseen = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
        if (unseen.length == 0) return;
        List<Message> ordered = new ArrayList<>(List.of(unseen));
        ordered.sort(Comparator.comparing(this::receivedEpoch));
        int start = Math.max(0, ordered.size() - props.getMaxMessagesPerPoll());
        for (Message message : ordered.subList(start, ordered.size())) {
          try {
            BackgroundIngestRequest request = toRequest(message);
            incomingMessageService.ingestAndCreateAlert(props.getUserId(), request);
            if (props.isMarkSeenOnProcess()) {
              message.setFlag(Flags.Flag.SEEN, true);
            }
          } catch (Exception e) {
            log.warn("Failed to process incoming email: {}", e.toString());
          }
        }
      } finally {
        folder.close(false);
      }
    } catch (Exception e) {
      log.warn("Incoming mail polling failed: {}", e.toString());
    }
  }

  private BackgroundIngestRequest toRequest(Message message) throws Exception {
    ParsedEmail parsed = new ParsedEmail();
    extractContent(message, parsed);

    BackgroundIngestRequest request = new BackgroundIngestRequest();
    request.setSourceType("EMAIL");
    request.setSourceMessageId(firstHeader(message, "Message-ID"));
    request.setSenderLabel(senderLabel(message.getFrom()));
    request.setSenderAddress(senderAddress(message.getFrom()));
    request.setSubjectLine(TextUtil.nullIfBlank(message.getSubject()));
    request.setTextContent(TextUtil.nullIfBlank(parsed.text.toString()));
    request.setHtmlContent(TextUtil.nullIfBlank(parsed.html.toString()));
    request.setAttachments(parsed.attachments);
    if (message.getReceivedDate() != null) {
      request.setReceivedAt(OffsetDateTime.ofInstant(message.getReceivedDate().toInstant(), ZoneOffset.UTC));
    } else if (message.getSentDate() != null) {
      request.setReceivedAt(OffsetDateTime.ofInstant(message.getSentDate().toInstant(), ZoneOffset.UTC));
    }
    return request;
  }

  private void extractContent(Part part, ParsedEmail parsed) throws Exception {
    if (part.isMimeType("text/plain")) {
      parsed.text.append(TextUtil.truncate(String.valueOf(part.getContent()), 12000)).append('\n');
      return;
    }
    if (part.isMimeType("text/html")) {
      parsed.html.append(TextUtil.truncate(String.valueOf(part.getContent()), 24000)).append('\n');
      return;
    }
    String disposition = part.getDisposition();
    String fileName = TextUtil.nullIfBlank(part.getFileName());
    if ((disposition != null && Part.ATTACHMENT.equalsIgnoreCase(disposition)) || fileName != null) {
      parsed.attachments.add(readAttachment(part, fileName));
      return;
    }
    Object content = part.getContent();
    if (content instanceof Multipart multipart) {
      for (int i = 0; i < multipart.getCount(); i++) {
        BodyPart bodyPart = multipart.getBodyPart(i);
        extractContent(bodyPart, parsed);
      }
    }
  }

  private IncomingAttachmentDto readAttachment(Part part, String fileName) throws Exception {
    IncomingAttachmentDto attachment = new IncomingAttachmentDto();
    attachment.setFileName(TextUtil.truncate(fileName, 255));
    attachment.setContentType(TextUtil.truncate(TextUtil.nullIfBlank(part.getContentType()), 255));
    attachment.setSizeBytes(part.getSize() > 0 ? (long) part.getSize() : null);

    byte[] bytes = readFirstBytes(part.getInputStream(), 2048);
    if (bytes.length > 0) {
      attachment.setSampleBase64(Base64.getEncoder().encodeToString(bytes));
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      attachment.setSha256(bytesToHex(digest.digest(bytes)));
    }
    return attachment;
  }

  private byte[] readFirstBytes(InputStream stream, int maxBytes) throws Exception {
    try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[512];
      int remaining = maxBytes;
      while (remaining > 0) {
        int read = in.read(buffer, 0, Math.min(buffer.length, remaining));
        if (read < 0) break;
        out.write(buffer, 0, read);
        remaining -= read;
      }
      return out.toByteArray();
    }
  }

  private String firstHeader(Message message, String name) throws Exception {
    String[] values = message.getHeader(name);
    return values == null || values.length == 0 ? null : values[0];
  }

  private String senderLabel(Address[] addresses) {
    if (addresses == null || addresses.length == 0) return null;
    Address first = addresses[0];
    if (first instanceof InternetAddress internet && TextUtil.nullIfBlank(internet.getPersonal()) != null) {
      return internet.getPersonal();
    }
    return first.toString();
  }

  private String senderAddress(Address[] addresses) {
    if (addresses == null || addresses.length == 0) return null;
    Address first = addresses[0];
    if (first instanceof InternetAddress internet) {
      return internet.getAddress();
    }
    return first.toString();
  }

  private long receivedEpoch(Message message) {
    try {
      if (message.getReceivedDate() != null) return message.getReceivedDate().getTime();
      if (message.getSentDate() != null) return message.getSentDate().getTime();
    } catch (Exception ignored) {
    }
    return 0L;
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte value : bytes) {
      builder.append(String.format("%02x", value));
    }
    return builder.toString();
  }

  private static class ParsedEmail {
    private final StringBuilder text = new StringBuilder();
    private final StringBuilder html = new StringBuilder();
    private final List<IncomingAttachmentDto> attachments = new ArrayList<>();
  }
}
