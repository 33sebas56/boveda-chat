"""Browser acceptance test against the real executable JAR; no external accounts.

Install: python -m pip install -r scripts/ui-requirements.txt
Then: python -m playwright install chromium
Run after mvnw verify: python scripts/test-ui.py
"""
from pathlib import Path
import base64
import http.client
import json
import os
import socket
import struct
import subprocess
import time
import zlib

from playwright.sync_api import sync_playwright, expect

ROOT = Path(__file__).resolve().parent.parent
TARGET = ROOT / "target"
TARGET.mkdir(exist_ok=True)
with socket.socket() as sock:
    sock.bind(("127.0.0.1", 0))
    port = sock.getsockname()[1]
JAVA = os.environ.get("UI_JAVA_BINARY", "java")
log = (TARGET / "ui-server.log").open("w", encoding="utf-8")
process = subprocess.Popen([
    JAVA, "-Xmx256m", "-jar", str(TARGET / "app.jar"),
    "--spring.profiles.active=local", f"--server.port={port}",
    "--spring.datasource.url=jdbc:h2:mem:uitest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DEFAULT_NULL_ORDERING=HIGH",
    "--app.storage.mode=local", "--app.storage.directory=target/ui-files",
    "--app.crypto.key-file=target/ui-key", "--app.crypto.require-key=false",
    "--app.sms.enabled=false", "--app.bootstrap.username=ui_admin",
    "--app.bootstrap.password=Contrasena UI de prueba 123",
], cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)

BASE = f"http://127.0.0.1:{port}"
checks = []


def ready():
    for _ in range(200):
        if process.poll() is not None:
            raise RuntimeError("The UI server stopped. See target/ui-server.log")
        try:
            conn = http.client.HTTPConnection("127.0.0.1", port, timeout=1)
            conn.request("GET", "/actuator/health")
            ok = conn.getresponse().status == 200
            conn.close()
            if ok:
                return
        except OSError:
            pass
        time.sleep(0.2)
    raise RuntimeError("UI server did not become ready")


def png():
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 32, 32, 8, 2, 0, 0, 0)) + chunk(b"IDAT", zlib.compress((b"\x00" + b"\x39\x75\x52" * 32) * 32)) + chunk(b"IEND", b"")


def login(page, username, password="Una clave UI larga 123"):
    page.goto(BASE + "/login")
    page.locator('[name="username"]').fill(username)
    page.locator('[name="password"]').fill(password)
    page.get_by_role("button", name="Iniciar sesión").click()
    page.wait_for_url(BASE + "/app")
    expect(page.locator("#my-name")).not_to_have_text("Mi cuenta")


def register(page, username, name):
    page.goto(BASE + "/registro")
    page.locator('[name="displayName"]').fill(name)
    page.locator('[name="username"]').fill(username)
    page.locator('[name="password"]').fill("Una clave UI larga 123")
    page.locator('[name="confirmation"]').fill("Una clave UI larga 123")
    page.get_by_role("button", name="Crear mi cuenta").click()
    page.wait_for_url(BASE + "/login?registro")
    login(page, username)


def screenshot(page, name):
    directory = os.environ.get("UI_SCREENSHOTS_DIR")
    if directory:
        Path(directory).mkdir(exist_ok=True, parents=True)
        page.screenshot(path=str(Path(directory) / name), full_page=True)


