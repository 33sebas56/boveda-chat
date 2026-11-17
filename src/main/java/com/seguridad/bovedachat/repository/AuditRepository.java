package com.seguridad.bovedachat.repository;
import com.seguridad.bovedachat.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.*;
public interface AuditRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findTop200ByOrderByIdDesc();
    long countByActionAndCreatedAtAfter(String action, Instant since);
}
