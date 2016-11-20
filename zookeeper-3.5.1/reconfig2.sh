#!/usr/bin/env bash

. ./readconfig

classpath=$target_sys_dir/build/classes
classpath=$classpath:$working_dir
lib=$target_sys_dir/build/lib
for k in `ls $lib/*.jar`; do
	classpath=$classpath:$j
done

export CLASSPATH=$CLASSPATH:$classpath

java -cp $CLASSPATH zkTest.ReconfigTwo 
