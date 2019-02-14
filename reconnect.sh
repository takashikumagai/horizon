#!/usr/bin/env bash

. set_device_ip_address.sh

adb kill-server
adb connect ${device_ip_address}
