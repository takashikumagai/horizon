#!/usr/bin/env bash

echo First, make sure that your phone is connected to PC, then press Enter
read a

adb kill-server

adb devices

adb tcpip 5555

echo Disconnect your phone from your PC, and enter the IP address of your phone
read ip_address

adb connect $ip_address

adb shell
