package com.seguridad.bovedachat.domain;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="stored_files")
public class StoredFile {
    @Id
    private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="owner_id", nullable=false)
    private Account owner;
    @Column(nullable=false, length=180)
    private String originalName;
    @Column(nullable=false, length=80)
    private String mime;

    private long sizeBytes;
    @Column(nullable=false, length=64)
    private String sha256;
    @Column(nullable=false, unique=true, length=80)
    private String objectKey;

    private boolean revoked;
    @Column(nullable=false)
    private Instant createdAt;
    public StoredFile() {}
    public UUID getId() { return id; }
    public void setId(UUID value) { id = value; }
    public Account getOwner() { return owner; }
    public void setOwner(Account value) { owner = value; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String value) { originalName = value; }
    public String getMime() { return mime; }
    public void setMime(String value) { mime = value; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long value) { sizeBytes = value; }
    public String getSha256() { return sha256; }
    public void setSha256(String value) { sha256 = value; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String value) { objectKey = value; }
    public boolean getRevoked() { return revoked; }
    public void setRevoked(boolean value) { revoked = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
}
