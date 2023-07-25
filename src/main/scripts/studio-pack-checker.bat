@echo off

set CWD=%~dp0

:: Set FFmpeg and FFprobe executables if not set in PATH
::set FFMPEG="C:\path\to\ffmpeg"
::set FFPROBE="C:\path\to\ffprobe"

:: Set debug mode
::DEBUG=-Dlogback.configurationFile=logback-dev.xml

:: batch args as java system properties
java %DEBUG% -jar "%CWD%/studio-pack-checker.jar" %*
