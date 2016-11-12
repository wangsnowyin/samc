package edu.uchicago.cs.ucare.samc.server;

import java.io.File;
import java.io.FileNotFoundException;

import edu.uchicago.cs.ucare.samc.event.DiskWrite;
import edu.uchicago.cs.ucare.samc.transition.Transition;
import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;

public class RelayModelChecker extends GuideModelChecker {
    
    
    public RelayModelChecker(String interceptorName, String ackName,
            int numNode, String globalStatePathDir,
            String workingDir, WorkloadDriver workloadDriver, String ipcDir)
            throws FileNotFoundException {
        super(interceptorName, ackName, numNode, globalStatePathDir, null, workingDir, workloadDriver, ipcDir);
        resetTest();
    }

    public RelayModelChecker(String interceptorName, String ackName,
            int numNode, String globalStatePathDir, File program,
            String workingDir, WorkloadDriver workloadDriver, String ipcDir)
            throws FileNotFoundException {
        super(interceptorName, ackName, numNode, globalStatePathDir, program, workingDir, workloadDriver, ipcDir);
        resetTest();
    }
    
    @Override
    public void resetTest() {
        super.resetTest();
        afterProgramModelChecker = new RelayWorker(this);
    }
    
    
    
    protected class RelayWorker extends ModelCheckingServerAbstract.Explorer {

        public RelayWorker(ModelCheckingServerAbstract checker) {
            super(checker);
        }
        
        public void run() {
        	LOG.debug("Start RelayModelChecker");
        	boolean hasWaited = false;
            while (true) {
                while (!writeQueue.isEmpty()) {
                    DiskWrite write = writeQueue.peek();
                    try {
                        writeAndWait(write);
                    } catch (InterruptedException e) {
                        LOG.error(e.getMessage());
                    }

                }             
                
            	getOutstandingTcpPacketTransition(currentEnabledTransitions);
                printTransitionQueues(currentEnabledTransitions);
                
                boolean terminationPoint = checkTerminationPoint(currentEnabledTransitions) || currentEnabledTransitions.isEmpty(); 
                if (terminationPoint && !hasWaited){
                	try {
                		if(interceptorName.equals("raftModelChecker") && waitForNextLE && waitedForNextLEInDiffTermCounter < 20){
	                        Thread.sleep(leaderElectionTimeout);
	                    } else {
	                    	hasWaited = true;
	                    	LOG.debug("Wait for any long process");
	                    	Thread.sleep(waitEndExploration);
	                    }
                    } catch (InterruptedException e) {
                    	e.printStackTrace();
                    }
                	continue;
                } else if(terminationPoint){
                	boolean verifiedResult = verifier.verify();
                    String detail = verifier.verificationDetail();
                    saveResult(verifiedResult + " ; " + detail + "\n");
                	LOG.info("---- End of Path Execution ----");

                    checker.stopEnsemble();
                    System.exit(0);
                }
                hasWaited = false;
                
                for (Transition transition : currentEnabledTransitions) {
                    if (transition.apply()) {
                        updateGlobalState();
                        currentEnabledTransitions.remove(transition);
                        break;
                    }
                }
                
            }
            
        }
        
    }

}
