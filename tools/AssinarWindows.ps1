param(
    [Parameter(Mandatory=$true)][string]$Arquivo,
    [string]$Certificado = $env:ORVALHO_CERT_THUMBPRINT,
    [string]$SignTool = $env:ORVALHO_SIGNTOOL,
    [string]$TimestampUrl = 'http://timestamp.digicert.com'
)
$ErrorActionPreference = 'Stop'
if (-not $Certificado) { throw 'Informe ORVALHO_CERT_THUMBPRINT com um certificado confiavel de assinatura de codigo. Nao use certificado autoassinado para distribuicao publica.' }
if (-not $SignTool) {
    $command = Get-Command signtool.exe -ErrorAction SilentlyContinue
    if ($command) { $SignTool = $command.Source }
}
if (-not $SignTool -or -not (Test-Path -LiteralPath $SignTool)) { throw 'Instale o Windows SDK e configure ORVALHO_SIGNTOOL com o caminho de signtool.exe.' }
& $SignTool sign /sha1 $Certificado /s My /fd SHA256 /tr $TimestampUrl /td SHA256 $Arquivo
if ($LASTEXITCODE -ne 0) { throw 'Falha ao assinar o arquivo.' }
& $SignTool verify /pa /all $Arquivo
if ($LASTEXITCODE -ne 0) { throw 'A assinatura nao passou na verificacao do Windows.' }
$assinatura = Get-AuthenticodeSignature -LiteralPath $Arquivo
if ($assinatura.Status -ne 'Valid' -or -not $assinatura.TimeStamperCertificate) {
    throw 'Assinatura invalida ou sem carimbo de tempo.'
}
