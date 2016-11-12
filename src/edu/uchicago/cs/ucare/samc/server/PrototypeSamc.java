package edu.uchicago.cs.ucare.samc.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;

import com.almworks.sqlite4java.SQLiteException;

import edu.uchicago.cs.ucare.samc.transition.AbstractNodeCrashTransition;
import edu.uchicago.cs.ucare.samc.transition.AbstractNodeOperationTransition;
import edu.uchicago.cs.ucare.samc.transition.AbstractNodeStartTransition;
import edu.uchicago.cs.ucare.samc.transition.DiskWriteTransition;
import edu.uchicago.cs.ucare.samc.transition.NodeCrashTransition;
import edu.uchicago.cs.ucare.samc.transition.NodeOperationTransition;
import edu.uchicago.cs.ucare.samc.transition.NodeStartTransition;
import edu.uchicago.cs.ucare.samc.transition.PacketSendTransition;
import edu.uchicago.cs.ucare.samc.transition.SleepTransition;
import edu.uchicago.cs.ucare.samc.transition.Transition;
import edu.uchicago.cs.ucare.samc.transition.TransitionTuple;
import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;
import edu.uchicago.cs.ucare.samc.util.ExploredBranchRecorder;
import edu.uchicago.cs.ucare.samc.util.LocalState;
import edu.uchicago.cs.ucare.samc.util.SqliteExploredBranchRecorder;

public abstract class PrototypeSamc extends ModelCheckingServerAbstract {
    
    ExploredBranchRecorder exploredBranchRecorder;

    Hashtable<Integer, Set<Transition>> enabledPacketTable;
    LinkedList<LinkedList<TransitionTuple>> dporInitialPaths;
    HashSet<LinkedList<TransitionTuple>> finishedDporInitialPaths;
    HashSet<String> finishedDporInitialPathStrings;
    HashSet<LinkedList<TransitionTuple>> initialPathSecondAttempt;
    LinkedList<TransitionTuple> currentDporPath;
    LinkedList<TransitionTuple> currentExploringPath = new LinkedList<TransitionTuple>();

    String workingDir;
    String stateDir;

    int numCrash;
    int numReboot;
    int currentCrash;
    int currentReboot;
    
    int globalState2;
    LinkedList<boolean[]> prevOnlineStatus;
    
    LinkedList<LocalState[]> prevLocalStates;
    
