package com.seguridad.bovedachat.security;
import com.seguridad.bovedachat.service.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean SecurityFilterChain web(HttpSecurity http,AuditService audit,RateLimiter limiter) throws Exception {
        var entries=new java.util.LinkedHashMap<RequestMatcher,AuthenticationEntryPoint>();
        entries.put(request->request.getRequestURI().startsWith(request.getContextPath()+"/api/"),(req,res,ex)->{res.setStatus(401);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"Tu sesion termino. Vuelve a iniciar sesion.\"}");});
        var entryPoint=new DelegatingAuthenticationEntryPoint(entries);
        entryPoint.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"));
        http.authorizeHttpRequests(auth->auth
            .requestMatchers("/","/login","/registro","/verificar","/verificar/**","/css/**","/js/**","/icons.svg","/favicon.svg","/error","/actuator/health").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN").anyRequest().authenticated());
        http.csrf(Customizer.withDefaults());
        http.formLogin(form->form.loginPage("/login").loginProcessingUrl("/login")
            .successHandler((request,response,authentication)->{
                audit.record(authentication.getName(),"LOGIN_OK","Inicio de sesion correcto"); response.sendRedirect("/app");
            }).failureHandler((request,response,error)->{
                audit.record(request.getParameter("username"),"LOGIN_ERROR","Credenciales incorrectas o cuenta pendiente"); response.sendRedirect("/login?error");
            }).permitAll());
        http.logout(logout->logout.logoutUrl("/logout").logoutSuccessUrl("/login?salida")
            .invalidateHttpSession(true).deleteCookies("JSESSIONID")
            .addLogoutHandler((request,response,authentication)->{ if(authentication!=null) audit.record(authentication.getName(),"LOGOUT","Sesion cerrada"); }));
        http.exceptionHandling(errors->errors
            .authenticationEntryPoint(entryPoint)
            .accessDeniedHandler((req,res,ex)->{
                if(req.getRequestURI().startsWith(req.getContextPath()+"/api/")) {res.setStatus(403);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"No se pudo autorizar la solicitud. Actualiza la pagina.\"}");}
                else res.sendError(403);
            }));
        http.headers(headers->headers
            .contentSecurityPolicy(csp->csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' blob: data:; connect-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'"))
            .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
            .referrerPolicy(ref->ref.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
            .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy","camera=(), microphone=(), geolocation=()")));
        http.addFilterBefore(new RequestRateFilter(limiter),UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
