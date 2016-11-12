package edu.uchicago.cs.ucare.samc.scm;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;

public class SCMWorkloadDriver extends WorkloadDriver{
	
    private final static Logger LOG = LoggerFactory.getLogger(SCMWorkloadDriver.class);
	
	private String ipcScmDir;
	
	private Process[] node;
	private Thread consoleWriter;
	private FileOutputStream[] consoleLog;
	
	public SCMWorkloadDriver(int numNode, String workingDir, String ipcDir, String samcDir, String targetSysDir) {
		super(numNode, workingDir, ipcDir, samcDir, targetSysDir);
        ipcScmDir = ipcDir + "-scm";
        node = new Process[numNode];
        consoleLog = new FileOutputStream[numNode];
        consoleWriter = new Thread(new LogWriter());
        consoleWriter.start();
    }

	public void startNode(int id) {
		// start receiver first
		if(id == 0){
			try {
				node[id] = Runtime.getRuntime().exec(workingDir + "/startSCMReceiver.sh " + ipcScmDir + 
						" " + ipcDir + " " + (numNode-1) + " " + testId);
				LOG.info("Start Receiver-" + id);	
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				node[id] = Runtime.getRuntime().exec(workingDir + "/startSCMSender.sh " + ipcScmDir + 
						" " + ipcDir + " " + id + " " + testId);
				LOG.info("Start Sender node-" + id);	
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stopNode(int id) {
		LOG.info("Kill node-" + id);
		try {
			node[id] = Runtime.getRuntime().exec(workingDir + "/killNode.sh " + id);
        } catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void startEnsemble() {
		LOG.info("Start Ensemble");
		for (int i = 0; i < numNode; ++i) {
            try {
            	startNode(i);
                Thread.sleep(300);
            } catch (InterruptedException e) {
            	e.printStackTrace();
            }
        }
	}

	public void stopEnsemble() {
		LOG.info("Stop Ensemble");
		for (int i=0; i<numNode; i++) {
            stopNode(i);
        }
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
            } catch (FileNotFoundException e) {
                LOG.error("", e);
            }
        }
		this.testId = testId;
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
//                            LOG.debug("", e);
                        }
                    }
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    LOG.warn("", e);
                }
            }
        }
        
    }

	public void runWorkload() {
		// none
	}
}