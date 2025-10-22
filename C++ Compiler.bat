@echo off
cd /d "C:\Users\Rodella\Downloads\mini-compiler-main"
echo Compiling MiniCompilerGUI.java...
"C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin\javac.exe" MiniCompilerGUI.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)
echo Compilation successful! Starting GUI...
"C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin\java.exe" MiniCompilerGUI
pause
