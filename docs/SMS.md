# Registro por celular con Twilio Verify

Esta integración envía un OTP real por SMS y comprueba la respuesta del proveedor. No existe un modo que escriba códigos en la consola o acepte un código fijo en producción. Las pruebas sustituyen las respuestas HTTP; eso no constituye evidencia de recepción en un teléfono real.

## Flujo

1. Al activar SMS, el registro pide nombre, usuario, contraseña y celular E.164 (por ejemplo, `+573001234567`).
2. Se guarda la cuenta deshabilitada y se solicita el código al proveedor. Un fallo de envío conserva el estado pendiente.
3. El navegador conserva únicamente la referencia de esa cuenta pendiente en su sesión. No puede cambiar el destinatario del código desde el formulario de verificación.
4. Twilio valida el código. Solo un estado `approved` habilita la cuenta y marca el celular como verificado.
5. La persona inicia sesión con usuario o celular y su contraseña.

Verificar el celular durante el registro prueba el control de ese número en ese momento. **No es segundo factor en cada inicio de sesión**, ni verifica identidad civil. SMS tiene limitaciones como SIM swapping y números reciclados.

## Configuración

Crea una cuenta en Twilio y habilita **Verify**. Obtén:

- Account SID: comienza con `AC`.
- Auth Token: secreto del servidor.
- Verify Service SID: comienza con `VA`.

En Environment de Render agrega `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` y `TWILIO_VERIFY_SERVICE_SID`. Configura `SMS_ENABLED=true` y vuelve a desplegar. La aplicación se detiene con un mensaje de configuración si faltan credenciales válidas: no omite la verificación silenciosamente.

Configura un código de 6 dígitos en el servicio Verify. La aplicación admite códigos de 4 a 10 dígitos para los ajustes que permita el proveedor. La expiración y los límites adicionales los determina Twilio.

## Costos y prueba

Instalar una librería no proporciona envío gratuito de SMS. El proveedor puede cobrar por verificación y mensaje, exigir habilitación de países o imponer restricciones de cuenta. En trial suele limitar destinatarios previamente verificados; consulta si permite probar con números colombianos en tu cuenta. No se promete SMS gratuito ilimitado ni envío a cualquier compañero con una cuenta trial.

La aplicación limita reenvíos a 60 segundos, 3 solicitudes por cuenta/hora y un presupuesto diario configurable (`SMS_DAILY_BUDGET`, por defecto 20 solicitudes globales). Reserva presupuesto antes de solicitar al proveedor, también ante fallos. Cuenta los eventos en PostgreSQL, por lo que el presupuesto diario sobrevive al reinicio. Configura además límites de gasto y países permitidos en el proveedor. Los límites no eliminan todos los riesgos de abuso o costos.

Si no quieres contratar o configurar SMS, deja `SMS_ENABLED=false`: el registro con usuario y contraseña continúa funcionando. No pedimos celular, no enviamos mensajes y no marcamos números como verificados.

## Prueba manual real

- Autoriza tu propio número en la cuenta trial cuando corresponda.
- Registra una cuenta nueva usando ese número.
- Comprueba que llegue el SMS; intenta entrar antes de verificar y observa que se deniega.
- Prueba un código incorrecto y luego el correcto.
- Inicia sesión con el número y la contraseña elegida.
- Revisa en la auditoría `SMS_REQUESTED` y `PHONE_VERIFIED`; el código y el número completo no aparecen allí.

Si pierdes la sesión durante el registro, usa **Retomar una verificación pendiente** desde el login. Debes conocer la contraseña de esa cuenta. El administrador inicial por variables es una cuenta operativa de contraseña y no se presenta como celular verificado.

Referencias: [Verify API](https://www.twilio.com/docs/verify/api), [Verification Check](https://www.twilio.com/docs/verify/api/verification-check), [Restricciones trial](https://www.twilio.com/docs/usage/trials/try-out-verify).
