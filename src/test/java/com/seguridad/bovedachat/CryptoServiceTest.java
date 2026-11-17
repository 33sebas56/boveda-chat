package com.seguridad.bovedachat;
import com.seguridad.bovedachat.config.AppProperties;
import com.seguridad.bovedachat.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class CryptoServiceTest {
    @TempDir Path temp;
    private AppProperties props(String key,boolean required){return new AppProperties(new AppProperties.Crypto(key,temp.resolve("key").toString(),required),null,null,null,null);}
    @Test void encryptsWithFreshNonceAndPreservesContent(){
        var crypto=new CryptoService(props(Base64.getEncoder().encodeToString(new byte[32]),true));byte[] plain="archivo confidencial".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] a=crypto.encrypt(plain,"file:1"),b=crypto.encrypt(plain,"file:1");
        assertThat(new String(a,java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("archivo confidencial");assertThat(a).isNotEqualTo(b);assertThat(crypto.decrypt(a,"file:1")).isEqualTo(plain);
    }
    @Test void rejectsManipulatedCiphertext(){var c=new CryptoService(props(Base64.getEncoder().encodeToString(new byte[32]),true));byte[] data=c.encrypt(new byte[]{1,2,3},"a");data[data.length-1]^=1;assertThatThrownBy(()->c.decrypt(data,"a")).isInstanceOf(AppException.class);}
    @Test void rejectsDifferentContext(){var c=new CryptoService(props(Base64.getEncoder().encodeToString(new byte[32]),true));byte[] data=c.encrypt(new byte[]{1,2,3},"a");assertThatThrownBy(()->c.decrypt(data,"b")).isInstanceOf(AppException.class);}
    @Test void rejectsWrongKey(){byte[] raw=new byte[32];raw[0]=1;var a=new CryptoService(props(Base64.getEncoder().encodeToString(new byte[32]),true));var b=new CryptoService(props(Base64.getEncoder().encodeToString(raw),true));assertThatThrownBy(()->b.decrypt(a.encrypt(new byte[]{1},"a"),"a")).isInstanceOf(AppException.class);}
    @Test void localKeySurvivesRestart(){var a=new CryptoService(props("",false));byte[] data=a.encrypt(new byte[]{8,9},"file");var b=new CryptoService(props("",false));assertThat(b.decrypt(data,"file")).isEqualTo(new byte[]{8,9});}
    @Test void productionCannotGenerateEphemeralKey(){assertThatThrownBy(()->new CryptoService(props("",true))).isInstanceOf(IllegalStateException.class);}
    @Test void shortKeyIsRejected(){assertThatThrownBy(()->new CryptoService(props("YWJj",true))).isInstanceOf(IllegalStateException.class);}
    @Test void fingerprintChangesWithContent(){var c=new CryptoService(props(Base64.getEncoder().encodeToString(new byte[32]),true));assertThat(c.sha256(new byte[]{1})).hasSize(64).isNotEqualTo(c.sha256(new byte[]{2}));}
}
