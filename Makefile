# :noTabs=false:

all: kessler/sfsmerge.exe kessler/sfsupload.exe

kessler/sfsmerge.exe: sfsmerge.lua sfs.lua util/*.lua
	enceladus -t kessler/enceladus.exe -o win32 sfsmerge.lua sfs.lua util/*.lua
	mv sfsmerge.lua-win32 kessler/sfsmerge.exe

kessler/sfsupload.exe: sfsupload.lua sfs.lua util/*.lua
	enceladus -t kessler/enceladus.exe -o win32 sfsupload.lua sfs.lua util/*.lua
	mv sfsupload.lua-win32 kessler/sfsupload.exe
