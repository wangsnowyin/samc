#!/bin/bash

if [ $# -ne 4 ]; then
  echo "Usage: ./startNode.sh <workingDir> <targetSysDir> <nodeId> <testId>"
  exit 1
fi
workingDir=$1
targetSysDir=$2
nodeId=$3
testId=$4

classpath=$workingDir
classpath=$classpath:$targetSysDir/build/classes
workingDirLib=$targetSysDir/lib
for j in `ls $workingDirLib/*.jar`; do
  classpath=$classpath:$j
done

java -cp $classpath -Xmx1G -Dzookeeper.log.dir=$workingDir/log/zk$nodeId -Dlog4j.configuration=zk_log.properties org.apache.zookeeper.server.quorum.QuorumPeerMain $workingDir/config/zk$nodeId/zoo.cfg >> $workingDir/console/$testId/zk$nodeId.out &
echo $! >> $workingDir/config/zk$nodeId/data/zookeeper_server.pid


