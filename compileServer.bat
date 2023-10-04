@if not "%~0"=="%~dp0.\%~nx0" start /min cmd /c,"%~dp0.\%~nx0" %* & goto :eof
cd %CD%/Server
javac -encoding utf-8 MyServer.java