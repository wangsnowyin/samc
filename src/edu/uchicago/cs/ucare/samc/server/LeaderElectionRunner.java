package edu.uchicago.cs.ucare.samc.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchicago.cs.ucare.samc.server.FileWatcher;
import edu.uchicago.cs.ucare.samc.server.ModelCheckingServerAbstract;
import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;
import edu.uchicago.cs.ucare.samc.util.SpecVerifier;

public class LeaderElectionRunner {
    
    final static Logger LOG = LoggerFactory.getLogger(LeaderElectionRunner.class);
    
    static WorkloadDriver workloadDriver;
    
    public static void main(String[] argv) throws IOException, ClassNotFoundException, 
            NoSuchMethodException, SecurityException, InstantiationException, 
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    	String testRunnerConf = null;
        if (argv.length == 0) {
            System.err.println("Please specify test config file");
            System.exit(1);
        }
        boolean isPausedEveryTest = false;
        for (String param : argv) {
            if (param.equals("-p")) {
                isPausedEveryTest = true;
            } else {
                testRunnerConf = param;
            }
        }
        
        prepareModelChecker(testRunnerConf, isPausedEveryTest);
    }
    
    public static void prepareModelChecker(String testRunnerConf, boolean isPausedEveryTest){
    	try{
	    	Properties configProp = new Properties();
	        FileInputStream configInputStream = new FileInputStream(testRunnerConf);
	        configProp.load(configInputStream);
	        configInputStream.close();
	        
	        String workingDir = configProp.getProperty("working_dir");
	        String ipcDir = configProp.getProperty("ipc_dir");
	        String samcDir = configProp.getProperty("samc_dir");
	        String targetSysDir = configProp.getProperty("target_sys_dir") != "" ? configProp.getProperty("target_sys_dir") : "";
	        int numNode = Integer.parseInt(configProp.getProperty("num_node"));
	        String workloadDriverName = configProp.getProperty("workload_driver");
	        @SuppressWarnings("unchecked")
	        Class<? extends WorkloadDriver> workloadDriverClass = (Class<? extends WorkloadDriver>) Class.forName(workloadDriverName);
	        Constructor<? extends WorkloadDriver> workloadDriverConstructor = workloadDriverClass.getConstructor(Integer.TYPE, String.class, 
	        		String.class, String.class, String.class);
	        workloadDriver = workloadDriverConstructor.newInstance(numNode, workingDir, ipcDir, samcDir, targetSysDir);
	        ModelCheckingServerAbstract checker = createModelCheckerFromConf(workingDir + "/target-sys.conf", workingDir, ipcDir);
	        
	        // activate Directory Watcher
	        FileWatcher dirWatcher = new FileWatcher(ipcDir, checker);
            Thread watcher = new Thread(dirWatcher);
        	watcher.start();
        	Thread.sleep(500);
	        
	        startExploreTesting(checker, numNode, workingDir, isPausedEveryTest, dirWatcher);
    	} catch (Exception e){
    		e.printStackTrace();
    	}
    }
    
    @SuppressWarnings("unchecked")
	protected static ModelCheckingServerAbstract createModelCheckerFromConf(String confFile, 
            String workingDir, String ipcDir) throws ClassNotFoundException, 
            NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, 
            IllegalArgumentException, InvocationTargetException {
        ModelCheckingServerAbstract modelCheckingServerAbstract = null;
        try {
            Properties prop = new Properties();
            FileInputStream configInputStream = new FileInputStream(confFile);
            prop.load(configInputStream);
            configInputStream.close();
            String interceptorName = prop.getProperty("mc_name");
            int numNode = Integer.parseInt(prop.getProperty("num_node"));
            String testRecordDir = prop.getProperty("test_record_dir");
            String traversalRecordDir = prop.getProperty("traversal_record_dir");
            String strategy = prop.getProperty("exploring_strategy");
            int numCrash = Integer.parseInt(prop.getProperty("num_crash"));
            int numReboot = Integer.parseInt(prop.getProperty("num_reboot"));
            String initialPath = prop.getProperty("initial_path") != null ? prop.getProperty("initial_path") : "";
            String verifierName = prop.getProperty("verifier");
            String ackName = "Ack";
            Class<? extends SpecVerifier> verifierClass = (Class<? extends SpecVerifier>) Class.forName(verifierName);
            Constructor<? extends SpecVerifier> verifierConstructor = verifierClass.getConstructor();
            SpecVerifier verifier = verifierConstructor.newInstance();
            workloadDriver.setVerifier(verifier);
            LOG.info("State exploration strategy is " + strategy);
            Class<? extends ModelCheckingServerAbstract> modelCheckerClass = (Class<? extends ModelCheckingServerAbstract>) Class.forName(strategy);
            
            if(GuideModelChecker.class.isAssignableFrom(modelCheckerClass)){
            	String programFileName = prop.getProperty("program");
                if (programFileName == null) {
                    throw new RuntimeException("No program file specified");
                }
                LOG.info("Inspect potential bug in: " + programFileName);
                File program = new File(programFileName);
                modelCheckingServerAbstract = new GuideModelChecker(interceptorName, ackName, numNode, 
                        testRecordDir, program, workingDir, workloadDriver, ipcDir);
            } else {
                Constructor<? extends ModelCheckingServerAbstract> modelCheckerConstructor = modelCheckerClass.getConstructor(String.class, 
                        String.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, String.class, String.class, 
                        String.class, WorkloadDriver.class, String.class);
                modelCheckingServerAbstract = modelCheckerConstructor.newInstance(interceptorName, ackName, 
                        numNode, numCrash, numReboot, testRecordDir, traversalRecordDir, workingDir,
                        workloadDriver, ipcDir);
            }
            modelCheckingServerAbstract.setInitialPath(initialPath);
            verifier.modelCheckingServer = modelCheckingServerAbstract;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (ModelCheckingServerAbstract) modelCheckingServerAbstract;
    }
    
    protected static void startExploreTesting(final ModelCheckingServerAbstract checker, int numNode, String workingDir, 
    		boolean isPausedEveryTest, FileWatcher dirWatcher) throws IOException {
        File gspathDir = new File(workingDir + "/record");
        int testId = gspathDir.list().length + 1;
        File finishedFlag = new File(workingDir + "/state/.finished");
        File waitingFlag = new File(workingDir + "/state/.waiting");
        try {

        	Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                	checker.stopEnsemble();
                }
            });
        	
            for (; !finishedFlag.exists(); ++testId) {
                waitingFlag.delete();
                checker.setTestId(testId);
                Process reset = Runtime.getRuntime().exec("./bin/resettest " + numNode + 
                        " " + workingDir);
                reset.waitFor();
                workloadDriver.resetTest(testId);
                checker.runEnsemble();
                workloadDriver.runWorkload();
                checker.waitOnSteadyStatesByTimeout(); // wait on first steady state timeout
                while (!waitingFlag.exists()) {
                    Thread.sleep(30);
                }
                checker.stopEnsemble();
                dirWatcher.resetPacketCount();
                if (isPausedEveryTest) {
                    System.out.println("Press enter to continue...");
                    System.in.read();
                }
            }
            System.exit(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
