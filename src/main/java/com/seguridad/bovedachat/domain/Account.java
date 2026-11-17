package com.seguridad.bovedachat.domain;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="app_users")
public class Account {
    @Id
    private UUID id;
    @Column(nullable=false, unique=true, length=24)
    private String username;
    @Column(nullable=false, length=60)
    private String displayName;
    @Column(nullable=false, length=100)
    private String passwordHash;
    @Column(unique=true, length=16)
    private String phone;

    private boolean phoneVerified;

    private boolean enabled;
    @Column(nullable=false, length=10)
    private String role;
    @Column(nullable=false)
    private Instant createdAt;
    public Account() {}
    public UUID getId() { return id; }
    public void setId(UUID value) { id = value; }
    public String getUsername() { return username; }
    public void setUsername(String value) { username = value; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String value) { passwordHash = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }
    public boolean getPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean value) { phoneVerified = value; }
    public boolean getEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public String getRole() { return role; }
    public void setRole(String value) { role = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
}
