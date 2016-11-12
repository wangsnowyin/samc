#!/usr/bin/env bash

. ./readconfig

classpath=$samc_dir/bin
lib=$samc_dir/lib
for j in `ls $lib/*.jar`; do
  classpath=$classpath:$j
done
export CLASSPATH=$CLASSPATH:$classpath

java -cp $CLASSPATH -Dlog4j.configuration=mc_log.properties -Ddmck.dir=WORKING_DIR edu.uchicago.cs.ucare.samc.scm.SCMRunner ./target-sys.conf
