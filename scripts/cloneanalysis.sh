#!/bin/bash

export project_dir="projects"
export setting_dir="settings"
export log_dir="logs"
export MAVEN_OPTS="-Xms1G -Xmx10G -ea"

working_dir=$PWD
target=guava

run() {
  export project=$1
  #mvn exec:java@test
  #mvn exec:java@cloneanalysis
  mvn compile > $working_dir/$log_dir/${1}.log
}

mvn clean

echo "Start analyzing $target on `date`"
echo "Analysis runtime: "
time run $target && sleep 0.1
echo "Finish analyzing $target on `date`"
