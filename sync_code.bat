@echo off

rem 同步代码到用户文件夹的批处理脚本

rem 设置源目录和目标目录
set SOURCE_DIR=e:/Multiplayer_Online_Compilation_Software
set TARGET_DIR=e:/Multiplayer_Online_Compilation_Software_Sync

rem 创建目标目录（如果不存在）
if not exist "%TARGET_DIR%" mkdir "%TARGET_DIR%"
echo 创建目标目录: %TARGET_DIR%

rem 同步前端代码
if exist "%SOURCE_DIR%\collab-editor-frontend" (
    xcopy "%SOURCE_DIR%\collab-editor-frontend" "%TARGET_DIR%\collab-editor-frontend" /E /I /H /C /Y
    echo 已同步前端代码
)

rem 同步后端代码
if exist "%SOURCE_DIR%\collab-editor-backend" (
    xcopy "%SOURCE_DIR%\collab-editor-backend" "%TARGET_DIR%\collab-editor-backend" /E /I /H /C /Y
    echo 已同步后端代码
)

rem 同步文档文件
if exist "%SOURCE_DIR%\api_documentation.md" (
    copy "%SOURCE_DIR%\api_documentation.md" "%TARGET_DIR%\api_documentation.md" /Y
    echo 已同步API文档
)

if exist "%SOURCE_DIR%\deployment_manual.md" (
    copy "%SOURCE_DIR%\deployment_manual.md" "%TARGET_DIR%\deployment_manual.md" /Y
    echo 已同步部署手册
)

if exist "%SOURCE_DIR%\test_report.md" (
    copy "%SOURCE_DIR%\test_report.md" "%TARGET_DIR%\test_report.md" /Y
    echo 已同步测试报告
)

echo 代码同步完成！
echo 同步后的代码目录: %TARGET_DIR%