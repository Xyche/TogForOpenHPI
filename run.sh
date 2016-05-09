mvn -q package
java -jar target/tog-1.0-SNAPSHOT-jar-with-dependencies.jar \
  -folder $1 \
  -id $2 \
  -log $1/$2.log \
  -mode 1 
