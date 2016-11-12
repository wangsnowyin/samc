#!/bin/sh

config=config
if [ $1 ]; then
  config=$1
fi
numnode=`wc -l $config | awk '{print $1}'`

i=0
while [ $i -lt $numnode ]; do
  java -cp ../bin edu.uchicago.cs.ucare.example.election.LeaderElectionMain $i $config > node$i.log &
  i=`expr $i + 1`
done
