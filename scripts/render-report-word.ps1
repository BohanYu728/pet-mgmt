param([string]$SourceName = '宠物管家系统个人技能实训综合报告.docx')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$source = Join-Path $projectRoot $SourceName
$outputDirectory = Join-Path $projectRoot '.local/report-qa'
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
$pdf = Join-Path $outputDirectory 'report.pdf'
$word = $null
$document = $null
try {
    $word = New-Object -ComObject Word.Application
    $word.Visible = $false
    $word.DisplayAlerts = 0
    $document = $word.Documents.Open($source, $false, $true)
    $document.ExportAsFixedFormat($pdf, 17)
    Write-Output $pdf
} finally {
    if ($document) { $document.Close(0) }
    if ($word) { $word.Quit() }
}
