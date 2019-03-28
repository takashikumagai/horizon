#!/usr/bin/env python3

# Run 'sudo pip3 install cairosvg' before executing this script for the first time
import cairosvg

# Drawables size proportions and their size variations in of this app.
# 
# mdpi    1.0   24x24   64x64
# hdpi    1.5   32x32   96x96
# xhdpi   2.0   48x48   128x128
# xxhdpi  3.0   72x72   192.192
# xxxhdpi 4.0   96x96   256x256


# Creates drawable resources (PNG image files) from the given SVG files assuming
# that there are a complete set of them (m, h, xh, xxh, and xxxh)
def convert_drawables(folder, img_name_stem):
    cairosvg.svg2png(url='svg/{}-mdpi.svg'.format(img_name_stem),    write_to='../app/src/main/res/{0}-mdpi/{1}.png'.format(folder,img_name_stem))
    cairosvg.svg2png(url='svg/{}-hdpi.svg'.format(img_name_stem),    write_to='../app/src/main/res/{0}-hdpi/{1}.png'.format(folder,img_name_stem))
    cairosvg.svg2png(url='svg/{}-xhdpi.svg'.format(img_name_stem),   write_to='../app/src/main/res/{0}-xhdpi/{1}.png'.format(folder,img_name_stem))
    cairosvg.svg2png(url='svg/{}-xxhdpi.svg'.format(img_name_stem),  write_to='../app/src/main/res/{0}-xxhdpi/{1}.png'.format(folder,img_name_stem))
    cairosvg.svg2png(url='svg/{}-xxxhdpi.svg'.format(img_name_stem), write_to='../app/src/main/res/{0}-xxxhdpi/{1}.png'.format(folder,img_name_stem))

convert_drawables('drawable', 'app_icon')
convert_drawables('drawable', 'audio_file')
convert_drawables('drawable', 'close')
convert_drawables('drawable', 'folder')
convert_drawables('drawable', 'newtab')
convert_drawables('drawable', 'next')
convert_drawables('drawable', 'pause')
convert_drawables('drawable', 'play')
convert_drawables('drawable', 'prev')
convert_drawables('drawable', 'stop')
convert_drawables('drawable', 'resume')
convert_drawables('mipmap', 'ic_launcher_round')
convert_drawables('mipmap', 'ic_launcher')
