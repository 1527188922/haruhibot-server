@echo off

title GetAdministratorPower
mode con cols=100 lines=20
color 6f

CLS
ECHO.
ECHO ��Start���.ע�⣺��ǰ�°汾�����ļ��������ã�����
ECHO.
ECHO ================================
ECHO ��ȡ�������ļ�����ԱȨ��
ECHO ================================
if exist "%SystemRoot%\SysWOW64" path %path%;%windir%\SysNative;%SystemRoot%\SysWOW64;%~dp0
bcdedit >nul
if '%errorlevel%' NEQ '0' (goto UACPrompt) else (goto UACAdmin)
:UACPrompt
%1 start "" mshta vbscript:createobject("shell.application").shellexecute("""%~0""","::",,"runas",1)(window.close)&exit
exit /B
:UACAdmin
cd /d "%~dp0"
ECHO ȡ��Ȩ�޳ɹ�

set path1="C:\ProgramData\Microsoft\Windows\Start Menu\Programs\StartUp\"
set fileName=%path1%checkMySQL.bat
if exist %fileName% (
		del %fileName%
		goto continue
	) else ( goto continue )

:continue
ECHO ================================
ECHO ��ǰĿ¼:%cd%,���ڶ�ȡ�����ļ�...
for /f "tokens=*" %%i in ('findstr "<id>.*</id>" haruhibot_service.xml')do set "s=%%i"
set "s=%s:"=����%"
for /f "delims=<" %%j in ("%s:*<id>=%")do set "cid=%%j"
set "cid=%cid:����="%"
set value= %cid%
ECHO ================================
echo ��ȡ��������Ϊ:%value%
ECHO ================================


SC QUERY %value%> NUL
if errorlevel 1060 goto notexist
goto exist
:exist 
goto checkstart
:notexist
echo ����ע��windows����...:%value%
haruhibot_service.exe install
goto checkstart
:checkstart
sc query |find /i "%value%" >nul 2>nul
if not errorlevel 1 (goto start) else goto notstart
:start
echo �����Ѿ�����.�����ظ�����...:%value%
goto end
:notstart
net start  %value%
goto end
:end
echo ------- 
echo �����ڽ���15s���Զ��ر�...
ping 127.1 -n 15 >nul



