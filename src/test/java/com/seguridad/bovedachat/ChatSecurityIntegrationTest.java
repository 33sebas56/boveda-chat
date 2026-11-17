package com.seguridad.bovedachat;
import com.fasterxml.jackson.databind.*;
import com.seguridad.bovedachat.domain.*;
import com.seguridad.bovedachat.repository.*;
import com.seguridad.bovedachat.security.VaultPrincipal;
import com.seguridad.bovedachat.service.*;
import com.seguridad.bovedachat.web.RegistrationForm;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ChatSecurityIntegrationTest {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired RegistrationService registration;@Autowired AccountRepository users;@Autowired MessageRepository messages;@Autowired FileRepository files;@Autowired AuditRepository events;@Autowired ChatService chats;
    private static final AtomicInteger serial=new AtomicInteger();
    private Account ana,juan,third;
    @BeforeEach void accounts(){int n=serial.incrementAndGet();ana=create("ana"+n);juan=create("juan"+n);third=create("tercero"+n);}
    Account create(String username){RegistrationForm form=new RegistrationForm();form.setUsername(username);form.setDisplayName("Persona "+username);form.setPassword("Una clave larga 123");form.setConfirmation("Una clave larga 123");return registration.register(form);}
    JsonNode parsed(MvcResult result)throws Exception{return json.readTree(result.getResponse().getContentAsString());}
    String conversation()throws Exception{return parsed(mvc.perform(post("/api/conversations").with(user(VaultPrincipal.from(ana))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username",juan.getUsername())))).andExpect(status().isOk()).andReturn()).get("id").asText();}
    String upload()throws Exception{var file=new MockMultipartFile("file","nota.txt","text/plain","Contenido de prueba".getBytes(StandardCharsets.UTF_8));return parsed(mvc.perform(multipart("/api/files").file(file).with(user(VaultPrincipal.from(ana))).with(csrf())).andExpect(status().isOk()).andReturn()).get("id").asText();}
    @Test void publicPagesRenderAndHeadersHardened()throws Exception{
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Entra a tu espacio"))).andExpect(header().string("X-Frame-Options","DENY")).andExpect(header().string("X-Content-Type-Options","nosniff")).andExpect(header().exists("Content-Security-Policy"));
        mvc.perform(get("/registro")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
    @Test void unauthenticatedApiIs401AndAppRedirects()throws Exception{mvc.perform(get("/api/conversations")).andExpect(status().isUnauthorized());mvc.perform(get("/app")).andExpect(status().is3xxRedirection());}
    @Test void csrfRequiredForChanges()throws Exception{mvc.perform(post("/api/conversations").with(user(VaultPrincipal.from(ana))).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"juan\"}")).andExpect(status().isForbidden());}
    @Test void realPasswordLoginWorksAndHashIsNotPlaintext()throws Exception{assertThat(ana.getPasswordHash()).startsWith("$2").doesNotContain("Una clave");mvc.perform(post("/login").with(csrf()).param("username",ana.getUsername()).param("password","Una clave larga 123")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app"));}
    @Test void registrationAndAppPagesActuallyRender()throws Exception{
        mvc.perform(post("/registro").with(csrf()).param("displayName","Nueva persona").param("username","registrada"+serial.incrementAndGet()).param("password","Segura y larga 456").param("confirmation","Segura y larga 456")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?registro"));
        mvc.perform(get("/app").with(user(VaultPrincipal.from(ana)))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Mi bóveda")));
    }
    @Test void thirdPartyCannotReadOrWriteConversation()throws Exception{
        String id=conversation();mvc.perform(get("/api/conversations/"+id+"/messages").with(user(VaultPrincipal.from(third)))).andExpect(status().isNotFound());
        mvc.perform(post("/api/conversations/"+id+"/messages").with(user(VaultPrincipal.from(third))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"intrusion\"}")).andExpect(status().isNotFound());
    }
    @Test void fileNeedsExplicitShareThenCanBeRevoked()throws Exception{
        String file=upload();String c=conversation();
        mvc.perform(get("/api/files/"+file+"/download").with(user(VaultPrincipal.from(juan)))).andExpect(status().isNotFound());
        mvc.perform(post("/api/conversations/"+c+"/messages").with(user(VaultPrincipal.from(ana))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("text","Para ti","attachmentId",file)))).andExpect(status().isOk());
        mvc.perform(get("/api/files/"+file+"/download").with(user(VaultPrincipal.from(juan)))).andExpect(status().isOk()).andExpect(content().bytes("Contenido de prueba".getBytes(StandardCharsets.UTF_8))).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get("/api/files/"+file+"/download").with(user(VaultPrincipal.from(third)))).andExpect(status().isNotFound());
        mvc.perform(patch("/api/files/"+file+"/sharing").with(user(VaultPrincipal.from(ana))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"revoked\":true}")).andExpect(status().isOk());
        mvc.perform(get("/api/files/"+file+"/download").with(user(VaultPrincipal.from(juan)))).andExpect(status().isNotFound());
        mvc.perform(get("/api/files/"+file+"/download").with(user(VaultPrincipal.from(ana)))).andExpect(status().isOk());
    }
    @Test void recipientCannotReshareSomeoneElsesAttachment()throws Exception{
        String f=upload(),c=conversation();mvc.perform(post("/api/conversations/"+c+"/messages").with(user(VaultPrincipal.from(juan))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("attachmentId",f)))).andExpect(status().isNotFound());
    }
    @Test void messagesStoredEncryptedAndReadReceiptAdvances()throws Exception{
        String c=conversation();String secret="Hola, este mensaje debe quedar cifrado";
        JsonNode sent=parsed(mvc.perform(post("/api/conversations/"+c+"/messages").with(user(VaultPrincipal.from(ana))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("text",secret)))).andExpect(status().isOk()).andReturn());
        ChatMessage stored=messages.findById(sent.get("id").asLong()).orElseThrow();assertThat(stored.getCiphertext()).doesNotContain(secret);
        mvc.perform(get("/api/conversations/"+c+"/messages").with(user(VaultPrincipal.from(juan)))).andExpect(status().isOk()).andExpect(jsonPath("$.messages[0].text").value(secret));
        mvc.perform(post("/api/conversations/"+c+"/read").with(user(VaultPrincipal.from(juan))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("lastSeenId",sent.get("id").asLong())))).andExpect(status().isNoContent());
        mvc.perform(get("/api/conversations/"+c+"/messages").with(user(VaultPrincipal.from(ana)))).andExpect(jsonPath("$.peerReadUpTo").value(sent.get("id").asLong()));
    }
    @Test void dangerousUploadRejectedAndVaultIsPrivate()throws Exception{
        mvc.perform(multipart("/api/files").file(new MockMultipartFile("file","evil.png","image/png","<script>alert(1)</script>".getBytes())).with(user(VaultPrincipal.from(ana))).with(csrf())).andExpect(status().isBadRequest());
        upload();mvc.perform(get("/api/files").with(user(VaultPrincipal.from(juan)))).andExpect(jsonPath("$.files").isEmpty());
    }
    @Test void regularUserCannotSeeAuditOrPromoteThroughRegistration()throws Exception{
        mvc.perform(get("/admin/auditoria").with(user(VaultPrincipal.from(ana)))).andExpect(status().isForbidden());assertThat(ana.getRole()).isEqualTo("USER");
        ana.setRole("ADMIN");users.save(ana);mvc.perform(get("/admin/auditoria").with(user(VaultPrincipal.from(ana)))).andExpect(status().isOk());
    }
    @Test void missingAndUnauthorizedFilesLookTheSameAndDenialIsAudited()throws Exception{
        long before=events.count();String f=upload();mvc.perform(get("/api/files/"+f+"/download").with(user(VaultPrincipal.from(third)))).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("El recurso no existe o no esta disponible para tu cuenta."));
        mvc.perform(get("/api/files/"+UUID.randomUUID()+"/download").with(user(VaultPrincipal.from(third)))).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("El recurso no existe o no esta disponible para tu cuenta."));assertThat(events.count()).isGreaterThan(before);
    }
    @Test void loginRateLimitEventuallyReturns429()throws Exception{
        String target="missing"+serial.incrementAndGet();for(int i=0;i<10;i++)mvc.perform(post("/login").with(csrf()).param("username",target).param("password","wrong")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/login").with(csrf()).param("username",target).param("password","wrong")).andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
    }
    @Test void sqlLookingUsernameDoesNotAuthenticate()throws Exception{mvc.perform(post("/login").with(csrf()).param("username","' OR '1'='1").param("password","anything")).andExpect(redirectedUrl("/login?error"));}
    @Test void messagesArePaginatedWithoutLosingOrder()throws Exception{
        String c=conversation();for(int i=0;i<3;i++)mvc.perform(post("/api/conversations/"+c+"/messages").with(user(VaultPrincipal.from(ana))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("text","mensaje "+i)))).andExpect(status().isOk());
        JsonNode page=parsed(mvc.perform(get("/api/conversations/"+c+"/messages").with(user(VaultPrincipal.from(juan)))).andReturn());long first=page.get("messages").get(0).get("id").asLong();
        mvc.perform(get("/api/conversations/"+c+"/messages").param("after",String.valueOf(first)).with(user(VaultPrincipal.from(juan)))).andExpect(jsonPath("$.messages.length()").value(2));
    }
    @Test void largeHistoryCanLoadOlderPageWithoutDuplicates()throws Exception{
        String c=conversation();for(int i=0;i<55;i++)chats.send(UUID.fromString(c),ana,"orden "+i,null);
        JsonNode latest=parsed(mvc.perform(get("/api/conversations/"+c+"/messages").with(user(VaultPrincipal.from(juan)))).andExpect(status().isOk()).andReturn());
        assertThat(latest.get("messages").size()).isEqualTo(50);assertThat(latest.get("hasOlder").asBoolean()).isTrue();assertThat(latest.get("messages").get(0).get("text").asText()).isEqualTo("orden 5");
        long before=latest.get("messages").get(0).get("id").asLong();
        mvc.perform(get("/api/conversations/"+c+"/messages").param("before",String.valueOf(before)).with(user(VaultPrincipal.from(juan))))
            .andExpect(jsonPath("$.messages.length()").value(5)).andExpect(jsonPath("$.messages[0].text").value("orden 0")).andExpect(jsonPath("$.hasOlder").value(false));
    }
    @Test void uploadedFileCannotExceedLimit()throws Exception{
        mvc.perform(multipart("/api/files").file(new MockMultipartFile("file","grande.txt","text/plain",new byte[5242881])).with(user(VaultPrincipal.from(ana))).with(csrf())).andExpect(status().isBadRequest());
    }
    @Test void onlyOwnerCanDeleteUnsharedFile()throws Exception{
        String f=upload();mvc.perform(delete("/api/files/"+f).with(user(VaultPrincipal.from(third))).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/files/"+f).with(user(VaultPrincipal.from(ana))).with(csrf())).andExpect(status().isNoContent());
        assertThat(files.findById(UUID.fromString(f))).isEmpty();
    }
    @Test void administratorRoleDoesNotGrantAccessToOtherFiles()throws Exception{
        third.setRole("ADMIN");users.save(third);String f=upload();mvc.perform(get("/api/files/"+f+"/download").with(user(VaultPrincipal.from(third)))).andExpect(status().isNotFound());
    }
    @Test void copiedDownloadLinkShowsFriendlyHtmlOnDenial()throws Exception{
        String f=upload();mvc.perform(get("/documentos/"+f+"/descargar").with(user(VaultPrincipal.from(third)))).andExpect(status().isNotFound()).andExpect(content().string(org.hamcrest.Matchers.containsString("No pudimos abrir esto.")));
    }
    @Test void usernameSearchTreatsUnderscoreLiterally()throws Exception{
        int n=serial.incrementAndGet();String prefix="literal"+n+"_";Account wanted=create(prefix+"uno");create("literal"+n+"xuno");
        mvc.perform(get("/api/people").param("q",prefix).with(user(VaultPrincipal.from(ana)))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].username").value(wanted.getUsername()));
    }
}
