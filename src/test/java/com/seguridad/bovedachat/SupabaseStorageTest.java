package com.seguridad.bovedachat;
import com.seguridad.bovedachat.config.AppProperties;
import com.seguridad.bovedachat.storage.SupabaseObjectStorage;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.assertj.core.api.Assertions.*;
class SupabaseStorageTest {
    private MockRestServiceServer server;private SupabaseObjectStorage storage;private final String root="https://unit.supabase.co/storage/v1";private final String key="00000000-0000-0000-0000-000000000001.enc";
    @BeforeEach void setup(){var builder=RestClient.builder();server=MockRestServiceServer.bindTo(builder).build();var props=new AppProperties(null,new AppProperties.Storage("supabase","","https://unit.supabase.co","unit-test-secret","boveda-chat"),null,null,null);storage=new SupabaseObjectStorage(props,builder);}
    @AfterEach void verified(){server.verify();}
    @Test void refusesPublicBucket(){server.expect(requestTo(root+"/bucket/boveda-chat")).andRespond(withSuccess("{\"public\":true}",MediaType.APPLICATION_JSON));assertThatThrownBy(storage::ensurePrivateBucket).isInstanceOf(IllegalStateException.class);}
    @Test void acceptsPrivateBucket(){server.expect(requestTo(root+"/bucket/boveda-chat")).andRespond(withSuccess("{\"public\":false}",MediaType.APPLICATION_JSON));storage.ensurePrivateBucket();}
    @Test void createsDedicatedPrivateBucketIfMissing(){server.expect(requestTo(root+"/bucket/boveda-chat")).andRespond(withResourceNotFound());server.expect(requestTo(root+"/bucket")).andExpect(content().json("{\"id\":\"boveda-chat\",\"name\":\"boveda-chat\",\"public\":false,\"file_size_limit\":5243000}")).andRespond(withSuccess());storage.ensurePrivateBucket();}
    @Test void uploadsOpaqueBytesAndDownloadsUsingAuthenticatedRoute(){byte[] bytes=new byte[]{1,2,3};server.expect(requestTo(root+"/object/boveda-chat/"+key)).andExpect(method(HttpMethod.POST)).andExpect(header("x-upsert","false")).andExpect(content().bytes(bytes)).andRespond(withSuccess());server.expect(requestTo(root+"/object/authenticated/boveda-chat/"+key)).andRespond(withSuccess(bytes,MediaType.APPLICATION_OCTET_STREAM));storage.put(key,bytes);assertThat(storage.get(key)).isEqualTo(bytes);}
    @Test void opaqueSecretUsesApikeyRatherThanPretendingToBeUserJwt(){
        var builder=RestClient.builder();server=MockRestServiceServer.bindTo(builder).build();
        storage=new SupabaseObjectStorage(new AppProperties(null,new AppProperties.Storage("supabase","","https://unit.supabase.co","sb_secret_unit-test-only","boveda-chat"),null,null,null),builder);
        server.expect(requestTo(root+"/bucket/boveda-chat")).andExpect(header("apikey","sb_secret_unit-test-only")).andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION)).andRespond(withSuccess("{\"public\":false}",MediaType.APPLICATION_JSON));storage.ensurePrivateBucket();
    }
}
