param([Parameter(Mandatory=$true)][string]$Origem, [Parameter(Mandatory=$true)][string]$Destino)
$ErrorActionPreference = 'Stop'
[xml]$xml = Get-Content -LiteralPath $Origem -Raw
$ns = [Xml.XmlNamespaceManager]::new($xml.NameTable)
$ns.AddNamespace('w', 'http://schemas.microsoft.com/wix/2006/wi')
$ns.AddNamespace('u', 'http://schemas.microsoft.com/wix/UtilExtension')
$limpezas = @($xml.SelectNodes('//u:RemoveFolderEx', $ns))
if ($limpezas.Count -ne 1) { throw 'Formato inesperado do jpackage: revisar a limpeza antes de empacotar.' }
foreach ($limpeza in $limpezas) {
    $componente = $limpeza.ParentNode
    if ($componente.LocalName -ne 'Component' -or $componente.SelectNodes('w:File', $ns).Count -ne 0) {
        throw 'O componente de limpeza contem arquivos. Empacotamento interrompido.'
    }
    $id = $componente.GetAttribute('Id')
    $propriedade = $limpeza.GetAttribute('Property')
    foreach ($referencia in @($xml.SelectNodes("//w:ComponentRef[@Id='$id']", $ns))) {
        $referencia.ParentNode.RemoveChild($referencia) | Out-Null
    }
    $componente.ParentNode.RemoveChild($componente) | Out-Null
    foreach ($referencia in @($xml.SelectNodes("//w:Property[@Id='$propriedade']", $ns))) {
        $referencia.ParentNode.RemoveChild($referencia) | Out-Null
    }
}
if ($xml.SelectNodes('//u:RemoveFolderEx | //w:RemoveFile', $ns).Count -ne 0) {
    throw 'Remocao ampla inesperada no fragmento do instalador.'
}
# As entradas RemoveFolder geradas pelo jpackage removem somente pastas vazias.
$xml.Save($Destino)
Write-Output 'Fragmento protegido: somente arquivos registrados e pastas vazias.'
