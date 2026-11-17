package com.seguridad.bovedachat.web;
import com.seguridad.bovedachat.config.AppProperties;
import com.seguridad.bovedachat.domain.Account;
import com.seguridad.bovedachat.repository.AccountRepository;
import com.seguridad.bovedachat.security.VaultPrincipal;
import com.seguridad.bovedachat.service.*;
import com.seguridad.bovedachat.web.Views.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.*;
@RestController @RequestMapping("/api")
public class ApiController {
    private final AccountRepository users;private final ChatService chats;private final FileService files;private final RateLimiter rates;private final AppProperties props;
    public ApiController(AccountRepository users,ChatService chats,FileService files,RateLimiter rates,AppProperties props){this.users=users;this.chats=chats;this.files=files;this.rates=rates;this.props=props;}
    private Account actor(VaultPrincipal principal) {return users.findById(principal.id()).filter(Account::getEnabled).orElseThrow(AppException::missing);}
    @GetMapping("/me") Me me(@AuthenticationPrincipal VaultPrincipal p){Account a=actor(p);return new Me(a.getId(),a.getUsername(),a.getDisplayName(),a.getRole(),a.getPhoneVerified(),props.sms().enabled());}
    @GetMapping("/people") List<Person> people(@RequestParam(defaultValue="") String q,@AuthenticationPrincipal VaultPrincipal p){rates.require("search:"+p.id(),30,Duration.ofMinutes(1));return chats.search(q,actor(p));}
    @GetMapping("/conversations") List<ConversationView> conversations(@AuthenticationPrincipal VaultPrincipal p){return chats.list(actor(p));}
    public record StartRequest(@NotBlank @Pattern(regexp="@?[a-z0-9_]{3,24}") String username){}
    @PostMapping("/conversations") ConversationView start(@Valid @RequestBody StartRequest body,@AuthenticationPrincipal VaultPrincipal p){rates.require("start:"+p.id(),20,Duration.ofMinutes(1));return chats.start(body.username(),actor(p));}
    @GetMapping("/conversations/{id}/messages") MessagePage history(@PathVariable UUID id,@RequestParam(defaultValue="0") long after,@RequestParam(defaultValue="0") long before,@AuthenticationPrincipal VaultPrincipal p){return chats.history(id,actor(p),after,before);}
    public record SendRequest(@Size(max=4000) String text,UUID attachmentId){}
    @PostMapping("/conversations/{id}/messages") MessageView send(@PathVariable UUID id,@Valid @RequestBody SendRequest body,@AuthenticationPrincipal VaultPrincipal p){rates.require("send:"+p.id(),40,Duration.ofMinutes(1));return chats.send(id,actor(p),body.text(),body.attachmentId());}
    public record ReadRequest(@Min(0) long lastSeenId){}
    @PostMapping("/conversations/{id}/read") ResponseEntity<Void> read(@PathVariable UUID id,@Valid @RequestBody ReadRequest body,@AuthenticationPrincipal VaultPrincipal p){chats.read(id,actor(p),body.lastSeenId());return ResponseEntity.noContent().build();}
    @GetMapping("/files") Vault vault(@AuthenticationPrincipal VaultPrincipal p){return files.list(actor(p));}
    @PostMapping(value="/files",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) FileView upload(@RequestPart("file") MultipartFile file,@AuthenticationPrincipal VaultPrincipal p){rates.require("upload:"+p.id(),10,Duration.ofMinutes(1));return files.upload(actor(p),file);}
    public record RevokeRequest(boolean revoked){}
    @PatchMapping("/files/{id}/sharing") FileView sharing(@PathVariable UUID id,@RequestBody RevokeRequest body,@AuthenticationPrincipal VaultPrincipal p){return files.revoke(id,actor(p),body.revoked());}
    @DeleteMapping("/files/{id}") ResponseEntity<Void> delete(@PathVariable UUID id,@AuthenticationPrincipal VaultPrincipal p){files.delete(id,actor(p));return ResponseEntity.noContent().build();}
    @GetMapping("/files/{id}/download") ResponseEntity<byte[]> download(@PathVariable UUID id,@AuthenticationPrincipal VaultPrincipal p){return content(files.download(id,actor(p),false),false);}
    @GetMapping("/files/{id}/preview") ResponseEntity<byte[]> preview(@PathVariable UUID id,@AuthenticationPrincipal VaultPrincipal p){return content(files.download(id,actor(p),true),true);}
    private ResponseEntity<byte[]> content(FileService.Download file,boolean inline){
        ContentDisposition disposition=(inline?ContentDisposition.inline():ContentDisposition.attachment()).filename(file.name(),StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,disposition.toString()).header(HttpHeaders.CACHE_CONTROL,"no-store")
            .header("X-Content-Type-Options","nosniff").contentType(MediaType.parseMediaType(file.mime())).body(file.bytes());
    }
}
