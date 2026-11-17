package com.seguridad.bovedachat.security;
import java.util.Locale;
public final class LoginNames {
    private LoginNames() {}
    public static String normalize(String value) {
        String login=value==null?"":value.trim().toLowerCase(Locale.ROOT).replaceFirst("^@","");
        return login.startsWith("+")?login.replaceAll("[ ()-]",""):login;
    }
}
