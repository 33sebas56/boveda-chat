package com.seguridad.bovedachat.web;
import jakarta.validation.constraints.*;
public class RegistrationForm {
    @NotBlank @Size(min=2,max=60) private String displayName;
    @NotBlank @Pattern(regexp="[a-z0-9_]{3,24}",message="Usa 3 a 24 minusculas, numeros o guiones bajos.") private String username;
    @NotBlank @Size(min=12,max=72,message="La contrasena debe tener entre 12 y 72 caracteres.") private String password;
    @NotBlank private String confirmation;
    private String phone;
    public String getDisplayName(){return displayName;} public void setDisplayName(String v){displayName=v;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public String getConfirmation(){return confirmation;} public void setConfirmation(String v){confirmation=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
}
