package edu.uchicago.cs.ucare.samc.transition;

import edu.uchicago.cs.ucare.samc.event.DiskWrite;
import edu.uchicago.cs.ucare.samc.server.ModelCheckingServerAbstract;

@SuppressWarnings("serial")
public class DiskWriteTransition extends Transition {
    
    public static final String ACTION = "diskwrite"; 
    private static final short ACTION_HASH = (short) ACTION.hashCode();
    
    ModelCheckingServerAbstract checker;
    DiskWrite write;
    boolean obsolete;
    int obsoleteBy;
    
    public DiskWriteTransition(ModelCheckingServerAbstract checker, DiskWrite write) {
        this.checker = checker;
        this.write = write;
        obsolete = false;
        obsoleteBy = -1;
    }

    @Override
    public boolean apply() {
        try {
            return checker.writeAndWait(write);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int getTransitionId() {
        final int prime = 31;
        int hash = 1;
        hash = prime * hash + write.hashCode();
        int tranId = ((int) ACTION_HASH) << 16;
        tranId = tranId | (0x0000FFFF & hash);
        return tranId;
    }

    public DiskWrite getWrite() {
        return write;
    }

    public void setWrite(DiskWrite write) {
        this.write = write;
    }

    public boolean isObsolete() {
        return obsolete;
    }

    public void setObsolete(boolean obsolete) {
        this.obsolete = obsolete;
    }

    public int getObsoleteBy() {
        return obsoleteBy;
    }

    public void setObsoleteBy(int obsoleteBy) {
        this.obsoleteBy = obsoleteBy;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((write == null) ? 0 : write.hashCode());
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
        DiskWriteTransition other = (DiskWriteTransition) obj;
        if (write == null) {
            if (other.write != null)
                return false;
        } else if (!write.equals(other.write))
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        return "diskwrite transition_id=" + getTransitionId() + " " + write.toString();
    }

}
