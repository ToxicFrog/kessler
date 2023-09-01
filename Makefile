NAME=FundingFloor
VERSION=0.1
DLL=GameData/$(NAME)/Plugins/$(NAME).dll
ZIP=$(NAME)-$(VERSION).zip
KSP=$(HOME)/Games/Steam/Kerbal Space Program
LIBS=Assembly-CSharp.dll,UnityEngine.dll,UnityEngine.CoreModule.dll
FLAGS=-codepage:utf8 -target:library -lib:"$(KSP)/KSP_Data/Managed" -r:$(LIBS)

all: $(DLL)

deploy: $(DLL)
	rsync -aPhv --delete GameData/FundingFloor/ durandal:/cygdrive/f/ksp-devel/GameData/FundingFloor/

deploy-prod: $(DLL)
	rsync -aPhv --delete GameData/FundingFloor/ durandal:steam/'Kerbal Space Program'/GameData/FundingFloor/

zip: release/$(ZIP)
release/$(ZIP): $(DLL)
	mkdir -p release
	rm -f $@
	cd GameData && zip -r -9 ../$@ $(NAME)

clean:
	rm -f $(DLL) $(ZIP)

$(DLL): *.cs
	mcs $(FLAGS) -out:$@ $^

.PHONY: all clean
