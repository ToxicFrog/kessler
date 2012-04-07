@ECHO OFF
TITLE KSP Kessler Client

ECHO Synchronizing save file with server...
CALL kessler\launcher.bat -jar kessler/client.jar sync saves/default/persistent.sfs
echo "Done!"
