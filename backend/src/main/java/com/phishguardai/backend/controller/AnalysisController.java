package com.phishguardai.backend.controller;

import com.phishguardai.backend.dto.AnalyzeRequest;
import com.phishguardai.backend.dto.AnalysisResponse;
import com.phishguardai.backend.dto.AnalysisSummaryDto;
import com.phishguardai.backend.service.AnalysisService;
import com.phishguardai.backend.util.AuthUserContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnalysisController {
  private final AnalysisService service;

  public AnalysisController(AnalysisService service) {
    this.service = service;
  }

  @PostMapping("/analyze")
  public ResponseEntity<AnalysisResponse> analyze(@Valid @RequestBody AnalyzeRequest request) {
    String uid = AuthUserContext.getRequired().uid();
    return ResponseEntity.ok(service.analyzeAndSave(uid, request));
  }

  @GetMapping("/analyses")
  public ResponseEntity<List<AnalysisSummaryDto>> list() {
    String uid = AuthUserContext.getRequired().uid();
    return ResponseEntity.ok(service.recentAnalyses(uid));
  }

  @GetMapping("/analyses/{id}")
  public ResponseEntity<AnalysisResponse> getOne(@PathVariable("id") Long id) {
    String uid = AuthUserContext.getRequired().uid();
    return ResponseEntity.ok(service.getOne(uid, id));
  }

  @DeleteMapping("/analyses/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
    String uid = AuthUserContext.getRequired().uid();
    service.deleteOne(uid, id);
    return ResponseEntity.noContent().build();
  }
}

