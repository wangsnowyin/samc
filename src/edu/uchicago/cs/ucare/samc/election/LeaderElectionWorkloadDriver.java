package edu.uchicago.cs.ucare.samc.election;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;

public class LeaderElectionWorkloadDriver extends WorkloadDriver {
    
    private final static Logger LOG = LoggerFactory.getLogger(LeaderElectionWorkloadDriver.class);
    
    Process[] node;
    Thread consoleWriter;
    FileOutputStream[] consoleLog;
    
    public LeaderElectionWorkloadDriver(int numNode, String workingDir, String ipcDir, String samcDir, String targetSysDir) {
        super(numNode, workingDir, ipcDir, samcDir, targetSysDir);
        node = new Process[numNode];
        consoleLog = new FileOutputStream[numNode];
        consoleWriter = new Thread(new LogWriter());
        consoleWriter.start();
    }
    
    public void resetTest(int testId) {
        for (int i = 0; i < numNode; ++i) {
            if (consoleLog[i] != null) {
                try {
                    consoleLog[i].close();
                } catch (IOException e) {
                    LOG.error("", e);
                }
            }
            try {
                consoleLog[i] = new FileOutputStream(workingDir + "/console/" + i);
            } catch (Exception e) {
                LOG.error("", e);
            }
        }
        this.testId = testId;
    }
    
    public void startEnsemble() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting ensemble");
        }
        for (int i = 0; i < numNode; ++i) {
            try {
            	startNode(i);
                Thread.sleep(300);
            } catch (InterruptedException e) {
                LOG.error("Error in starting node " + i);
            }
        }
    }
    
    public void stopEnsemble() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Stopping ensemble");
        }
        for (int i=0; i<numNode; i++) {
        	try {
            	stopNode(i);
                Thread.sleep(200);
            } catch (InterruptedException e) {
                LOG.error("Error in stopping node " + i);
            }
        }
        
        if(ipcDir != ""){
        	cleanUpIPCDir();
        }
    }
    
    public void stopNode(int id) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Stop node-" + id);
        }
        try {
            Runtime.getRuntime().exec(workingDir + "/killNode.sh " + id);
        } catch (Exception e) {
        	LOG.error("Error when DMCK tries to kill node " + id);
        }
    }
    
    public void startNode(int id) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting node-" + id);
        }
        try {
        	node[id] = Runtime.getRuntime().exec(workingDir + "/startNode.sh " + id + " " + ipcDir);
        } catch (Exception e) {
        	LOG.error("Error when DMCK tries to start node " + id);
        }
    }
    
    class LogWriter implements Runnable {

        public void run() {
            byte[] buff = new byte[256];
            while (true) {
                for (int i = 0; i < numNode; ++i) {
                    if (node[i] != null) {
                        int r = 0;
                        InputStream stdout = node[i].getInputStream();
                        InputStream stderr = node[i].getErrorStream();
                        try {
                            while((r = stdout.read(buff)) != -1) {
                                consoleLog[i].write(buff, 0, r);
                                consoleLog[i].flush();
                            }
                            while((r = stderr.read(buff)) != -1) {
                                consoleLog[i].write(buff, 0, r);
                                consoleLog[i].flush();
                            }
                        } catch (IOException e) {
                            LOG.warn("Error in writing log");
                        }
                    }
                }
                try {
                    Thread.sleep(300);
                } catch (Exception e) {
                    LOG.warn("Error in LogWriter thread sleep");
                }
            }
        }
        
    }

    @Override
    public void runWorkload() {
        
    }

    private void cleanUpIPCDir(){
    	try {
        	Runtime.getRuntime().exec(new String[]{"sh","-c", "rm " + ipcDir + "/new/*"});
        	Runtime.getRuntime().exec(new String[]{"sh","-c", "rm " + ipcDir + "/send/*"});
        	Runtime.getRuntime().exec(new String[]{"sh","-c", "rm " + ipcDir + "/ack/*"});
        } catch (IOException e) {
        	LOG.error("Error in cleaning up ipcDir");
        }
    }
}
