package com.seguridad.bovedachat.service;
import org.springframework.stereotype.Component;
import java.nio.*;
import java.nio.charset.*;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
@Component
public class FileInspector {
    public record Inspected(String name,String mime) {}
    public Inspected inspect(String original,byte[] bytes) {
        if(original==null) throw AppException.invalid("El archivo necesita un nombre.");
        String name=original.replace('\\','/'); name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","_").trim();
        if(name.isBlank() || name.length()>180 || !name.contains(".")) throw AppException.invalid("El nombre del archivo no es valido.");
        String ext=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        String mime;
        if(ext.equals("pdf") && starts(bytes,new byte[]{37,80,68,70,45})) mime="application/pdf";
        else if(ext.equals("png") && starts(bytes,new byte[]{(byte)137,80,78,71,13,10,26,10})) { dimensions(bytes); mime="image/png"; }
        else if((ext.equals("jpg") || ext.equals("jpeg")) && starts(bytes,new byte[]{(byte)255,(byte)216,(byte)255})) { dimensions(bytes); mime="image/jpeg"; }
        else if(ext.equals("txt")) { validText(bytes); mime="text/plain"; }
        else throw AppException.invalid("Admitimos PDF, PNG, JPG y TXT. El contenido debe coincidir con el tipo de archivo.");
        return new Inspected(name,mime);
    }
    private boolean starts(byte[] content,byte[] prefix) {
        if(content.length<prefix.length) return false;
        for(int i=0;i<prefix.length;i++) if(content[i]!=prefix[i]) return false;
        return true;
    }
    private void validText(byte[] bytes) {
        try {
            String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            if(text.chars().anyMatch(c->Character.isISOControl(c) && c!='\n' && c!='\r' && c!='\t')) throw AppException.invalid("El TXT debe ser texto UTF-8.");
        } catch(CharacterCodingException ex) { throw AppException.invalid("El TXT debe estar guardado como UTF-8."); }
    }
    private void dimensions(byte[] bytes) {
        try(var input=ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw AppException.invalid("La imagen no es valida.");
            ImageReader reader=readers.next();
            try {
                reader.setInput(input,true,true);
                long width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<1 || height<1 || width*height>20000000L) throw AppException.invalid("La imagen supera el limite de 20 megapixeles.");
            } finally { reader.dispose(); }
        } catch(java.io.IOException ex) { throw AppException.invalid("La imagen no es valida."); }
    }
}
