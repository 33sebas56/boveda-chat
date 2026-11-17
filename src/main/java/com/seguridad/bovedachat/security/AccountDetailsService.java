package com.seguridad.bovedachat.security;
import com.seguridad.bovedachat.repository.AccountRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class AccountDetailsService implements UserDetailsService {
    private final AccountRepository users;
    public AccountDetailsService(AccountRepository users) { this.users=users; }
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        String login=LoginNames.normalize(input);
        return VaultPrincipal.from((login.startsWith("+")?users.findByPhone(login):users.findByUsername(login))
            .orElseThrow(()->new UsernameNotFoundException("Credenciales incorrectas.")));
    }
}
