# 将 runs\segment\predict* 里的裂缝预测图片复制到前端 public/images/crack
# 适合你把 runs 放在：桌面\软件系统开发实训\runs

$ProjectRoot = Join-Path $env:USERPROFILE "Desktop\软件系统开发实训\agv-inspection-frontend-runs-ready"
$RunsRoot = Join-Path $env:USERPROFILE "Desktop\软件系统开发实训\runs\segment"
$TargetDir = Join-Path $ProjectRoot "public\images\crack"

New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null

Get-ChildItem $RunsRoot -Recurse -Include *.jpg,*.jpeg,*.png | Where-Object {
  $_.FullName -match "predict"
} | ForEach-Object {
  $stem = [System.IO.Path]::GetFileNameWithoutExtension($_.Name)
  $ext = $_.Extension
  Copy-Item $_.FullName -Destination (Join-Path $TargetDir $_.Name) -Force
  Copy-Item $_.FullName -Destination (Join-Path $TargetDir ($stem + "_predict" + $ext)) -Force
}

Write-Host "已复制预测图片到：$TargetDir"
