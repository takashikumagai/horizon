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
NotificationActivity:D \
BackgroundAudioService:D \
mySessionTag:D \
RecyclerViewAdapter:D \
StorageHelper:D \
StorageSelector:D \
DirectoryNavigator:D \
Playback:D \
FileSystemNavigator:D \
HorizonUtils:D \
LockScreenMediaControl:D \
MediaInfoPopupWindow:D \
MetadataUpdateManager:D \
MediaMetadataUpdateTask:D \
MetadataUpdateRunnable:D \
HrzBroadcastReceiver:D \
HrzNotificationListener:D \
AndroidRuntime:E \
*:S
