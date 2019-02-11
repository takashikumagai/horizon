#!/usr/bin/env bash

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
