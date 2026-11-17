package com.seguridad.bovedachat.web;
import com.seguridad.bovedachat.repository.AccountRepository;
import com.seguridad.bovedachat.security.VaultPrincipal;
import com.seguridad.bovedachat.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
@Controller
public class DownloadController {
    private final AccountRepository users; private final FileService files;
    public DownloadController(AccountRepository users,FileService files){this.users=users;this.files=files;}
    @GetMapping("/documentos/{id}/descargar")
    ResponseEntity<byte[]> download(@PathVariable UUID id,@AuthenticationPrincipal VaultPrincipal principal){
        var actor=users.findById(principal.id()).filter(a->a.getEnabled()).orElseThrow(AppException::missing);
        var file=files.download(id,actor,false);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.name(),StandardCharsets.UTF_8).build().toString())
            .header(HttpHeaders.CACHE_CONTROL,"no-store").contentType(MediaType.parseMediaType(file.mime())).body(file.bytes());
    }
}
