package com.seguridad.bovedachat.storage;
import com.seguridad.bovedachat.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.nio.file.*;
@Component
@ConditionalOnProperty(name="app.storage.mode",havingValue="local")
public class LocalObjectStorage implements ObjectStorage {
    private final Path root;
    public LocalObjectStorage(AppProperties props) { root=Path.of(props.storage().directory()).toAbsolutePath().normalize(); }
    private Path safe(String key) {
        if(!key.matches("[0-9a-f-]{36}\\.enc")) throw new IllegalArgumentException("Objeto invalido.");
        Path path=root.resolve(key).normalize();
        if(!path.startsWith(root)) throw new IllegalArgumentException("Ruta invalida.");
        return path;
    }
    public void put(String key,byte[] bytes) {
        try { Files.createDirectories(root); Files.write(safe(key),bytes,StandardOpenOption.CREATE_NEW); }
        catch(java.io.IOException ex) { throw new IllegalStateException("No se pudo guardar el archivo.",ex); }
    }
    public byte[] get(String key) {
        try { return Files.readAllBytes(safe(key)); }
        catch(java.io.IOException ex) { throw new IllegalStateException("No se pudo recuperar el archivo.",ex); }
    }
    public void delete(String key) {
        try { Files.deleteIfExists(safe(key)); }
        catch(java.io.IOException ex) { throw new IllegalStateException("No se pudo eliminar el objeto.",ex); }
    }
}
