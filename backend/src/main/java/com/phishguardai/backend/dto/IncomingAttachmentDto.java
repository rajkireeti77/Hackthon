package com.phishguardai.backend.dto;

import jakarta.validation.constraints.Size;

public class IncomingAttachmentDto {
  @Size(max = 255, message = "attachment fileName is too long (max 255 chars)")
  private String fileName;

  @Size(max = 255, message = "attachment contentType is too long (max 255 chars)")
  private String contentType;

  private Long sizeBytes;

  @Size(max = 64, message = "attachment sha256 is too long (max 64 chars)")
  private String sha256;

  @Size(max = 16384, message = "attachment sampleBase64 is too long")
  private String sampleBase64;

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public Long getSizeBytes() {
    return sizeBytes;
  }

  public void setSizeBytes(Long sizeBytes) {
    this.sizeBytes = sizeBytes;
  }

  public String getSha256() {
    return sha256;
  }

  public void setSha256(String sha256) {
    this.sha256 = sha256;
  }

  public String getSampleBase64() {
    return sampleBase64;
  }

  public void setSampleBase64(String sampleBase64) {
    this.sampleBase64 = sampleBase64;
  }
}
