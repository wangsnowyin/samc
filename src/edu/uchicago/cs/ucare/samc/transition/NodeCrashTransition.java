package edu.uchicago.cs.ucare.samc.transition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchicago.cs.ucare.samc.server.ModelCheckingServerAbstract;

@SuppressWarnings("serial")
public class NodeCrashTransition extends NodeOperationTransition {
    
    final static Logger LOG = LoggerFactory.getLogger(NodeCrashTransition.class);

    public static final String ACTION = "nodecrash"; 
    private static final short ACTION_HASH = (short) ACTION.hashCode();

    protected ModelCheckingServerAbstract checker;
    
    public NodeCrashTransition(ModelCheckingServerAbstract checker, int id) {
        this.checker = checker;
        this.id = id;
    }

    @Override
    public boolean apply() {
        LOG.info("Killing node " + id);
        if(checker.killNode(id)) {
            checker.numCurrentCrash++;
            return true;
        }
        return false;
    }

    @Override
    public int getTransitionId() {
        final int prime = 31;
        int hash = 1;
        hash = prime * hash + id;
        int tranId = ((int) ACTION_HASH) << 16;
        tranId = tranId | (0x0000FFFF & hash);
        return tranId;
    }
    
    public String toString() {
        return "nodecrash id=" + id;
    }

    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NodeCrashTransition other = (NodeCrashTransition) obj;
        if (id != other.id)
            return false;
        return true;
    }

}
