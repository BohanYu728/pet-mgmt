# Read-only conversion of the supplied legacy Word template for report rebuilding.
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
$originalPath = Join-Path $projectPath '实验报告参考模板00931.doc'
$reviewPath = Join-Path $projectPath '.local/template-review'
$referencePath = Join-Path $reviewPath 'reference.docx'
$inventoryPath = Join-Path $reviewPath 'reference-inventory.json'
New-Item -ItemType Directory -Force -Path $reviewPath | Out-Null
$originalHash = (Get-FileHash -LiteralPath $originalPath -Algorithm SHA256).Hash.ToLowerInvariant()
if (Test-Path -LiteralPath $inventoryPath) {
    $record = Get-Content -LiteralPath $inventoryPath -Raw | ConvertFrom-Json
    if ($record.original_sha256 -ne $originalHash) {
        throw '用户模板已更新，请重新核对模板结构后再生成报告。'
    }
    if (Test-Path -LiteralPath $referencePath) {
        Write-Output '只读模板副本已准备。'
        return
    }
}
$wordApp = New-Object -ComObject Word.Application
$wordApp.Visible = $false
$wordApp.DisplayAlerts = 0
$templateDoc = $null
try {
    $templateDoc = $wordApp.Documents.Open($originalPath, $false, $true)
    $templateDoc.SaveAs2($referencePath, 16)
} finally {
    if ($templateDoc) { $templateDoc.Close(0) }
    $wordApp.Quit()
}
@{
    sha256 = (Get-FileHash -LiteralPath $referencePath -Algorithm SHA256).Hash.ToLowerInvariant()
    original_sha256 = $originalHash
} | ConvertTo-Json | Set-Content -LiteralPath $inventoryPath -Encoding utf8
Write-Output '只读模板副本已准备；原始模板未修改。'
