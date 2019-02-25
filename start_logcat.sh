#!/usr/bin/env bash

result=$(adb devices | wc -l)
echo $resuslt

# Connect to the device first if it's not already connected.
if [ "$result" = "2" ]; then
  # 'adb decices' printed only 2 lines -> there is currently no device connected.
  echo "Connecting to the device"
  ./reconnect.sh
fi

adb logcat \
MainActivity:D \
BackgroundAudioService:D \
mySessionTag:D \
RecyclerViewAdapter:D \
DirectoryNavigation:D \
StorageSelector:D \
DirectoryNavigator:D \
Playback:D \
HorizonUtils:D \
LockScreenMediaControl:D \
AndroidRuntime:E \
*:S
