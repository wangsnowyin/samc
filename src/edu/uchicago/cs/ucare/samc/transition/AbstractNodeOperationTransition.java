package edu.uchicago.cs.ucare.samc.transition;

import java.util.LinkedList;
import java.util.Random;

import edu.uchicago.cs.ucare.samc.server.ModelCheckingServerAbstract;

@SuppressWarnings("serial")
public abstract class AbstractNodeOperationTransition extends NodeOperationTransition {

	protected final Random RANDOM = new Random(System.currentTimeMillis());
	protected boolean isRandom;

    protected ModelCheckingServerAbstract checker;
    
    public AbstractNodeOperationTransition(ModelCheckingServerAbstract checker) {
        id = -1;
        this.checker = checker;
        this.isRandom = false;
    }
    
    public AbstractNodeOperationTransition(ModelCheckingServerAbstract checker, boolean isRandom) {
        id = -1;
        this.checker = checker;
        this.isRandom = isRandom;
    }
    
    public void setRandom(boolean isRandom){
    	this.isRandom = isRandom;
    }

    public abstract NodeOperationTransition getRealNodeOperationTransition();
    public abstract LinkedList<NodeOperationTransition> getAllRealNodeOperationTransitions(boolean[] onlineStatus);

}
