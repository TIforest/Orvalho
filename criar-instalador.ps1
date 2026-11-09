param(
    [string]$Versao = '1.2.30',
    [string]$Imagem = (Join-Path $PSScriptRoot "distribuicao\versao-$Versao\Orvalho"),
    [string]$Destino = (Join-Path $PSScriptRoot 'distribuicao\instalador'),
    [string]$Wix = (Join-Path $env:LOCALAPPDATA 'OrvalhoBuild\wix-3.14.1'),
    [switch]$Assinar,
    [string]$Temporarios
)
$ErrorActionPreference = 'Stop'
$jdk = $env:JAVA_HOME
if (-not $jdk) { $jdk = 'C:\Program Files\Apache NetBeans\jdk' }
$jpackage = Join-Path $jdk 'bin\jpackage.exe'
$versaoJpackage = & $jpackage --version
if ($LASTEXITCODE -ne 0 -or $versaoJpackage -notmatch '^26\.') {
    throw 'Este modelo WiX foi validado com jpackage 26. Use o JDK 26 ou revise packaging/windows/main.wxs.'
}
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
    if ($Assinar -and (Get-AuthenticodeSignature -LiteralPath (Join-Path $Imagem 'Orvalho.exe')).Status -ne 'Valid') {
        throw 'Assine a imagem primeiro com empacotar.ps1 -Assinar.'
    }
    & (Join-Path $PSScriptRoot 'tools\ManifestoUsuario.ps1') -Arquivo (Join-Path $Imagem 'Orvalho.exe')
    if (-not $Temporarios) { $Temporarios = Join-Path $PSScriptRoot ('target\instalador-' + [guid]::NewGuid().ToString('N')) }
    $Temporarios = [IO.Path]::GetFullPath($Temporarios)
    $recursos = Join-Path $Temporarios 'recursos-seguros'
    New-Item -ItemType Directory -Path $recursos -Force | Out-Null
    Copy-Item -Path (Join-Path $PSScriptRoot 'packaging\windows\*') -Destination $recursos
    $opcoes = @('--name', 'Orvalho', '--app-image', $Imagem, '--app-version', $Versao,
        '--vendor', 'Orvalho', '--description', 'Lembretes para beber agua',
        '--icon', (Join-Path $PSScriptRoot 'distribuicao\orvalho.ico'), '--install-dir', 'Orvalho',
        '--win-per-user-install', '--win-menu', '--win-menu-group', 'Orvalho', '--win-shortcut',
        '--win-shortcut-prompt', '--win-upgrade-uuid', 'e9b82729-66b0-4a7a-b53e-b07c45e13f24')
    # A primeira passagem gera o inventario exato desta imagem. Seu MSI nunca e distribuido.
    $rascunho = Join-Path $Temporarios 'rascunho'
    & $jpackage --type msi @opcoes --dest (Join-Path $Temporarios 'msi-rascunho') --temp $rascunho --resource-dir $recursos
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar inventario do instalador.' }
    & (Join-Path $PSScriptRoot 'tools\ProtegerFragmentoInstalador.ps1') -Origem (Join-Path $rascunho 'config\bundle.wxf') -Destino (Join-Path $recursos 'bundle.wxf')
    $final = Join-Path $Temporarios 'final'
    & $jpackage --type exe @opcoes --dest $Destino --temp $final --resource-dir $recursos
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar instalador.' }
    & (Join-Path $PSScriptRoot 'tools\VerificarInstalador.ps1') -Arquivo (Join-Path $final "msi\Orvalho-$Versao.msi")
    & (Join-Path $PSScriptRoot 'tools\ManifestoUsuario.ps1') -Arquivo (Join-Path $Destino "Orvalho-$Versao.exe") -Incorporar
    & (Join-Path $PSScriptRoot 'tools\VerificarInstalador.ps1') -Arquivo (Join-Path $final "msi\Orvalho-$Versao.msi") -Executavel (Join-Path $Destino "Orvalho-$Versao.exe")
    if ($Assinar) { & (Join-Path $PSScriptRoot 'tools\AssinarWindows.ps1') -Arquivo (Join-Path $Destino "Orvalho-$Versao.exe") }
    Get-ChildItem -LiteralPath $Destino -Filter '*.exe' | Select-Object FullName, Length
} finally { $env:PATH = $originalPath }
