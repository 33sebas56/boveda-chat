package com.seguridad.bovedachat.config;
import com.seguridad.bovedachat.domain.Account;
import com.seguridad.bovedachat.repository.AccountRepository;
import com.seguridad.bovedachat.service.RegistrationService;
import org.springframework.boot.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
@Component
public class BootstrapAdmin implements ApplicationRunner {
    private final AppProperties props;private final AccountRepository users;private final PasswordEncoder passwords;private final RegistrationService registration;
    public BootstrapAdmin(AppProperties props,AccountRepository users,PasswordEncoder passwords,RegistrationService registration){this.props=props;this.users=users;this.passwords=passwords;this.registration=registration;}
    @Override @Transactional
    public void run(ApplicationArguments args) {
        String name=props.bootstrap().username(),password=props.bootstrap().password();
        if((name==null || name.isBlank()) && (password==null || password.isBlank())) return;
        if(name==null || !name.matches("[a-z0-9_]{3,24}")) throw new IllegalStateException("ADMIN_USERNAME debe tener 3 a 24 minusculas, numeros o guiones bajos.");
        registration.validatePassword(password);
        var existing=users.findByUsername(name);
        if(existing.isPresent()) {
            if(!"ADMIN".equals(existing.get().getRole())) throw new IllegalStateException("ADMIN_USERNAME ya pertenece a una cuenta de usuario. Elige un nombre de administrador nuevo.");
            return; // Never reset a stored password merely by restarting the application.
        }
        Account a=new Account();a.setId(UUID.randomUUID());a.setUsername(name);a.setDisplayName("Administracion");a.setPasswordHash(passwords.encode(password));a.setEnabled(true);a.setPhoneVerified(false);a.setRole("ADMIN");a.setCreatedAt(Instant.now());users.save(a);
    }
}
