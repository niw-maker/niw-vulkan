@echo off
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat" >nul
rem MESON = pasta Scripts de um venv com meson, ninja, mako, pyyaml, packaging e setuptools
set "PATH=%MESON%;C:\niwmesa\flex;C:\Program Files\Git\cmd;%PATH%"
set CC=cl
set CXX=cl
cd /d C:\niwmesa\mesa-26.3.1
if not exist ..\bmt\build.ninja meson setup ..\bmt --default-library=static --wrap-mode=default -Dbuildtype=release -Db_ndebug=true -Db_vscrt=mt -Dgallium-drivers=zink -Dvulkan-drivers= -Dllvm=disabled -Dplatforms=windows -Dopengl=true -Dgles1=disabled -Dgles2=disabled -Degl=disabled -Dglx=disabled -Dgbm=disabled -Dzlib=enabled -Dshader-cache=disabled -Dvideo-codecs= -Dmin-windows-version=10 -Dvalgrind=disabled -Dlibunwind=disabled -Dintel-rt=disabled
if errorlevel 1 exit /b 1
ninja -C ..\bmt -j 10
