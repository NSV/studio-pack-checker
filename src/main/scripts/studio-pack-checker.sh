#!/bin/sh

CWD="`dirname \"$0\"`"

# Set FFmpeg and FFprobe executables if not set in PATH
#export FFMPEG="/path/to/ffmpeg"
#export FFPROBE="/path/to/ffprobe"

# Set debug mode
#DEBUG=-Dlogback.configurationFile=logback-dev.xml

# batch args as java system properties
exec java $DEBUG -jar $CWD/studio-pack-checker.jar $@
