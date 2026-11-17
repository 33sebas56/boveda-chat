package com.seguridad.bovedachat.service;
import com.seguridad.bovedachat.config.AppProperties;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.util.*;
@Service
public class CryptoService {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();
    public CryptoService(AppProperties props) {
        try {
            String value=props.crypto().key();
            if(value==null || value.isBlank()) {
                if(props.crypto().requireKey()) throw new IllegalStateException("Configura BOVEDA_KEY: 32 bytes aleatorios en Base64. Conserva esa misma clave entre despliegues.");
                Path path=Path.of(props.crypto().keyFile());
                Files.createDirectories(path.toAbsolutePath().getParent());
                if(!Files.exists(path)) {
                    byte[] bytes=new byte[32]; random.nextBytes(bytes);
                    try { Files.writeString(path,Base64.getEncoder().encodeToString(bytes),StandardOpenOption.CREATE_NEW); }
                    catch(FileAlreadyExistsException ignored) { /* Another local process created it. */ }
                    Arrays.fill(bytes,(byte)0);
                    try { Files.setPosixFilePermissions(path,PosixFilePermissions.fromString("rw-------")); }
                    catch(UnsupportedOperationException ignored) { /* Windows ACLs are inherited. */ }
                }
                value=Files.readString(path).trim();
            }
            byte[] raw=Base64.getDecoder().decode(value.trim());
            if(raw.length!=32) throw new IllegalStateException("BOVEDA_KEY debe contener exactamente 32 bytes en Base64.");
            key=new SecretKeySpec(raw,"AES"); Arrays.fill(raw,(byte)0);
        } catch(java.io.IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("No se pudo cargar la clave de cifrado. Comprueba BOVEDA_KEY o el archivo local.",ex);
        }
    }
    public byte[] encrypt(byte[] plain, String context) {
        try {
            byte[] nonce=new byte[12]; random.nextBytes(nonce);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,nonce));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted=cipher.doFinal(plain);
            byte[] envelope=new byte[1+12+encrypted.length];
            envelope[0]=1; System.arraycopy(nonce,0,envelope,1,12); System.arraycopy(encrypted,0,envelope,13,encrypted.length);
            return envelope;
        } catch(GeneralSecurityException ex) { throw new IllegalStateException("No se pudo cifrar el contenido.",ex); }
    }
    public byte[] decrypt(byte[] envelope, String context) {
        if(envelope==null || envelope.length<29 || envelope[0]!=1) throw AppException.invalid("El contenido cifrado no es valido.");
        try {
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Arrays.copyOfRange(envelope,1,13)));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return cipher.doFinal(envelope,13,envelope.length-13);
        } catch(GeneralSecurityException ex) { throw new AppException(org.springframework.http.HttpStatus.CONFLICT,"No se pudo verificar la integridad del contenido. Revisa la clave del servidor."); }
    }
    public String encryptText(String plain,String context) { return Base64.getEncoder().encodeToString(encrypt(plain.getBytes(StandardCharsets.UTF_8),context)); }
    public String decryptText(String encrypted,String context) { return new String(decrypt(Base64.getDecoder().decode(encrypted),context),StandardCharsets.UTF_8); }
    public String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch(NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
