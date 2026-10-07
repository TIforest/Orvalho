param(
 [Parameter(Mandatory=$true)][ValidateSet('empresa','piloto')][string]$Canal,
 [string]$PastaDados = (Join-Path $env:USERPROFILE '.orvalho')
)
$ErrorActionPreference='Stop'
# Executar no perfil do funcionário. Nunca altera outro usuário implicitamente.
New-Item -ItemType Directory -Force -Path $PastaDados | Out-Null
$arquivo=Join-Path $PastaDados 'canal-atualizacao.txt'
[IO.File]::WriteAllText($arquivo,$Canal,[Text.UTF8Encoding]::new($false))
Write-Output "Canal definido como $Canal. O aplicativo lerá a configuração na próxima consulta."
