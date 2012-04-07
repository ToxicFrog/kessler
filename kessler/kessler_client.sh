#!/bin/bash
echo -ne "\033]0;KSP Kessler Client\007"

cd "$(dirname "$0")"
echo "Synchronizing save file with server..."
java -cp kessler -jar kessler/client.jar sync saves/default/persistent.sfs
echo "Done!"
