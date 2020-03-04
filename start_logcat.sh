#!/usr/bin/env bash

result=$(sudo adb devices | wc -l)
echo $result

# Connect to the device first if it's not already connected.
if [ "$result" = "2" ]; then
  # 'adb devices' printed only 2 lines -> there is currently no device connected.
  echo "Connecting to the device"
  ./reconnect.sh
fi

mkdir -p log
log_file_path="log/$(date +%Y-%m-%d).log"

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
*:S \
2>&1 | tee ${log_file_path}
