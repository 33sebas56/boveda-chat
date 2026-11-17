package com.seguridad.bovedachat.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("app")
public record AppProperties(Crypto crypto, Storage storage, Sms sms, Bootstrap bootstrap, Limits limits) {
    public record Crypto(String key, String keyFile, boolean requireKey) {}
    public record Storage(String mode, String directory, String url, String secret, String bucket) {}
    public record Sms(boolean enabled, String accountSid, String authToken, String serviceSid, int dailyBudget) {}
    public record Bootstrap(String username, String password) {}
    public record Limits(long fileBytes, long accountBytes, int accountFiles) {}
}
