package com.seguridad.bovedachat;
import com.seguridad.bovedachat.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
class FileInspectorTest {
    private final FileInspector inspector=new FileInspector();
    @Test void sanitizesWindowsAndUnixPaths(){assertThat(inspector.inspect("C:\\secret\\notes.txt","Hola".getBytes()).name()).isEqualTo("notes.txt");assertThat(inspector.inspect("../../notes.txt","Hola".getBytes()).name()).isEqualTo("notes.txt");}
    @ParameterizedTest @ValueSource(strings={"image.png","photo.jpg","report.pdf","run.exe","vector.svg","script.html"})
    void rejectsSpoofedOrDangerousTypes(String name){assertThatThrownBy(()->inspector.inspect(name,"not an image".getBytes())).isInstanceOf(AppException.class);}
    @Test void rejectsBinaryDisguisedAsText(){assertThatThrownBy(()->inspector.inspect("x.txt",new byte[]{1,0,4})).isInstanceOf(AppException.class);}
    @Test void acceptsUtf8AndPdf(){assertThat(inspector.inspect("nota.txt","niño\ntexto".getBytes(java.nio.charset.StandardCharsets.UTF_8)).mime()).isEqualTo("text/plain");assertThat(inspector.inspect("x.pdf","%PDF-1.7\nexample".getBytes()).mime()).isEqualTo("application/pdf");}
}
