package edu.uchicago.cs.ucare.samc.scm;

import edu.uchicago.cs.ucare.samc.event.Event;
import edu.uchicago.cs.ucare.samc.server.DporModelChecker;
import edu.uchicago.cs.ucare.samc.util.LocalState;
import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;

public class SCMSAMC extends DporModelChecker {

	public SCMSAMC(String interceptorName, String ackName, int maxId, int numCrash, int numReboot,
			String globalStatePathDir, String packetRecordDir, String cacheDir, WorkloadDriver workloadDriver,
			String ipcDir) {
		super(interceptorName, ackName, maxId, numCrash, numReboot, globalStatePathDir, packetRecordDir, cacheDir,
				workloadDriver, ipcDir);
	}

	@Override
	public boolean isDependent(LocalState state, Event e1, Event e2) {
		int v1 = (Integer) e1.getValue("vote");
		int v2 = (Integer) e2.getValue("vote");
		if((Integer)state.getValue("vote") < v1 || (Integer)state.getValue("vote") < v2){
			return true;
		}
		return false;
	}

}
