package com.seguridad.bovedachat.sms;
public interface VerificationGateway {
    void send(String phone);
    boolean check(String phone,String code);
}
