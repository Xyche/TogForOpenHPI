#!/bin/bash
# 1. lecture video (desktop video) : wird von mein Program gebraucht
# 2. pdf slide file: wird von Xiaoyin's Program gebraucht
# 3. result directory path: all the result files should be stored in this dir, and afterwards a zip file will be created based on this dir for the download
# 4. language info
# 5. config file path von mein Program
# 6. lecture id or name

OCR_EXECUTABLE="/usr/local/bin/Video-OCR-TT"
TOG_EXECUTABLE="java -jar target/tog-1.0-SNAPSHOT-jar-with-dependencies.jar"
SLIDE_EXTRACTOR="python ../slide_detection/convert_slides_to_images.py"

VIDEO="$1"
PDF="$2"
ID="$3"
RESULTS_DIR="$4"
LANG=${6:-"en"}
OCR_CONFIG=${6:-"/usr/local/share/Video-OCR-TT/configs/config_video-ocr-tt.xml"}


$OCR_EXECUTABLE \
 -v $VIDEO \
 -lang $LANG \
 -r $RESULTS_DIR  \
 -config $OCR_CONFIG  \
 -i $ID  \
 -useResultPathForSaving || {echo "OCR failed"; exit 1 }

$TOG_EXECUTABLE \
  -folder $RESULTS_DIR \
  -id $ID \
  # -log /dev/null \
  || {echo "TOG failed"; exit 2 }

$SLIDE_EXTRACTOR \
  -f $RESULTS_DIR \
  -i $ID || {echo "slide extraction failed"; exit 3 }
