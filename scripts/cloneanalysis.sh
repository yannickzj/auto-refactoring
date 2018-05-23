#!/bin/bash

export project_dir="projects"
export setting_dir="settings"
export MAVEN_OPTS="-Xms1G -Xmx10G -ea"

working_dir="/home/yannick/workspace/auto-refactoring"
log_dir="logs"

target=commons-collections

run() {
  export project=$1
  #mvn exec:java@test
  mvn compile > $working_dir/$log_dir/${1}.log
}

mvn clean
run $target
#echo "Start benchmarking $PROJECT"
#mvn initialize
#mvn exec:exec@cloneanalysis

#mvn compile exec:exec@test
#mvn initialize
