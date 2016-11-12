package edu.uchicago.cs.ucare.example.election.interposition;

import java.rmi.Naming;
import java.util.HashMap;
import java.util.Map;

import edu.uchicago.cs.ucare.example.election.ElectionMessage;
import edu.uchicago.cs.ucare.example.election.LeaderElectionMain.Sender;
import edu.uchicago.cs.ucare.samc.election.LeaderElectionAspectProperties;
import edu.uchicago.cs.ucare.samc.election.LeaderElectionPacketGenerator;
import edu.uchicago.cs.ucare.samc.event.Event;
import edu.uchicago.cs.ucare.samc.server.ModelCheckingServer;
import edu.uchicago.cs.ucare.samc.util.LocalState;
import edu.uchicago.cs.ucare.samc.util.PacketReceiveAck;

public class LeaderElectionInterposition {
	
    public static boolean SAMC_ENABLED;

	public static int id;
	
	public static boolean isBound;
	
	public static ModelCheckingServer modelCheckingServer;
	
	public static Map<Integer, Event> nodeSenderMap;
	public static Map<Integer, Sender> msgSenderMap;
	
	public static LeaderElectionPacketGenerator packetGenerator;
	public static LeaderElectionPacketGenerator packetGenerator2;
	
    public static PacketReceiveAck ack;
    
    public static int numNode;
	public static boolean[] isReading;
	
	public static LocalState localState;
	
	public static boolean firstSent = false;

    static {
		SAMC_ENABLED = Boolean.parseBoolean(System.getProperty("samc_enabled", "false"));
        packetGenerator = new LeaderElectionPacketGenerator();
		if (SAMC_ENABLED) {
			nodeSenderMap = new HashMap<Integer, Event>();
            msgSenderMap = new HashMap<Integer, Sender>();
            packetGenerator2 = new LeaderElectionPacketGenerator();
            isBound = false;
            localState = new LocalState();
		    try {
                modelCheckingServer = (ModelCheckingServer) Naming.lookup(LeaderElectionAspectProperties.getInterceptorName());
                ack = (PacketReceiveAck) Naming.lookup(LeaderElectionAspectProperties.getInterceptorName() + "Ack");
            } catch (Exception e) {
                System.err.println("Cannot find model checking server, switch to no-SAMC mode");
                SAMC_ENABLED = false;
            }
		}
    }

	public static boolean isReadingForAll() {
        for (int i = 0; i < numNode; ++i) {
            if (i != id) {
                if (!isReading[i]) {
                    return false;
                }
            }
        }
        return true;
    }

	public static boolean isThereSendingMessage() {
        if (!firstSent) return true;
        for (Sender sender : msgSenderMap.values()) {
            synchronized (sender.queue) {
                if (!sender.queue.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

	public static Integer hash(ElectionMessage msg, int toId) {
        return packetGenerator.getHash(msg, toId);
    }

}
