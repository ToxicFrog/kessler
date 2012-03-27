#!/bin/bash
echo -ne "\033]0;KSP Kessler Server\007"

cd "$(dirname "$0")"
java -cp kessler -jar kessler/server.jar
