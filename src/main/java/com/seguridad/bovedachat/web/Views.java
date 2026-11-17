package com.seguridad.bovedachat.web;
import java.time.Instant;
import java.util.*;
public final class Views {
    private Views() {}
    public record Person(UUID id,String username,String name) {}
    public record Me(UUID id,String username,String name,String role,boolean phoneVerified,boolean smsEnabled) {}
    public record FileView(UUID id,String name,String mime,long size,String sha256,boolean revoked,long shares,Instant createdAt,boolean mine) {}
    public record Vault(List<FileView> files,long usedBytes,long quotaBytes) {}
    public record ConversationView(UUID id,Person peer,long unread,long peerReadUpTo,Instant updatedAt) {}
    public record MessageView(long id,UUID senderId,String text,FileView attachment,Instant createdAt) {}
    public record MessagePage(List<MessageView> messages,long peerReadUpTo,boolean hasOlder,boolean hasMore) {}
}
