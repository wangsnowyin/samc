package edu.uchicago.cs.ucare.samc.zookeeper;

import edu.uchicago.cs.ucare.samc.transition.Transition;
import edu.uchicago.cs.ucare.samc.util.LocalState;
import edu.uchicago.cs.ucare.samc.util.SpecVerifier;

public class ZKVerifier extends SpecVerifier {

	private static final String MORE_THAN_ONE_LEADER = "There is more than one leader in the system.";
    private static final String NO_LEADER = "No leader is in place.";
    
    private int numLeader;
    private String errorType;
    
	@Override
	public boolean verify() {
		updateGlobalState();
		if(numLeader < 1){
			errorType = NO_LEADER;
			return false;
		} else if(numLeader > 1){
			errorType = MORE_THAN_ONE_LEADER;
			return false;
		}
		return true;
	}

	@Override
	public boolean verifyNextTransition(Transition transition) {
		return true;
	}

	@Override
	public String verificationDetail() {
		String result = "";
		for(int node=0; node<modelCheckingServer.numNode; node++){
			LocalState state = this.modelCheckingServer.localStates[node];
			result += "(node-" + node + " state:" + state.getValue("state") + " with proposedLeader:" + state.getValue("proposedLeader")+ ") "; 
		}
		
		result += errorType;
		
		return result;
	}
	
	private void updateGlobalState(){
		numLeader = 0;
		errorType = "";
		for(int node=0; node<modelCheckingServer.numNode; node++){
			LocalState state = this.modelCheckingServer.localStates[node];
			switch((int)state.getValue("state")){
				case 2: // LEADING
					numLeader++;
					break;
			} 
		}
	}

}
