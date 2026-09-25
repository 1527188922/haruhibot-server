@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0.."

echo [INFO] 当前目录: %CD%
echo.

if exist jre (
    echo [INFO] 检测到已存在的 jre 目录，正在删除...
    rmdir /s /q jre
    if exist jre (
        echo [ERROR] 无法删除 jre 目录，请检查是否被占用。
        goto :error
    )
)

echo [INFO] 正在执行 jlink...
call jlink --add-modules ALL-MODULE-PATH --output jre --strip-debug --no-header-files --no-man-pages --compress=zip-6
if errorlevel 1 (
    echo.
    echo [ERROR] jlink 执行失败，错误码: %errorlevel%
    goto :error
)

echo.
echo [INFO] jlink 执行成功，验证 java 版本:
jre\bin\java.exe -version
if errorlevel 1 (
    echo [ERROR] java -version 执行失败
    goto :error
)

echo.
echo [OK] 全部完成。
pause
exit /b 0

:error
echo.
echo [FAILED] 脚本执行失败，请检查上面的错误信息。
pause
exit /b 1