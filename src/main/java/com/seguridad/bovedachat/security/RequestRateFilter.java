package com.seguridad.bovedachat.security;
import com.seguridad.bovedachat.service.RateLimiter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
public class RequestRateFilter extends OncePerRequestFilter {
    private final RateLimiter limiter;
    public RequestRateFilter(RateLimiter limiter) { this.limiter=limiter; }
    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String path=request.getRequestURI().substring(request.getContextPath().length()); boolean allowed=true;
        if(request.getMethod().equals("POST") && path.equals("/login")) {
            String login=LoginNames.normalize(request.getParameter("username"));
            if(login.length()>100) login=login.substring(0,100);
            allowed=limiter.allow("login-all",200,Duration.ofMinutes(1)) && limiter.allow("login:"+login,10,Duration.ofMinutes(5));
        } else if(request.getMethod().equals("POST") && (path.equals("/registro") || path.equals("/verificar/retomar"))) {
            allowed=limiter.allow("registration-all",30,Duration.ofHours(1)) && limiter.allow("registration-session:"+request.getSession().getId(),5,Duration.ofHours(1));
        }
        if(!allowed) {
            response.setStatus(429); response.setHeader("Retry-After","60"); response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Demasiados intentos. Espera unos minutos y vuelve a intentar."); return;
        }
        chain.doFilter(request,response);
    }
}
