@echo off

title GetAdministratorPower
mode con cols=100 lines=20
color 6f

CLS
ECHO.
ECHO 【Stop命令】.注意：当前新版本，本文件无须配置！！！
ECHO.
ECHO ================================
ECHO 获取批处理文件管理员权限
ECHO ================================
if exist "%SystemRoot%\SysWOW64" path %path%;%windir%\SysNative;%SystemRoot%\SysWOW64;%~dp0
bcdedit >nul
if '%errorlevel%' NEQ '0' (goto UACPrompt) else (goto UACAdmin)
:UACPrompt
%1 start "" mshta vbscript:createobject("shell.application").shellexecute("""%~0""","::",,"runas",1)(window.close)&exit
exit /B
:UACAdmin
cd /d "%~dp0"
ECHO 取得权限成功



ECHO ================================
ECHO 当前目录:%cd%,正在读取配置文件...
for /f "tokens=*" %%i in ('findstr "<id>.*</id>" haruhibot_service.xml')do set "s=%%i"
set "s=%s:"=“”%"
for /f "delims=<" %%j in ("%s:*<id>=%")do set "cid=%%j"
set "cid=%cid:“”="%"
set value= %cid%

ECHO ================================
echo 读取服务名称为:%value%
ECHO ================================

sc query |find /i "%value%" >nul 2>nul
if not errorlevel 1 (goto start) else goto notstart
:start
echo 正在关闭服务...:%value%
net stop %value%
goto checkExist
:notstart
goto checkExist
:checkExist
SC QUERY %value%> NUL
if errorlevel 1060 goto notexist goto exist
:exist
echo 正在卸载服务...
haruhibot_service.exe uninstall
echo -------
echo 卸载成功!
echo ------- 
goto end
:notexist
echo 服务不存在:%value%

goto end
:end
echo ------- 
echo 本窗口将在15s后自动关闭...
ping 127.1 -n 15 >nul



