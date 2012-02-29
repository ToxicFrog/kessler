# :noTabs=false:

all: kessler/sfsmerge.exe kessler/sfsupload.exe KSPKessler.zip

kessler/sfsmerge.exe: sfsmerge.lua sfs.lua util/*.lua
	enceladus -t kessler/enceladus.exe -o win32 sfsmerge.lua sfs.lua util/*.lua
	mv sfsmerge.lua-win32 kessler/sfsmerge.exe

kessler/sfsupload.exe: sfsupload.lua sfs.lua util/*.lua
	enceladus -t kessler/enceladus.exe -o win32 sfsupload.lua sfs.lua util/*.lua
	mv sfsupload.lua-win32 kessler/sfsupload.exe

KSPKessler.zip: KSPKessler.bat $(find kessler)
	rm -f KSPKessler.zip
	zip -ur KSPKessler.zip KSPKessler.bat kessler
