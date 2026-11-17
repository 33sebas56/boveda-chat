package com.seguridad.bovedachat.service;
import com.seguridad.bovedachat.config.AppProperties;
import com.seguridad.bovedachat.domain.Account;
import com.seguridad.bovedachat.repository.AccountRepository;
import com.seguridad.bovedachat.web.RegistrationForm;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
@Service
public class RegistrationService {
    private final AccountRepository users; private final PasswordEncoder passwords; private final AppProperties props; private final AuditService audit;
    public RegistrationService(AccountRepository users,PasswordEncoder passwords,AppProperties props,AuditService audit) {this.users=users;this.passwords=passwords;this.props=props;this.audit=audit;}
    @Transactional
    public Account register(RegistrationForm form) {
        if(!Objects.equals(form.getPassword(),form.getConfirmation())) throw AppException.invalid("Las contrasenas no coinciden.");
        validatePassword(form.getPassword());
        if(form.getUsername()==null || !form.getUsername().matches("[a-z0-9_]{3,24}")) throw AppException.invalid("El usuario no es valido.");
        if(form.getDisplayName()==null || form.getDisplayName().trim().length()<2 || form.getDisplayName().length()>60) throw AppException.invalid("El nombre debe tener entre 2 y 60 caracteres.");
        String phone=null;
        if(props.sms().enabled()) {
            phone=form.getPhone()==null?"":form.getPhone().replaceAll("[ ()-]","");
            if(!phone.matches("\\+[1-9][0-9]{7,14}")) throw AppException.invalid("Escribe el celular con indicativo. Ejemplo: +573001234567.");
        }
        if(users.existsByUsername(form.getUsername()) || (phone!=null && users.existsByPhone(phone)))
            throw AppException.invalid("Ya existe una cuenta con esos datos. Inicia sesion o retoma la verificacion pendiente.");
        Account a=new Account();a.setId(UUID.randomUUID());a.setUsername(form.getUsername());a.setDisplayName(form.getDisplayName().trim());
        a.setPasswordHash(passwords.encode(form.getPassword()));a.setPhone(phone);a.setPhoneVerified(false);a.setEnabled(!props.sms().enabled());a.setRole("USER");a.setCreatedAt(Instant.now());
        users.saveAndFlush(a);audit.record(a.getUsername(),"REGISTERED",props.sms().enabled()?"Cuenta pendiente de verificar celular":"Cuenta creada con contrasena");
        return a;
    }
    public void validatePassword(String password) {
        if(password==null || password.length()<12 || password.getBytes(StandardCharsets.UTF_8).length>72) throw AppException.invalid("Usa una contrasena de al menos 12 caracteres y hasta 72 bytes.");
    }
    @Transactional(readOnly=true)
    public Account pending(UUID id) {
        if(!props.sms().enabled()) throw AppException.invalid("La verificacion SMS no esta activada.");
        return users.findById(id).filter(a->!a.getEnabled() && a.getPhone()!=null).orElseThrow(AppException::missing);
    }
    @Transactional(readOnly=true)
    public Account resume(String login,String password) {
        String normalized=com.seguridad.bovedachat.security.LoginNames.normalize(login);
        var account=normalized.startsWith("+")?users.findByPhone(normalized):users.findByUsername(normalized);
        if(account.isEmpty() || !passwords.matches(password==null?"":password,account.get().getPasswordHash()) || account.get().getEnabled() || !props.sms().enabled())
            throw AppException.invalid("No se pudo retomar la verificacion con esos datos.");
        return account.get();
    }
    @Transactional
    public void activate(UUID id) {
        Account a=pending(id);a.setPhoneVerified(true);a.setEnabled(true);
        audit.record(a.getUsername(),"PHONE_VERIFIED","Celular verificado mediante el proveedor SMS");
    }
}
