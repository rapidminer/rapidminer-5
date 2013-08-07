@echo off
SETLOCAL ENABLEDELAYEDEXPANSION

rem ##############################################################
rem ###                                                        ###
rem ###            Windows Start Script for RapidMiner         ###
rem ###                                                        ###
rem ###  This script tries to determine the location of        ###
rem ###  RapidMiner, searches for a proper Java executable     ###
rem ###  and start the program.                                ###
rem ###                                                        ###
rem ###  Please adapt the line containing MAX_JAVA_MEMORY in   ###
rem ###  order to allow for more memory usage.                 ###
rem ###  Alternatively, you can define an environment variable ###
rem ###  MAX_JAVA_MEMORY!                                      ###
rem ###                                                        ###
rem ###  You might also want to add a JDBC driver library in   ###
rem ###  the line with RAPIDMINER_JDBC_DRIVERS!                ###
rem ###                                                        ###
rem ##############################################################


rem ##########################################
rem ###                                    ###
rem ###  Setting Maximal Amount of Memory  ###
rem ###                                    ###
rem ##########################################

if "%MAX_JAVA_MEMORY%"=="" set MAX_JAVA_MEMORY=1024


rem ###########################################
rem ###                                     ###
rem ###  Setting Additional Operators Path  ###
rem ###                                     ###
rem ###########################################

if "%RAPIDMINER_OPERATORS_ADDITIONAL%"=="" set RAPIDMINER_OPERATORS_ADDITIONAL=


rem ##########################################
rem ###                                    ###
rem ###  Setting JDBC Driver Libraries     ###
rem ###                                    ###
rem ##########################################

if "%RAPIDMINER_JDBC_DRIVERS%"=="" set RAPIDMINER_JDBC_DRIVERS=


rem #############################################
rem ###                                       ###
rem ###  Setting or Guessing RAPIDMINER_HOME  ###
rem ###                                       ###
rem #############################################

rem ###  set RAPIDMINER_HOME to the correct directory if you changed the location of this start script  ###

if "%RAPIDMINER_HOME%"=="" goto guessrapidminerhome
goto javahome

:guessrapidminerhome
set RAPIDMINER_BATCHDIR=%~dp0
set RAPIDMINER_HOME=%RAPIDMINER_BATCHDIR%..
echo RAPIDMINER_HOME is not set. Trying the directory '%RAPIDMINER_HOME%'...
goto javahome


rem ############################
rem ###                      ###
rem ###  Searching for Java  ###
rem ###                      ###
rem ############################

:javahome
set LOCAL_JRE_JAVA=%RAPIDMINER_HOME%\jre\bin\java.exe
if exist "%LOCAL_JRE_JAVA%" goto localjre
goto checkjavahome

:localjre
set JAVA=%LOCAL_JRE_JAVA%
echo Using local jre: %JAVA%...
goto commandlinearguments

:checkjavahome
if "%JAVA_HOME%"=="" goto checkpath
set JAVA_CHECK=%JAVA_HOME%\bin\java.exe
if exist "%JAVA_CHECK%" goto globaljre 
goto error3

:globaljre
set JAVA=%JAVA_HOME%\bin\java
echo Using global jre: %JAVA%...
goto commandlinearguments

:checkpath
java -version 2> nul:
if errorlevel 1 goto error2
goto globaljrepath

:globaljrepath
set JAVA=java
echo Using global jre found on path: %JAVA%
goto commandlinearguments


rem #########################################
rem ###                                   ###
rem ###  Handling Command Line Arguments  ###
rem ###                                   ###
rem #########################################

:commandlinearguments
set CMD_LINE_ARGS=
:args
if [%1]==[] goto update
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto args


rem ###########################
rem ###                     ###
rem ###  Performing Update  ###
rem ###                     ###
rem ###########################

:update
set RUINSTALL_DIR=%RAPIDMINER_HOME%\RUinstall
if exist "%RUINSTALL_DIR%" goto perform_update
goto start

:perform_update
xcopy "%RUINSTALL_DIR%" "%RAPIDMINER_HOME%" /c /s /y /i
rmdir "%RUINSTALL_DIR%" /s /q
goto start


rem #############################
rem ###                       ###
rem ###  Starting RapidMiner  ###
rem ###                       ###
rem #############################

:start
set RAPIDMINER_JAR=%RAPIDMINER_HOME%\lib\rapidminer.jar
set BUILD=%RAPIDMINER_HOME%\build
set RAPIDMINER_CLASSPATH=

if exist "%RAPIDMINER_JAR%" set RAPIDMINER_CLASSPATH=%RAPIDMINER_JAR%
if exist "%BUILD%" set RAPIDMINER_CLASSPATH=%BUILD%
if "%RAPIDMINER_CLASSPATH%"=="" goto error1

set RAPIDMINER_LIBRARIES=
for %%f in ("%RAPIDMINER_HOME%\lib\*.jar") do set RAPIDMINER_LIBRARIES=!RAPIDMINER_LIBRARIES!;%%f
for %%f in ("%RAPIDMINER_HOME%\lib\jdbc\*.jar") do set RAPIDMINER_LIBRARIES=!RAPIDMINER_LIBRARIES!;%%f
for %%f in ("%RAPIDMINER_HOME%\lib\freehep\*.jar") do set RAPIDMINER_LIBRARIES=!RAPIDMINER_LIBRARIES!;%%f

