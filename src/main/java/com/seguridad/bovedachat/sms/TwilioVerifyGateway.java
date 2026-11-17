package com.seguridad.bovedachat.sms;
import com.seguridad.bovedachat.config.AppProperties;
import com.seguridad.bovedachat.service.AppException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import java.util.Map;
@Component
@ConditionalOnProperty(name="app.sms.enabled",havingValue="true")
public class TwilioVerifyGateway implements VerificationGateway {
    private final RestClient client; private final String service;
    public TwilioVerifyGateway(AppProperties props,RestClient.Builder builder) {
        var sms=props.sms(); service=sms.serviceSid();
        if(sms.accountSid()==null || !sms.accountSid().matches("AC[0-9a-fA-F]{32}") || sms.authToken()==null || sms.authToken().isBlank() || service==null || !service.matches("VA[0-9a-fA-F]{32}"))
            throw new IllegalStateException("SMS_ENABLED=true requiere TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN y TWILIO_VERIFY_SERVICE_SID validos.");
        client=builder.clone().baseUrl("https://verify.twilio.com/v2/Services/"+service)
            .defaultHeaders(headers->headers.setBasicAuth(sms.accountSid(),sms.authToken())).build();
    }
    public void send(String phone) {
        var form=new LinkedMultiValueMap<String,String>();form.add("To",phone);form.add("Channel","sms");form.add("Locale","es");
        try {
            Map<?,?> body=client.post().uri("/Verifications").contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(Map.class);
            if(body==null || !"pending".equals(body.get("status"))) throw unavailable();
        } catch(RestClientException ex) { throw unavailable(); }
    }
    public boolean check(String phone,String code) {
        if(code==null || !code.matches("[0-9]{4,10}")) return false;
        var form=new LinkedMultiValueMap<String,String>();form.add("To",phone);form.add("Code",code);
        try {
            Map<?,?> body=client.post().uri("/VerificationCheck").contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(Map.class);
            return body!=null && "approved".equals(body.get("status"));
        } catch(HttpClientErrorException.NotFound ex) { return false; }
        catch(HttpClientErrorException.TooManyRequests ex) { throw new AppException(HttpStatus.TOO_MANY_REQUESTS,"Alcanzaste el limite de verificacion. Espera antes de reenviar."); }
        catch(RestClientException ex) { throw unavailable(); }
    }
    private AppException unavailable() {return new AppException(HttpStatus.SERVICE_UNAVAILABLE,"El proveedor no pudo procesar el SMS. Revisa la cuenta, el saldo y los numeros autorizados.");}
}
