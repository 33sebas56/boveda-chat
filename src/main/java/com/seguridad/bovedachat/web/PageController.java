package com.seguridad.bovedachat.web;
import com.seguridad.bovedachat.repository.AuditRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class PageController {
    private final AuditRepository audit;
    public PageController(AuditRepository audit){this.audit=audit;}
    @GetMapping("/app") String app(){return "app";}
    @GetMapping("/admin/auditoria") String audit(Model model){model.addAttribute("events",audit.findTop200ByOrderByIdDesc());return "audit";}
}
