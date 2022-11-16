DLL=GameData/FundingFloor/Plugins/FundingFloor.dll
KSP=$(HOME)/Games/Steam/Kerbal Space Program
LIBS=Assembly-CSharp.dll,UnityEngine.dll,UnityEngine.CoreModule.dll
FLAGS=-codepage:utf8 -target:library -lib:"$(KSP)/KSP_Data/Managed" -r:$(LIBS)

all: $(DLL)

deploy: $(DLL)
	rsync -aPhv --delete GameData/FundingFloor/ durandal:/cygdrive/f/ksp-devel/GameData/FundingFloor/

clean:
	rm -f $(DLL)

$(DLL): *.cs
	mcs $(FLAGS) -out:$@ $^

.PHONY: all clean