    @SuppressWarnings("unchecked")
    public PrototypeSamc(String interceptorName, String ackName, int maxId,
            int numCrash, int numReboot, String globalStatePathDir, String packetRecordDir, String workingDir,
            WorkloadDriver workloadDriver, String ipcDir) {
        super(interceptorName, ackName, maxId, globalStatePathDir, workingDir, workloadDriver, ipcDir);
        dporInitialPaths = new LinkedList<LinkedList<TransitionTuple>>();
        finishedDporInitialPaths = new HashSet<LinkedList<TransitionTuple>>();
        finishedDporInitialPathStrings = new HashSet<String>();
        initialPathSecondAttempt = new HashSet<LinkedList<TransitionTuple>>();
        this.numCrash = numCrash;
        this.numReboot = numReboot;
        try {
            File initialPathFile = new File(workingDir + "/initialPaths");
            if (initialPathFile.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(initialPathFile));
                LinkedList<LinkedList<TransitionTuple>> dumbDporInitialPaths = (LinkedList<LinkedList<TransitionTuple>>) ois.readObject();
                for (LinkedList<TransitionTuple> dumbInitPath : dumbDporInitialPaths) {
                    LinkedList<TransitionTuple> initPath = new LinkedList<TransitionTuple>();
                    for (TransitionTuple dumbTuple : dumbInitPath) {
                        initPath.add(TransitionTuple.getRealTransitionTuple(this, dumbTuple));
                    }
                    dporInitialPaths.add(initPath);
                }
                ois.close();
                currentDporPath = dporInitialPaths.poll();
            }
            File finishedInitialPathFile = new File(workingDir + "/finishedInitialPaths");
            if (finishedInitialPathFile.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(finishedInitialPathFile));
                HashSet<LinkedList<TransitionTuple>> dumbFinishedDporInitialPaths = (HashSet<LinkedList<TransitionTuple>>) ois.readObject();
                for (LinkedList<TransitionTuple> dumbFinishedPath : dumbFinishedDporInitialPaths) {
                    LinkedList<TransitionTuple> finishedPath = new LinkedList<TransitionTuple>();
                    for (TransitionTuple dumbTuple : dumbFinishedPath) {
                        finishedPath.add(TransitionTuple.getRealTransitionTuple(this, dumbTuple));
                    }
                    finishedDporInitialPaths.add(finishedPath);
                }
                ois.close();
            } else {
                finishedDporInitialPaths = new HashSet<LinkedList<TransitionTuple>>();
            }
        } catch (FileNotFoundException e1) {
            LOG.warn("", e1);
        } catch (IOException e1) {
            LOG.warn("", e1);
        } catch (ClassNotFoundException e) {
            LOG.warn("", e);
        }
        stateDir = packetRecordDir;
        try {
            exploredBranchRecorder = new SqliteExploredBranchRecorder(packetRecordDir);
        } catch (SQLiteException e) {
            LOG.error("", e);
        }
        this.workingDir = workingDir;
        resetTest();
    }
    
    @Override
    public void resetTest() {
        if (exploredBranchRecorder == null) {
            return;
        }
        super.resetTest();
        currentCrash = 0;
        currentReboot = 0;
        modelChecking = new PathTraversalWorker();
        currentEnabledTransitions = new LinkedList<Transition>();
        currentExploringPath = new LinkedList<TransitionTuple>();
        enabledPacketTable = new Hashtable<Integer, Set<Transition>>();
        exploredBranchRecorder.resetTraversal();
        prevOnlineStatus = new LinkedList<boolean[]>();
        File waiting = new File(stateDir + "/.waiting");
        try {
            waiting.createNewFile();
        } catch (IOException e) {
            LOG.error("", e);
        }
        prevLocalStates = new LinkedList<LocalState[]>();
    }
    
    public Transition nextTransition(LinkedList<Transition> transitions) {
        ListIterator<Transition> iter = transitions.listIterator();
        while (iter.hasNext()) {
            Transition transition = iter.next();
            if (!exploredBranchRecorder.isSubtreeBelowChildFinished(transition.getTransitionId())) {
                iter.remove();
                return transition;
            }
        }
        return null;
    }
    
    public void recordEnabledTransitions(int globalState, LinkedList<Transition> currentEnabledTransitions) {
        if (enabledPacketTable.containsKey(globalState)) {
            ((Set<Transition>) enabledPacketTable.get(globalState)).addAll(currentEnabledTransitions);
        } else {
            enabledPacketTable.put(globalState, new HashSet<Transition>(currentEnabledTransitions));
        }
    }

    protected void adjustCrashAndReboot(LinkedList<Transition> enabledTransitions) {
        int numOnline = 0;
        for (int i = 0; i < numNode; ++i) {
            if (isNodeOnline(i)) {
                numOnline++;
            }
        }
        int numOffline = numNode - numOnline;
        int tmp = numOnline < numCrash - currentCrash ? numOnline : numCrash - currentCrash;
        for (int i = 0; i < tmp; ++i) {
            enabledTransitions.add(new AbstractNodeCrashTransition(this));
            currentCrash++;
//            numOffline++;
        }
        tmp = numOffline < numReboot - currentReboot ? numOffline : numReboot - currentReboot;
        for (int i = 0; i < tmp; ++i) {
            enabledTransitions.add(new AbstractNodeStartTransition(this));
            currentReboot++;
        }
    }
    

    protected void markPacketsObsolete(int obsoleteBy, int crashingNode, LinkedList<Transition> enabledTransitions) {
        ListIterator<Transition> iter = enabledTransitions.listIterator();
        while (iter.hasNext()) {
            Transition t = iter.next();
            if (t instanceof PacketSendTransition) {
                PacketSendTransition p = (PacketSendTransition) t;
                if (p.getPacket().getFromId() == crashingNode || p.getPacket().getToId() == crashingNode) {
                    p.getPacket().setObsolete(true);
                    p.getPacket().setObsoleteBy(obsoleteBy);
                }
            } else if (t instanceof DiskWriteTransition) {
                DiskWriteTransition w = (DiskWriteTransition) t;
                if (w.getWrite().getNodeId() == crashingNode) {
                    w.setObsolete(true);
                    w.setObsoleteBy(obsoleteBy);
                }
            }
        }
    }
    
    protected void removeCrashedSenderPackets(int crashedSender, LinkedList<Transition> enabledTransitions) {
        ListIterator<Transition> iter = enabledTransitions.listIterator();
        while (iter.hasNext()) {
            Transition t = iter.next();
            if (t instanceof PacketSendTransition) {
                if (((PacketSendTransition) t).getPacket().getFromId() == crashedSender) {
                    iter.remove();
                }
            }
        }
    }
    
    public void updateGlobalState2() {
        int prime = 31;
        globalState2 = getGlobalState();
        globalState2 = prime * globalState2 + currentEnabledTransitions.hashCode();
        for (int i = 0; i < numNode; ++i) {
            for (int j = 0; j < numNode; ++j) {
                globalState2 = prime * globalState2 + Arrays.hashCode(messagesQueues[i][j].toArray());
            }
        }
    }
    
    protected int getGlobalState2() {
        return globalState2;
    }
    
    protected void convertExecutedAbstractTransitionToReal(LinkedList<TransitionTuple> executedPath) {
        ListIterator<TransitionTuple> iter = executedPath.listIterator();
        while (iter.hasNext()) {
            TransitionTuple iterItem = iter.next();
            if (iterItem.transition instanceof AbstractNodeCrashTransition) {
                AbstractNodeCrashTransition crash = (AbstractNodeCrashTransition) iterItem.transition;
                iter.set(new TransitionTuple(iterItem.state, new NodeCrashTransition(PrototypeSamc.this, crash.id)));
            } else if (iterItem.transition instanceof AbstractNodeStartTransition) {
                AbstractNodeStartTransition start = (AbstractNodeStartTransition) iterItem.transition;
                iter.set(new TransitionTuple(iterItem.state, new NodeStartTransition(PrototypeSamc.this, start.id)));
            }
        }
    }
    
    protected String pathToString(LinkedList<TransitionTuple> dporInitialPath) {
        String path = "";
        for (TransitionTuple tuple : dporInitialPath) {
            path += "/" + tuple.transition.getTransitionId(); 
        }
        return path;
    }
    
    protected void addToDporInitialPathList(LinkedList<TransitionTuple> dporInitialPath) {
        convertExecutedAbstractTransitionToReal(dporInitialPath);
        String path = pathToString(dporInitialPath);
        if (!finishedDporInitialPaths.contains(dporInitialPath) && !finishedDporInitialPathStrings.contains(path)) {
            dporInitialPaths.add(dporInitialPath);
            finishedDporInitialPaths.add(dporInitialPath);
            finishedDporInitialPathStrings.add(path);
        } else {
//            log.info("This dependent is duplicated so we will drop it");
        }
    }
    
    @SuppressWarnings("unchecked")
    protected void addNewDporInitialPath(LinkedList<TransitionTuple> initialPath, 
            TransitionTuple oldTransition, TransitionTuple newTransition) {
        LinkedList<TransitionTuple> oldPath = (LinkedList<TransitionTuple>) initialPath.clone();
        convertExecutedAbstractTransitionToReal(oldPath);
        oldPath.add(new TransitionTuple(0, oldTransition.transition));
        String oldPathStr = pathToString(oldPath);
        finishedDporInitialPaths.add(oldPath);
        finishedDporInitialPathStrings.add(oldPathStr);
        LinkedList<TransitionTuple> newDporInitialPath = (LinkedList<TransitionTuple>) initialPath.clone();
        convertExecutedAbstractTransitionToReal(newDporInitialPath);
        newDporInitialPath.add(newTransition);
        String newPath = pathToString(newDporInitialPath);
        if (!finishedDporInitialPaths.contains(newDporInitialPath) && !finishedDporInitialPathStrings.contains(newPath)) {
            LOG.info("Transition " + newTransition.transition + " is dependent with " + oldTransition.transition + " at state " + oldTransition.state + " " + newDporInitialPath.hashCode());
            dporInitialPaths.add(newDporInitialPath);
            finishedDporInitialPaths.add(newDporInitialPath);
            finishedDporInitialPathStrings.add(newPath);
        }
    }
    
    protected void saveDPORInitialPaths() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(workingDir + "/initialPaths"));
            LinkedList<LinkedList<TransitionTuple>> dumbDporInitialPaths = new LinkedList<LinkedList<TransitionTuple>>();
            for (LinkedList<TransitionTuple> initPath : dporInitialPaths) {
                LinkedList<TransitionTuple> dumbPath = new LinkedList<TransitionTuple>();
                for (TransitionTuple realTuple : initPath) {
                    dumbPath.add(realTuple.getSerializable());
                }
                dumbDporInitialPaths.add(dumbPath);
            }
            oos.writeObject(dumbDporInitialPaths);
            oos.close();
            HashSet<LinkedList<TransitionTuple>> dumbFinishedDporInitialPaths = new HashSet<LinkedList<TransitionTuple>>();
            for (LinkedList<TransitionTuple> finishedPath : finishedDporInitialPaths) {
                LinkedList<TransitionTuple> dumbPath = new LinkedList<TransitionTuple>();
                for (TransitionTuple realTuple : finishedPath) {
                    dumbPath.add(realTuple.getSerializable());
                }
                dumbFinishedDporInitialPaths.add(dumbPath);
            }
            oos = new ObjectOutputStream(new FileOutputStream(workingDir + "/finishedInitialPaths"));
            oos.writeObject(dumbFinishedDporInitialPaths);
            oos.close();
        } catch (FileNotFoundException e) {
            LOG.error("", e);
        } catch (IOException e) {
            LOG.error("", e);
        }
    }
    
    protected void findDPORInitialPaths() {
        calculateDPORInitialPaths();
        LOG.info("There are " + dporInitialPaths.size() + " initial path of DPOR");
        int i = 1;
        for (LinkedList<TransitionTuple> path : dporInitialPaths) {
            String tmp = "DPOR path no. " + i++ + "\n";
            for (TransitionTuple tuple : path) {
                tmp += tuple.toString() + "\n";
            }
            LOG.info(tmp);
        }
        saveDPORInitialPaths();
    }
    
    protected int findTransition(Transition transition){
    	int result = -1;
    	for(int index = 0; index<currentEnabledTransitions.size(); index++){
    		if(transition.getTransitionId() == currentEnabledTransitions.get(index).getTransitionId()){
    			result = index;
    			break;
    		}
    	}
    	return result;
    }

    protected abstract void calculateDPORInitialPaths();
    
    class PathTraversalWorker extends Thread {
        
		@Override
        public void run() {
			if (currentDporPath != null) {
                LOG.info("There is existing DPOR initial path, start with this path first");
                String tmp = "DPOR initial path\n";
                for (TransitionTuple tuple : currentDporPath) {
                    tmp += tuple.toString() + "\n";
                }
                LOG.info(tmp);
                int tupleCounter = 0;
                for (TransitionTuple tuple : currentDporPath) {
                	tupleCounter++;
                	updateSAMCQueue();
                    updateGlobalState2();
                    recordEnabledTransitions(globalState2, currentEnabledTransitions);
                    boolean isThereThisTuple = false;
                    for (int i = 0; i < 50; ++i) {
                        if (tuple.transition instanceof NodeCrashTransition) {
                            isThereThisTuple = currentEnabledTransitions.remove(new AbstractNodeCrashTransition(null));
                        } else if (tuple.transition instanceof NodeStartTransition) {
                            isThereThisTuple = currentEnabledTransitions.remove(new AbstractNodeStartTransition(null));
                        } else if (tuple.transition instanceof SleepTransition){
                        	isThereThisTuple = true;
                        } else {
                            int indexOfTuple = findTransition(tuple.transition);
                            isThereThisTuple = indexOfTuple != -1;
                            if (isThereThisTuple) {
                                tuple.transition = (Transition) currentEnabledTransitions.remove(indexOfTuple);
                            }
                        }
                        if (isThereThisTuple) {
                            break;
                        } else {
                            try {
                                Thread.sleep(300);
                                updateSAMCQueue();
                            } catch (InterruptedException e) {
                                LOG.error("", e);
                            }
                        }
                    }
                    if (!isThereThisTuple) {
                        LOG.error("Being in wrong state, there is not transition " + 
                                tuple.transition.getTransitionId() + " to apply");
                        try {
                            pathRecordFile.write("no transition\n".getBytes());
                        } catch (IOException e) {
                            LOG.error("", e);
                        }
                        if (!initialPathSecondAttempt.contains(currentDporPath)) {
                            LOG.warn("Try this initial path one more time");
                            dporInitialPaths.addFirst(currentDporPath);
                            initialPathSecondAttempt.add(currentDporPath);
                        }
                        if (dporInitialPaths.size() == 0) {
                            exploredBranchRecorder.resetTraversal();
                            exploredBranchRecorder.markBelowSubtreeFinished();
                        } else {
                            currentDporPath = dporInitialPaths.remove();
                        }
                        LOG.error("ERROR: Expected to execute " + tuple.transition.getTransitionId() + ", but the event is not in queue.");
                    	LOG.warn("---- Quit of Path Execution because an error ----");
                        resetTest();
                        return;
                    }
                    if(tupleCounter >= initialPath.size()){
	                    exploredBranchRecorder.createChild(tuple.transition.getTransitionId());
	                    exploredBranchRecorder.traverseDownTo(tuple.transition.getTransitionId());
                    }
                    try {
                        currentExploringPath.add(new TransitionTuple(globalState2, tuple.transition));
                        prevOnlineStatus.add(isNodeOnline.clone());
                        prevLocalStates.add(localStates.clone());
                        saveLocalState();
                        if (tuple.transition instanceof AbstractNodeOperationTransition) {
                            AbstractNodeOperationTransition nodeOperationTransition = (AbstractNodeOperationTransition) tuple.transition;
                            tuple.transition = ((AbstractNodeOperationTransition) tuple.transition).getRealNodeOperationTransition();
                            nodeOperationTransition.id = ((NodeOperationTransition) tuple.transition).getId();
                        }
                        if (tuple.transition.apply()) {
                            pathRecordFile.write((tuple.transition.toString() + "\n").getBytes());
                            updateGlobalState();
                            updateSAMCQueueAfterEventExecution(tuple.transition);
                        }
                    } catch (IOException e) {
                        LOG.error("", e);
                    }
                }
            }
            LOG.info("Try to find new path/Continue from DPOR initial path");
            boolean hasWaited = false;
            while (true) {
            	updateSAMCQueue();
                updateGlobalState2();
                recordEnabledTransitions(globalState2, currentEnabledTransitions);
            	boolean terminationPoint = checkTerminationPoint(currentEnabledTransitions);
                if (terminationPoint && hasWaited) {
                    boolean verifiedResult = verifier.verify();
                    String detail = verifier.verificationDetail();
                    saveResult(verifiedResult + " ; " + detail + "\n");
                    exploredBranchRecorder.markBelowSubtreeFinished();
                    findDPORInitialPaths();
                    if (dporInitialPaths.size() == 0) {
                        exploredBranchRecorder.resetTraversal();
                        exploredBranchRecorder.markBelowSubtreeFinished();
                        LOG.warn("There is no more interesting Initial Paths. "
                        		+ "Finished exploring all states.");
                        workloadDriver.stopEnsemble();
                        System.exit(0);
                    } else {
                        currentDporPath = dporInitialPaths.remove();
                    }
                	LOG.info("---- End of Path Execution ----");
                    resetTest();
                    break;
                } else if (terminationPoint) {
                    try {
                    	if(interceptorName.equals("raftModelChecker") && waitForNextLE && waitedForNextLEInDiffTermCounter < 20){
	                        Thread.sleep(leaderElectionTimeout);
	                    } else {
	                    	hasWaited = true;
	                    	LOG.debug("Wait for any long process");
	                    	Thread.sleep(waitEndExploration);
	                    }
                    } catch (InterruptedException e) {
                    	e.printStackTrace();
                    }
                    continue;
                }
                hasWaited = false;
                Transition transition;
                boolean recordPath = true;
                if(hasInitialPath && !hasFinishedInitialPath && currentDporPath == null){
                	LOG.info("Next transition is directed by initialPath");
                	transition = nextInitialTransition();
                	recordPath = false;
                } else {
                	transition = nextTransition(currentEnabledTransitions);
                }
                if (transition != null) {
                    if(recordPath){
	                    exploredBranchRecorder.createChild(transition.getTransitionId());
	                    exploredBranchRecorder.traverseDownTo(transition.getTransitionId());
                    }
                    boolean cleanTransition = verifier.verifyNextTransition(transition);
                	if(!cleanTransition){
                		boolean verifiedResult = verifier.verify();
                        String detail = verifier.verificationDetail();
                        saveResult(verifiedResult + " ; " + detail + "\n");
                        exploredBranchRecorder.markBelowSubtreeFinished();
                        findDPORInitialPaths();
                        if (dporInitialPaths.size() == 0) {
                            exploredBranchRecorder.resetTraversal();
                            exploredBranchRecorder.markBelowSubtreeFinished();
                            LOG.warn("There is no more interesting Initial Paths. "
                            		+ "Finished exploring all states.");
                            workloadDriver.stopEnsemble();
                            System.exit(0);
                        } else {
                            currentDporPath = dporInitialPaths.remove();
                        }
                    	LOG.info("---- End of Path Execution ----");
                        resetTest();
                        break;
                	}
                    try {
                        currentExploringPath.add(new TransitionTuple(globalState2, transition));
                        prevOnlineStatus.add(isNodeOnline.clone());
                        prevLocalStates.add(localStates.clone());
                        saveLocalState();
                        if (transition instanceof AbstractNodeOperationTransition) {
                            AbstractNodeOperationTransition nodeOperationTransition = (AbstractNodeOperationTransition) transition;
                            transition = ((AbstractNodeOperationTransition) transition).getRealNodeOperationTransition();
                            nodeOperationTransition.id = ((NodeOperationTransition) transition).getId();
                        }
                        if (transition.apply()) {
                        	pathRecordFile.write((transition.toString() + "\n").getBytes());
                            updateGlobalState();
                            updateSAMCQueueAfterEventExecution(transition);
                        }
                    } catch (IOException e) {
                        LOG.error("", e);
                    }
                } else if (exploredBranchRecorder.getCurrentDepth() == 0) {
                    LOG.warn("Finished exploring all states");
                } else {
                    if (dporInitialPaths.size() == 0) {
                        exploredBranchRecorder.resetTraversal();
                        exploredBranchRecorder.markBelowSubtreeFinished();
                        LOG.warn("There is no more interesting Initial Paths. "
                        		+ "Finished exploring all states.");
                        System.exit(0);
                    } else {
                        currentDporPath = dporInitialPaths.remove();
                    }
                    try {
                        pathRecordFile.write("duplicated\n".getBytes());
                    } catch (IOException e) {
                        LOG.error("", e);
                    }
                    resetTest();
                    break;
                }
            }
        }
    }
}
