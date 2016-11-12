package edu.uchicago.cs.ucare.samc.transition;

import java.io.Serializable;

import edu.uchicago.cs.ucare.samc.server.ModelCheckingServerAbstract;

public class TransitionTuple implements Serializable {

    private static final long serialVersionUID = -592045758670070432L;

    public int state;
    public Transition transition;

    public TransitionTuple(int state, Transition transition) {
        this.state = state;
        this.transition = transition;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((transition == null) ? 0 : transition.hashCode());
        result = prime * result + state;
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
        TransitionTuple other = (TransitionTuple) obj;
        if (transition == null) {
            if (other.transition != null)
                return false;
        } else if (!transition.equals(other.transition))
            return false;
        if (state != other.state)
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        return "State=" + state + ",Transition=" + transition.toString();
    }
    
    public TransitionTuple getSerializable() {
        if (transition instanceof PacketSendTransition) {
            return new TransitionTuple(state, new PacketSendTransition(null, ((PacketSendTransition) transition).getPacket()));
        } else if (transition instanceof NodeCrashTransition) {
            return new TransitionTuple(state, new NodeCrashTransition(null, ((NodeCrashTransition) transition).getId()));
        } else if (transition instanceof NodeStartTransition) {
            return new TransitionTuple(state, new NodeStartTransition(null, ((NodeStartTransition) transition).getId()));
        } else if (transition instanceof AbstractNodeCrashTransition) {
            return new TransitionTuple(state, new AbstractNodeCrashTransition(null));
        } else if (transition instanceof AbstractNodeStartTransition) {
            return new TransitionTuple(state, new AbstractNodeStartTransition(null));
        } else if (transition instanceof DiskWriteTransition) {
            return new TransitionTuple(state, new DiskWriteTransition(null, ((DiskWriteTransition) transition).getWrite()));
        } else {
            return null;
        }
    }
    
    public static TransitionTuple getRealTransitionTuple(ModelCheckingServerAbstract mc, TransitionTuple t) {
        if (t.transition instanceof PacketSendTransition) {
            return new TransitionTuple(t.state, new PacketSendTransition(mc, ((PacketSendTransition) t.transition).getPacket()));
        } else if (t.transition instanceof NodeCrashTransition) {
            return new TransitionTuple(t.state, new NodeCrashTransition(mc, ((NodeCrashTransition) t.transition).getId()));
        } else if (t.transition instanceof NodeStartTransition) {
            return new TransitionTuple(t.state, new NodeStartTransition(mc, ((NodeStartTransition) t.transition).getId()));
        } else if (t.transition instanceof AbstractNodeCrashTransition) {
            AbstractNodeCrashTransition u = new AbstractNodeCrashTransition(mc);
            u.id = ((AbstractNodeCrashTransition) t.transition).getId();
            return new TransitionTuple(t.state, u);
        } else if (t.transition instanceof AbstractNodeStartTransition) {
            AbstractNodeStartTransition u = new AbstractNodeStartTransition(mc);
            u.id = ((AbstractNodeStartTransition) t.transition).getId();
            return new TransitionTuple(t.state, u);
        } else if (t.transition instanceof DiskWriteTransition) {
            return new TransitionTuple(t.state, new DiskWriteTransition(mc, ((DiskWriteTransition) t.transition).write));
        } else {
            return null;
        }
    }
}
