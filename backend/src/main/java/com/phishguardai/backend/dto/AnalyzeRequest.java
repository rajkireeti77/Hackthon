package com.phishguardai.backend.dto;

import jakarta.validation.constraints.Size;

public class AnalyzeRequest {
  @Size(max = 10000, message = "textContent is too long (max 10000 chars)")
  private String textContent;

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

  public String getTextContent() {
    return textContent;
  }

  public void setTextContent(String textContent) {
    this.textContent = textContent;
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
}

