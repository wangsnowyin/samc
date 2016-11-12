package edu.uchicago.cs.ucare.samc.server;

import java.util.LinkedList;
import java.util.ListIterator;

import edu.uchicago.cs.ucare.samc.transition.Transition;
import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;

public class DfsTreeTravelModelChecker extends TreeTravelModelChecker {

    public DfsTreeTravelModelChecker(String interceptorName, String ackName, int numNode,
            int numCrash, int numReboot, String globalStatePathDir, String packetRecordDir,
            String workingDir, WorkloadDriver workloadDriver, String ipcDir) {
        super(interceptorName, ackName, numNode, numCrash, numReboot, globalStatePathDir, 
                packetRecordDir, workingDir, workloadDriver, ipcDir);
    }
    
    @Override
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
    
}
