package com.seguridad.bovedachat.web;
import com.seguridad.bovedachat.service.AppException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.Map;
@ControllerAdvice
public class ErrorAdvice {
    @ExceptionHandler(AppException.class)
    Object app(AppException ex,HttpServletRequest req){return response(ex.status(),ex.getMessage(),req);}
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    Object size(MaxUploadSizeExceededException ex,HttpServletRequest req){return response(HttpStatus.PAYLOAD_TOO_LARGE,"El archivo supera el limite de 5 MB.",req);}
    @ExceptionHandler({MethodArgumentNotValidException.class,org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    Object invalid(Exception ex,HttpServletRequest req){return response(HttpStatus.BAD_REQUEST,"Revisa los datos de la solicitud.",req);}
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    Object duplicate(Exception ex,HttpServletRequest req){return response(HttpStatus.CONFLICT,"La operacion coincide con otro cambio. Actualiza la pagina e intenta de nuevo.",req);}
    @ExceptionHandler(IllegalStateException.class)
    Object unavailable(Exception ex,HttpServletRequest req){
        org.slf4j.LoggerFactory.getLogger(ErrorAdvice.class).warn("Operacion temporalmente no disponible; tipo={}",ex.getClass().getSimpleName());
        return response(HttpStatus.SERVICE_UNAVAILABLE,"No pudimos completar la operacion. Revisa la conexion o intenta mas tarde.",req);
    }
    private Object response(HttpStatus status,String message,HttpServletRequest req){
        if(req.getRequestURI().startsWith(req.getContextPath()+"/api/")) {
            var builder=ResponseEntity.status(status);if(status==HttpStatus.TOO_MANY_REQUESTS) builder.header("Retry-After","60");
            return builder.body(Map.of("message",message,"status",status.value()));
        }
        ModelAndView view=new ModelAndView("error");view.setStatus(status);view.addObject("status",status.value());view.addObject("friendlyMessage",message);return view;
    }
}
