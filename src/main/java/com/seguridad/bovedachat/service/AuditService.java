package com.seguridad.bovedachat.service;
import com.seguridad.bovedachat.domain.AuditEvent;
import com.seguridad.bovedachat.repository.AuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
@Service
public class AuditService {
    private final AuditRepository events;
    public AuditService(AuditRepository events) { this.events=events; }
    @Transactional
    public void record(String actor,String action,String detail) {
        persist(actor,action,detail);
    }
    // Denials survive rollback; successful events commit together with the business operation.
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void denied(String actor,String action,String detail) {
        persist(actor,action,detail);
    }
    private void persist(String actor,String action,String detail) {
        AuditEvent e=new AuditEvent();
        e.setActor(clean(actor,100)); e.setAction(clean(action,40)); e.setDetail(clean(detail,240)); e.setCreatedAt(Instant.now());
        events.save(e);
    }
    private String clean(String value,int max) {
        String text=value==null?"anonimo":value.replaceAll("[\\p{Cntrl}]","_");
        return text.substring(0,Math.min(max,text.length()));
    }
}
