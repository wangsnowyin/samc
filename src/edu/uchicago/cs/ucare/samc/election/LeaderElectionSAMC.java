package edu.uchicago.cs.ucare.samc.election;

import java.util.HashMap;
import java.util.Map;

import edu.uchicago.cs.ucare.example.election.LeaderElectionMain;
import edu.uchicago.cs.ucare.samc.event.Event;
import edu.uchicago.cs.ucare.samc.server.DporModelChecker;
import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;
import edu.uchicago.cs.ucare.samc.util.LocalState;

public class LeaderElectionSAMC extends DporModelChecker {

    public boolean isDependent(LocalState state, Event e1, Event e2) {
        if ((int)state.getValue("role") == LeaderElectionMain.LOOKING) {
            int currSup = (int) state.getValue("leader");
            int sup1 = (Integer) e1.getValue("leader");
            int sup2 = (Integer) e2.getValue("leader");
            if (currSup < sup1 || currSup < sup2) {
                return true;
            } else if (isFinished(state)) {
                return true;
            }
        }
        return false;
    }

    public LeaderElectionSAMC(String interceptorName,
            String ackName, int maxId, int numCrash, int numReboot,
            String globalStatePathDir, String packetRecordDir, String workingDir,
            WorkloadDriver workloadDriver, String ipcDir) {
        super(interceptorName, ackName, maxId, numCrash, numReboot,
                globalStatePathDir, packetRecordDir, workingDir, workloadDriver, ipcDir);                
    }

    public boolean isFinished(LocalState state) {
        int totalNode = this.numNode;
        Map<Integer, Integer> count = new HashMap<Integer, Integer>();
        String[] electionTable = state.getValue("electionTable").toString().split(",");
        for(String row : electionTable){
        	int electedLeader = Integer.parseInt(row.split(":")[1]);
        	count.put(electedLeader, count.containsKey(electedLeader) ? count.get(electedLeader) + 1 : 1);
        }
        for(Integer electedLeader : count.keySet()){
        	int totalElect = count.get(electedLeader);
        	if(totalElect > totalNode / 2){
        		return true;
        	}
        }
        return false;
    }
}
