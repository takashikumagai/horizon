#!/usr/bin/env bash

./gradlew clean

./gradlew build

./copy_apks_to_my_device.sh
