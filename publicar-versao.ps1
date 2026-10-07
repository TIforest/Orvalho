param(
    [Parameter(Mandatory=$true)][ValidatePattern('^\d{1,5}\.\d{1,5}\.\d{1,5}$')][string]$Versao,
    [string]$Notas = 'Melhorias e correcoes do Orvalho.',
    [ValidateSet('piloto','empresa')][string]$Canal = 'piloto',
    [switch]$UsarInstaladorExistente,
    [string]$Instalador,
    [string]$Imagem,
    [switch]$PermitirRevisaoMesmaVersao,
    [switch]$PermitirSemAssinatura
)
$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
$nodeOrvalho = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe'
$nodeDisponivel = Get-Command node -ErrorAction SilentlyContinue
if ($nodeDisponivel) { $nodeOrvalho = $nodeDisponivel.Source }
$wranglerOrvalho = Join-Path $project 'competicao\node_modules\wrangler\bin\wrangler.js'
foreach ($dependencia in @($nodeOrvalho, $wranglerOrvalho, (Join-Path $env:USERPROFILE '.orvalho-publicacao\ed25519.pk8'))) {
    if (-not (Test-Path -LiteralPath $dependencia -PathType Leaf)) { throw "Dependencia de publicacao ausente: $dependencia" }
}
$jdk = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Apache NetBeans\jdk' }
$repo = 'BrunoTrech/Orvalho-Releases'
if ($Canal -eq 'empresa' -and $PermitirSemAssinatura) { throw 'O canal empresa exige instalador assinado.' }
if (-not $Imagem) { $Imagem = Join-Path $project "distribuicao\versao-$Versao" }
$installer = if ($Instalador) { $Instalador } else { Join-Path $project "distribuicao\instalador\Orvalho-$Versao.exe" }
if (($Instalador -or $PermitirRevisaoMesmaVersao) -and -not $UsarInstaladorExistente) {
    throw 'Uma revisao deve usar o instalador existente que foi validado.'
}
if (-not $UsarInstaladorExistente) {
    if (-not $PermitirSemAssinatura -and -not $env:ORVALHO_CERT_THUMBPRINT) {
        throw 'Publicacao requer assinatura digital. Configure o certificado; para um piloto sem assinatura use explicitamente -PermitirSemAssinatura.'
    }
    & (Join-Path $project 'empacotar.ps1') -Destino $imagem -Versao $Versao -Assinar:(-not $PermitirSemAssinatura)
    & (Join-Path $project 'criar-instalador.ps1') -Imagem (Join-Path $imagem 'Orvalho') -Versao $Versao -Assinar:(-not $PermitirSemAssinatura)
}
if (-not (Test-Path -LiteralPath $installer)) { throw 'Instalador ausente.' }
if (-not $PermitirSemAssinatura) {
    foreach ($arquivo in @($installer, (Join-Path $imagem 'Orvalho\Orvalho.exe'))) {
        if (-not (Test-Path -LiteralPath $arquivo)) { throw 'Imagem assinada ausente para verificacao.' }
        $assinatura = Get-AuthenticodeSignature -LiteralPath $arquivo
        if ($assinatura.Status -ne 'Valid' -or -not $assinatura.TimeStamperCertificate) { throw "Assinatura confiavel com carimbo de tempo ausente: $arquivo" }
    }
} else { Write-Warning 'Publicacao de piloto SEM assinatura: avisos e bloqueios de seguranca podem continuar.' }
$config = Get-Content (Join-Path $project 'src\main\resources\atualizacao.properties') -Raw | ConvertFrom-StringData
if ($config.version -ne $Versao) { throw 'Versao do aplicativo diferente da publicacao.' }
$env:GIT_TERMINAL_PROMPT = '0'
$env:GCM_INTERACTIVE = 'Never'
$request = "protocol=https`nhost=github.com`npath=BrunoTrech/BebaAgua.git`n`n"
$credential = $request | git credential fill
$values = @{}
foreach ($line in $credential) {
    $parts = $line -split '=', 2
    if ($parts.Count -eq 2) { $values[$parts[0]] = $parts[1] }
}
if (-not $values['password']) { throw 'Autentique o GitHub no Git Credential Manager.' }
$headers = @{ Authorization = ('Bearer ' + $values['password']); 'User-Agent' = 'Orvalho-release' }
$release = $null
try { $release = Invoke-RestMethod -Uri "https://api.github.com/repos/$repo/releases/tags/v$Versao" -Headers $headers }
catch { if ([int]$_.Exception.Response.StatusCode -ne 404) { throw } }
if (-not $release) {
    $body = @{ tag_name = "v$Versao"; name = "Orvalho $Versao"; body = $Notas; draft = $true; prerelease = ($Canal -eq 'piloto') } | ConvertTo-Json
    $release = Invoke-RestMethod -Method Post -Uri "https://api.github.com/repos/$repo/releases" -Headers $headers -ContentType 'application/json' -Body $body
}
$hash = (Get-FileHash -LiteralPath $installer -Algorithm SHA256).Hash.ToLowerInvariant()
$nomeAsset = 'Orvalho-' + $Versao + '.exe'
$asset = $release.assets | Where-Object name -eq $nomeAsset
if ($asset -and ($asset.size -ne (Get-Item -LiteralPath $installer).Length -or $asset.digest -ne ('sha256:' + $hash))) {
    if (-not $PermitirRevisaoMesmaVersao) { throw 'O instalador remoto difere do local. Autorize explicitamente uma revisao da mesma versao ou informe uma nova versao.' }
    # Mantem URLs e hashes anteriores validos. O numero da versao do aplicativo nao muda.
    $nomeAsset = 'Orvalho-' + $Versao + '-' + $hash.Substring(0,12) + '.exe'
    $asset = $release.assets | Where-Object name -eq $nomeAsset
}
if (-not $asset) {
    if (-not $release.draft -and -not $PermitirRevisaoMesmaVersao) { throw 'Release publicada sem instalador esperado. Corrija antes de continuar.' }
    $upload = $release.upload_url -replace '\{.*$', ''
    $asset = Invoke-RestMethod -Method Post -Uri ($upload + '?name=' + $nomeAsset) -Headers $headers -ContentType 'application/octet-stream' -InFile $installer
}
if ($asset.size -ne (Get-Item -LiteralPath $installer).Length -or $asset.digest -ne ('sha256:' + $hash)) {
    throw 'O instalador remoto difere do local. Use uma nova versao; arquivos publicados nao sao substituidos.'
}
$promocao = @{ draft = $false; body = $Notas; prerelease = ($Canal -eq 'piloto' -and ($release.draft -or $release.prerelease)) } | ConvertTo-Json
$release = Invoke-RestMethod -Method Patch -Uri $release.url -Headers $headers -ContentType 'application/json' -Body $promocao
$release = Invoke-RestMethod -Uri $release.url -Headers $headers
$asset = $release.assets | Where-Object name -eq $nomeAsset
if (-not $asset.browser_download_url) { throw 'Link definitivo do instalador ausente.' }
$updates = Join-Path $project 'site\updates'
New-Item -ItemType Directory -Force -Path $updates | Out-Null
$manifest = Join-Path $updates $(if ($Canal -eq 'piloto') { 'pilot.properties' } else { 'latest.properties' })
& (Join-Path $jdk 'bin\java.exe') (Join-Path $project 'tools\AssinarVersao.java') sign $Versao $asset.browser_download_url $installer $manifest
if ($LASTEXITCODE -ne 0) { throw 'Falha ao assinar a versao.' }
if ($Canal -eq 'empresa') {
$siteFile = Join-Path $project 'site\index.html'
$html = [IO.File]::ReadAllText($siteFile)
$html = [regex]::Replace($html, 'https://github\.com/BrunoTrech/Orvalho-Releases/releases/download/v\d+\.\d+\.\d+/Orvalho-\d+\.\d+\.\d+(?:-[a-f0-9]{12})?\.exe', $asset.browser_download_url)
$html = [regex]::Replace($html, 'Versão \d+\.\d+\.\d+ ·', "Versão $Versao ·")
[IO.File]::WriteAllText($siteFile, $html, [Text.UTF8Encoding]::new($false))
}
$env:CLOUDFLARE_ACCOUNT_ID = '9ebe6f7a9beb0de8a8cb3a1d63468d12'
$branch = if ($Canal -eq 'piloto') { 'piloto' } else { 'main' }
& $nodeOrvalho $wranglerOrvalho pages deploy (Join-Path $project 'site') --project-name orvalho-forest --branch $branch --commit-dirty=true --commit-message $Notas
if ($LASTEXITCODE -ne 0) { throw 'Instalador publicado no GitHub; publicacao no Pages falhou. Repita apenas o comando pages deploy para concluir.' }
Write-Output ("Publicado no canal " + $Canal + ": " + $(if ($Canal -eq 'piloto') { 'https://piloto.orvalho-forest.pages.dev' } else { 'https://orvalho-forest.pages.dev' }))
