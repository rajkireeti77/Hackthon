package com.phishguardai.backend.util.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiErrorResponse> handleApi(ApiException ex, HttpServletRequest req) {
    var status = ex.getStatus();
    var body =
        ApiErrorResponse.of(
            status.getReasonPhrase(), ex.getMessage(), status.value(), req.getRequestURI(), null);
    return ResponseEntity.status(status).body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    Map<String, Object> details = new HashMap<>();
    Map<String, String> fieldErrors = new HashMap<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.put(fe.getField(), fe.getDefaultMessage());
    }
    details.put("fields", fieldErrors);
    var status = HttpStatus.BAD_REQUEST;
    var body =
        ApiErrorResponse.of(
            status.getReasonPhrase(),
            "Validation failed",
            status.value(),
            req.getRequestURI(),
            details);
    return ResponseEntity.status(status).body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArg(
      IllegalArgumentException ex, HttpServletRequest req) {
    var status = HttpStatus.BAD_REQUEST;
    var body =
        ApiErrorResponse.of(
            status.getReasonPhrase(), ex.getMessage(), status.value(), req.getRequestURI(), null);
    return ResponseEntity.status(status).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleOther(Exception ex, HttpServletRequest req) {
    log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
    var status = HttpStatus.INTERNAL_SERVER_ERROR;
    var body =
        ApiErrorResponse.of(
            status.getReasonPhrase(),
            "Something went wrong. Please try again.",
            status.value(),
            req.getRequestURI(),
            null);
    return ResponseEntity.status(status).body(body);
  }
}

