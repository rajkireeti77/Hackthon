package com.phishguardai.backend.repository;

import com.phishguardai.backend.entity.IncomingEvent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomingEventRepository extends JpaRepository<IncomingEvent, Long> {
  Optional<IncomingEvent> findByUserIdAndSourceTypeAndSourceMessageId(String userId, String sourceType, String sourceMessageId);
}
