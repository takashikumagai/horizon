#!/usr/bin/env python3

# Run 'sudo pip3 install cairosvg' before executing this script for the first time
import cairosvg

# Drawables usually come in these sizes.
# - mdpi: 24x24 pixels
# - hdpi: 32x32 pixels
# - xhdpi: 48x48 pixels
# - xxhdpi: 72x72 pixels
# - xxxhdpi: 96x96 pixels


# Creates drawable resources (PNG image files) from the given SVG files assuming
# that there are a complete set of them (m, h, xh, xxh, and xxxh)
def convert_drawables(img_name_stem):
    cairosvg.svg2png(url='svg/{}-mdpi.svg'.format(img_name_stem),    write_to='../app/src/main/res/drawable-mdpi/{}.png'.format(img_name_stem))
    cairosvg.svg2png(url='svg/{}-hdpi.svg'.format(img_name_stem),    write_to='../app/src/main/res/drawable-hdpi/{}.png'.format(img_name_stem))
    cairosvg.svg2png(url='svg/{}-xhdpi.svg'.format(img_name_stem),   write_to='../app/src/main/res/drawable-xhdpi/{}.png'.format(img_name_stem))
    cairosvg.svg2png(url='svg/{}-xxhdpi.svg'.format(img_name_stem),  write_to='../app/src/main/res/drawable-xxhdpi/{}.png'.format(img_name_stem))
    cairosvg.svg2png(url='svg/{}-xxxhdpi.svg'.format(img_name_stem), write_to='../app/src/main/res/drawable-xxxhdpi/{}.png'.format(img_name_stem))

convert_drawables('app_icon')
convert_drawables('audio_file')
convert_drawables('close')
convert_drawables('folder')
convert_drawables('newtab')
convert_drawables('next')
convert_drawables('pause')
convert_drawables('play')
convert_drawables('prev')
convert_drawables('stop')
