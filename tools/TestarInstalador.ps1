param(
    [Parameter(Mandatory=$true)][string]$MsiNovo,
    [string]$MsiAnterior,
    [switch]$SomenteNovo,
    [switch]$AnteriorPorUsuario
)
# Testa o mecanismo MSI com identidades, componentes e Registro exclusivos.
# Nao instala nem remove o produto Orvalho real. Mantem logs e sentinelas para auditoria.
$ErrorActionPreference = 'Stop'
if (-not $SomenteNovo -and -not $MsiAnterior) { throw 'Informe MsiAnterior ou use SomenteNovo para testar apenas instalacao, reparo e remocao.' }
$teste = [guid]::NewGuid().ToString('N')
$raizTestes = Join-Path $env:PUBLIC 'OrvalhoInstallerTests'
$raiz = Join-Path $raizTestes $teste
$seguro = Join-Path $raiz 'Orvalho'
$antigo = Join-Path $raiz 'perfil-simulado'
New-Item -ItemType Directory -Path $raiz -Force | Out-Null
$upgrade = '{' + [guid]::NewGuid().ToString().ToUpperInvariant() + '}'
$produtoAntigo = '{' + [guid]::NewGuid().ToString().ToUpperInvariant() + '}'
$produtoNovo = '{' + [guid]::NewGuid().ToString().ToUpperInvariant() + '}'
$componentes = @{}
$inst = New-Object -ComObject WindowsInstaller.Installer
function Call-Com($obj, $name, [object[]]$values) {
    $unwrapped = @($values | ForEach-Object { $_.PSObject.BaseObject })
    $obj.GetType().InvokeMember($name, 'InvokeMethod', $null, $obj, $unwrapped)
}
function Rows($db, $table) {
    $view = Call-Com $db 'OpenView' @('SELECT * FROM `' + $table + '`')
    Call-Com $view 'Execute' @() | Out-Null
    while ($record = Call-Com $view 'Fetch' @()) {
        $n = $record.GetType().InvokeMember('FieldCount','GetProperty',$null,$record,$null)
        ,@(for($i=1;$i -le $n;$i++) { $record.GetType().InvokeMember('StringData','GetProperty',$null,$record,@($i)) })
    }
    Call-Com $view 'Close' @() | Out-Null
}
function Sql($db, $text) {
    $view = Call-Com $db 'OpenView' @($text)
    Call-Com $view 'Execute' @() | Out-Null
    Call-Com $view 'Close' @() | Out-Null
}
function Quoted([string]$value) { "'" + $value.Replace("'", "''") + "'" }
function Set-Field($db,$table,$keyColumn,$key,$column,$value) {
    Sql $db ('UPDATE `' + $table + '` SET `' + $column + '` = ' + (Quoted $value) + ' WHERE `' + $keyColumn + '` = ' + (Quoted $key))
}
function New-Fixture($source,$name,$code) {
    $dest = Join-Path $raiz $name
    Copy-Item -LiteralPath $source -Destination $dest
    $db = Call-Com $inst 'OpenDatabase' @($dest,1)
    Set-Field $db 'Property' 'Property' 'ProductCode' 'Value' $code
    Set-Field $db 'Property' 'Property' 'UpgradeCode' 'Value' $upgrade
    Set-Field $db 'Property' 'Property' 'ProductName' 'Value' ('Orvalho Teste ' + $teste)
    Set-Field $db 'Property' 'Property' 'Manufacturer' 'Value' 'Orvalho Testes Isolados'
    $fixtureDir = if ($name -eq 'anterior.msi' -and -not $AnteriorPorUsuario) { $antigo } else { $seguro }
    Sql $db ('INSERT INTO `Property` (`Property`,`Value`) VALUES (''INSTALLDIR'',' + (Quoted ($fixtureDir+'\')) + ')')
    $upgradeRows = @(Rows $db 'Upgrade')
    Sql $db 'DELETE FROM `Upgrade`'
    foreach ($r in $upgradeRows) {
        $r[0] = $upgrade
        $vals = @($r | ForEach-Object { Quoted $_ }) -join ','
        Sql $db ('INSERT INTO `Upgrade` (`UpgradeCode`,`VersionMin`,`VersionMax`,`Language`,`Attributes`,`Remove`,`ActionProperty`) VALUES ('+$vals+')')
    }
    foreach ($r in (Rows $db 'Component')) {
        if ($r[1]) {
            if (-not $componentes.ContainsKey($r[0])) { $componentes[$r[0]] = '{'+[guid]::NewGuid().ToString().ToUpperInvariant()+'}' }
            Set-Field $db 'Component' 'Component' $r[0] 'ComponentId' $componentes[$r[0]]
        }
    }
    foreach ($table in @('Registry','RegLocator')) {
        $pk = if ($table -eq 'RegLocator') { 'Signature_' } else { 'Registry' }
        foreach ($r in (Rows $db $table)) {
            $key = $r[2].Replace('Software\Orvalho\Orvalho\', ('Software\OrvalhoInstallerTests\'+$teste+'\'))
            Set-Field $db $table $pk $r[0] 'Key' $key
            if ($r[3] -eq 'Orvalho') { Set-Field $db $table $pk $r[0] 'Name' ('OrvalhoTeste'+$teste) }
        }
    }
    foreach ($r in (Rows $db 'CustomAction')) {
        $target = $r[3].Replace('HKCU\Software\Orvalho\Orvalho\', ('HKCU\Software\OrvalhoInstallerTests\'+$teste+'\'))
        $target = $target.Replace('/v Orvalho ', ('/v OrvalhoTeste'+$teste+' '))
        if ($r[0] -eq 'OrvalhoSafeDirectory') { $target = $seguro + '\' }
        Set-Field $db 'CustomAction' 'Action' $r[0] 'Target' $target
    }
    # Atalhos exclusivos, mesmo se uma condicao de atalhos mudar no jpackage.
    foreach ($r in (Rows $db 'Shortcut')) {
        Set-Field $db 'Shortcut' 'Shortcut' $r[0] 'Name' ('OrvalhoTeste'+$teste)
    }
    foreach ($r in (Rows $db 'Directory')) {
        if ($r[1] -eq 'ProgramMenuFolder') { Set-Field $db 'Directory' 'Directory' $r[0] 'DefaultDir' ('OrvalhoTeste'+$teste) }
    }
    $summary = $db.GetType().InvokeMember('SummaryInformation','GetProperty',$null,$db,@(1))
    $summary.GetType().InvokeMember('Property','SetProperty',$null,$summary,@(9,('{'+[guid]::NewGuid().ToString().ToUpperInvariant()+'}'))) | Out-Null
    Call-Com $summary 'Persist' @() | Out-Null
    Call-Com $db 'Commit' @() | Out-Null
    [Runtime.InteropServices.Marshal]::FinalReleaseComObject($db) | Out-Null
    return $dest
}
function Run-Msi([string]$label,[string]$arguments,[int[]]$expected=@(0)) {
    $log = Join-Path $env:PUBLIC ('OrvalhoTeste-'+$teste+'-'+$label+'.log')
    $clock = [Diagnostics.Stopwatch]::StartNew()
    $p = Start-Process -FilePath "$env:SystemRoot\System32\msiexec.exe" -ArgumentList ($arguments+' /qn /norestart /L*V "'+$log+'"') -PassThru -WindowStyle Hidden
    if (-not $p.WaitForExit(180000)) { throw "Teste demorou mais de 3 minutos. Processo $($p.Id) preservado, sem matar transacao: $log" }
    if ($p.ExitCode -notin $expected) { throw "Falha em $label, codigo $($p.ExitCode). Log: $log" }
    Copy-Item -LiteralPath $log -Destination (Join-Path $raiz ($label+'.log'))
    Write-Output "$label : codigo $($p.ExitCode), $([Math]::Round($clock.Elapsed.TotalSeconds,1)) segundos"
}
$old = if (-not $SomenteNovo) { New-Fixture $MsiAnterior 'anterior.msi' $produtoAntigo }
$new = New-Fixture $MsiNovo 'novo.msi' $produtoNovo
[GC]::Collect()
[GC]::WaitForPendingFinalizers()
$opts = ' JP_INSTALL_DESKTOP_SHORTCUT=0 JP_INSTALL_STARTMENU_SHORTCUT=0'
Run-Msi '01-destino-rejeitado' ('/i "'+$new+'" INSTALLDIR="'+$antigo+'"'+$opts) @(1603)
if (Test-Path -LiteralPath (Join-Path $antigo 'Orvalho.exe')) { throw 'Destino inadequado recebeu arquivos.' }
Run-Msi '02-instalacao-limpa' ('/i "'+$new+'" INSTALLDIR="'+$seguro+'"'+$opts)
if (-not (Test-Path -LiteralPath (Join-Path $seguro 'Orvalho.exe'))) { throw 'Executavel nao instalado.' }
Set-Content -LiteralPath (Join-Path $seguro 'documento-pessoal.txt') -Value 'preservar-117'
Run-Msi '03-reparo' ('/fomus '+$produtoNovo)
Run-Msi '04-desinstalacao' ('/x '+$produtoNovo)
if ((Get-Content -LiteralPath (Join-Path $seguro 'documento-pessoal.txt') -Raw).Trim() -ne 'preservar-117') { throw 'Sentinela perdida na desinstalacao.' }
if (Test-Path -LiteralPath (Join-Path $seguro 'Orvalho.exe')) { throw 'Executavel nao removido.' }

if (-not $SomenteNovo) {
$destinoAnterior = if ($AnteriorPorUsuario) { $seguro } else { $antigo }
Run-Msi '05-instalar-legado' ('/i "'+$old+'" INSTALLDIR="'+$destinoAnterior+'"'+$opts)
New-Item -ItemType Directory -Path (Join-Path $destinoAnterior 'Chrome\User Data') -Force | Out-Null
Set-Content -LiteralPath (Join-Path $destinoAnterior 'Chrome\User Data\documento-pessoal.txt') -Value 'preservar-legado'
Run-Msi '06-atualizar-legado' ('/i "'+$new+'" INSTALLDIR="'+$seguro+'"'+$opts)
if ((Get-Content -LiteralPath (Join-Path $destinoAnterior 'Chrome\User Data\documento-pessoal.txt') -Raw).Trim() -ne 'preservar-legado') { throw 'Upgrade perdeu dados alheios.' }
$upgradeLog = Get-Content -LiteralPath (Join-Path $raiz '06-atualizar-legado.log') -Raw
if ($upgradeLog -match 'Recursing path:') { throw 'Upgrade executou varredura recursiva.' }
if (-not $AnteriorPorUsuario -and $upgradeLog -notmatch 'OrvalhoProtectLegacy_1.1.6') { throw 'Protecao legada nao registrada.' }
if (-not (Test-Path -LiteralPath (Join-Path $seguro 'Orvalho.exe'))) { throw 'Versao nova nao instalada.' }
Run-Msi '07-remover-atualizado' ('/x '+$produtoNovo)
}
Write-Output "TESTES CONCLUIDOS. Sentinelas e logs preservados em: $raiz"
@{Root=$raiz;OldProduct=$produtoAntigo;NewProduct=$produtoNovo;UpgradeCode=$upgrade} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $raiz 'teste.json')
