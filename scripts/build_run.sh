#!/bin/bash

PROJECT="auto-refactor"
JAR="$PROJECT-1.0-SNAPSHOT-jar-with-dependencies.jar"
MAIN=refactor.analysis.Main

mvn compile exec:exec
