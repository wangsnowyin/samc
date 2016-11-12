package edu.uchicago.cs.ucare.samc.event;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class Event implements Serializable {
    
	public static final String FILENAME = "filename";
    public static final String HASH_ID_KEY = "hashId";
    public static final String FROM_ID = "sendNode";
    public static final String TO_ID = "recvNode";
    
    protected Map<String, Serializable> keyValuePairs;
    protected boolean obsolete;
    protected int obsoleteBy;
    
    public Event(int hashId) {
        keyValuePairs = new HashMap<String, Serializable>();
        addKeyValue(HASH_ID_KEY, hashId);
        obsolete = false;
        obsoleteBy = -1;
    }
    
    public void addKeyValue(String key, Serializable value) {
        keyValuePairs.put(key, value);
    }

    public Object getValue(String key) {
        return keyValuePairs.get(key);
    }
    
    public int getId(){
    	return (int) getValue(HASH_ID_KEY);
    }
    
    public int getFromId(){
    	return (int) getValue(FROM_ID);
    }
    
    public int getToId(){
    	return (int) getValue(TO_ID);
    }

    public boolean isObsolete() {
        return obsolete;
    }

    public void setObsolete(boolean obsolete) {
        this.obsolete = obsolete;
    }

    public int getObsoleteBy() {
        return obsoleteBy;
    }

    public void setObsoleteBy(int obsoleteBy) {
        if (this.obsoleteBy == -1) {
            this.obsoleteBy = obsoleteBy;
        }
    }

    @Override
    public String toString() {
        return "Event=" + keyValuePairs + "";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((keyValuePairs == null) ? 0 : keyValuePairs.hashCode());
        result = prime * result + (obsolete ? 1231 : 1237);
        result = prime * result + obsoleteBy;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Event other = (Event) obj;
        if (keyValuePairs == null) {
            if (other.keyValuePairs != null)
                return false;
        } else if (!keyValuePairs.equals(other.keyValuePairs))
            return false;
        if (obsolete != other.obsolete)
            return false;
        if (obsoleteBy != other.obsoleteBy)
            return false;
        return true;
    }

}
