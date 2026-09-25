param(
    [Parameter(Mandatory=$true)][ValidatePattern('^\d{1,5}\.\d{1,5}\.\d{1,5}$')][string]$Versao,
    [string]$Notas = 'Melhorias e correcoes do Orvalho.',
    [switch]$UsarInstaladorExistente
)
$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
$jdk = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Apache NetBeans\jdk' }
$repo = 'BrunoTrech/Orvalho-Releases'
$imagem = Join-Path $project "distribuicao\versao-$Versao"
$installer = Join-Path $project "distribuicao\instalador\Orvalho-$Versao.exe"
if (-not $UsarInstaladorExistente) {
    & (Join-Path $project 'empacotar.ps1') -Destino $imagem -Versao $Versao
    & (Join-Path $project 'criar-instalador.ps1') -Imagem (Join-Path $imagem 'Orvalho') -Versao $Versao
}
if (-not (Test-Path -LiteralPath $installer)) { throw 'Instalador ausente.' }
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
$body = @{ tag_name = "v$Versao"; name = "Orvalho $Versao"; body = $Notas; draft = $true; prerelease = $false } | ConvertTo-Json
$release = Invoke-RestMethod -Method Post -Uri "https://api.github.com/repos/$repo/releases" -Headers $headers -ContentType 'application/json' -Body $body
$upload = $release.upload_url -replace '\{.*$', ''
$asset = Invoke-RestMethod -Method Post -Uri ($upload + '?name=Orvalho-' + $Versao + '.exe') -Headers $headers -ContentType 'application/octet-stream' -InFile $installer
$hash = (Get-FileHash -LiteralPath $installer -Algorithm SHA256).Hash.ToLowerInvariant()
if ($asset.size -ne (Get-Item -LiteralPath $installer).Length -or $asset.digest -ne ('sha256:' + $hash)) {
    throw 'O arquivo no GitHub nao corresponde ao instalador local. A release permanece em rascunho.'
}
$release = Invoke-RestMethod -Method Patch -Uri $release.url -Headers $headers -ContentType 'application/json' -Body '{"draft":false}'
$release = Invoke-RestMethod -Uri $release.url -Headers $headers
$asset = $release.assets | Where-Object name -eq ('Orvalho-' + $Versao + '.exe')
if (-not $asset.browser_download_url) { throw 'Link definitivo do instalador ausente.' }
$updates = Join-Path $project 'site\updates'
New-Item -ItemType Directory -Force -Path $updates | Out-Null
$manifest = Join-Path $updates 'latest.properties'
& (Join-Path $jdk 'bin\java.exe') (Join-Path $project 'tools\AssinarVersao.java') sign $Versao $asset.browser_download_url $installer $manifest
if ($LASTEXITCODE -ne 0) { throw 'Falha ao assinar a versao.' }
$siteFile = Join-Path $project 'site\index.html'
$html = [IO.File]::ReadAllText($siteFile)
$html = [regex]::Replace($html, '/releases/download/v\d+\.\d+\.\d+/Orvalho-\d+\.\d+\.\d+\.exe', "/releases/download/v$Versao/Orvalho-$Versao.exe")
$html = [regex]::Replace($html, 'Versão \d+\.\d+\.\d+ ·', "Versão $Versao ·")
[IO.File]::WriteAllText($siteFile, $html, [Text.UTF8Encoding]::new($false))
$env:CLOUDFLARE_ACCOUNT_ID = '9ebe6f7a9beb0de8a8cb3a1d63468d12'
& node (Join-Path $env:LOCALAPPDATA 'OrvalhoBuild\cloudflare\node_modules\wrangler\bin\wrangler.js') pages deploy (Join-Path $project 'site') --project-name orvalho-forest --branch main --commit-dirty=true
if ($LASTEXITCODE -ne 0) { throw 'Instalador publicado no GitHub; publicacao no Pages falhou. Repita apenas o comando pages deploy para concluir.' }
Write-Output 'Publicado: https://orvalho-forest.pages.dev'
