package com.phishguardai.backend.util.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends ApiException {
  public ExternalServiceException(String message) {
    super(HttpStatus.BAD_GATEWAY, message);
  }

  public ExternalServiceException(String message, Throwable cause) {
    super(HttpStatus.BAD_GATEWAY, message, cause);
  }
}

