@echo off
cd /d "%~dp0"
echo ====================================================================
echo   KHOI DONG BACKEND HET THONG CUU HO THIEN TAI (DISASTER RESCUE)
echo ====================================================================
echo Dang ket noi toi MySQL (localhost:3306 - disaster_rescue)...
java -jar target\disaster-rescue-backend-1.0.0-SNAPSHOT.jar
pause
