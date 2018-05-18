#!/bin/bash

JAVA_CP="lib/soot/3.0.1/sootclasses-trunk-jar-with-dependencies.jar"

SOOT_CP="inputs/:/Library/Java/JavaVirtualMachines/zulu1.7.0_171.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/zulu1.7.0_171.jdk/Contents/Home/jre/lib/jce.jar:lib/junit-4.12.jar:lib/hamcrest-core-1.3.jar"
#SOOT_CP="inputs/:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/jce.jar:lib/junit-4.12.jar:lib/hamcrest-core-1.3.jar"
SOOT_OPTIONS="-p jb use-original-names:true -f jimple -w"

TARGET_CLASS="refactor.analysis.examples.MainTest"

java -cp $JAVA_CP soot.Main -cp $SOOT_CP $SOOT_OPTIONS $TARGET_CLASS
