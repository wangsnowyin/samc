package edu.uchicago.cs.ucare.samc.zookeeper2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchicago.cs.ucare.samc.transition.Transition;
import edu.uchicago.cs.ucare.samc.server.ModelCheckingServerAbstract;
import edu.uchicago.cs.ucare.samc.util.LocalState;
import edu.uchicago.cs.ucare.samc.util.SpecVerifier;

public class LeaderElectionVerifier extends SpecVerifier {
    
    protected static final Logger log = LoggerFactory.getLogger(LeaderElectionVerifier.class);
    
    
    public LeaderElectionVerifier() {
    	
    }
    
    public LeaderElectionVerifier(ModelCheckingServerAbstract modelCheckingServer) {
    	this.modelCheckingServer = modelCheckingServer;
    }
    
    /*updated by Xueyin Wang*/
    @Override
    public boolean verify(){
    	int onlineNode = 0;
    	int[] supportTable = new int[modelCheckingServer.isNodeOnline.length];
        for (boolean status : modelCheckingServer.isNodeOnline) {
            if (status) {
                onlineNode++;
            }
        }
        for (int i = 0; i < 3; i++) {
            int numLeader = 0;
            int numFollower = 0;
            int numLooking = 0;
            int numCrashed = 0;
            for (int j = 0; j < modelCheckingServer.isNodeOnline.length; j++) {
                if (modelCheckingServer.isNodeOnline[j]) {
                    LocalState localState = modelCheckingServer.localStates[j];
                    if((Integer)localState.getValue("state") == null) continue;
                    switch((Integer)localState.getValue("state")){
                        case 0: {
                        	numLooking++;
                            supportTable[j] = -1;
                            break;
                        }
                        case 1: {
                        	numFollower++;
                        	supportTable[j] = (Integer)localState.getValue("proposedLeader");
                        	break;
                        }
                        case 2: {
                            numLeader++;
                            supportTable[j] = j;
                            break;
                        }
                        case 3: break;
                        case 4: {
                        	numCrashed++;
                        	break;
                        }
                        default: break;
                    }
                }
            }
            int quorum = modelCheckingServer.numNode / 2 + 1;
            System.out.println("Verifier: onlineNode-" + onlineNode + " quorum-" + quorum + " numLeader-" + 
            					numLeader + " numFollower-" + numFollower + " numLooking-" + numLooking + " numCrashed-" + numCrashed);
            
            if(numCrashed > 0) return false;
            if (onlineNode < quorum) {
                if (numLeader == 0 && numFollower == 0 && numLooking == onlineNode) {
                    return true;
                }
            } else {
                if (numLeader == 1 && numFollower == (onlineNode - 1)) {
                    int leader = -1;
                    for (int j = 0; j < modelCheckingServer.isNodeOnline.length; ++j) {
                        if (modelCheckingServer.isNodeOnline[j]) {
                            if (leader == -1) {
                                leader = supportTable[j];
                            } else if (supportTable[j] != leader) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }
        }
        return false;
    }
    
    
    @Override
    public String verificationDetail() {
        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i < modelCheckingServer.isNodeOnline.length; i++) {
            if (modelCheckingServer.isNodeOnline[i]) {
                LocalState localState = modelCheckingServer.localStates[i];
                if((Integer)localState.getValue("state") == null) {
                	strBuilder.append("node " + i + " is down ; ");
                	continue;
                }
                switch((Integer)localState.getValue("state")){
                    case 0: {
                        strBuilder.append("node " + i + " is LOOKING; ");
                        break;
                    }
                    case 1: {
                    	strBuilder.append("node " + i + " is FOLLOWING; ");
                    	break;
                    }
                    case 2: {
                        strBuilder.append("node " + i + " is LEADING; ");
                        break;
                    }
                    case 3: {
                        strBuilder.append("node " + i + " is OBSERVING; ");
                        break;
                    }
                    case 4: {
                    	strBuilder.append("node " + i + " is CRASHED; ");
                    }
                    default: {
                        strBuilder.append("node " + i + " is UNSET; ");
                        break;
                    }
                } 
            }                
        }
        return strBuilder.toString();
    }
    
    @Override
    public boolean verifyNextTransition(Transition transition) {
        return true;
    }

}
