package edu.uchicago.cs.ucare.samc.zookeeper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;

public class ZKWorkloadDriver extends WorkloadDriver {

	private final static Logger LOG = LoggerFactory.getLogger(ZKWorkloadDriver.class);

	private Process[] node;
	
	public ZKWorkloadDriver(int numNode, String workingDir, String ipcDir, String samcDir, String targetSysDir) {
		super(numNode, workingDir, ipcDir, samcDir, targetSysDir);
		node = new Process[numNode];
	}

	@Override
	public void startNode(int id) {
		try {
            Thread.sleep(200);
            node[id] = Runtime.getRuntime().exec(workingDir + "/startNode.sh " + workingDir + " " + 
            		targetSysDir + " " + id + " " + testId);
			LOG.info("Start Node-" + id);	
		} catch (Exception e) {
			LOG.error("Error in Starting Node " + id);	
		}
	}

	@Override
	public void stopNode(int id) {
		try {
			Runtime.getRuntime().exec(workingDir + "/killNode.sh " + workingDir + " " + id + " " + testId);
            Thread.sleep(10);
			LOG.info("Stop Node-" + id);
		} catch (Exception e) {
			LOG.error("Error in Killing Node " + id);	
		}
	}

	@Override
	public void startEnsemble() {
		clearIPCDir();
		LOG.info("Start Ensemble");
		for (int i = 0; i < numNode; ++i) {
            try {
            	startNode(i);
            } catch (Exception e) {
    			LOG.error("Error in starting ensemble");	
            }
        }
	}

	@Override
	public void stopEnsemble() {
		LOG.info("Stop Ensemble");
		for (int i=0; i<numNode; i++) {
			try {
            	stopNode(i);
            } catch (Exception e) {
    			LOG.error("Error in stopping ensemble");	
            }
        }
	}

	@Override
	public void resetTest(int testId) {
		this.testId = testId;
		try{
			Runtime.getRuntime().exec(workingDir + "/resettest " + workingDir + " " + numNode + " " + testId);
		} catch (Exception e) {
			LOG.error("Error in creating new directory in log");	
		}
	}

	@Override
	public void runWorkload() {
		// TODO Auto-generated method stub
		
	}

	private void clearIPCDir(){
		try {
			Runtime.getRuntime().exec(new String[]{"sh", "-c", "rm " + ipcDir + "/new/*"});
			Runtime.getRuntime().exec(new String[]{"sh", "-c", "rm " + ipcDir + "/send/*"});
			Runtime.getRuntime().exec(new String[]{"sh", "-c", "rm " + ipcDir + "/ack/*"});
			LOG.debug("[DEBUG] Remove all files in ipc directory");
		} catch (Exception e) {
			LOG.error("Error in clear IPC Dir");
		}
	}
}
