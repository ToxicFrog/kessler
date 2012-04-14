CALL :FindJRE

:RunJAR
set PATH=%PATH%;%JAVAPATH%
java.exe -cp kessler %*
GOTO :EOF

:FindJRE
CALL :TestJRE JavaSoft\Java Runtime Environment
CALL :TestJRE Wow6432Node\JavaSoft\Java Runtime Environment
CALL :TestJRE JavaSoft\Java Development Kit
CALL :TestJRE Wow6432Node\JavaSoft\Java Development Kit
IF "%JAVAPATH%" NEQ "" GOTO :EOF

ECHO Error: could not find a Java installation
ECHO Please make sure Java is properly installed, including java.exe
PAUSE
GOTO :EOF

:TestJRE
SET KIT=%*
call:ReadRegValue VER "HKLM\Software\%KIT%" "CurrentVersion"
IF "%VER%" NEQ "" GOTO :FoundJRE
GOTO :EOF

:FoundJRE
IF "%JAVAPATH%" NEQ "" GOTO :EOF
call:ReadRegValue JAVAPATH "HKLM\Software\%KIT%\%VER%" "JavaHome"
ECHO Found Java at %JAVAPATH%
GOTO :EOF

:ReadRegValue
SET key=%2%
SET name=%3%
SET "%~1="
SET reg=reg
IF DEFINED ProgramFiles(x86) (
  IF EXIST %WINDIR%\sysnative\reg.exe SET reg=%WINDIR%\sysnative\reg.exe
)
FOR /F "usebackq tokens=3* skip=1" %%A IN (`%reg% QUERY %key% /v %name% 2^>NUL`) DO SET "%~1=%%A %%B"
GOTO :EOF
