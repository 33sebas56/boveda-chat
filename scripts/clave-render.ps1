$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
New-Item -ItemType Directory -Force -Path .local | Out-Null
$rutaClave = '.local/render-key.txt'
if (-not (Test-Path $rutaClave)) {
    $bytes = New-Object byte[] 32
    $generador = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generador.GetBytes($bytes) } finally { $generador.Dispose() }
    [System.Convert]::ToBase64String($bytes) | Set-Content -Encoding Ascii -NoNewline $rutaClave
    [Array]::Clear($bytes, 0, $bytes.Length)
}
Write-Host 'Copia el siguiente valor en BOVEDA_KEY de Render. Guardalo y no lo subas a GitHub:'
Get-Content $rutaClave
Write-Host 'El archivo se conserva para no generar una clave diferente por accidente.'
