package edu.uchicago.cs.ucare.samc.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;

import edu.uchicago.cs.ucare.samc.transition.NodeCrashTransition;
import edu.uchicago.cs.ucare.samc.transition.NodeOperationTransition;
import edu.uchicago.cs.ucare.samc.transition.PacketSendTransition;
import edu.uchicago.cs.ucare.samc.transition.Transition;
import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;

public class DfsLevelModelChecker extends LevelModelChecker {
    
    public DfsLevelModelChecker(String interceptorName, String ackName, int numNode, 
            int numCrash, int numReboot, String globalStatePathDir, String levelRecordDir, 
            String workingDir, WorkloadDriver workloadDriver, String ipcDir) throws FileNotFoundException {
        super(interceptorName, ackName, numNode, numCrash, numReboot, globalStatePathDir, 
                levelRecordDir, workingDir, workloadDriver, ipcDir);
    }
    
    @Override
    public void resetTest() {
        super.resetTest();
        afterProgramModelChecker = new LevelExplorer(this);
    }

    public int nextTransitionOrder(LinkedList<PacketSendTransition> packetTransition, 
            LinkedList<Transition> nextTransitionOrder) {
        Collections.sort(packetTransition, new Comparator<Transition>() {
            public int compare(Transition o1, Transition o2) {
                Integer id1 = o1.getTransitionId();
                Integer id2 = o2.getTransitionId();
                return id1.compareTo(id2);
            }
        });
        nextTransitionOrder.clear();
        nextTransitionOrder.addAll(packetTransition);
        int hash = nextPermutation(nextTransitionOrder, packetTransition.size());
        return hash;
    }
    
    private int nextPermutation(LinkedList<Transition> packets, int n) {
        if (n == 1) {
            int hash = packets.hashCode();
            exploredTransitionRecorder.createChild(hash);
            exploredTransitionRecorder.traverseDownTo(hash);
            byte[] tmp = exploredTransitionRecorder.readThisNode(".next_level");
            exploredTransitionRecorder.traverseUpward(1);
            if (tmp == null) {
                if (!exploredTransitionRecorder.isSubtreeBelowChildFinished(hash)) {
                    return hash;
                }
            } else {
                boolean isThereNextLevel = Boolean.parseBoolean(new String(tmp));
                if (isThereNextLevel && !exploredTransitionRecorder.isSubtreeBelowChildFinished(hash)) {
                    return hash;
                } else if (numCurrentCrash < numCrash || numCurrentReboot < numReboot) {
                    return injectCrash(packets);
                }
            }
            return 0;
        } else {
            for (int i = 0; i < n; ++i) {
                Collections.swap(packets, i, n - 1);
                int hash = nextPermutation(packets, n - 1);
                if (hash != 0) {
                    return hash;
                }
                Collections.swap(packets, i, n - 1);
            }
            return 0;
        }
    }
    
