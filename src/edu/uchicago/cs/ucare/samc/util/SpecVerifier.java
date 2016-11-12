package edu.uchicago.cs.ucare.samc.util;

import edu.uchicago.cs.ucare.samc.server.ModelCheckingServerAbstract;
import edu.uchicago.cs.ucare.samc.transition.Transition;

public abstract class SpecVerifier {
    
    public ModelCheckingServerAbstract modelCheckingServer;

    public abstract boolean verify();
    public abstract boolean verifyNextTransition(Transition transition);
    public abstract String verificationDetail();

}
