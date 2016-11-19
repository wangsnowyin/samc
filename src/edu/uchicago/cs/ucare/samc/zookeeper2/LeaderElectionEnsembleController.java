package edu.uchicago.cs.ucare.samc.zookeeper2;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;

public class LeaderElectionEnsembleController extends WorkloadDriver {
    
    private final static Logger LOG = LoggerFactory.getLogger(LeaderElectionEnsembleController.class);
    
    String ipcDir;
    
    Process[] node;
    Thread consoleWriter;
    FileOutputStream[] consoleLog;
    
    public LeaderElectionEnsembleController(int numNode, String workingDir, String sIpcDir, String samcDir, String targetSysDir) {
        super(numNode, workingDir, sIpcDir, samcDir, targetSysDir);
        ipcDir = sIpcDir;
        node = new Process[numNode];
        consoleLog = new FileOutputStream[numNode];
        consoleWriter = new Thread(new LogWriter());
        consoleWriter.start();
    }
    
    public void resetTest(int testId) {
    	this.testId = testId;
		try{
			Runtime.getRuntime().exec(workingDir + "/resettest " + workingDir + " " + numNode + " " + testId);
		} catch (Exception e) {
			LOG.error("Error in creating new directory in log");	
		}
    }
    
    /*updated by Xueyin Wang*/
    public void startEnsemble() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting ensemble");
        }
        try{
        	for(int i=0; i<numNode; i++){
        		resetDynamic(i);
        	}      	
        	
        	startNode(0);
            Thread.sleep(200);
            
            startNode(1);
            Thread.sleep(200);
            //reconfig(1);
            
            startNode(2);
            Thread.sleep(200);
            //reconfig(2);
            
        } catch (InterruptedException e) {
            LOG.error("Error in starting node");
        }      
    }
    
    String zk_dir = "/Users/wangsnowyin/Documents/zookeeper-3.5.1-alpha";
    public void resetDynamic(int id){
    	BufferedWriter out = null;
    	try  
    	{
    	    FileWriter fstream = new FileWriter(zk_dir + "/conf/zoo" + id + ".cfg.dynamic", false);
    	    out = new BufferedWriter(fstream);
    	    if(id == 0){
    	    	out.write("server.0=localhost:2891:3891:participant;2180");
    	    }
    	    if(id == 1){
    	    	out.write("server.0=localhost:2891:3891:participant;2180\nserver.1=localhost:2892:3892:participant;2181");
    	    }
    	    if(id == 2){
    	    	out.write("server.0=localhost:2891:3891:participant;2180\nserver.1=localhost:2892:3892:participant;2181\nserver.2=localhost:2893:3893:participant;2182");
    	    }
    	    out.close();
    	}
    	catch (IOException e)
    	{
    	    e.printStackTrace();
    	}
    }
    
    public void reconfig(int id){
    	if (LOG.isDebugEnabled()) {
            LOG.debug("Reconfig node " + id);
        }
    	try {
            Runtime.getRuntime().exec(zk_dir + "/bin/reconfig" + id + ".sh");
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            LOG.debug("Stopping node " + id);
        }
        System.out.println("Stopping node " + id);
        try {
            //Runtime.getRuntime().exec(zk_dir + "/bin/zkServer.sh stop zoo" + (id+1) + ".cfg");
        	Runtime.getRuntime().exec(workingDir + "/killNode.sh " + workingDir + " " + id + " " + testId);
            Thread.sleep(10);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public void startNode(int id) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting node " + id);
        }
        System.out.println("Starting node " + id);
        try {
        	node[id] = Runtime.getRuntime().exec(workingDir + "/startNode.sh " + workingDir + " " + 
            		targetSysDir + " " + id + " " + testId);
        	//node[id] = Runtime.getRuntime().exec(zk_dir + "/bin/zkServer.sh start zoo" + (id+1) + ".cfg");
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    @Override
    public void runWorkload() {
        
    }

    private void cleanUpIPCDir(){
    	try {
        	Runtime.getRuntime().exec(new String[]{"sh","-c", "rm " + ipcDir + "/new/*"});
        	Runtime.getRuntime().exec(new String[]{"sh","-c", "rm " + ipcDir + "/send/*"});
        	Runtime.getRuntime().exec(new String[]{"sh","-c", "rm " + ipcDir + "/ack/*"});
        	
        	System.out.println("Finished cleaning up");
        } catch (IOException e) {
        	LOG.error("Error in cleaning up ipcDir");
        }
    }
}
