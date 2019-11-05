#!/usr/bin/env bash

. set_device_ip_address.sh

sudo adb kill-server
sudo adb start-server
#sudo adb connect ${device_ip_address}
