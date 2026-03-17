package com.phishguardai.backend.dto;

import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class BackgroundIngestRequest {
  @Size(max = 32, message = "sourceType is too long (max 32 chars)")
  private String sourceType;

  @Size(max = 255, message = "sourceMessageId is too long (max 255 chars)")
  private String sourceMessageId;

  @Size(max = 180, message = "senderLabel is too long (max 180 chars)")
  private String senderLabel;

  @Size(max = 255, message = "senderAddress is too long (max 255 chars)")
  private String senderAddress;

  @Size(max = 240, message = "subjectLine is too long (max 240 chars)")
  private String subjectLine;

  @Size(max = 10000, message = "textContent is too long (max 10000 chars)")
  private String textContent;

  @Size(max = 30000, message = "htmlContent is too long (max 30000 chars)")
  private String htmlContent;

  @Size(max = 2048, message = "url is too long (max 2048 chars)")
  private String url;

  @Size(max = 255, message = "fileName is too long (max 255 chars)")
  private String fileName;

  @Size(max = 255, message = "fileContentType is too long (max 255 chars)")
  private String fileContentType;

  private Long fileSizeBytes;

  @Size(max = 64, message = "fileSha256 is too long (max 64 chars)")
  private String fileSha256;

  @Size(max = 16384, message = "fileSampleBase64 is too long")
  private String fileSampleBase64;

  private OffsetDateTime receivedAt;

  private List<IncomingAttachmentDto> attachments = new ArrayList<>();

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  public String getSourceMessageId() {
    return sourceMessageId;
  }

  public void setSourceMessageId(String sourceMessageId) {
    this.sourceMessageId = sourceMessageId;
  }

  public String getSenderLabel() {
    return senderLabel;
  }

  public void setSenderLabel(String senderLabel) {
    this.senderLabel = senderLabel;
  }

  public String getSenderAddress() {
    return senderAddress;
  }

  public void setSenderAddress(String senderAddress) {
    this.senderAddress = senderAddress;
  }

  public String getSubjectLine() {
    return subjectLine;
  }

  public void setSubjectLine(String subjectLine) {
    this.subjectLine = subjectLine;
  }

  public String getTextContent() {
    return textContent;
  }

  public void setTextContent(String textContent) {
    this.textContent = textContent;
  }

  public String getHtmlContent() {
    return htmlContent;
  }

  public void setHtmlContent(String htmlContent) {
    this.htmlContent = htmlContent;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFileContentType() {
    return fileContentType;
  }

  public void setFileContentType(String fileContentType) {
    this.fileContentType = fileContentType;
  }

  public Long getFileSizeBytes() {
    return fileSizeBytes;
  }

  public void setFileSizeBytes(Long fileSizeBytes) {
    this.fileSizeBytes = fileSizeBytes;
  }

  public String getFileSha256() {
    return fileSha256;
  }

  public void setFileSha256(String fileSha256) {
    this.fileSha256 = fileSha256;
  }

  public String getFileSampleBase64() {
    return fileSampleBase64;
  }

  public void setFileSampleBase64(String fileSampleBase64) {
    this.fileSampleBase64 = fileSampleBase64;
  }

  public OffsetDateTime getReceivedAt() {
    return receivedAt;
  }

  public void setReceivedAt(OffsetDateTime receivedAt) {
    this.receivedAt = receivedAt;
  }

  public List<IncomingAttachmentDto> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<IncomingAttachmentDto> attachments) {
    this.attachments = attachments == null ? new ArrayList<>() : attachments;
  }
}
