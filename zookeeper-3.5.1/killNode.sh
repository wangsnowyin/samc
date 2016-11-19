#!/bin/bash

if [ $# -ne 3 ]; then
  echo "Usage: ./startNode.sh <workingDir> <nodeId> <testId>"
  exit 1
fi
workingDir=$1
nodeId=$2
testId=$3

while read line
do
		kill -9 $line
done < $workingDir/config/zk$nodeId/data/zookeeper_server.pid

echo "Kill node $nodeId." >> $workingDir/console/$testId/zk$nodeId.out
rm $workingDir/config/zk$nodeId/data/zookeeper_server.pid
