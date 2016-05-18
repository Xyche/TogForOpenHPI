#!/bin/zsh

rm -f errors && touch errors
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
#      -useResultPathForSaving && echo "Video OCR for $ID succeeded" >> results || {echo "$ID failed" >> errors }
    if [[ (-f $FOLDER/$ID/recognition/recognition.xml) ]];
    then
    java -jar target/tog-1.0-SNAPSHOT-jar-with-dependencies.jar \
      -folder $FOLDER \
      -id $ID \
      -log logs/$ID.log && echo "java for $ID succeeded" >> errors || {echo "java for $ID failed" >> errors }
    fi
  fi
done




#mvn -q package
#java -jar target/tog-1.0-SNAPSHOT-jar-with-dependencies.jar \
#  -folder $1 \
#  -id $2 \
#  -log $1/$2.log \
#  -mode 1 
