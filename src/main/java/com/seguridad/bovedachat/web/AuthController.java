package com.seguridad.bovedachat.web;
import com.seguridad.bovedachat.config.AppProperties;
import com.seguridad.bovedachat.domain.Account;
import com.seguridad.bovedachat.service.*;
import com.seguridad.bovedachat.sms.SmsService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
@Controller
public class AuthController {
    private static final String PENDING="pendingVerification";
    private record Pending(UUID id,Instant expires) implements Serializable {}
    private final RegistrationService registration;private final SmsService sms;private final AppProperties props;
    public AuthController(RegistrationService registration,SmsService sms,AppProperties props){this.registration=registration;this.sms=sms;this.props=props;}
    @ModelAttribute("smsEnabled") boolean smsEnabled(){return props.sms().enabled();}
    @GetMapping("/") String index(){return "redirect:/app";}
    @GetMapping("/login") String login(){return "login";}
    @GetMapping("/registro") String register(Model model){model.addAttribute("form",new RegistrationForm());return "register";}
    @PostMapping("/registro") String register(@Valid @ModelAttribute("form") RegistrationForm form,BindingResult errors,Model model,HttpSession session,RedirectAttributes redirect) {
        if(errors.hasErrors()) return "register";
        Account account;
        try {account=registration.register(form);}
        catch(AppException ex){model.addAttribute("error",ex.getMessage());return "register";}
        catch(org.springframework.dao.DataIntegrityViolationException ex){model.addAttribute("error","Ya existe una cuenta con esos datos.");return "register";}
        if(!props.sms().enabled()) return "redirect:/login?registro";
        session.setAttribute(PENDING,new Pending(account.getId(),Instant.now().plusSeconds(1800)));
        try {sms.send(account);redirect.addFlashAttribute("notice","Enviamos un codigo al celular que registraste.");}
        catch(AppException ex){redirect.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/verificar";
    }
    @GetMapping("/verificar") String verify(Model model,HttpSession session) {
        Account account=registration.pending(pending(session).id());
        String phone=account.getPhone();model.addAttribute("maskedPhone","•••• "+phone.substring(Math.max(0,phone.length()-4)));return "verify";
    }
    @PostMapping("/verificar") String verify(@RequestParam String code,HttpSession session,RedirectAttributes redirect) {
        Pending pending=pending(session);Account account=registration.pending(pending.id());
        try {
            if(!sms.check(account,code)){redirect.addFlashAttribute("error","El codigo no es valido o ya vencio.");return "redirect:/verificar";}
            registration.activate(account.getId());session.removeAttribute(PENDING);return "redirect:/login?verificado";
        } catch(AppException ex){redirect.addFlashAttribute("error",ex.getMessage());return "redirect:/verificar";}
    }
    @PostMapping("/verificar/reenviar") String resend(HttpSession session,RedirectAttributes redirect) {
        try {sms.send(registration.pending(pending(session).id()));redirect.addFlashAttribute("notice","Solicitamos otro codigo. Revisa tu celular.");}
        catch(AppException ex){redirect.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/verificar";
    }
    @GetMapping("/verificar/retomar") String resume(){return "resume";}
    @PostMapping("/verificar/retomar") String resume(@RequestParam String username,@RequestParam String password,HttpSession session,RedirectAttributes redirect) {
        try {
            Account a=registration.resume(username,password);session.setAttribute(PENDING,new Pending(a.getId(),Instant.now().plusSeconds(1800)));
            try {sms.send(a);}catch(AppException ex){redirect.addFlashAttribute("error",ex.getMessage());}
            return "redirect:/verificar";
        }catch(AppException ex){redirect.addFlashAttribute("error",ex.getMessage());return "redirect:/verificar/retomar";}
    }
    private Pending pending(HttpSession session) {
        Object value=session.getAttribute(PENDING);
        if(!(value instanceof Pending pending) || !pending.expires().isAfter(Instant.now())) throw AppException.invalid("La verificacion pendiente termino. Retomala desde el inicio de sesion.");
        return pending;
    }
}
