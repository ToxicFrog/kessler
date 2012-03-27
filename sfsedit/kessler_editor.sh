#!/bin/bash
echo -ne "\033]0;KSP Kessler Editor\007"

cd $(dirname "$0")
java -cp kessler -jar kessler/sfsedit.jar put saves/default/persistent.sfs
