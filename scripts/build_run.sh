#!/bin/bash

PROJECT="auto-refactor"
MAIN=refactor.analysis.Main

mvn clean package
java -cp target/$PROJECT-1.0-SNAPSHOT.jar $MAIN
