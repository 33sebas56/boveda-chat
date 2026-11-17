package com.seguridad.bovedachat.domain;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="audit_events")
public class AuditEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, length=100)
    private String actor;
    @Column(nullable=false, length=40)
    private String action;
    @Column(nullable=false, length=240)
    private String detail;
    @Column(nullable=false)
    private Instant createdAt;
    public AuditEvent() {}
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public String getActor() { return actor; }
    public void setActor(String value) { actor = value; }
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }
    public String getDetail() { return detail; }
    public void setDetail(String value) { detail = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
}
