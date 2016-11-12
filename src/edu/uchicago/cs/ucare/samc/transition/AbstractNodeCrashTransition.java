package edu.uchicago.cs.ucare.samc.transition;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchicago.cs.ucare.samc.server.ModelCheckingServerAbstract;

@SuppressWarnings("serial")
public class AbstractNodeCrashTransition extends AbstractNodeOperationTransition {

	private final static Logger LOG = LoggerFactory.getLogger(AbstractNodeCrashTransition.class);
	
	public AbstractNodeCrashTransition(ModelCheckingServerAbstract checker) {
        super(checker);
    }
	
    public AbstractNodeCrashTransition(ModelCheckingServerAbstract checker, boolean isRandom) {
        super(checker, isRandom);
    }

    @Override
    public boolean apply() {
        NodeCrashTransition t = getRealNodeOperationTransition();
        if (t == null) {
            return false;
        }
        id = t.getId();
        return t.apply();
    }

    @Override
    public int getTransitionId() {
        return 101;
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof AbstractNodeCrashTransition;
    }
    
    @Override 
    public int hashCode() {
        return 101;
    }
    
    public NodeCrashTransition getRealNodeOperationTransition() {
    	if (isRandom){
    		LinkedList<NodeOperationTransition> allPossible = getAllRealNodeOperationTransitions(checker.isNodeOnline);
            if (allPossible.isEmpty()) {
            	LOG.debug("Try to execute crash node event, but currently there is no online node");
                return null;
            }
            int i = RANDOM.nextInt(allPossible.size());
            return (NodeCrashTransition) allPossible.get(i);
    	} else {
    		for (int i = 0; i < checker.numNode; ++i) {
	            if (checker.isNodeOnline(i)) {
	                return new NodeCrashTransition(checker, i);
	            }
	        }
    		LOG.debug("Try to execute crash node event, but currently there is no online node");
	        return null;
    	}
    }
    
    @Override
    public LinkedList<NodeOperationTransition> getAllRealNodeOperationTransitions(boolean[] onlineStatus) {
        LinkedList<NodeOperationTransition> result = new LinkedList<NodeOperationTransition>();
        for (int i = 0; i < onlineStatus.length; ++i) {
            if (onlineStatus[i]) {
                result.add(new NodeCrashTransition(checker, i));
            }
        }
        return result;
    }

    public String toString() {
        return "abstract_node_crash";
    }
    
}
