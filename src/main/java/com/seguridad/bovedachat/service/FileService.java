package com.seguridad.bovedachat.service;
import com.seguridad.bovedachat.config.AppProperties;
import com.seguridad.bovedachat.domain.*;
import com.seguridad.bovedachat.repository.*;
import com.seguridad.bovedachat.storage.ObjectStorage;
import com.seguridad.bovedachat.web.Views.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.Instant;
import java.util.*;
@Service
public class FileService {
    private final FileRepository files; private final AccountRepository users; private final MessageRepository messages;
    private final ObjectStorage storage; private final CryptoService crypto; private final FileInspector inspector;
    private final AuditService audit; private final AppProperties props;
    public FileService(FileRepository files,AccountRepository users,MessageRepository messages,ObjectStorage storage,CryptoService crypto,FileInspector inspector,AuditService audit,AppProperties props) {
        this.files=files;this.users=users;this.messages=messages;this.storage=storage;this.crypto=crypto;this.inspector=inspector;this.audit=audit;this.props=props;
    }
    @Transactional(readOnly=true)
    public Vault list(Account actor) {
        return new Vault(files.findByOwner_IdOrderByCreatedAtDesc(actor.getId()).stream().map(f->view(f,actor)).toList(),files.usedBytes(actor.getId()),props.limits().accountBytes());
    }
    @Transactional
    public FileView upload(Account actor,MultipartFile upload) {
        if(upload==null || upload.isEmpty() || upload.getSize()>props.limits().fileBytes()) throw AppException.invalid("Selecciona un archivo de hasta 5 MB.");
        users.lockById(actor.getId()).orElseThrow(AppException::missing);
        if(files.countByOwner_Id(actor.getId())>=props.limits().accountFiles() || files.usedBytes(actor.getId())+upload.getSize()>props.limits().accountBytes())
            throw AppException.invalid("Alcanzaste la capacidad de tu boveda.");
        byte[] plain;
        try { plain=upload.getBytes(); } catch(java.io.IOException ex) { throw AppException.invalid("No se pudo leer el archivo."); }
        if(plain.length==0 || plain.length>props.limits().fileBytes()) throw AppException.invalid("Selecciona un archivo de hasta 5 MB.");
        var checked=inspector.inspect(upload.getOriginalFilename(),plain);
        StoredFile file=new StoredFile(); file.setId(UUID.randomUUID()); file.setOwner(actor);
        file.setOriginalName(checked.name()); file.setMime(checked.mime()); file.setSizeBytes(plain.length);
        file.setSha256(crypto.sha256(plain)); file.setObjectKey(file.getId()+".enc"); file.setCreatedAt(Instant.now());
        storage.put(file.getObjectKey(),crypto.encrypt(plain,"file:"+file.getId()));
        // Remove the object if the metadata transaction fails after the upload.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            public void afterCompletion(int status) {
                if(status!=STATUS_COMMITTED) { try { storage.delete(file.getObjectKey()); } catch(RuntimeException ignored) { /* Orphan can be cleaned by its UUID. */ } }
            }
        });
        files.saveAndFlush(file);
        audit.record(actor.getUsername(),"FILE_UPLOADED","Documento "+file.getId());
        return view(file,actor);
    }
    @Transactional(readOnly=true)
    public StoredFile ownedForShare(UUID id,Account actor) {
        StoredFile f=files.findById(id).orElseThrow(AppException::missing);
        if(!f.getOwner().getId().equals(actor.getId()) || f.getRevoked()) throw AppException.missing();
        return f;
    }
    @Transactional
    public FileView revoke(UUID id,Account actor,boolean revoked) {
        StoredFile f=files.findById(id).orElseThrow(AppException::missing);
        if(!f.getOwner().getId().equals(actor.getId())) throw AppException.missing();
        f.setRevoked(revoked);
        audit.record(actor.getUsername(),revoked?"FILE_REVOKED":"FILE_RESTORED","Documento "+id);
        return view(f,actor);
    }
    @Transactional
    public void delete(UUID id,Account actor) {
        StoredFile f=files.findById(id).orElseThrow(AppException::missing);
        if(!f.getOwner().getId().equals(actor.getId())) throw AppException.missing();
        if(messages.countByAttachment_Id(id)>0) throw AppException.invalid("Este archivo ya forma parte de una conversacion. Puedes retirar el acceso a nuevas descargas.");
        String objectKey=f.getObjectKey();
        files.delete(f);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            public void afterCommit() {
                try { storage.delete(objectKey); }
                catch(RuntimeException ex) { org.slf4j.LoggerFactory.getLogger(FileService.class).warn("Objeto cifrado pendiente de limpieza: {}",objectKey); }
            }
        });
        audit.record(actor.getUsername(),"FILE_DELETED","Documento "+id);
    }
    public record Download(String name,String mime,byte[] bytes) {}
    @Transactional
    public Download download(UUID id,Account actor,boolean preview) {
        StoredFile f=files.findById(id).orElse(null);
        boolean owner=f!=null && f.getOwner().getId().equals(actor.getId());
        if(f==null || (!owner && (f.getRevoked() || messages.authorizedShares(id,actor.getId())==0))) {
            audit.denied(actor.getUsername(),"DOWNLOAD_DENIED","Recurso inexistente o sin permiso"); throw AppException.missing();
        }
        if(preview && !f.getMime().startsWith("image/")) throw AppException.missing();
        byte[] encrypted=storage.get(f.getObjectKey());
        if(encrypted.length!=f.getSizeBytes()+29) throw new AppException(org.springframework.http.HttpStatus.CONFLICT,"El archivo almacenado esta incompleto.");
        byte[] plain=crypto.decrypt(encrypted,"file:"+f.getId());
        if(!java.security.MessageDigest.isEqual(crypto.sha256(plain).getBytes(java.nio.charset.StandardCharsets.US_ASCII),f.getSha256().getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
            throw new AppException(org.springframework.http.HttpStatus.CONFLICT,"La huella del archivo no coincide.");
        if(!preview) audit.record(actor.getUsername(),"FILE_DOWNLOADED","Documento "+id);
        return new Download(f.getOriginalName(),f.getMime(),plain);
    }
    public FileView view(StoredFile f,Account actor) {
        boolean owner=f.getOwner().getId().equals(actor.getId());
        return new FileView(f.getId(),f.getOriginalName(),f.getMime(),f.getSizeBytes(),f.getSha256(),f.getRevoked(),owner?messages.countByAttachment_Id(f.getId()):0,f.getCreatedAt(),owner);
    }
}
