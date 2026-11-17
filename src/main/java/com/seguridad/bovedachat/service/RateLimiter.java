package com.seguridad.bovedachat.service;
import com.github.benmanes.caffeine.cache.*;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.concurrent.atomic.AtomicBoolean;
@Service
public class RateLimiter {
    private record Window(Instant end,int count) {}
    private final Cache<String,Window> entries=Caffeine.newBuilder().maximumSize(20000).expireAfterAccess(Duration.ofHours(1)).build();
    public boolean allow(String key,int limit,Duration duration) {
        AtomicBoolean allowed=new AtomicBoolean(false);
        Instant now=Instant.now();
        entries.asMap().compute(key,(k,old)-> {
            Window current=old==null || !old.end().isAfter(now) ? new Window(now.plus(duration),0) : old;
            if(current.count()>=limit) return current;
            allowed.set(true); return new Window(current.end(),current.count()+1);
        });
        return allowed.get();
    }
    public void require(String key,int limit,Duration duration) {
        if(!allow(key,limit,duration)) throw new AppException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,"Demasiados intentos. Espera un momento y vuelve a intentar.");
    }
}
