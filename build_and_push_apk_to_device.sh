#!/usr/bin/env bash

# Update drawables (convert SVG to PNG)
cd assets
./update_images.py
cd ..

# Build
./gradlew clean

./gradlew build; gradle_return_code=$?

# echo Gradle exit code: $gradle_return_code

if [ $gradle_return_code -eq 0 ]
then
  ./copy_apks_to_my_device.sh
else
  echo $gradle_return_code
  echo "Build error(s). Check the error message."
fi
