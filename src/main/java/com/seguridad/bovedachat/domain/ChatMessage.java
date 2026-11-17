package com.seguridad.bovedachat.domain;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="chat_messages")
public class ChatMessage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="conversation_id", nullable=false)
    private Conversation conversation;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sender_id", nullable=false)
    private Account sender;
    @Column(columnDefinition="text")
    private String ciphertext;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="attachment_id")
    private StoredFile attachment;
    @Column(nullable=false)
    private Instant createdAt;
    public ChatMessage() {}
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation value) { conversation = value; }
    public Account getSender() { return sender; }
    public void setSender(Account value) { sender = value; }
    public String getCiphertext() { return ciphertext; }
    public void setCiphertext(String value) { ciphertext = value; }
    public StoredFile getAttachment() { return attachment; }
    public void setAttachment(StoredFile value) { attachment = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
}
