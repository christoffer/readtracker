#!/usr/bin/env bash
# Lifted from: http://blog.daanraman.com/coding/automatically-detect-if-the-android-emulator-is-running/
 
OUT=`adb shell getprop init.svc.bootanim`
RES="stopped"
 
while [[ ${OUT:0:7}  != 'stopped' ]]; do
		OUT=`adb shell getprop init.svc.bootanim`
		echo 'Waiting for emulator to fully boot...'
		sleep 1
done
 
echo "Emulator booted!"
