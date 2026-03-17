package com.phishguardai.backend.repository;

import com.phishguardai.backend.entity.ExtractedUrl;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractedUrlRepository extends JpaRepository<ExtractedUrl, Long> {
  List<ExtractedUrl> findByIncomingEventIdOrderByRiskScoreDescIdAsc(Long incomingEventId);
}
