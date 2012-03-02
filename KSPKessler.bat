@ECHO

set SERVER=localhost
set PORT=8988
set NAME=Jeb--nosave

@ECHO Backing up original saves...

set TIMESTAMP=
FOR /F "tokens=1,2,3,4,5,6 delims=-/.:" %%a in ("%DATE%:%TIME%") do @set TIMESTAMP=%%a-%%b-%%c %%d.%%e.%%f

mkdir "saves\backup"
xcopy /E /I "saves\default" "saves\backup\%TIMESTAMP%"

@ECHO Getting merged save from server...

pushd kessler
sfsupload.exe %SERVER% %PORT% %NAME%--nosave ..\saves\default\persistent.sfs tmp.sfs
move tmp.sfs ..\saves\default\persistent.sfs
popd

@ECHO Liftoff!

KSP.exe

@ECHO Sending changes to server...

pushd kessler
sfsupload.exe %SERVER% %PORT% %NAME%--nomerge ..\saves\default\persistent.sfs > NUL
popd

@ECHO Done!

pause
