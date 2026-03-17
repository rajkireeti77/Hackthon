package com.phishguardai.backend.repository;

import com.phishguardai.backend.entity.BlockedSender;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedSenderRepository extends JpaRepository<BlockedSender, Long> {
  boolean existsByUserIdAndSourceTypeAndSenderKey(String userId, String sourceType, String senderKey);

  List<BlockedSender> findTop100ByUserIdOrderByCreatedAtDesc(String userId);
}
