#!/bin/sh

if [ ! $1 ]; then
  echo 'Please specify node ID' > /dev/stderr
  exit 1
fi

config=config
if [ $2 ]; then
  config=$2
fi

leader_election=../bin

java -cp $CLASSPATH:$leader_election edu.uchicago.cs.ucare.example.election.LeaderElectionMain $1 $config > node$1.log &
