package com.phishguardai.backend.util.exception;

import java.time.OffsetDateTime;
import java.util.Map;

public class ApiErrorResponse {
  private String error;
  private String message;
  private int status;
  private String path;
  private OffsetDateTime timestamp;
  private Map<String, Object> details;

  public static ApiErrorResponse of(
      String error, String message, int status, String path, Map<String, Object> details) {
    ApiErrorResponse r = new ApiErrorResponse();
    r.error = error;
    r.message = message;
    r.status = status;
    r.path = path;
    r.timestamp = OffsetDateTime.now();
    r.details = details;
    return r;
  }

  public String getError() {
    return error;
  }

  public String getMessage() {
    return message;
  }

  public int getStatus() {
    return status;
  }

  public String getPath() {
    return path;
  }

  public OffsetDateTime getTimestamp() {
    return timestamp;
  }

  public Map<String, Object> getDetails() {
    return details;
  }
}

