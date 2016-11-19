package edu.uchicago.cs.ucare.samc.raft;

import edu.uchicago.cs.ucare.samc.server.DporModelChecker;
import edu.uchicago.cs.ucare.samc.event.Event;
import edu.uchicago.cs.ucare.samc.util.LocalState;
import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;

public class RaftSAMC extends DporModelChecker {

	public RaftSAMC(String inceptorName, String ackName, int maxId, int numCrash, int numReboot,
			String globalStatePathDir, String packetRecordDir, String cacheDir, WorkloadDriver workloadDriver,
			String ipcDir) {
		super(inceptorName, ackName, maxId, numCrash, numReboot, globalStatePathDir, packetRecordDir, cacheDir, 
				workloadDriver, ipcDir);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean isDependent(LocalState state, Event e1, Event e2) {
		// add policies here (initial policy)
		int p1Term = (Integer) e1.getValue("term");
		int p2Term = (Integer) e2.getValue("term");
		if((Integer)state.getValue("term") <= p1Term || (Integer)state.getValue("term") <= p2Term){
			return true;
		}
		return false;
	}

}
