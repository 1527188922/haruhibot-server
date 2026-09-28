@echo off
setlocal
call "%~dp0scripts\build.bat"
set "BUILD_EXIT_CODE=%errorlevel%"
echo.
if "%BUILD_EXIT_CODE%"=="0" (
    echo [SUCCESS] Build completed successfully.
) else (
    echo [FAILED] Build failed with exit code %BUILD_EXIT_CODE%. Please check the output above.
)
echo.
pause
exit /b %BUILD_EXIT_CODE%
