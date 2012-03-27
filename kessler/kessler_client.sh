#!/bin/bash
echo -ne "\033]0;KSP Kessler Client\007"

cd $(dirname "$0")
echo "Uploading save to server..."
java -cp kessler -jar kessler/client.jar put saves/default/persistent.sfs
echo "Downloading merged save..."
java -cp kessler -jar kessler/client.jar get saves/default/persistent.sfs
echo "Done!"
