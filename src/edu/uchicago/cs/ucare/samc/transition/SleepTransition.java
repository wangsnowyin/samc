package edu.uchicago.cs.ucare.samc.transition;

@SuppressWarnings("serial")
public class SleepTransition extends NodeOperationTransition {

	private long sleep;
	
	public SleepTransition(long sleep){
		this.sleep = sleep;
	}
	
	@Override
	public boolean apply() {
		try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            return false;
        }
		return true;
	}

	@Override
	public int getTransitionId() {
		return 0;
	}
	
	@Override
    public String toString(){
    	return "sleep=" + sleep;
    }

}
