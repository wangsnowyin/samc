#!/usr/bin/env bash

if [ $# -ne 3 ]; then
  echo "usage: startNode.sh <targetSysDir> <nodeId> <testId>"
  exit 1
fi

logcabin_dir=$1
nodeId=$2
testId=$3

logcabin_config_dir=$logcabin_dir/config/logcabin-$nodeId.conf

$logcabin_dir/build/LogCabin -c $logcabin_config_dir -l WORKING_DIR/log/$testId/node-$nodeId.log & 
echo $nodeId":"$! >> pid_file
