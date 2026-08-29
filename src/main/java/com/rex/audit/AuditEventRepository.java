package com.rex.audit;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Read and append only. No update or delete method is exposed, by design. */
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

  List<AuditEvent> findByTargetTypeAndTargetIdOrderByOccurredAtDesc(
      String targetType, Long targetId);

  List<AuditEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