    protected int injectCrash(LinkedList<Transition> packetTransition) {
        LinkedList<LinkedList<NodeOperationTransition>> allCrashes = getAllPossibleCrashReboot();
        LinkedList<Transition> result = new LinkedList<Transition>();
        if (allCrashes.size() == 0) {
            return packetTransition.hashCode();
        }
        int hash = 0;
        for (LinkedList<NodeOperationTransition> crashes : allCrashes) {
            Transition t = packetTransition.removeFirst();
            result.add(t);
            hash = mergePacketsAndCrashes(packetTransition, crashes, result);
            if (hash != 0) {
                packetTransition.clear();
                packetTransition.addAll(result);
                return hash;
            }
            packetTransition.addFirst(t);
            result.clear();
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public int mergePacketsAndCrashes(LinkedList<Transition> packets, 
            LinkedList<NodeOperationTransition> crashes, LinkedList<Transition> result) {
        int transitionHash;
        if (crashes.isEmpty() || packets.isEmpty()) {
            result.addAll(packets);
            result.addAll(crashes);
            transitionHash = result.hashCode();
            if (exploredTransitionRecorder.isSubtreeBelowChildFinished(transitionHash)) {
                result.removeAll(packets);
                result.removeAll(crashes);
                return 0;
            }   
            return transitionHash;
        }   
        Transition nextTransition;
        nextTransition = packets.removeFirst();
        result.add(nextTransition);
        transitionHash = mergePacketsAndCrashes(packets, crashes, result);
        if (transitionHash != 0) {
            return transitionHash;
        }   
        result.removeLast();
        packets.addFirst(nextTransition);
        LinkedList<Transition> clonedPackets = (LinkedList<Transition>) packets.clone();
        nextTransition = crashes.removeFirst();
        result.add(nextTransition);
        if (nextTransition instanceof NodeCrashTransition) {
            long crashId = ((NodeCrashTransition) nextTransition).getId();
            ListIterator<Transition> iter = clonedPackets.listIterator();
            while (iter.hasNext()) {
                PacketSendTransition t = (PacketSendTransition) iter.next();
                if (t.getPacket().getFromId() == crashId || t.getPacket().getToId() == crashId) {
                    iter.remove();
                } 
            }   
        }
        transitionHash = mergePacketsAndCrashes(clonedPackets, crashes, result);
        if (transitionHash != 0) {
            return transitionHash;
        }   
        result.removeLast();
        crashes.addFirst((NodeOperationTransition) nextTransition);
        return 0;
    } 
    
    class LevelExplorer extends ModelCheckingServerAbstract.Explorer {
        
       public LevelExplorer(ModelCheckingServerAbstract checker) {
          super(checker);
      }
     
      @Override
      public void run() {
          currentLevelPackets = PacketSendTransition.buildTransitions(checker, enabledPackets);
          while (true) {
              getOutstandingTcpPacketTransition(currentLevelTransitions);
              if (currentLevelPackets.size() == 0) {
                  while (waitPacket.get() > 0) {
                      try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        continue;
                    }
                  }
                  boolean verifiedResult = workloadDriver.verifier.verify();
                  exploredTransitionRecorder.markBelowSubtreeFinished();
                  recordTestId();
                  exploredTransitionRecorder.noteThisNode(".result", verifiedResult ? "pass" : "failed");
                  LinkedList<Transition> transitions = new LinkedList<Transition>();
                  int depth = exploredTransitionRecorder.getCurrentDepth();
                  for (int i = 0; i < depth; ++i) {
                      loadPreviousLevelInfo();
                      if (nextTransitionOrder(currentLevelPackets, transitions) == 0) {
                          exploredTransitionRecorder.markBelowSubtreeFinished();
                          transitions.clear();
                      } else {
                          break;
                      }
                  }
                  break;
              }
              saveCurrentLevelInfo();
              LinkedList<Transition> transitions = new LinkedList<Transition>();
              int transitionHash = nextTransitionOrder(currentLevelPackets, transitions);
              if (transitionHash != 0) {
                  if (LOG.isDebugEnabled()) {
                      LOG.debug("Transition order for " + transitionHash + "\n" + 
                              Transition.extract(transitions));
                  }
                  exploredTransitionRecorder.createChild(transitionHash);
                  exploredTransitionRecorder.traverseDownTo(transitionHash);
                  exploredTransitionRecorder.noteThisNode(".transitions", 
                          Transition.extract(transitions));
                  for (Transition transition : transitions) {
                      try {
                          if (transition.apply()) {
                              pathRecordFile.write((getGlobalState() + "," + 
                                      transition.getTransitionId() + " ; " + transition.toString() + "\n").getBytes());
                              updateGlobalState();
                          }
                      } catch (IOException e) {
                          LOG.error("", e);
                      }
                  }
                  boolean isThereNextLevel = isThereEnabledPacket();
                  exploredTransitionRecorder.noteThisNode(".next_level", isThereNextLevel + "", true);
                  Transition nodeTransition;
                  currentLevelTransitions.addAll(currentLevelPackets);
                  while (!(isThereNextLevel = isThereEnabledPacket()) && 
                          (nodeTransition = getNextCrashOrReboot(transitions)) != null) {
                       if (nodeTransition.apply()) {
                           try {
                               exploredTransitionRecorder.markBelowSubtreeFinished();
                               pathRecordFile.write((getGlobalState() + "," + 
                                     nodeTransition.getTransitionId() + " ; " + nodeTransition.toString() +  "\n").getBytes());
                               updateGlobalState();
                               transitions.add(nodeTransition);
                               currentLevelTransitions.add(nodeTransition);
                               int hash = transitions.hashCode();
                               exploredTransitionRecorder.traverseUpward(1);
                               exploredTransitionRecorder.createChild(hash);
                               exploredTransitionRecorder.traverseDownTo(hash);
                               exploredTransitionRecorder.noteThisNode(".transitions", 
                                       Transition.extract(transitions));
                               exploredTransitionRecorder.noteThisNode(".next_level", isThereNextLevel + "",  true);
                           } catch (IOException e) {
                               LOG.error("", e);
                           }
                       }
                  }
                  currentLevelPackets.clear();
                  currentLevelTransitions.clear();
              } else if (exploredTransitionRecorder.getCurrentDepth() == 0) {
                  LOG.warn("Finished exploring all states");
                  System.exit(1);
              } else {
                  LOG.error("There might be some errors");
                  workloadDriver.stopEnsemble();
                  System.exit(1);
              }
          }
          resetTest();
      }
  }

}

