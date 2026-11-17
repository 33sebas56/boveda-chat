package com.seguridad.bovedachat;

import com.seguridad.bovedachat.sms.VerificationGateway;
import com.seguridad.bovedachat.repository.AccountRepository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "app.sms.enabled=true",
        "app.sms.account-sid=AC00000000000000000000000000000000",
        "app.sms.auth-token=unit-test-token",
        "app.sms.service-sid=VA00000000000000000000000000000000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SmsRegistrationIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AccountRepository users;

    @MockitoBean
    VerificationGateway gateway;

    @Test
    void phoneMustBeApprovedBeforeAccountCanLogin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String phone = "+573001234567";

        mvc.perform(
                post("/registro")
                        .session(session)
                        .with(csrf())
                        .param("displayName", "SMS Persona")
                        .param("username", "sms_persona")
                        .param("password", "Mi contrasena larga 123")
                        .param("confirmation", "Mi contrasena larga 123")
                        .param("phone", phone)
        ).andExpect(redirectedUrl("/verificar"));

        verify(gateway).send(phone);

        assertThat(
                users.findByUsername("sms_persona")
                        .orElseThrow()
                        .getEnabled()
        ).isFalse();

        mvc.perform(
                        get("/verificar")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Revisa tu celular")
                ));

        mvc.perform(
                post("/login")
                        .with(csrf())
                        .param("username", "sms_persona")
                        .param("password", "Mi contrasena larga 123")
        ).andExpect(redirectedUrl("/login?error"));

        when(gateway.check(phone, "111111")).thenReturn(false);

        mvc.perform(
                post("/verificar")
                        .session(session)
                        .with(csrf())
                        .param("code", "111111")
        ).andExpect(redirectedUrl("/verificar"));

        assertThat(
                users.findByUsername("sms_persona")
                        .orElseThrow()
                        .getEnabled()
        ).isFalse();

        when(gateway.check(phone, "123456")).thenReturn(true);

        mvc.perform(
                post("/verificar")
                        .session(session)
                        .with(csrf())
                        .param("code", "123456")
        ).andExpect(redirectedUrl("/login?verificado"));

        var account = users.findByUsername("sms_persona").orElseThrow();

        assertThat(account.getEnabled()).isTrue();
        assertThat(account.getPhoneVerified()).isTrue();

        mvc.perform(
                post("/login")
                        .with(csrf())
                        .param("username", phone)
                        .param("password", "Mi contrasena larga 123")
        ).andExpect(redirectedUrl("/app"));
    }
}