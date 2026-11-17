package com.seguridad.bovedachat;
import com.seguridad.bovedachat.service.RateLimiter;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
class RateLimiterTest {
    @Test void clientsHaveIndependentQuotas(){var rate=new RateLimiter();assertThat(rate.allow("a",1,Duration.ofMinutes(1))).isTrue();assertThat(rate.allow("a",1,Duration.ofMinutes(1))).isFalse();assertThat(rate.allow("b",1,Duration.ofMinutes(1))).isTrue();}
    @Test void concurrentRequestsCannotExceedQuota() throws Exception {var rate=new RateLimiter();var accepted=new AtomicInteger();try(var executor=Executors.newFixedThreadPool(10)){for(int i=0;i<100;i++)executor.submit(()->{if(rate.allow("a",7,Duration.ofMinutes(1)))accepted.incrementAndGet();});}assertThat(accepted.get()).isEqualTo(7);}
}
