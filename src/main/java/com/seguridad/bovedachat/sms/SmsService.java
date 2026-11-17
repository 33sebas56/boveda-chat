package com.seguridad.bovedachat.sms;
import com.seguridad.bovedachat.config.AppProperties;
import com.seguridad.bovedachat.domain.Account;
import com.seguridad.bovedachat.repository.AuditRepository;
import com.seguridad.bovedachat.service.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import java.time.*;
@Service
public class SmsService {
    private final AppProperties props; private final ObjectProvider<VerificationGateway> gateway; private final RateLimiter limiter; private final AuditRepository events; private final AuditService audit;
    public SmsService(AppProperties props,ObjectProvider<VerificationGateway> gateway,RateLimiter limiter,AuditRepository events,AuditService audit) {this.props=props;this.gateway=gateway;this.limiter=limiter;this.events=events;this.audit=audit;}
    public synchronized void send(Account account) {
        if(!props.sms().enabled()) throw AppException.invalid("La verificacion SMS no esta activada.");
        limiter.require("sms-cooldown:"+account.getId(),1,Duration.ofSeconds(60));
        limiter.require("sms-account:"+account.getId(),3,Duration.ofHours(1));
        Instant midnight=LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        if(events.countByActionAndCreatedAtAfter("SMS_REQUESTED",midnight)>=props.sms().dailyBudget()) throw new AppException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,"Se alcanzo el limite diario de SMS de esta aplicacion.");
        // Reserve budget before the network request, including failed attempts. Never store or log OTPs.
        audit.record(account.getUsername(),"SMS_REQUESTED","Solicitud de verificacion de celular");
        gateway.getObject().send(account.getPhone());
    }
    public boolean check(Account account,String code) {
        limiter.require("sms-check:"+account.getId(),5,Duration.ofMinutes(10));
        return props.sms().enabled() && gateway.getObject().check(account.getPhone(),code);
    }
}
