#!/usr/bin/env bash
# Lifted from: http://blog.daanraman.com/coding/automatically-detect-if-the-android-emulator-is-running/
# and slightly modified
 
OUT=`adb shell getprop init.svc.bootanim`
RES="stopped"
 
while [[ ${OUT:0:7}  != 'stopped' ]]; do
		OUT=`adb shell getprop init.svc.bootanim`
		printf "."
		sleep 2
done
 
echo "Emulator booted!"