set COMPLETE_CLASSPATH=%RAPIDMINER_CLASSPATH%;%RAPIDMINER_LIBRARIES%
set JDBC_DRIVER_PARAMETER=

if not "%RAPIDMINER_JDBC_DRIVERS%"=="" set COMPLETE_CLASSPATH=%COMPLETE_CLASSPATH%;%RAPIDMINER_JDBC_DRIVERS%
if not "%RAPIDMINER_JDBC_DRIVERS%"=="" set RAPIDMINER_JDBC_DRIVER_PARAMETER=-Drapidminer.init.jdbc.location="%RAPIDMINER_JDBC_DRIVERS%"

echo Starting RapidMiner from '%RAPIDMINER_HOME%' using classes from '%RAPIDMINER_CLASSPATH%'...
rem echo The used classpath is '%COMPLETE_CLASSPATH%'...

:checkJavaVersion
set CHECK_VERSION_FILE="%APPDATA%\check_rm_java_version"
@echo Creating file %CHECK_VERSION_FILE%
"%JAVA%" -version 2>&1 | findstr /i "version" >  %CHECK_VERSION_FILE%

for /F "tokens=3" %%g in ('type %CHECK_VERSION_FILE%') do (
    set JAVAVER=%%g
)

set JAVAVER=%JAVAVER:"=%
@echo Java Version: %JAVAVER%

for /f "delims=. tokens=1-3" %%v in ("%JAVAVER%") do (
	set JAVA_MAJOR=%%v
    set JAVA_MINOR=%%w
    set JAVA_BUILD=%%x
)

rem @echo Major: %JAVA_MAJOR%
rem @echo Minor: %JAVA_MINOR%
rem @echo Build: %JAVA_BUILD%

@echo Removing file %CHECK_VERSION_FILE%
del %CHECK_VERSION_FILE%

if %NUMBER_OF_PROCESSORS% GEQ 2 goto startMultiCoreMode
@echo Starting single core mode...
"%JAVA%" -Xms%MAX_JAVA_MEMORY%m -Xmx%MAX_JAVA_MEMORY%m -classpath "%COMPLETE_CLASSPATH%" -Drapidminer.home="%RAPIDMINER_HOME%" -Drapidminer.operators.additional="%RAPIDMINER_OPERATORS_ADDITIONAL%" %RAPIDMINER_JDBC_DRIVER_PARAMETER% com.rapidminer.RapidMinerCommandLine %CMD_LINE_ARGS%
goto startEnd

:startMultiCoreMode
SET /A NUMBER_OF_GC_THREADS=%NUMBER_OF_PROCESSORS%-1
if %JAVA_MINOR% GEQ 7 goto :startMultiCoreMode_withG1GC
@echo Starting in multicore mode with CMS garbage collector...
"%JAVA%" -XX:+UseParallelGC -XX:+UseParallelOldGC -XX:ParallelGCThreads=%NUMBER_OF_GC_THREADS% -Xms%MAX_JAVA_MEMORY%m -Xmx%MAX_JAVA_MEMORY%m -classpath "%COMPLETE_CLASSPATH%" -Drapidminer.home="%RAPIDMINER_HOME%" -Drapidminer.operators.additional="%RAPIDMINER_OPERATORS_ADDITIONAL%" %RAPIDMINER_JDBC_DRIVER_PARAMETER% com.rapidminer.RapidMinerCommandLine %CMD_LINE_ARGS%

:startMultiCoreMode_withG1GC
@echo Starting in multicore mode with G1 garbage collector...
"%JAVA%" -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -XX:ConcGCThreads=%NUMBER_OF_GC_THREADS% -XX:ParallelGCThreads=%NUMBER_OF_PROCESSORS% -Xms%MAX_JAVA_MEMORY%m -Xmx%MAX_JAVA_MEMORY%m -classpath "%COMPLETE_CLASSPATH%" -Drapidminer.home="%RAPIDMINER_HOME%" -Drapidminer.operators.additional="%RAPIDMINER_OPERATORS_ADDITIONAL%" %RAPIDMINER_JDBC_DRIVER_PARAMETER% com.rapidminer.RapidMinerCommandLine %CMD_LINE_ARGS%
:startEnd

"%JAVA%" 
goto end


rem ########################
rem ###                  ###
rem ###  Error messages  ###
rem ###                  ###
rem ########################

:error1
echo.
echo ERROR: Neither 
echo %RAPIDMINER_JAR% 
echo nor 
echo %BUILD% 
echo was found.
echo If you use the source version of RapidMiner, try 
echo 'ant build' or 'ant dist' first.
echo.
pause
goto end

:error2
echo.
echo ERROR: Java cannot be found. 
echo Please install Java properly (check if JAVA_HOME is 
echo correctly set or ensure that 'java' is part of the 
echo PATH environment variable).
echo.
pause
goto end

:error3
echo.
echo ERROR: Java cannot be found in the path JAVA_HOME
echo Please install Java properly (it seems that the 
echo environment variable JAVA_HOME does not point to 
echo a Java installation).
echo.
pause
goto end

rem #############
rem ###       ###
rem ###  END  ###
rem ###       ###
rem #############

:end
