@echo off
set CP=.;./libraries/*
javac -cp %CP% Agent.java Resource.java Model.java ModelUI.java
if %errorlevel% neq 0 goto end
rem  
java -cp %CP% ModelUI
del *.class
:end