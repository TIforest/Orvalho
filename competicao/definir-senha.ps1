# Executar somente pelo TI. A senha digitada nao aparece no historico do terminal.
$ErrorActionPreference = 'Stop'
$nodeOrvalho = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe'
if (-not (Test-Path -LiteralPath $nodeOrvalho)) { $nodeOrvalho = (Get-Command node.exe -ErrorAction Stop).Source }
$wranglerOrvalho = Join-Path $PSScriptRoot 'node_modules\wrangler\bin\wrangler.js'
if (-not (Test-Path -LiteralPath $wranglerOrvalho)) { throw 'Restaure as dependencias com npm ci na pasta competicao.' }
function Ler-SenhaOrvalho([string]$Mensagem) {
    $segura = Read-Host $Mensagem -AsSecureString
    $ponteiro = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ponteiro) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ponteiro); $segura.Dispose() }
}
$senhaOrvalho = Ler-SenhaOrvalho 'Nova senha da competicao (10 a 128 caracteres)'
$confirmacaoOrvalho = Ler-SenhaOrvalho 'Repita a senha'
if ($senhaOrvalho -cne $confirmacaoOrvalho) { throw 'As senhas nao conferem.' }
if ($senhaOrvalho.Length -lt 10 -or $senhaOrvalho.Length -gt 128 -or $senhaOrvalho -match '[\x00-\x1f\x7f]') { throw 'Use de 10 a 128 caracteres, sem caracteres de controle.' }
$shaOrvalho = [Security.Cryptography.SHA256]::Create()
$contaAnterior = $env:CLOUDFLARE_ACCOUNT_ID
try {
    $hashOrvalho = [BitConverter]::ToString($shaOrvalho.ComputeHash([Text.Encoding]::UTF8.GetBytes($senhaOrvalho))).Replace('-','').ToLowerInvariant()
    $env:CLOUDFLARE_ACCOUNT_ID = '9ebe6f7a9beb0de8a8cb3a1d63468d12'
    $hashOrvalho | & $nodeOrvalho $wranglerOrvalho secret put COMPETITION_PASSWORD_HASH --config (Join-Path $PSScriptRoot 'wrangler.toml')
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao configurar a senha no Cloudflare.' }
    Write-Output 'Senha alterada. Cadastros existentes continuam conectados; novos cadastros usam a nova senha.'
} finally {
    $env:CLOUDFLARE_ACCOUNT_ID = $contaAnterior
    $shaOrvalho.Dispose()
    $senhaOrvalho = $null; $confirmacaoOrvalho = $null; $hashOrvalho = $null
}
