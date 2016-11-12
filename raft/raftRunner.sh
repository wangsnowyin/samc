#!/usr/bin/env bash

. ./readconfig

classpath=$samc_dir/bin
classpath=$classpath:$working_dir
lib=$samc_dir/lib
for j in `ls $lib/*.jar`; do
  classpath=$classpath:$j
done
export CLASSPATH=$CLASSPATH:$classpath
export PATH=$PATH:bin/

java -Dlog4j.configuration=mc_log.properties -Ddmck.dir=WORKING_DIR edu.uchicago.cs.ucare.samc.raft.RaftRunner -p ./target-sys.conf

