package edu.uchicago.cs.ucare.samc.transition;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchicago.cs.ucare.samc.server.ModelCheckingServerAbstract;

@SuppressWarnings("serial")
public class AbstractNodeStartTransition extends AbstractNodeOperationTransition {

	private final static Logger LOG = LoggerFactory.getLogger(AbstractNodeStartTransition.class);
	
	public AbstractNodeStartTransition(ModelCheckingServerAbstract checker) {
        super(checker);
    }
	
    public AbstractNodeStartTransition(ModelCheckingServerAbstract checker, boolean isRandom) {
        super(checker, isRandom);
    }

    @Override
    public boolean apply() {
        NodeOperationTransition t = getRealNodeOperationTransition();
        if (t == null) {
            return false;
        }
        id = t.getId();
        return t.apply();
    }

    @Override
    public int getTransitionId() {
        return 112;
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof AbstractNodeStartTransition;
    }
    
    @Override 
    public int hashCode() {
        return 112;
    }
    
    @Override
    public NodeStartTransition getRealNodeOperationTransition() {
    	if (isRandom){
    		LinkedList<NodeOperationTransition> allPossible = getAllRealNodeOperationTransitions(checker.isNodeOnline);
            if (allPossible.isEmpty()) {
            	LOG.debug("Try to execute start node event, but currently there is no offline node");
                return null;
            }
            int i = RANDOM.nextInt(allPossible.size());
            return (NodeStartTransition) allPossible.get(i);
    	} else {
	        for (int i = 0; i < checker.numNode; ++i) {
	            if (!checker.isNodeOnline(i)) {
	                return new NodeStartTransition(checker, i);
	            }
	        }
	        LOG.debug("Try to execute start node event, but currently there is no offline node");
	        return null;
    	}
    }
    
    @Override
    public LinkedList<NodeOperationTransition> getAllRealNodeOperationTransitions(boolean[] onlineStatus) {
        LinkedList<NodeOperationTransition> result = new LinkedList<NodeOperationTransition>();
        for (int i = 0; i < onlineStatus.length; ++i) {
            if (!onlineStatus[i]) {
                result.add(new NodeStartTransition(checker, i));
            }
        }
        return result;
    }

    public String toString() {
        return "abstract_node_start";
    }
    
}