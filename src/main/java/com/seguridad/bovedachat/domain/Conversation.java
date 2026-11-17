package com.seguridad.bovedachat.domain;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="conversations")
public class Conversation {
    @Id
    private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_low_id", nullable=false)
    private Account userLow;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_high_id", nullable=false)
    private Account userHigh;

    private long lowReadId;

    private long highReadId;
    @Column(nullable=false)
    private Instant updatedAt;
    public Conversation() {}
    public UUID getId() { return id; }
    public void setId(UUID value) { id = value; }
    public Account getUserLow() { return userLow; }
    public void setUserLow(Account value) { userLow = value; }
    public Account getUserHigh() { return userHigh; }
    public void setUserHigh(Account value) { userHigh = value; }
    public long getLowReadId() { return lowReadId; }
    public void setLowReadId(long value) { lowReadId = value; }
    public long getHighReadId() { return highReadId; }
    public void setHighReadId(long value) { highReadId = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
}
