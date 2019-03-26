#!/usr/bin/env bash

new_fill_color=$1
new_stroke_color=$2

sed -E -i "s/fill:#[A-Fa-f0-9]{6}/fill:#${new_fill_color}/g" \
svg/prev-hdpi.svg svg/prev-mdpi.svg svg/prev-xhdpi.svg svg/prev-xxhdpi.svg svg/prev-xxxhdpi.svg \
svg/play-hdpi.svg svg/play-mdpi.svg svg/play-xhdpi.svg svg/play-xxhdpi.svg svg/play-xxxhdpi.svg \
svg/next-hdpi.svg svg/next-mdpi.svg svg/next-xhdpi.svg svg/next-xxhdpi.svg svg/next-xxxhdpi.svg \
svg/stop-hdpi.svg svg/stop-mdpi.svg svg/stop-xhdpi.svg svg/stop-xxhdpi.svg svg/stop-xxxhdpi.svg \
svg/pause-hdpi.svg svg/pause-mdpi.svg svg/pause-xhdpi.svg svg/pause-xxhdpi.svg svg/pause-xxxhdpi.svg

sed -E -i "s/stroke:#[A-Fa-f0-9]{6}/stroke:#${new_stroke_color}/g" \
svg/prev-hdpi.svg svg/prev-mdpi.svg svg/prev-xhdpi.svg svg/prev-xxhdpi.svg svg/prev-xxxhdpi.svg \
svg/play-hdpi.svg svg/play-mdpi.svg svg/play-xhdpi.svg svg/play-xxhdpi.svg svg/play-xxxhdpi.svg \
svg/next-hdpi.svg svg/next-mdpi.svg svg/next-xhdpi.svg svg/next-xxhdpi.svg svg/next-xxxhdpi.svg \
svg/stop-hdpi.svg svg/stop-mdpi.svg svg/stop-xhdpi.svg svg/stop-xxhdpi.svg svg/stop-xxxhdpi.svg \
svg/pause-hdpi.svg svg/pause-mdpi.svg svg/pause-xhdpi.svg svg/pause-xxhdpi.svg svg/pause-xxxhdpi.svg

