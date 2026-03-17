package com.phishguardai.backend.repository;

import com.phishguardai.backend.entity.SecurityAlert;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {
  List<SecurityAlert> findTop20ByUserIdOrderByCreatedAtDesc(String userId);

  List<SecurityAlert> findTop100ByUserIdOrderByCreatedAtDesc(String userId);

  List<SecurityAlert> findTop20ByUserIdAndReadStatusIsFalseOrderByCreatedAtDesc(String userId);

  Optional<SecurityAlert> findByIdAndUserId(Long id, String userId);

  Optional<SecurityAlert> findByAnalysisIdAndUserId(Long analysisId, String userId);
}
