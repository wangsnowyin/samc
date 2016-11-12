package edu.uchicago.cs.ucare.samc.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import com.almworks.sqlite4java.SQLiteException;

import edu.uchicago.cs.ucare.samc.transition.NodeCrashTransition;
import edu.uchicago.cs.ucare.samc.transition.NodeOperationTransition;
import edu.uchicago.cs.ucare.samc.transition.NodeStartTransition;
import edu.uchicago.cs.ucare.samc.transition.PacketSendTransition;
import edu.uchicago.cs.ucare.samc.transition.Transition;
import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;
import edu.uchicago.cs.ucare.samc.util.ExploredBranchRecorder;
import edu.uchicago.cs.ucare.samc.util.SqliteExploredBranchRecorder;

public abstract class LevelModelChecker extends GuideModelChecker {
    
    protected int numCrash;
    protected int numReboot;
    protected String stateDir;
    protected ExploredBranchRecorder exploredTransitionRecorder;
    protected LinkedList<PacketSendTransition> currentLevelPackets;
    protected LinkedList<Transition> currentLevelTransitions;
    protected LinkedList<LinkedList<PacketSendTransition>> previousLevelPacketInfo;
    protected LinkedList<LinkedList<Transition>> previousLevelTransitionInfo;
    protected LinkedList<NodeCrashTransition> currentCrashList;
    protected LinkedList<Integer> previousLevelNumCrashInfo;
    protected LinkedList<Integer> previousLevelNumRebootInfo;
    protected LinkedList<boolean[]> previousOnlineStatus;

    protected AtomicInteger waitPacket;
    
