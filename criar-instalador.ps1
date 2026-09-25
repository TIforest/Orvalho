param(
    [string]$Versao = '1.1.3',
    [string]$Imagem = (Join-Path $PSScriptRoot "distribuicao\versao-$Versao\Orvalho"),
    [string]$Destino = (Join-Path $PSScriptRoot 'distribuicao\instalador'),
    [string]$Wix = (Join-Path $env:LOCALAPPDATA 'OrvalhoBuild\wix-3.14.1')
)
$ErrorActionPreference = 'Stop'
$jdk = $env:JAVA_HOME
if (-not $jdk) { $jdk = 'C:\Program Files\Apache NetBeans\jdk' }
$jpackage = Join-Path $jdk 'bin\jpackage.exe'
if (-not (Test-Path -LiteralPath (Join-Path $Imagem 'Orvalho.exe'))) {
    throw 'Gere primeiro a imagem atualizada com empacotar.ps1 e informe -Imagem.'
}
if (-not (Test-Path -LiteralPath (Join-Path $Wix 'candle.exe'))) {
    throw 'Informe em -Wix a pasta dos binarios do WiX 3.14.1.'
}
[xml]$estado = Get-Content -LiteralPath (Join-Path $Imagem 'app\.jpackage.xml') -Raw
if ($estado.'jpackage-state'.'app-version' -ne $Versao) {
    throw 'A versao da imagem deve corresponder a versao do instalador. Gere a imagem novamente.'
}
$originalPath = $env:PATH
try {
    $env:PATH = "$Wix;$originalPath"
    & $jpackage --type exe --name Orvalho --app-image $Imagem --dest $Destino --app-version $Versao --vendor 'Orvalho' --description 'Lembretes para beber agua' --icon (Join-Path $PSScriptRoot 'distribuicao\orvalho.ico') --install-dir Orvalho --win-per-user-install --win-menu --win-menu-group Orvalho --win-shortcut --win-shortcut-prompt --win-dir-chooser --win-upgrade-uuid 'e9b82729-66b0-4a7a-b53e-b07c45e13f24'
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar instalador.' }
    Get-ChildItem -LiteralPath $Destino -Filter '*.exe' | Select-Object FullName, Length
} finally { $env:PATH = $originalPath }
