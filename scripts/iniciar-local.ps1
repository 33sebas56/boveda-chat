param([switch]$CrearAdmin)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$env:SPRING_PROFILES_ACTIVE = 'local'
if ($CrearAdmin) {
    $env:ADMIN_USERNAME = (Read-Host 'Nombre del administrador inicial (por ejemplo admin)').Trim()
    $env:ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Contrasena de al menos 12 caracteres' -AsSecureString)).Password
}
Write-Host 'Abre http://localhost:8080 cuando aparezca Started BovedaChatApplication.'
& .\mvnw.cmd spring-boot:run
exit $LASTEXITCODE
