package com.phishguardai.backend.repository;

import com.phishguardai.backend.entity.AnalysisRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRecordRepository extends JpaRepository<AnalysisRecord, Long> {
  List<AnalysisRecord> findTop20ByUserIdOrderByCreatedAtDesc(String userId);

  Optional<AnalysisRecord> findByIdAndUserId(Long id, String userId);
}

