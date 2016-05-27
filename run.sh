#!/bin/zsh

rm -f results && touch results
for f in $1/*;
do 
  if [[ -d $f ]];
  then
    ID=$(basename $f);
    FOLDER=$(dirname $f);
#    Video-OCR-TT \
#      -v $FOLDER/$ID/video.mp4 \
#      -lang en \
#      -r $FOLDER/$ID  \
#      -config /usr/local/share/Video-OCR-TT/configs/config_video-ocr-tt.xml  \
#      -i $ID  \
#      -useResultPathForSaving && echo "Video OCR for $ID succeeded" >> results || {echo "$ID failed" >> results }
    #if [[ (-f $FOLDER/$ID/thumbnails.json) ]];
    #then
    java -jar target/tog-1.0-SNAPSHOT-jar-with-dependencies.jar \
      -folder $FOLDER \
      -id $ID \
      -log logs/$ID.log && echo "java for $ID succeeded" >> results || {echo "java for $ID failed" >> results }
    #fi
  fi
done

python /home/dimitri/repos/slide_detection/convert_slides_to_images.py -f $1 --overwrite -s "160x90" 2> /dev/null


#mvn -q package
#java -jar target/tog-1.0-SNAPSHOT-jar-with-dependencies.jar \
#  -folder $1 \
#  -id $2 \
#  -log $1/$2.log \
#  -mode 1 