try:
    ready()
    with sync_playwright() as playwright:
        options = {"headless": True}
        binary = os.environ.get("UI_CHROMIUM_BINARY")
        if binary:
            options.update(executable_path=binary, args=["--no-sandbox", "--single-process", "--no-zygote", "--disable-dev-shm-usage"])
        browsers = []
        shared = None if binary else playwright.chromium.launch(**options)
        if shared:
            browsers.append(shared)
        def new_context(**kwargs):
            # A restricted local headless shell needs one process per browser context.
            current = shared or playwright.chromium.launch(**options)
            if not shared:
                browsers.append(current)
            return current.new_context(**kwargs)
        context = [new_context(viewport={"width":1440,"height":900}) for _ in range(3)]
        ana, juan, third = [c.new_page() for c in context]
        errors = []
        for page in (ana, juan, third):
            page.on("pageerror", lambda error: errors.append(str(error)))
            page.set_default_timeout(15000)
        ana.goto(BASE + "/login")
        screenshot(ana, "login.png")
        register(ana, "ana_ui", "Ana")
        register(juan, "juan_ui", "Juan")
        register(third, "tercero_ui", "Tercera persona")
        checks.append("Registro e inicio de sesion en navegadores separados")

        ana.locator("#new-chat").click()
        ana.locator("#people-query").fill("juan_ui")
        ana.locator(".person-result").click()
        expect(ana.locator("#peer-name")).to_have_text("Juan")
        text = "Hola Juan. <img src=x onerror=window.__xss=1>"
        ana.locator("#message-input").fill(text)
        ana.locator("#send-button").click()
        expect(ana.locator(".message-text").last).to_have_text(text)
        juan.locator(".conversation-item").filter(has_text="Ana").click()
        expect(juan.locator(".message-text").last).to_have_text(text)
        assert juan.evaluate("window.__xss") is None
        expect(ana.locator(".receipt").last).to_have_text("Leído", timeout=20000)
        checks.append("Chat, actualizacion, confirmacion de lectura y texto sin ejecutar HTML")

        ana.locator("#nav-vault").click()
        ana.locator("#file-input").set_input_files({"name":"nota-ui.txt","mimeType":"text/plain","buffer":b"Documento UI de prueba"})
        expect(ana.locator(".file-name-button").filter(has_text="nota-ui.txt")).to_be_visible()
        ana.locator("#file-input").set_input_files({"name":"captura-ui.png","mimeType":"image/png","buffer":png()})
        expect(ana.locator(".file-name-button").filter(has_text="captura-ui.png")).to_be_visible()
        screenshot(ana, "boveda.png")
        ana.locator("#nav-chats").click()
        ana.locator("#attach-button").click()
        ana.locator(".attachment-choice").filter(has_text="nota-ui.txt").click()
        ana.locator("#send-button").click()
        link = ana.locator(".attachment-card").filter(has_text="nota-ui.txt")
        expect(link).to_be_visible()
        url = BASE + link.get_attribute("href")
        allowed = context[1].request.get(url)
        assert allowed.status == 200 and allowed.body() == b"Documento UI de prueba"
        denied = context[2].request.get(url)
        assert denied.status == 404 and "No pudimos abrir" in denied.text()
        checks.append("Boveda, adjunto compartido, descarga original y tercero sin permiso")

        ana.locator("#attach-button").click()
        ana.locator(".attachment-choice").filter(has_text="captura-ui.png").click()
        ana.locator("#send-button").click()
        expect(juan.locator(".image-attachment img")).to_be_visible(timeout=20000)
        screenshot(ana, "chat.png")
        checks.append("Imagen cifrada con previsualizacion autorizada")

        ana.locator("#nav-vault").click()
        ana.locator(".file-name-button").filter(has_text="nota-ui.txt").click()
        expect(ana.locator("#detail-hash")).to_have_text(__import__('hashlib').sha256(b"Documento UI de prueba").hexdigest())
        ana.locator("#detail-revoke").click()
        expect(ana.locator("#file-dialog")).not_to_be_visible()
        assert context[1].request.get(url).status == 404
        assert context[0].request.get(url).status == 200
        checks.append("Huella y revocacion: el propietario conserva acceso")

        mobile_context = new_context(viewport={"width":390,"height":844}, is_mobile=True, has_touch=True)
        mobile = mobile_context.new_page()
        login(mobile, "ana_ui")
        mobile.locator(".conversation-item").filter(has_text="Juan").click()
        expect(mobile.locator("#peer-name")).to_have_text("Juan")
        assert mobile.evaluate("document.documentElement.scrollWidth <= innerWidth + 1")
        screenshot(mobile, "movil.png")
        mobile.locator("#back-to-list").click()
        expect(mobile.locator("#conversation-list")).to_be_visible()
        checks.append("Navegacion movil sin desbordamiento horizontal")

        admin = new_context().new_page()
        login(admin,"ui_admin","Contrasena UI de prueba 123")
        admin.goto(BASE + "/admin/auditoria")
        expect(admin.get_by_role("heading",name="Registro de actividad")).to_be_visible()
        expect(admin.locator(".event-tag").filter(has_text="DOWNLOAD_DENIED").first).to_be_visible()
        assert not errors, errors
        checks.append("Auditoria administrativa y ausencia de errores JavaScript")
        for browser in browsers:
            browser.close()
    result={"passed":True,"checks":checks}
    (TARGET / "ui-results.json").write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding="utf-8")
    print(json.dumps(result,ensure_ascii=False,indent=2))
finally:
    process.terminate()
    try: process.wait(timeout=10)
    except subprocess.TimeoutExpired: process.kill()
    log.close()
