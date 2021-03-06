:: Copyright 2015 Johns Hopkins University
::
:: Licensed under the Apache License, Version 2.0 (the "License");
:: you may not use this file except in compliance with the License.
:: You may obtain a copy of the License at
::
::     http://www.apache.org/licenses/LICENSE-2.0
::
:: Unless required by applicable law or agreed to in writing, software
:: distributed under the License is distributed on an "AS IS" BASIS,
:: WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
:: See the License for the specific language governing permissions and
:: limitations under the License.
@ECHO OFF

java -version 2>nul 1>nul

IF %ERRORLEVEL%==0 (goto java-found) ELSE (goto java-not-found)

:java-found
java -cp dcs-packaging-tool-cli-standalone.jar org.dataconservancy.packaging.tool.cli.PseudoPackageGeneratorApp  %1 %2 %3 %4 %5 %6 %7 %8 %9
goto exit

:java-not-found
echo Java was not found.
echo Please insure that Java is installed
echo and that it is on your command path.
goto exit


:exit
echo Exiting.
@ECHO OFF

SET JAVA_BIN=java
SET EXECUTABLE_JAR=${project.artifactId}-standalone.jar
SET COMMAND_LINE=%JAVA_BIN% -jar %EXECUTABLE_JAR% %1 %2 %3 %4 %5 %6 %7 %8 %9

java -version 2>nul 1>nul

IF %ERRORLEVEL%==0 (goto java-found) ELSE (goto java-not-found)

:java-found
%COMMAND_LINE%
goto exit

:java-not-found
echo Did not find %JAVA_BIN% on the command path.
echo Please insure that Java is installed
echo and that it is on your command path.
goto exit


:exit
echo Exiting.
