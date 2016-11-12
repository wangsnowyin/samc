package edu.uchicago.cs.ucare.samc.transition;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("serial")
public abstract class Transition implements Serializable {
    
    public final String ACTION = "nothing";
    
    public abstract boolean apply();
    public abstract int getTransitionId();
    
    public static final Comparator<Transition> COMPARATOR = new Comparator<Transition>() {
        public int compare(Transition o1, Transition o2) {
            Integer i1 = o1.getTransitionId();
            Integer i2 = o2.getTransitionId();
            return i1.compareTo(i2);
        }
    };
    
    public static String extract(List<? extends Transition> transitions) {
        StringBuilder strBuilder = new StringBuilder();
        for (Transition transition : transitions) {
            strBuilder.append(transition.toString());
            strBuilder.append("\n");
        }
        return strBuilder.length() > 0 ? strBuilder.substring(0, strBuilder.length() - 1) : "";
    }
    
}
