package edu.uchicago.cs.ucare.samc.raft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;

public class RaftWorkloadDriver extends WorkloadDriver{
	
	private final static Logger LOG = LoggerFactory.getLogger(RaftWorkloadDriver.class);

	private Process[] node;
	
	public RaftWorkloadDriver(int numNode, String workingDir, String ipcDir, String samcDir, String targetSysDir) {
		super(numNode, workingDir, ipcDir, samcDir, targetSysDir);
		node = new Process[numNode];
	}

	@Override
	public void startNode(int id) {
		try {
            Thread.sleep(200);
            node[id] = Runtime.getRuntime().exec(workingDir + "/startNode.sh " + targetSysDir + " " + id + " " + testId);
			LOG.info("Start Node " + id);	
		} catch (Exception e) {
			LOG.error("Error in Starting Node " + id);	
		}
	}

	@Override
	public void stopNode(int id) {
		try {
			Runtime.getRuntime().exec(workingDir + "/killNode.sh " + id);
			refreshRaftNodeStorage(id);
            Thread.sleep(100);
			LOG.info("Stop Node " + id);
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
			Runtime.getRuntime().exec("mkdir " + workingDir + "/log/" + testId);
		} catch (Exception e) {
			LOG.error("Error in creating new directory in log");	
		}
	}

	@Override
	public void runWorkload() {
		// not used
	}

	private void clearIPCDir(){
		try {
			Runtime.getRuntime().exec(new String[]{"sh", "-c", "rm " + ipcDir + "/new/*"});
			Runtime.getRuntime().exec(new String[]{"sh", "-c", "rm " + ipcDir + "/send/*"});
			Runtime.getRuntime().exec(new String[]{"sh", "-c", "rm " + ipcDir + "/ack/*"});
			LOG.debug("Remove all files in ipc directory");
		} catch (Exception e) {
			LOG.error("Error in clear IPC Dir");
		}
	}
	
	private void refreshRaftNodeStorage(int id){
		try {
			Runtime.getRuntime().exec(workingDir + "/refreshStorageNode.sh " + samcDir + " " + id);
		} catch (Exception e) {
			LOG.error("Error in refresh raft storage dir in node " + id);
		}
	}
	
	public void raftSnapshot(int leaderId){
		try {
            Runtime.getRuntime().exec(workingDir + "/snapshot.sh " + targetSysDir +  " " + leaderId);
			LOG.debug("Execute Take Snapshot in node-" + leaderId);	
		} catch (Exception e) {
			LOG.error("Error in Take Snapshot of " + leaderId);	
		}
	}
}
