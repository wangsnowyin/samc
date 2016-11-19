#!/usr/bin/env bash

. ./readconfig

classpath=$samc_dir/bin
classpath=$classpath:$working_dir
lib=$samc_dir/lib
for j in `ls $lib/*.jar`; do
  classpath=$classpath:$j
done

export CLASSPATH=$CLASSPATH:$classpath

java -cp $CLASSPATH -Dlog4j.configuration=mc_log.properties -Ddmck.dir=$working_dir edu.uchicago.cs.ucare.samc.zookeeper2.TestRunner -p ./target-sys.conf
