package edu.uchicago.cs.ucare.samc.scm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchicago.cs.ucare.samc.server.ModelCheckingServerAbstract;
import edu.uchicago.cs.ucare.samc.transition.Transition;
import edu.uchicago.cs.ucare.samc.util.LocalState;
import edu.uchicago.cs.ucare.samc.util.SpecVerifier;

public class SCMVerifier extends SpecVerifier {
	
	protected static final Logger LOG = LoggerFactory.getLogger(SCMVerifier.class);
    
	boolean error;
	LocalState receiver;
	
    public SCMVerifier() {
    	error = false;
    	receiver = new LocalState();
    }
    
    public SCMVerifier(ModelCheckingServerAbstract modelCheckingServer) {
    	this.modelCheckingServer = modelCheckingServer;
    }

    @Override
    public boolean verify(){
    	receiver = modelCheckingServer.localStates[0];
    	if((Integer)receiver.getValue("vote") != 4){
    		error = true;
    		return false;
    	} else {
    		return true;
    	}
    }
    
    @Override
	public boolean verifyNextTransition(Transition transition) {
    	// none
		return true;
	}

    @Override
    public String verificationDetail(){
    	if(error){
        	return "Receiver vote is not 4, but " + receiver;
    	} else {
        	return "Receiver is in correct state. The vote is " + receiver;
    	}
    }

	

}