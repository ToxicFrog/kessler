DLL=GameData/FundingFloor/Plugins/FundingFloor.dll
KSP=$(HOME)/Games/Steam/Kerbal Space Program
LIBS=Assembly-CSharp.dll,UnityEngine.dll,UnityEngine.CoreModule.dll
FLAGS=-codepage:utf8 -target:library -lib:"$(KSP)/KSP_Data/Managed" -r:$(LIBS)

all: $(DLL)

clean:
	rm -f $(DLL)

$(DLL): *.cs
	mcs $(FLAGS) -out:$@ $^

.PHONY: all clean
