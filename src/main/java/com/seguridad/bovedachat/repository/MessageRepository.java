package com.seguridad.bovedachat.repository;
import com.seguridad.bovedachat.domain.ChatMessage;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface MessageRepository extends JpaRepository<ChatMessage, Long> {
    @EntityGraph(attributePaths={"sender","attachment","attachment.owner"})
    List<ChatMessage> findByConversation_IdOrderByIdDesc(UUID conversationId, Pageable page);
    @EntityGraph(attributePaths={"sender","attachment","attachment.owner"})
    List<ChatMessage> findByConversation_IdAndIdGreaterThanOrderByIdAsc(UUID conversationId, long after, Pageable page);
    @EntityGraph(attributePaths={"sender","attachment","attachment.owner"})
    List<ChatMessage> findByConversation_IdAndIdLessThanOrderByIdDesc(UUID conversationId, long before, Pageable page);
    long countByConversation_IdAndSender_IdNotAndIdGreaterThan(UUID conversationId, UUID senderId, long lastRead);
    long countByAttachment_Id(UUID fileId);
    @Query("select count(m) from ChatMessage m where m.attachment.id=:file and (m.conversation.userLow.id=:user or m.conversation.userHigh.id=:user)")
    long authorizedShares(UUID file, UUID user);
    @Query("select coalesce(max(m.id),0) from ChatMessage m where m.conversation.id=:id")
    long lastId(UUID id);
}
