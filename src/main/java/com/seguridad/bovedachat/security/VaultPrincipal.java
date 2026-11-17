package com.seguridad.bovedachat.security;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.seguridad.bovedachat.domain.Account;
import java.util.*;
public record VaultPrincipal(UUID id,String username,String password,boolean enabled,String role) implements UserDetails {
    public static VaultPrincipal from(Account a) { return new VaultPrincipal(a.getId(),a.getUsername(),a.getPasswordHash(),a.getEnabled(),a.getRole()); }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public boolean isEnabled() { return enabled; }
    public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_"+role)); }
}
