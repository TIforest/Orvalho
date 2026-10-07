param([string]$Destino, [string]$Versao = '1.2.22', [switch]$Assinar)
$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
$jdk = $env:JAVA_HOME
if (-not $jdk) { $jdk = 'C:\Program Files\Apache NetBeans\jdk' }
$jpackage = Join-Path $jdk 'bin\jpackage.exe'
if (-not (Test-Path -LiteralPath $jpackage)) { throw 'Configure JAVA_HOME para o JDK 26.' }
$env:JAVA_HOME = $jdk
if ($Versao -notmatch '^\d{1,5}\.\d{1,5}\.\d{1,5}$') { throw 'Use uma versao como 1.1.0.' }
$config = Join-Path $project 'src\main\resources\atualizacao.properties'
if (Test-Path -LiteralPath $config) {
    $conteudo = [IO.File]::ReadAllText($config)
    $conteudo = [regex]::Replace($conteudo, '(?m)^version=.*$', ('version=' + $Versao))
    [IO.File]::WriteAllText($config, $conteudo, [Text.UTF8Encoding]::new($false))
}
$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
$mvn = if ($maven) { $maven.Source } else { 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd' }
& $mvn -B -f (Join-Path $project 'pom.xml') package
if ($LASTEXITCODE -ne 0) { throw 'Compilação ou testes falharam.' }
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$entrada = Join-Path $project "target\empacotamento-$stamp"
New-Item -ItemType Directory -Path $entrada | Out-Null
Copy-Item -LiteralPath (Join-Path $project 'target\BebaAgua-1.0-SNAPSHOT.jar') -Destination $entrada
if (-not $Destino) { $Destino = Join-Path $project "distribuicao\versao-$stamp" }
if (Test-Path -LiteralPath (Join-Path $Destino 'Orvalho')) {
    throw 'O destino já contém Orvalho. Use uma pasta de saída nova.'
}
& $jpackage --type app-image --name Orvalho --input $entrada --main-jar BebaAgua-1.0-SNAPSHOT.jar --main-class com.mycompany.bebaAgua.Main --dest $Destino --app-version $Versao --icon (Join-Path $project 'distribuicao\orvalho.ico') --add-modules java.desktop,java.prefs,java.logging,jdk.crypto.mscapi --java-options '-Djava.net.useSystemProxies=true' --java-options '--enable-native-access=ALL-UNNAMED'
if ($LASTEXITCODE -ne 0) { throw 'Empacotamento falhou.' }
& (Join-Path $project 'tools\ManifestoUsuario.ps1') -Arquivo (Join-Path $Destino 'Orvalho\Orvalho.exe') -Incorporar
if ($Assinar) { & (Join-Path $project 'tools\AssinarWindows.ps1') -Arquivo (Join-Path $Destino 'Orvalho\Orvalho.exe') }
Write-Output (Join-Path $Destino 'Orvalho\Orvalho.exe')
