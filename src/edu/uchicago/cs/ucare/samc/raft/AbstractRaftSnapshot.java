package edu.uchicago.cs.ucare.samc.raft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchicago.cs.ucare.samc.server.ModelCheckingServerAbstract;
import edu.uchicago.cs.ucare.samc.transition.NodeOperationTransition;

@SuppressWarnings("serial")
public class AbstractRaftSnapshot extends NodeOperationTransition{

	private final static Logger LOG = LoggerFactory.getLogger(AbstractRaftSnapshot.class);
	
    protected ModelCheckingServerAbstract checker;
	
	public AbstractRaftSnapshot(ModelCheckingServerAbstract checker, int id){
		this.checker = checker;
		this.id = id;
	}
	
	@Override
	public boolean apply() {
		int leaderId = -1;
		int totalLeader = 0;
		for (int i=0; i<checker.numNode; i++){
			if((int)checker.localStates[i].getValue("state") == 2){
				leaderId = i;
				totalLeader++;
				if(totalLeader > 1){
					LOG.error("Leader is more than one in Raft Snapshot execution");
					return false;
				}
			}
		}
		if(leaderId != -1){
			checker.raftSnapshot(leaderId);
			return true;
		}
		return false;
	}

	@Override
	public int getTransitionId() {
		return 349*id;
	}
	
	public String toString() {
        return "abstract_snapshot";
    }

}
