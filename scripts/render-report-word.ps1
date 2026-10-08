param([string]$SourceName = '宠物管家系统个人技能实训综合报告_AI图完善版.docx', [switch]$TemplateReport)
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
    $document = $word.Documents.Open($source, $false, $false)
    $document.Fields.Update() | Out-Null
    foreach ($contents in $document.TablesOfContents) { $contents.Update() }
    if ($TemplateReport) {
        foreach ($styleId in @(-20, -21)) {
            $contentsStyle = $document.Styles.Item($styleId)
            $contentsStyle.Font.Name = 'Times New Roman'
            $contentsStyle.Font.NameFarEast = '宋体'
            $contentsStyle.Font.Size = 10.5
            $contentsStyle.ParagraphFormat.LineSpacingRule = 4
            $contentsStyle.ParagraphFormat.LineSpacing = 12.2
            $contentsStyle.ParagraphFormat.SpaceBefore = 0
            $contentsStyle.ParagraphFormat.SpaceAfter = 0
        }
    }
    $document.Repaginate()
    foreach ($contents in $document.TablesOfContents) { $contents.UpdatePageNumbers() }
    $document.Save()
    $document.ExportAsFixedFormat($pdf, 17)
    Write-Output $pdf
} finally {
    if ($document) { $document.Close(0) }
    if ($word) { $word.Quit() }
}
