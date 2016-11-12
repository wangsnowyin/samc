package edu.uchicago.cs.ucare.samc.event;

import java.util.HashMap;

public class DiskWriteGenerator {
    
    private HashMap<Integer, Integer> writeCount;
    
    public DiskWriteGenerator() {
        writeCount = new HashMap<Integer, Integer>();
    }
    
    public DiskWrite createNewDiskWrite(int nodeId, int dataHash) {
        int hash = diskWriteHashCodeWithoutId(nodeId, dataHash);
        Integer count = writeCount.get(hash);
        if (count == null) {
            count = 0;
        }
        ++count;
        int id = 31 * hash + count;
        writeCount.put(hash, count);
        return new DiskWrite(id, nodeId, dataHash);
    }
    
    private static int diskWriteHashCodeWithoutId(int nodeId, int dataHash) {
        final int prime = 31;
        int result = 1;
        result = prime * result + nodeId;
        result = prime * result + dataHash;
        return result;
    }

}
