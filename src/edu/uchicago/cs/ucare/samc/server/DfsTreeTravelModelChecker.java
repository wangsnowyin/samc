package edu.uchicago.cs.ucare.samc.server;

import java.util.LinkedList;
import java.util.ListIterator;

import edu.uchicago.cs.ucare.samc.event.Event;
import edu.uchicago.cs.ucare.samc.transition.PacketSendTransition;
import edu.uchicago.cs.ucare.samc.transition.Transition;
import edu.uchicago.cs.ucare.samc.util.LocalState;
import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;

public class DfsTreeTravelModelChecker extends TreeTravelModelChecker {
	
	boolean enableNodeTwoEvent;
    boolean enableNodeTwoSync;

    public DfsTreeTravelModelChecker(String interceptorName, String ackName, int numNode,
            int numCrash, int numReboot, String globalStatePathDir, String packetRecordDir,
            String workingDir, WorkloadDriver workloadDriver, String ipcDir) {
        super(interceptorName, ackName, numNode, numCrash, numReboot, globalStatePathDir, 
                packetRecordDir, workingDir, workloadDriver, ipcDir);
        
        this.enableNodeTwoEvent = false;
        this.enableNodeTwoSync = false;
    }
    
    @Override
    public Transition nextTransition(LinkedList<Transition> transitions) {
    	ListIterator<Transition> iter = transitions.listIterator();
        while (iter.hasNext()) {
            Transition transition = iter.next();
            
            if(interceptorName.equals("zkChecker-3.5.1")) {
            	if(transition instanceof PacketSendTransition) {
            		LocalState ls0 = ((PacketSendTransition) transition).getChecker().localStates[0];
            		LocalState ls1 = ((PacketSendTransition) transition).getChecker().localStates[1];
            		Integer state1 = (Integer)ls0.getValue("state");
            		Integer state2 = (Integer)ls1.getValue("state");
            		if(state1 != null && state2 != null) {
            			if((state1==1 && state2==2) || (state1==2 && state2==1))
            				enableNodeTwoEvent = true;
            		}
            		
            		Event event = ((PacketSendTransition) transition).getPacket();
                	if(event.getFromId() == 2 && !enableNodeTwoEvent) {
                		continue;
                	}
                	
                	if(((String)event.getValue(Event.FILENAME)).substring(0, 3).equals("rc-")) {
                		enableNodeTwoSync = true;
                	}
                	
                	if(((String)event.getValue(Event.FILENAME)).substring(0, 5).equals("sync-") 
                			&& (event.getFromId()==2 || event.getToId()==2) && !enableNodeTwoSync) {
                		continue;
                	}
                }
            }
            
            if (!exploredBranchRecorder.isSubtreeBelowChildFinished(transition.getTransitionId())) {
                iter.remove();
                return transition;
            }
        }
        return null;
    }
    
}
