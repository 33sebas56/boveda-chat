package com.seguridad.bovedachat.service;
import org.springframework.http.HttpStatus;
public class AppException extends RuntimeException {
    private final HttpStatus status;
    public AppException(HttpStatus status, String message) { super(message); this.status=status; }
    public HttpStatus status() { return status; }
    public static AppException invalid(String message) { return new AppException(HttpStatus.BAD_REQUEST,message); }
    public static AppException missing() { return new AppException(HttpStatus.NOT_FOUND,"El recurso no existe o no esta disponible para tu cuenta."); }
}
