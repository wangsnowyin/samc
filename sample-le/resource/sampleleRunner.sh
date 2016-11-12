#!/usr/bin/env bash

. ./readconfig

classpath=$samc_dir/bin
lib=$samc_dir/lib
for jar in `ls $lib/*.jar`; do
  classpath=$classpath:$jar
done

export CLASSPATH=$CLASSPATH:$classpath

# optional : you can add -p as a parameter, to put a pause in the end of every path execution
java -cp $CLASSPATH -Dlog4j.configuration=mc_log.properties -Ddmck.dir=WORKING_DIR edu.uchicago.cs.ucare.samc.server.LeaderElectionRunner ./target-sys.conf


