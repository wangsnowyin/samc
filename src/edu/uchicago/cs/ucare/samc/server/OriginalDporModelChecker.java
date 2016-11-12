package edu.uchicago.cs.ucare.samc.server;

import edu.uchicago.cs.ucare.samc.event.Event;
import edu.uchicago.cs.ucare.samc.util.LocalState;
import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;

public class OriginalDporModelChecker extends DporModelChecker {

	public OriginalDporModelChecker(String interceptorName, String ackName, int maxId, int numCrash, int numReboot,
			String globalStatePathDir, String packetRecordDir, String cacheDir, WorkloadDriver workloadDriver,
			String ipcDir) {
		super(interceptorName, ackName, maxId, numCrash, numReboot, globalStatePathDir, packetRecordDir, cacheDir,
				workloadDriver, ipcDir);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean isDependent(LocalState state, Event e1, Event e2) {
		if(e1.getToId() == e2.getToId()){
			return true;
		}
		return false;
	}

}
