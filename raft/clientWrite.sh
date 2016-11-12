#!/usr/bin/env bash

if [ $# -ne 2 ]; then
  echo "usage: clientWrite.sh <targetSysDir> <testId>"
  exit 1
fi

logcabin_dir=$1
test_id=$2
ALLSERVERS=127.0.0.1:5254,127.0.0.1:5255,127.0.0.1:5256

echo -n jeff | $logcabin_dir/build/Examples/TreeOps --cluster=$ALLSERVERS write /ucare &> /tmp/raft/log/$test_id/client.log &
echo "3:$!" >> pid_file
