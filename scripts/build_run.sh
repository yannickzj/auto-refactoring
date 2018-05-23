#!/bin/bash

PROJECT="auto-refactor"
JAR="$PROJECT-1.0-SNAPSHOT-jar-with-dependencies.jar"
MAIN=refactor.analysis.Main

run() {
  echo "Start benchmarking $1"

}


#mvn compile exec:exec@test
export PROJECT_DIR="hello, world"
mvn initialize
