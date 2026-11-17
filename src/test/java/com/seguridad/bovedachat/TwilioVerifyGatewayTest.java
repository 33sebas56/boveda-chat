package com.seguridad.bovedachat;

import com.seguridad.bovedachat.config.AppProperties;
import com.seguridad.bovedachat.service.AppException;
import com.seguridad.bovedachat.sms.TwilioVerifyGateway;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.assertj.core.api.Assertions.*;

class TwilioVerifyGatewayTest {

    private MockRestServiceServer server;
    private TwilioVerifyGateway gateway;

    private final String service = "VA" + "0".repeat(32);

    @BeforeEach
    void setup() {
        var builder = RestClient.builder();

        server = MockRestServiceServer.bindTo(builder).build();

        var props = new AppProperties(
                null,
                null,
                new AppProperties.Sms(
                        true,
                        "AC" + "0".repeat(32),
                        "unit-test-token",
                        service,
                        20
                ),
                null,
                null
        );

        gateway = new TwilioVerifyGateway(props, builder);
    }

    @AfterEach
    void checked() {
        server.verify();
    }

    @Test
    void sendsActualVerifyApiContract() {
        server.expect(
                        requestTo(
                                "https://verify.twilio.com/v2/Services/"
                                        + service
                                        + "/Verifications"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        header(
                                HttpHeaders.AUTHORIZATION,
                                org.hamcrest.Matchers.startsWith("Basic ")
                        )
                )
                .andExpect(
                        content().string(
                                org.hamcrest.Matchers.containsString(
                                        "To=%2B573001234567"
                                )
                        )
                )
                .andRespond(
                        withSuccess(
                                "{\"status\":\"pending\"}",
                                MediaType.APPLICATION_JSON
                        )
                );

        gateway.send("+573001234567");
    }

    @Test
    void onlyApprovedIsAccepted() {
        server.expect(
                        requestTo(
                                "https://verify.twilio.com/v2/Services/"
                                        + service
                                        + "/VerificationCheck"
                        )
                )
                .andRespond(
                        withSuccess(
                                "{\"status\":\"pending\"}",
                                MediaType.APPLICATION_JSON
                        )
                );

        assertThat(
                gateway.check("+573001234567", "123456")
        ).isFalse();
    }

    @Test
    void approvedWorks() {
        server.expect(anything())
                .andRespond(
                        withSuccess(
                                "{\"status\":\"approved\"}",
                                MediaType.APPLICATION_JSON
                        )
                );

        assertThat(
                gateway.check("+573001234567", "123456")
        ).isTrue();
    }

    @Test
    void expiredCodeFails() {
        server.expect(anything())
                .andRespond(withResourceNotFound());

        assertThat(
                gateway.check("+573001234567", "123456")
        ).isFalse();
    }

    @Test
    void providerFailureIsNotPretendedSuccess() {
        server.expect(anything())
                .andRespond(
                        withStatus(HttpStatus.UNAUTHORIZED)
                                .body("sensitive provider body")
                );

        assertThatThrownBy(
                () -> gateway.send("+573001234567")
        )
                .isInstanceOf(AppException.class)
                .hasMessageNotContaining("sensitive");
    }
}