    public LevelModelChecker(String interceptorName, String ackName, int numNode,
            int numCrash, int numReboot, String globalStatePathDir, String levelRecordDir, 
            String workingDir, WorkloadDriver workloadDriver, String ipcDir) throws FileNotFoundException {
        super(interceptorName, ackName, numNode, globalStatePathDir, null, workingDir, workloadDriver, ipcDir);
        this.numCrash = numCrash;
        this.numReboot = numReboot;
        if (numCrash > numNode || numReboot > numCrash) {
            throw new RuntimeException("The number of node, crash, or reboot is wrong");
        }
//        exploredTransitionRecorder = new FileSystemExploredBranchRecorder(levelRecordDir);
        this.stateDir = levelRecordDir;
        try {
            exploredTransitionRecorder = new SqliteExploredBranchRecorder(levelRecordDir);
        } catch (SQLiteException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
        currentLevelPackets = new LinkedList<PacketSendTransition>();
        previousLevelPacketInfo = new LinkedList<LinkedList<PacketSendTransition>>();
        currentLevelTransitions = new LinkedList<Transition>();
        previousLevelTransitionInfo = new LinkedList<LinkedList<Transition>>();
        previousLevelNumCrashInfo = new LinkedList<Integer>();
        previousLevelNumRebootInfo = new LinkedList<Integer>();
        previousOnlineStatus = new LinkedList<boolean[]>();
        currentCrashList = null;
        resetTest();
    }

    @SuppressWarnings("unchecked")
    public void saveCurrentLevelInfo() {
//        previousLevelTransitions.add((LinkedList<Transition>) currentLevelTransitions.clone());
        previousLevelPacketInfo.add((LinkedList<PacketSendTransition>) currentLevelPackets.clone());
        previousLevelNumCrashInfo.add(numCurrentCrash);
        previousLevelNumRebootInfo.add(numCurrentReboot);
        previousOnlineStatus.add(isNodeOnline.clone());
    }
    
    public void loadPreviousLevelInfo() {
//        currentLevelTransitions = previousLevelTransitions.removeLast();
        currentLevelPackets = previousLevelPacketInfo.removeLast();
        numCurrentCrash = previousLevelNumCrashInfo.removeLast(); 
        numCurrentReboot = previousLevelNumRebootInfo.removeLast();
        isNodeOnline = previousOnlineStatus.removeLast();
        exploredTransitionRecorder.traverseUpward(1);
    }
    
    @Override
    public void resetTest() {
        if (exploredTransitionRecorder == null) {
            return;
        }
        super.resetTest();
//        afterProgramModelChecker = new LevelExplorer(this);
        exploredTransitionRecorder.resetTraversal();
//        exploredTransitionRecorder.noteThisNode(".waiting", "");
        File waiting = new File(stateDir + "/.waiting");
        try {
            waiting.createNewFile();
        } catch (IOException e) {
            LOG.error("", e);
        }
        previousLevelNumCrashInfo.clear();
        previousOnlineStatus.clear();
        currentLevelPackets.clear();
        previousLevelPacketInfo.clear();
        currentLevelTransitions.clear();
        previousLevelTransitionInfo.clear();
        waitPacket = new AtomicInteger();
        numCurrentCrash = 0;
        numCurrentReboot = 0;
    }
    
    protected void recordTestId() {
        exploredTransitionRecorder.noteThisNode(".test_id", testId + "");
    }
    
//    @Override
//    public boolean waitPacket(int toId) throws RemoteException {
//        waitPacket.incrementAndGet();
//        LOG.info("korn wait packet " + toId);
//        boolean getPacket = super.waitPacket(toId);
//        LOG.info("korn get packet " + toId);
//        waitPacket.decrementAndGet();
//        return getPacket;
//    }
    
    /*
    class LevelExplorer extends SteadyStateInformedModelChecker.Explorer {
        
//        boolean[] onlineCarryTable;

         public LevelExplorer(SteadyStateInformedModelChecker checker) {
            super(checker);
        }
       
        @Override
        public void run() {
//            onlineCarryTable = Arrays.copyOf(isNodeOnline, numNode);
            currentLevelPackets = PacketSendTransition.buildTransitions(checker, enabledPackets);
            while (true) {
                try {
                    waitBufferQueueStable();
                } catch (InterruptedException e) {
                    LOG.error("", e);
                    continue;
                }
                LinkedList<InterceptPacket> thisLevelPackets = new LinkedList<InterceptPacket>();
                retrieveTcpEnabledPackets(thisLevelPackets);
                currentLevelPackets.addAll(PacketSendTransition.buildTransitions(checker, thisLevelPackets));
                if (feeder.areAllWorkDone()) {
                    if (currentLevelPackets.size() == 0) {
                        while (waitPacket.get() > 0) {
                            try {
                              Thread.sleep(10);
                          } catch (InterruptedException e) {
                              continue;
                          }
                        }
                        boolean verifiedResult = feeder.verify();
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
                } else if (currentLevelPackets.size() == 0) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        LOG.error(e.toString());
                    }
                    continue;
                }
                saveCurrentLevelInfo();
                LinkedList<Transition> transitions = new LinkedList<Transition>();
                int transitionHash = nextTransitionOrder(currentLevelPackets, transitions);
                if (transitionHash != 0) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Transition order for " + transitionHash + "\n" + 
                                Transition.extract(transitions));
                    }
//                    onlineCarryTable = Arrays.copyOf(isNodeOnline, numNode);
//                    for (Transition t : transitions) {
//                        if (t instanceof NodeCrashTransition) {
//                            int id = (int) ((NodeCrashTransition) t).getId();
//                            onlineCarryTable[id] = false;
//                            currentCrash++;
//                        }
//                    }
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
                    try {
                        waitBufferQueueStable();
                    } catch (InterruptedException e) {
                        LOG.error("", e);
                    }
                    boolean isThereNextLevel = isThereEnabledPacket();
                    exploredTransitionRecorder.noteThisNode(".next_level", isThereNextLevel + "", true);
                    Transition nodeTransition;
//                    saveCurrentLevelInfo();
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
                                 waitBufferQueueStable();
                             } catch (IOException e) {
                                 LOG.error("", e);
                             } catch (InterruptedException e) {
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
    */
    
    protected Transition getNextCrashOrReboot(LinkedList<Transition> transitions) {
        if (numCurrentCrash < numCrash) {
            for (int i = 0; i < numNode; ++i) {
                if (isNodeOnline(i)) {
                    return new NodeCrashTransition(this, i);
                }
            }
        }
        if (numCurrentReboot < numReboot) {
            for (int i = 0; i < numNode; ++i) {
                if (!isNodeOnline(i)) {
                    return new NodeStartTransition(this, i);
                }
            }
        }
        return null;
    }
    
    protected LinkedList<NodeCrashTransition> getPossibleCrash(boolean[] onlineStatus) {
        LinkedList<NodeCrashTransition> result = new LinkedList<NodeCrashTransition>();
        for (int i = 0; i < numNode; ++i) {
            if (onlineStatus[i]) {
                result.add(new NodeCrashTransition(this, i));
            }
        }
        return result;
    }
    
    protected LinkedList<NodeStartTransition> getPossibleReboot(boolean[] onlineStatus) {
        LinkedList<NodeStartTransition> result = new LinkedList<NodeStartTransition>();
        for (int i = 0; i < numNode; ++i) {
            if (!onlineStatus[i]) {
                result.add(new NodeStartTransition(this, i));
            }
        }
        return result;
    }
    
    protected LinkedList<LinkedList<NodeOperationTransition>> getAllPossibleCrashReboot() {
        LinkedList<LinkedList<NodeOperationTransition>> result = new LinkedList<LinkedList<NodeOperationTransition>>();
        int availCrash = numCrash - numCurrentCrash;
        int availReboot = numReboot - numCurrentReboot;
        availReboot = availReboot > availCrash ? availCrash : availReboot;
        for (int i = 0; i < availCrash + 1; ++i) {
            for (int j = 0; j < availReboot + 1; ++j) {
                if (i != 0 || j != 0) {
                    generateAllPossibleCrashReboot(result, isNodeOnline.clone(), i, j,
                            0, 0, new LinkedList<NodeOperationTransition>());
                }
            }
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    protected void generateAllPossibleCrashReboot(LinkedList<LinkedList<NodeOperationTransition>> result, 
            boolean[] onlineStatus, int numCrash, int numReboot, int curCrash, int curReboot, 
            LinkedList<NodeOperationTransition> buff) {
        if (curCrash == numCrash && curReboot == numReboot) {
            result.add((LinkedList<NodeOperationTransition>) buff.clone());
        } else {
            if (curCrash < numCrash) {
                LinkedList<NodeCrashTransition> possibleCrash = getPossibleCrash(onlineStatus);
                for (NodeCrashTransition crash : possibleCrash) {
                    buff.add(crash);
                    onlineStatus[crash.getId()] = false;
                    generateAllPossibleCrashReboot(result, onlineStatus, numCrash, numReboot, curCrash + 1, curReboot, buff);
                    onlineStatus[crash.getId()] = true;
                    buff.removeLast();
                }
            }
            if (curReboot < numReboot) {
                LinkedList<NodeStartTransition> possibleReboot = getPossibleReboot(onlineStatus);
                for (NodeStartTransition reboot : possibleReboot) {
                    buff.add(reboot);
                    onlineStatus[reboot.getId()] = true;
                    generateAllPossibleCrashReboot(result, onlineStatus, numCrash, numReboot, curCrash, curReboot + 1, buff);
                    onlineStatus[reboot.getId()] = false;
                    buff.removeLast();
                }
            }
        }
    }
    
    /*
    protected LinkedList<LinkedList<NodeCrashTransition>> getAllCrashes() {
        LinkedList<LinkedList<NodeCrashTransition>> allCrashes = 
                new LinkedList<LinkedList<NodeCrashTransition>>();
        getAllCrashes(allCrashes, new LinkedList<NodeCrashTransition>());
        return  allCrashes;
    }
  
    @SuppressWarnings("unchecked")
    protected void getAllCrashes(LinkedList<LinkedList<NodeCrashTransition>> allCrashes, 
            LinkedList<NodeCrashTransition> crashes) {
        if (crashes.size() == numCrash) {
            allCrashes.add((LinkedList<NodeCrashTransition>) crashes.clone());
        } else {
            for (int i = 0; i < numNode; ++i) {
                NodeCrashTransition crash = new NodeCrashTransition(this, i);
                crashes.addLast(crash);
                getAllCrashes(allCrashes, crashes);
                crashes.removeLast();
            }
        }
    }

    protected HashMap<LinkedList<NodeCrashTransition>, LinkedList<LinkedList<NodeStartTransition>>> 
        getAllReboots(LinkedList<LinkedList<NodeCrashTransition>> allCrashes) {
        HashMap<LinkedList<NodeCrashTransition>, LinkedList<LinkedList<NodeStartTransition>>> allReboots = 
                new HashMap<LinkedList<NodeCrashTransition>, LinkedList<LinkedList<NodeStartTransition>>>();
        for (LinkedList<NodeCrashTransition> crashes : allCrashes) {
            allReboots.put(crashes, getReboots(crashes));
        }
        return allReboots;
    }
    
    protected LinkedList<LinkedList<NodeStartTransition>> getReboots(LinkedList<NodeCrashTransition> crashes) {
        LinkedList<LinkedList<NodeStartTransition>> allReboots = new LinkedList<LinkedList<NodeStartTransition>>();
        NodeCrashTransition[] crashArray = new NodeCrashTransition[crashes.size()];
        getReboots(crashes.toArray(crashArray), allReboots, new LinkedList<NodeStartTransition>(), 0);
        return allReboots;
    }
    
    @SuppressWarnings("unchecked")
    protected void getReboots(NodeCrashTransition[] crashes, LinkedList<LinkedList<NodeStartTransition>> allReboots, 
            LinkedList<NodeStartTransition> reboots, int i) {
        if (reboots.size() == numReboot) {
            allReboots.add((LinkedList<NodeStartTransition>) reboots.clone());
        } else {
            for (int j = i; j < crashes.length - (numReboot - reboots.size()) + 1; ++j) {
                NodeStartTransition reboot = new NodeStartTransition(this, crashes[j].getId());
                reboots.addLast(reboot);
                getReboots(crashes, allReboots, reboots, j + 1);
                reboots.removeLast();
            }
        }
    }
  
  */
}