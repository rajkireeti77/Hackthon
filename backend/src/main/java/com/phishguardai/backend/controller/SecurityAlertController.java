package com.phishguardai.backend.controller;

import com.phishguardai.backend.dto.AlertDetailsDto;
import com.phishguardai.backend.dto.BackgroundIngestRequest;
import com.phishguardai.backend.dto.PhishingReportRequest;
import com.phishguardai.backend.dto.SecurityAlertDto;
import com.phishguardai.backend.service.IncomingMessageService;
import com.phishguardai.backend.service.SecurityAlertService;
import com.phishguardai.backend.util.AuthUserContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SecurityAlertController {
  private final IncomingMessageService incomingMessageService;
  private final SecurityAlertService service;

  public SecurityAlertController(IncomingMessageService incomingMessageService, SecurityAlertService service) {
    this.incomingMessageService = incomingMessageService;
    this.service = service;
  }

  @PostMapping("/background/ingest")
  public ResponseEntity<SecurityAlertDto> ingest(@Valid @RequestBody BackgroundIngestRequest request) {
    String uid = AuthUserContext.getRequired().uid();
    return ResponseEntity.ok(incomingMessageService.ingestAndCreateAlert(uid, request));
  }

  @GetMapping("/alerts")
  public ResponseEntity<List<SecurityAlertDto>> recentAlerts(
      @RequestParam(value = "sourceType", required = false) String sourceType,
      @RequestParam(value = "verdict", required = false) String verdict,
      @RequestParam(value = "read", required = false) Boolean read) {
    String uid = AuthUserContext.getRequired().uid();
    return ResponseEntity.ok(service.recentAlerts(uid, sourceType, verdict, read));
  }

  @GetMapping("/alerts/unread")
  public ResponseEntity<List<SecurityAlertDto>> unreadAlerts() {
    String uid = AuthUserContext.getRequired().uid();
    return ResponseEntity.ok(service.unreadAlerts(uid));
  }

  @GetMapping("/alerts/{id}")
  public ResponseEntity<AlertDetailsDto> alertDetails(@PathVariable("id") Long id) {
    String uid = AuthUserContext.getRequired().uid();
    return ResponseEntity.ok(service.getDetails(uid, id));
  }

  @PostMapping("/alerts/{id}/read")
  public ResponseEntity<SecurityAlertDto> markRead(@PathVariable("id") Long id) {
    String uid = AuthUserContext.getRequired().uid();
    return ResponseEntity.ok(service.markRead(uid, id));
  }

  @PostMapping("/alerts/{id}/open-anyway")
  public ResponseEntity<SecurityAlertDto> openAnyway(@PathVariable("id") Long id) {
    String uid = AuthUserContext.getRequired().uid();
    return ResponseEntity.ok(service.openAnyway(uid, id));
  }

  @PostMapping("/alerts/{id}/cancel")
  public ResponseEntity<SecurityAlertDto> cancel(@PathVariable("id") Long id) {
    String uid = AuthUserContext.getRequired().uid();
    return ResponseEntity.ok(service.cancel(uid, id));
  }

  @PostMapping("/alerts/{id}/block-sender")
  public ResponseEntity<SecurityAlertDto> blockSender(@PathVariable("id") Long id) {
    String uid = AuthUserContext.getRequired().uid();
    return ResponseEntity.ok(service.blockSender(uid, id));
  }

  @PostMapping("/alerts/{id}/report-phishing")
  public ResponseEntity<SecurityAlertDto> reportPhishing(
      @PathVariable("id") Long id, @Valid @RequestBody(required = false) PhishingReportRequest request) {
    String uid = AuthUserContext.getRequired().uid();
    return ResponseEntity.ok(service.reportPhishing(uid, id, request));
  }

  @PostMapping("/alerts/read-all")
  public ResponseEntity<Void> markAllRead() {
    String uid = AuthUserContext.getRequired().uid();
    service.markAllRead(uid);
    return ResponseEntity.noContent().build();
  }
}
