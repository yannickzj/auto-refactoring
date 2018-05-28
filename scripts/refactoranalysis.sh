#!/bin/bash

export project_dir="projects"
export setting_dir="settings"
export MAVEN_OPTS="-Xms1G -Xmx10G -ea"

working_dir=$PWD
log_dir="logs"

target=gson

run() {
  export project=$1
  #mvn exec:java@test
  mvn compile > $working_dir/$log_dir/${1}.log
}

mvn clean

echo "Start benchmarking $target"
#run $target
echo $working_dir
echo "Finish benchmarking $target"
#mvn initialize
#mvn exec:java@cloneanalysis
