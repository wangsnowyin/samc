package edu.uchicago.cs.ucare.samc.event;

@SuppressWarnings("serial")
public class DiskWrite extends Event {
    
    public static final String WRITE_NODE_KEY = "writeNode";
    public static final String DATA_HASH_KEY = "dataHash";
    
    public DiskWrite(int id, int nodeId, int dataHash) {
        super(id);
        addKeyValue(WRITE_NODE_KEY, nodeId);
        addKeyValue(DATA_HASH_KEY, dataHash);
    }
    
    public int getWriteId() {
        return (Integer) getValue(HASH_ID_KEY);
    }
    
    public void setWriteId(int id) {
        addKeyValue(HASH_ID_KEY, id);
    }
    
    public int getNodeId() {
        return (Integer) getValue(WRITE_NODE_KEY);
    }
    
    public void setNodeId(int nodeId) {
        addKeyValue(WRITE_NODE_KEY, nodeId);
    }
    
    public int getDataHash() {
        return (Integer) getValue(DATA_HASH_KEY);
    }
    
    public void setDataHash(int dataHash) {
        addKeyValue(DATA_HASH_KEY, dataHash);
    }
    
}
