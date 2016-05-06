MVN_Q = mvn -q
MVN = mvn

run:
	$(MVN_Q) exec:java

compile:
	$(MVN) compile

build:
	$(MVN) compile

