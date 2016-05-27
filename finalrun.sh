#!/bin/bash

## USAGE: ./finalrun.sh <video> <slides> <id> <output_dir> [<lang> <ocr_config>]
## EXAMPLES:
##    normal: ./finalrun.sh /home/dimitri/test_slides/video.mp4 /home/dimitri/test_slides/slides.pdf test /home/dimitri/test_slides/outdir
##    no OCR: NO_OCR=1 ./finalrun.sh /home/dimitri/test_slides/video.mp4 /home/dimitri/test_slides/slides.pdf test /home/dimitri/test_slides/outdir
##    no OCR and no TOG: NO_OCR=1 NO_TOG=1 ./finalrun.sh /home/dimitri/test_slides/video.mp4 /home/dimitri/test_slides/slides.pdf test /home/dimitri/test_slides/outdir


OCR_EXECUTABLE="/usr/local/bin/Video-OCR-TT"
TOG_EXECUTABLE="java -jar target/tog-1.0-SNAPSHOT-jar-with-dependencies.jar"
SLIDE_EXTRACTOR="python ../slide_detection/convert_synced_pdf_to_images.py"

VIDEO="$1"
PDF="$2"
ID="$3"
RESULTS_DIR="$4"
LANG=${5:-"en"}
OCR_CONFIG=${6:-"/usr/local/share/Video-OCR-TT/configs/config_video-ocr-tt.xml"}

if [[ ! $NO_OCR ]];
then
  $OCR_EXECUTABLE \
   -v $VIDEO \
   -lang $LANG \
   -r $RESULTS_DIR  \
   -config $OCR_CONFIG  \
   -i $ID  \
   -useResultPathForSaving
fi

if [[ ! $NO_TOG ]];
then
  $TOG_EXECUTABLE \
    -slides $PDF \
    -folder $RESULTS_DIR\
    -log "$(dirname $PDF)/$ID.log"
fi

if [[ ! $NO_EXTRACT ]];
then
  $SLIDE_EXTRACTOR \
    --pdf $PDF \
    --id $ID \
    --output $RESULTS_DIR
fi
