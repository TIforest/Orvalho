param([Parameter(Mandatory=$true)][string]$Arquivo, [string]$Executavel)
$ErrorActionPreference = 'Stop'
if ($Executavel) {
    if (-not ('OrvalhoPacoteEmbutido' -as [type])) {
        Add-Type -TypeDefinition @'
using System.IO;
public static class OrvalhoPacoteEmbutido {
 public static bool Confere(string exe,string msi) {
  byte[] pacote=File.ReadAllBytes(exe), esperado=File.ReadAllBytes(msi);
  if(esperado.Length<8)return false;
  for(int i=0;i<=pacote.Length-esperado.Length;i++) {
   if(pacote[i]!=esperado[0]||pacote[i+1]!=esperado[1]||pacote[i+2]!=esperado[2]||pacote[i+3]!=esperado[3])continue;
   int j=4;while(j<esperado.Length&&pacote[i+j]==esperado[j])j++;
   if(j==esperado.Length)return true;
  }
  return false;
 }
}
'@
    }
    if (-not [OrvalhoPacoteEmbutido]::Confere([IO.Path]::GetFullPath($Executavel),[IO.Path]::GetFullPath($Arquivo))) { throw 'O EXE nao contem o MSI validado intacto.' }
}
function Invoke-MsiMember($Objeto, [string]$Nome, [object[]]$Argumentos) {
    $Objeto.GetType().InvokeMember($Nome, 'InvokeMethod', $null, $Objeto, $Argumentos)
}
$installer = New-Object -ComObject WindowsInstaller.Installer
$db = Invoke-MsiMember $installer 'OpenDatabase' @([IO.Path]::GetFullPath($Arquivo), 0)
function Read-MsiRows([string]$Tabela) {
    $view = Invoke-MsiMember $db 'OpenView' @(('SELECT * FROM `' + $Tabela + '`'))
    Invoke-MsiMember $view 'Execute' @() | Out-Null
    while ($record = Invoke-MsiMember $view 'Fetch' @()) {
        $count = $record.GetType().InvokeMember('FieldCount', 'GetProperty', $null, $record, $null)
        $row = @(for ($i = 1; $i -le $count; $i++) {
            $record.GetType().InvokeMember('StringData', 'GetProperty', $null, $record, @($i))
        })
        ,$row
    }
    Invoke-MsiMember $view 'Close' @() | Out-Null
}
$tables = @(Read-MsiRows '_Tables' | ForEach-Object { $_[0] })
if ('WixRemoveFolderEx' -in $tables -and @(Read-MsiRows 'WixRemoveFolderEx').Count -ne 0) {
    throw 'MSI contem remocao recursiva.'
}
$actions = @(Read-MsiRows 'CustomAction')
if (@($actions | Where-Object { $_[0] -eq 'WixRemoveFoldersEx' }).Count -ne 0) {
    throw 'MSI ainda inclui a acao de remocao recursiva.'
}
if (@(Read-MsiRows 'RemoveFile' | Where-Object { $_[2] }).Count -ne 0) {
    throw 'MSI contem exclusao de arquivos fora do inventario.'
}
$sequence = @{}
foreach ($row in (Read-MsiRows 'InstallExecuteSequence')) { $sequence[$row[0]] = $row }
if ([int]$sequence['OrvalhoRejectDirectory'][2] -ge [int]$sequence['RemoveExistingProducts'][2] -or
    [int]$sequence['OrvalhoRejectDirectory'][2] -le [int]$sequence['CostFinalize'][2] -or
    $sequence['OrvalhoRejectDirectory'][1] -ne 'NOT Installed AND NOT (INSTALLDIR ~= ORVALHOSAFEDIR)') {
    throw 'Protecao de diretorio ausente ou fora de ordem.'
}
if ([int]$sequence['RemoveExistingProducts'][2] -le [int]$sequence['InstallInitialize'][2]) {
    throw 'Remocao anterior fora da transacao.'
}
foreach ($version in @('1.0.0','1.1.0','1.1.1','1.1.2','1.1.3','1.1.4','1.1.5','1.1.6')) {
    $row = $sequence['OrvalhoProtectLegacy_' + $version]
    if (-not $row -or [int]$row[2] -ge [int]$sequence['RemoveExistingProducts'][2] -or
        [int]$row[2] -le [int]$sequence['InstallValidate'][2] -or
        -not $row[1].Contains('JP_UPGRADABLE_FOUND >< LEGACYPRODUCT_' + $version)) {
        throw "Protecao de upgrade ausente para $version."
    }
}
$startup = @($actions | Where-Object { $_[0] -eq 'OrvalhoRemoveStartup' })
if ($startup.Count -ne 1 -or (([int]$startup[0][1] -band 64) -eq 0)) {
    throw 'Falha ao limpar inicializacao ainda pode impedir desinstalacao.'
}
$properties = @{}
foreach ($row in (Read-MsiRows 'Property')) { $properties[$row[0]] = $row[1] }
if ($properties['ALLUSERS'] -or $properties['MSIINSTALLPERUSER']) { throw 'MSI deve ter escopo exclusivo do usuario, sem selecao de contexto.' }
$summary = $db.GetType().InvokeMember('SummaryInformation','GetProperty',$null,$db,@(0))
$wordCount = [int]$summary.GetType().InvokeMember('Property','GetProperty',$null,$summary,@(15))
if (($wordCount -band 8) -eq 0) { throw 'MSI nao declara instalacao sem elevacao no SummaryInformation.' }
foreach ($table in @('Registry','RemoveRegistry')) {
    if ($table -in $tables) {
        foreach ($row in (Read-MsiRows $table)) { if ([int]$row[1] -ne 1) { throw "$table contem operacao fora de HKCU." } }
    }
}
foreach ($table in @('ServiceInstall','ServiceControl')) {
    if ($table -in $tables -and @(Read-MsiRows $table).Count) { throw "MSI inclui servico privilegiado: $table" }
}
foreach ($action in $actions) {
    if (([int]$action[1] -band 2048) -ne 0) { throw "Acao sem impersonacao do usuario: $($action[0])" }
}
$directories = @{}
foreach ($row in (Read-MsiRows 'Directory')) { $directories[$row[0]] = $row[1] }
$fileComponents = @(Read-MsiRows 'File' | ForEach-Object { $_[1] })
foreach ($row in (Read-MsiRows 'Component')) {
    if ($row[0] -notin $fileComponents) { continue }
    $directory = $row[2]; $visited = @{}
    while ($directory -and $directory -ne 'INSTALLDIR') {
        if ($visited.ContainsKey($directory)) { throw 'Ciclo nos diretorios do MSI.' }
        $visited[$directory] = $true; $directory = $directories[$directory]
    }
    if ($directory -ne 'INSTALLDIR') { throw "Componente fora da pasta do app: $($row[0])" }
}
$directory = $directories['INSTALLDIR']
if ($directory -ne 'LocalAppDataFolder') { throw 'INSTALLDIR nao esta sob LocalAppDataFolder.' }
foreach ($row in (Read-MsiRows 'Shortcut')) {
    $directory = $row[1]; $visited = @{}
    while ($directory -and $directory -notin @('ProgramMenuFolder','DesktopFolder')) {
        if ($visited.ContainsKey($directory)) { throw 'Ciclo nos diretorios de atalhos.' }
        $visited[$directory]=$true; $directory=$directories[$directory]
    }
    if (-not $directory) { throw 'Atalho fora do Menu Iniciar/Area de Trabalho do usuario.' }
}
if ($properties['ProductName'] -ne 'Orvalho' -or $properties['UpgradeCode'] -ne '{E9B82729-66B0-4A7A-B53E-B07C45E13F24}') {
    throw 'Identidade do MSI inesperada.'
}
Write-Output ('MSI verificado: versao ' + $properties['ProductVersion'] + ', ' + @(Read-MsiRows 'File').Count + ' arquivos; por usuario, sem elevacao, HKCU; sem remocao recursiva; diretorio e upgrades protegidos.')
