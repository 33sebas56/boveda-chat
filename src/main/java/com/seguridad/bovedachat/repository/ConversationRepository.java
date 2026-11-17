package com.seguridad.bovedachat.repository;
import com.seguridad.bovedachat.domain.Conversation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Optional<Conversation> findByUserLow_IdAndUserHigh_Id(UUID low, UUID high);
    @EntityGraph(attributePaths={"userLow","userHigh"})
    @Query("select c from Conversation c where c.userLow.id=:id or c.userHigh.id=:id order by c.updatedAt desc")
    List<Conversation> listFor(UUID id, Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id=:id")
    Optional<Conversation> lockById(UUID id);
}
