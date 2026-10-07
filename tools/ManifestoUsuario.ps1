param(
    [Parameter(Mandatory=$true)][string]$Arquivo,
    [switch]$Incorporar,
    [string]$Mt = $env:ORVALHO_MT
)
$ErrorActionPreference = 'Stop'
if (-not $Mt) {
    $comando = Get-Command mt.exe -ErrorAction SilentlyContinue
    if ($comando) { $Mt = $comando.Source }
    else {
        $bin = Join-Path ${env:ProgramFiles(x86)} 'Windows Kits\10\bin'
        $Mt = Get-ChildItem -Path (Join-Path $bin '*\x64\mt.exe') -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
    }
}
if (-not $Mt -or -not (Test-Path -LiteralPath $Mt -PathType Leaf)) { throw 'Configure ORVALHO_MT com o mt.exe do Windows SDK (somente na maquina de compilacao).' }
$Arquivo = (Resolve-Path -LiteralPath $Arquivo).Path
if ($Incorporar) {
    if ((Get-AuthenticodeSignature -LiteralPath $Arquivo).SignerCertificate) { throw 'Incorpore o manifesto ANTES de assinar. Nao modificar executavel ja assinado.' }
    $manifesto = Join-Path $PSScriptRoot '..\packaging\windows\orvalho.manifest'
    & $Mt -nologo -manifest $manifesto "-outputresource:$Arquivo;#1"
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao incorporar manifesto por usuario.' }
}
$temporario = Join-Path ([IO.Path]::GetTempPath()) ('orvalho-manifest-'+[guid]::NewGuid().ToString('N')+'.xml')
try {
    & $Mt -nologo "-inputresource:$Arquivo;#1" "-out:$temporario"
    if ($LASTEXITCODE -ne 0) { throw "Executavel sem manifesto incorporado: $Arquivo" }
    [xml]$xml = Get-Content -LiteralPath $temporario -Raw
    $n = @($xml.SelectNodes("//*[local-name()='requestedExecutionLevel']"))
    if ($n.Count -ne 1 -or $n[0].level -ne 'asInvoker' -or $n[0].uiAccess -ne 'false') { throw "Manifesto exige privilegios inesperados: $Arquivo" }
    Write-Output ('Manifesto confirmado: asInvoker, uiAccess=false: '+[IO.Path]::GetFileName($Arquivo))
} finally { if (Test-Path -LiteralPath $temporario) { Remove-Item -LiteralPath $temporario } }
