# 同步代码到用户文件夹的PowerShell脚本

# 设置源目录和目标目录
$sourceDir = "e:/Multiplayer_Online_Compilation_Software"
$targetDir = "e:/Multiplayer_Online_Compilation_Software_Sync"

# 创建目标目录（如果不存在）
if (-not (Test-Path -Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir | Out-Null
    Write-Host "创建目标目录: $targetDir"
}

# 同步前后端代码
$itemsToSync = @("collab-editor-frontend", "collab-editor-backend", "api_documentation.md", "deployment_manual.md", "test_report.md")

foreach ($item in $itemsToSync) {
    $sourcePath = Join-Path -Path $sourceDir -ChildPath $item
    $targetPath = Join-Path -Path $targetDir -ChildPath $item
    
    if (Test-Path -Path $sourcePath) {
        # 如果是文件，直接复制
        if (Test-Path -Path $sourcePath -PathType Leaf) {
            Copy-Item -Path $sourcePath -Destination $targetPath -Force
            Write-Host "已复制文件: $item"
        }
        # 如果是目录，递归复制
        else {
            Copy-Item -Path $sourcePath -Destination $targetPath -Recurse -Force
            Write-Host "已复制目录: $item"
        }
    } else {
        Write-Host "警告: 源文件/目录不存在: $item"
    }
}

Write-Host "代码同步完成！"
Write-Host "同步后的代码目录: $targetDir"