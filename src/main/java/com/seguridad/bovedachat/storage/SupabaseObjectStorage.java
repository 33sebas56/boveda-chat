package com.seguridad.bovedachat.storage;
import com.seguridad.bovedachat.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.http.*;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.Map;
@Component
@ConditionalOnProperty(name="app.storage.mode",havingValue="supabase")
public class SupabaseObjectStorage implements ObjectStorage {
    private final RestClient client;
    private final String bucket;
    public SupabaseObjectStorage(AppProperties props,RestClient.Builder builder) {
        var config=props.storage(); bucket=config.bucket();
        if(config.url()==null || !"https".equals(URI.create(config.url()).getScheme()) || config.secret()==null || config.secret().isBlank())
            throw new IllegalStateException("Configura SUPABASE_URL https y SUPABASE_SECRET_KEY en el servidor.");
        if(bucket==null || !bucket.matches("[a-z0-9-]{3,63}")) throw new IllegalStateException("El nombre del bucket debe usar minusculas, numeros y guiones.");
        var configured=builder.clone().baseUrl(config.url().replaceAll("/+$","")+"/storage/v1").defaultHeader("apikey",config.secret());
        // New opaque sb_secret keys belong in apikey; legacy service_role keys are JWTs.
        if(!config.secret().startsWith("sb_secret_")) configured.defaultHeader(HttpHeaders.AUTHORIZATION,"Bearer "+config.secret());
        client=configured.build();
    }
    @PostConstruct
    public void ensurePrivateBucket() {
        try {
            Map<?,?> response=client.get().uri("/bucket/{bucket}",bucket).retrieve().body(Map.class);
            if(response==null || !Boolean.FALSE.equals(response.get("public"))) throw new IllegalStateException("El bucket de Boveda Chat debe ser privado. No uses uno publico.");
        } catch(HttpClientErrorException.NotFound ex) {
            try { client.post().uri("/bucket").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("id",bucket,"name",bucket,"public",false,"file_size_limit",5243000)).retrieve().toBodilessEntity(); }
            catch(RestClientException failed) { throw unavailable(); }
        } catch(RestClientException ex) { throw unavailable(); }
    }
    private void validate(String key) { if(!key.matches("[0-9a-f-]{36}\\.enc")) throw new IllegalArgumentException("Objeto invalido."); }
    public void put(String key,byte[] encrypted) {
        validate(key);
        try { client.post().uri("/object/{bucket}/{key}",bucket,key).contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header("x-upsert","false").body(encrypted).retrieve().toBodilessEntity(); }
        catch(RestClientException ex) { throw unavailable(); }
    }
    public byte[] get(String key) {
        validate(key);
        try { byte[] bytes=client.get().uri("/object/authenticated/{bucket}/{key}",bucket,key).retrieve().body(byte[].class);
            if(bytes==null) throw unavailable(); return bytes; }
        catch(RestClientException ex) { throw unavailable(); }
    }
    public void delete(String key) {
        validate(key);
        try { client.delete().uri("/object/{bucket}/{key}",bucket,key).retrieve().toBodilessEntity(); }
        catch(RestClientException ex) { throw unavailable(); }
    }
    private IllegalStateException unavailable() { return new IllegalStateException("No se pudo acceder a Supabase Storage. Revisa URL, clave secreta y bucket privado."); }
}
