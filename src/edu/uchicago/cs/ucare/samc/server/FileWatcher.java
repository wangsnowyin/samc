package edu.uchicago.cs.ucare.samc.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchicago.cs.ucare.samc.event.Event;
import edu.uchicago.cs.ucare.samc.util.LocalState;

public class FileWatcher implements Runnable{
	
	private final static Logger LOG = LoggerFactory.getLogger(FileWatcher.class);
	
	String ipcDir;
	File path;
	ModelCheckingServerAbstract checker;

	private HashMap<Integer, Integer> packetCount;
	
	public FileWatcher(String sPath, ModelCheckingServerAbstract modelChecker){
		ipcDir = sPath;
		path = new File(sPath + "/send");
		checker = modelChecker;
		resetPacketCount();
		
		if (!path.isDirectory()) {
			throw new IllegalArgumentException("Path: " + path + " is not a folder");
		}
	}
	
	public void run(){
		LOG.debug("FileWatcher is looking after: " + path);
		
		while(!Thread.interrupted()){
			if(path.listFiles().length > 0){
				for(File file : path.listFiles()){
					processNewFile(file.getName());
				}
			}
			try {
				Thread.sleep(25);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
	}
	
	public void resetPacketCount(){
		packetCount = new HashMap<Integer, Integer>();
	}
	
	public synchronized void processNewFile(String filename){
		try{			
			Properties ev = new Properties();
			FileInputStream evInputStream = new FileInputStream(path + "/" + filename);
	    	ev.load(evInputStream);
	    	
	    	// we can inform the steady state manually to dmck to make the
	    	// dmck response's quicker, but it means we need to know when 
	    	// our target system node gets into steady state.
	    	// the current setting is, the target-sys nodes will get into steady state
	    	// after some time, specified by initSteadyStateTimeout
	    	/*
	    	if(filename.startsWith("s-")){
	    		LOG.info("DMCK receives steady state " + filename);
	    		checker.informSteadyState(sendNode, 0);
	    	} else
	    	*/ 
	    	// SAMPLE-LE
	    	if(filename.startsWith("le-")) {
		    	int sendNode = Integer.parseInt(ev.getProperty("sendNode"));
		    	int recvNode = Integer.parseInt(ev.getProperty("recvNode"));
		    	int role = Integer.parseInt(ev.getProperty("sendRole"));
		    	int leader = Integer.parseInt(ev.getProperty("leader"));
		    	int eventId = Integer.parseInt(filename.substring(3));
		    	int hashId = commonHashId(eventId);
		    	
		    	LOG.info("Process new File " + filename + " : hashId-" + hashId +
		    			" sendNode-" + sendNode + " sendRole-" + role + " recvNode-" + recvNode + 
		    			" leader-" + leader);
		    	
		    	// create eventPacket and store it to DMCK queue
		    	Event packet = new Event(hashId);
		    	packet.addKeyValue(Event.FROM_ID, sendNode);
		    	packet.addKeyValue(Event.TO_ID, recvNode);
		    	packet.addKeyValue(Event.FILENAME, filename);
		    	packet.addKeyValue("role", role);
		    	packet.addKeyValue("leader", leader);
		    	checker.offerPacket(packet);
	    	} else if(filename.startsWith("u-")){
		    	int sendNode = Integer.parseInt(ev.getProperty("sendNode"));
	    		int role = Integer.parseInt(ev.getProperty("sendRole"));
	    		int leader = Integer.parseInt(ev.getProperty("leader"));
	    		String electionTable = ev.getProperty("electionTable");
	    		
	    		LOG.info("Receive update state " + filename + " role: " + role + 
	    				" leader: " + leader + " electionTable: " + electionTable);
	    		
	    		LocalState state = new LocalState(sendNode);
	    		state.addKeyValue("role", role);
	    		state.addKeyValue("leader", leader);
	    		state.addKeyValue("electionTable", electionTable);
	    		checker.setLocalState(sendNode, state);
	    	} else 
	    	// SCM
	    	if(filename.startsWith("scm-")){
		    	int sendNode = Integer.parseInt(ev.getProperty("sendNode"));
	    		int eventId = Integer.parseInt(filename.substring(4));
		    	int recvNode = Integer.parseInt(ev.getProperty("recvNode"));
		    	int vote = Integer.parseInt(ev.getProperty("vote"));
		    	int hashId = commonHashId(eventId);
	    		
		    	LOG.info("Receive msg " + filename + " : hashId-" + hashId +  " from node-" + sendNode +
		    			" to node-" + recvNode + " vote-" + vote);
		    	
		    	Event event = new Event(hashId);
		    	event.addKeyValue(Event.FROM_ID, sendNode);
		    	event.addKeyValue(Event.TO_ID, recvNode);
		    	event.addKeyValue(Event.FILENAME, filename);
		    	event.addKeyValue("vote", vote);
		    	checker.offerPacket(event);
	    	} else if (filename.startsWith("updatescm-")){
		    	int sendNode = Integer.parseInt(ev.getProperty("sendNode"));
	    		int vote = Integer.parseInt(ev.getProperty("vote"));
		    	
		    	LOG.info("Update receiver node-" + sendNode + " with vote-" + vote);
		    	
		    	LocalState state = new LocalState(sendNode);
		    	state.addKeyValue("vote", vote);
		    	checker.setLocalState(0, state);
	    	} else
	    	// Raft
	    	if(filename.startsWith("raft-")){
		    	int sendNode = Integer.parseInt(ev.getProperty("sendNode"));
	    		int recvNode = Integer.parseInt(ev.getProperty("recvNode"));
		    	int eventMode = Integer.parseInt(ev.getProperty("eventMode"));
		    	int eventType = Integer.parseInt(ev.getProperty("eventType"));
		    	String sendNodeState = ev.getProperty("sendNodeState");
		    	int senderStateInt = Integer.parseInt(ev.getProperty("sendNodeStateInt"));
		    	int eventId = Integer.parseInt(ev.getProperty("eventId"));
		    	int hashId = commonHashId(eventId);
		    	int currentTerm = Integer.parseInt(ev.getProperty("currentTerm"));
		    	
		    	Event event = new Event(hashId);
		    	event.addKeyValue(Event.FROM_ID, sendNode);
		    	event.addKeyValue(Event.TO_ID, recvNode);
		    	event.addKeyValue(Event.FILENAME, filename);
		    	event.addKeyValue("eventMode", eventMode);
		    	event.addKeyValue("eventType", eventType);
		    	event.addKeyValue("state", senderStateInt);
		    	event.addKeyValue("term", currentTerm);

		    	if(eventMode == 0){
			    	LOG.info("DMCK receives raft local event at node-" + sendNode + 
			    			" hashId-" + hashId + " eventType-" + eventType + " currentTerm-" + currentTerm +
			    			" currentState-" + sendNodeState + " filename-" + filename);
		    		
		    		// timeout control
		    		if(eventType == 0 && checker.initTimeoutEnabling[sendNode] && 
		    				checker.timeoutEventCounter[sendNode] < checker.timeoutEventIterations){
		    			checker.timeoutEventCounter[sendNode]++;
		    			ignoreEvent(filename);
		    		} else {
		    			if(!checker.initTimeoutEnabling[sendNode]){
		    				checker.initTimeoutEnabling[sendNode] = true;
		    			}
		    			checker.offerLocalEvent(event);
		    			checker.timeoutEventCounter[sendNode] = 0;
		    		}
		    	} else {
		    		LOG.info("DMCK receives raft msg event from sendNode-" + sendNode +
			    			" recvNode-" + recvNode + " hashId-" + hashId + 
			    			" eventMode-" + eventMode + " eventType-" + eventType + " currentTerm-" + currentTerm +
			    			" currentState-" + sendNodeState + " filename-" + filename);
		    		
		    		if(eventMode == 1 && eventType == 2){
		    			checker.timeoutEventCounter[recvNode] = 0;
		    			checker.timeoutEventCounter[sendNode] = 0;
		    			ignoreEvent(filename);
		    		} else {
		    			checker.offerPacket(event);
		    		}
		    	}
	    	} else if (filename.startsWith("raftUpdate-")){
		    	int sendNode = Integer.parseInt(ev.getProperty("sendNode"));
		    	String sendNodeState = ev.getProperty("sendNodeState");
		    	int senderStateInt = Integer.parseInt(ev.getProperty("sendNodeStateInt"));
		    	int currentTerm = Integer.parseInt(ev.getProperty("currentTerm"));
		    	
		    	LOG.info("DMCK receives raft update state at node-" + sendNode +
		    			" currentState-" + sendNodeState + " filename-" + filename +
		    			" currentTerm-" + currentTerm);
	    		
		    	LocalState state = new LocalState(sendNode);
		    	state.addKeyValue("state", senderStateInt);
		    	state.addKeyValue("term", currentTerm);
		    	
		    	checker.setLocalState(sendNode, state);
	    	} else
	    		// ZK
	    		if (filename.startsWith("zk-")){
	    		int sender = Integer.parseInt(ev.getProperty("sender"));
	    		int recv = Integer.parseInt(ev.getProperty("recv"));
		    	int state = Integer.parseInt(ev.getProperty("state"));
		    	long leader = Long.parseLong(ev.getProperty("leader"));
		    	long zxid = Long.parseLong(ev.getProperty("zxid"));
		    	long epoch = Long.parseLong(ev.getProperty("epoch"));
		    	int eventId = (int) Long.parseLong(filename.substring(3));
		    	int hashId = commonHashId(eventId);

		    	System.out.println("DMCK receives ZK event with hashId-" + hashId + " sender-" + sender + 
		    			" recv-" + recv + " state-" + state + " leader-" + leader +
		    			" zxid-" + zxid + " epoch-" + epoch + " filename-" + filename);
		    	
		    	Event event = new Event(hashId);
		    	event.addKeyValue(Event.FROM_ID, sender);
		    	event.addKeyValue(Event.TO_ID, recv);
		    	event.addKeyValue(Event.FILENAME, filename);
		    	event.addKeyValue("state", state);
		    	event.addKeyValue("leader", leader);
		    	event.addKeyValue("zxid", zxid);
		    	event.addKeyValue("epoch", epoch);
		    	
		    	checker.offerPacket(event);
	    	} else if(filename.startsWith("zkUpdate-")){
	    		int sender = Integer.parseInt(ev.getProperty("sender"));
		    	int state = Integer.parseInt(ev.getProperty("state"));
		    	long proposedLeader = Long.parseLong(ev.getProperty("proposedLeader"));
		    	long proposedZxid = Long.parseLong(ev.getProperty("proposedZxid"));
		    	long logicalclock = Long.parseLong(ev.getProperty("logicalclock"));
		    	
		    	LOG.info("DMCK receives ZK state update at node-" + sender +
		    			" state-" + state + " proposedLeader-" + proposedLeader + 
		    			" proposedZxid-" + proposedZxid + " logicalclock-" + logicalclock);
		    	
		    	LocalState localstate = new LocalState(sender);
	    		localstate.addKeyValue("state", state);
	    		localstate.addKeyValue("proposedLeader", proposedLeader);
	    		localstate.addKeyValue("proposedZxid", proposedZxid);
	    		localstate.addKeyValue("logicalclock", logicalclock);
	    		checker.setLocalState(sender, localstate);
	    	} else 
	    	//ZK-3.5
	    	if (filename.startsWith("zkls-")) {
	    		int sender = Integer.parseInt(ev.getProperty("sender"));
		    	int state = Integer.parseInt(ev.getProperty("state"));
		    	int leader = Integer.parseInt(ev.getProperty("proposedLeader"));
		    	String sElectionTable = ev.getProperty("electionTable");

		    	Map<Integer, Integer> electionTable = new HashMap<Integer, Integer>();	    		
	    		if(sElectionTable != null && sElectionTable.length() > 0){
	    			sElectionTable = sElectionTable.substring(0, ev.getProperty("electionTable").length()-1);
	    			String[] electionTableValues = sElectionTable.split(",");	    		
	    			for (String value : electionTableValues){
	    				String[] temp = value.split(":");
	    				electionTable.put(Integer.parseInt(temp[0]), Integer.parseInt(temp[1]));
	    			}
	    		}

	    		System.out.println("[DEBUG] Receive update state " + filename + ", sendrole=" + 
	    				ev.getProperty("strSendRole") + ", leader=" + ev.getProperty("proposedLeader"));

	    		LocalState localstate = new LocalState(sender);
	    		localstate.addKeyValue("state", state);
	    		localstate.addKeyValue("proposedLeader", leader);
	    		localstate.addKeyValue("electionTable", sElectionTable);

	    		checker.setLocalState(sender, localstate);
	    	} else if(filename.startsWith("sync-")) { 
	    		int sendNode = Integer.parseInt(ev.getProperty("sender"));
		    	int recvNode = Integer.parseInt(ev.getProperty("recv"));
		    	String strRole = ev.getProperty("strSendRole");
		    	int leader = Integer.parseInt(ev.getProperty("leader"));
		    	long zxid = Long.parseLong(ev.getProperty("zxid"));
		    	int eventId = Integer.parseInt(filename.substring(5));
		    	int hashId = commonHashId(eventId);

		    	Event event = new Event(hashId);
		    	event.addKeyValue(Event.FROM_ID, sendNode);
		    	event.addKeyValue(Event.TO_ID, recvNode);
		    	event.addKeyValue(Event.FILENAME, filename);
		    	event.addKeyValue("senderRole", strRole);
		    	event.addKeyValue("leader", leader);
		    	event.addKeyValue("zxid", zxid);

		    	System.out.println("DMCK receives ZK sync File : eventId-"+ filename.substring(5) + 
		    		" sendNode-" + sendNode + " sendRole-" + ev.getProperty("sendRole") + 
		    		" recvNode-" + ev.getProperty("recvNode") + " leader-" + ev.getProperty("leader"));

		    	checker.offerPacket(event);
	    	} else if(filename.startsWith("rc-")) {
	    		int eventId = Integer.parseInt(filename.substring(3));
	    		int sendNode = Integer.parseInt(ev.getProperty("sender"));
	    		int recvNode = Integer.parseInt(ev.getProperty("recv"));
	    		int hashId = commonHashId(eventId);

	    		Event event = new Event(hashId);
	    		event.addKeyValue(Event.FROM_ID, sendNode);
		    	event.addKeyValue(Event.TO_ID, recvNode);
		    	event.addKeyValue(Event.FILENAME, filename);

	    		System.out.println("[DEBUG] Process reconfig File : eventId-"+ filename.substring(3) + 
	    			"sender-" + ev.getProperty("sender") + " recvNode-" + ev.getProperty("recvNode"));
	    		
	    		checker.offerPacket(event);
	    	}
	    	
	    	// remove the received msg
	    	LOG.debug("DMCK deletes " + path + "/" + filename);
	    	Runtime.getRuntime().exec("rm " + path + "/" + filename);
	    	
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error("DMCK experiences error in processing message " + filename);
		}
	}
	
	// if we would like to directly release an event, 
	// use this function instead of offering the packet to SAMC
	public void ignoreEvent(String filename){
		try{
        	PrintWriter writer = new PrintWriter(ipcDir + "/new/" + filename, "UTF-8");
        	writer.println("filename=" + filename);
        	writer.println("execute=false");
	        writer.close();
	        
	        LOG.info("DMCK for now ignores event with ID : " + filename);
	        
	        Runtime.getRuntime().exec("mv " + ipcDir + "/new/" + filename + " " + 
	        		ipcDir + "/ack/" + filename);
    	} catch (Exception e) {
    		LOG.error("Error in ignoring event with file : " + filename);
    	}
	}
	
	private int commonHashId(int eventId){
		Integer count = packetCount.get(eventId);
        if (count == null) {
            count = 0;
        }
        count++;
        packetCount.put(eventId, count);
        return 31 * eventId + count;
	}
}