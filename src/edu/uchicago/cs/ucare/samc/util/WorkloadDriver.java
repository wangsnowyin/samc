package edu.uchicago.cs.ucare.samc.util;

public abstract class WorkloadDriver {
    
    protected int numNode;
    protected String workingDir;
    protected String ipcDir;
    protected String samcDir;
    protected String targetSysDir;
    protected int testId;
    
    public SpecVerifier verifier;

    public WorkloadDriver(int numNode, String workingDir, String ipcDir, String samcDir, String targetSysDir) {
        this.numNode = numNode;
        this.workingDir = workingDir;
        this.ipcDir = ipcDir;
        this.samcDir = samcDir;
        this.targetSysDir = targetSysDir;
        testId = 0;
    }
    
    public WorkloadDriver(int numNode, String workingDir, SpecVerifier verifier) {
        this.numNode = numNode;
        this.workingDir = workingDir;
        this.verifier = verifier;
    }
    public abstract void startNode(int id);
    public abstract void stopNode(int id);
    public abstract void startEnsemble();
    public abstract void stopEnsemble();
    public abstract void resetTest(int testId);
    
    public abstract void runWorkload();
    
    public void setVerifier(SpecVerifier verifier) {
        this.verifier = verifier;
    }

}
