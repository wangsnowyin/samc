#!/usr/bin/env bash

if [ $# -ne 2 ]; then
  echo "usage: refreshStorage.sh <samcDir> <nodeId>"
  exit 1
fi

samc_dir=$1
node_id=$2

working_dir=WORKING_DIR

rm -r $working_dir/storage/server$node_id
cp -r $samc_dir/raft/storage/server$node_id $working_dir/storage/
