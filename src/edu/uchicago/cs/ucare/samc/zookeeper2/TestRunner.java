package edu.uchicago.cs.ucare.samc.zookeeper2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchicago.cs.ucare.samc.server.FileWatcher;
import edu.uchicago.cs.ucare.samc.server.GuideModelChecker;
import edu.uchicago.cs.ucare.samc.server.ModelCheckingServerAbstract;
import edu.uchicago.cs.ucare.samc.util.WorkloadDriver;
import edu.uchicago.cs.ucare.samc.util.SpecVerifier;

public class TestRunner {
	
	final static Logger LOG = LoggerFactory.getLogger(TestRunner.class);

    static WorkloadDriver workloadDriver;
	
	public static void main(String[] argv){
		String configFileLocation = null;
		if (argv.length == 0) {
            System.err.println("Please specify test config file");
            System.exit(1);
        }
		boolean pauseEveryPathExploration = false;
        for (String param : argv) {
            if (param.equals("-p")) {
            	pauseEveryPathExploration = true;
            } else {
                configFileLocation = param;
            }
        }
        
        prepareModelChecker(configFileLocation, pauseEveryPathExploration);
	}
	
	public static void prepareModelChecker(String configFileLocation, boolean pauseEveryPathExploration){
    	try{
	    	Properties config = new Properties();
	        FileInputStream configInputStream = new FileInputStream(configFileLocation);
	        
	        config.load(configInputStream);
	        configInputStream.close();
	        String workingDir = config.getProperty("working_dir");
	        String ipcDir = config.getProperty("ipc_dir");
	        String samcDir = config.getProperty("samc_dir");
	        String targetSysDir = config.getProperty("target_sys_dir");
	        String workload = config.getProperty("workload_driver");
	        int numNode = Integer.parseInt(config.getProperty("num_node"));
	        
	        @SuppressWarnings("unchecked")
	        Class<? extends WorkloadDriver> workloadDriverClass = (Class<? extends WorkloadDriver>) Class.forName(workload);
	        Constructor<? extends WorkloadDriver> workloadDriverConstructor = workloadDriverClass.getConstructor(Integer.TYPE, 
	        		String.class, String.class, String.class, String.class);
	        workloadDriver = workloadDriverConstructor.newInstance(numNode, workingDir, ipcDir, samcDir, targetSysDir);
	        ModelCheckingServerAbstract checker = createModelCheckerFromConf(workingDir + "/target-sys.conf", workingDir, 
	        		workloadDriver, ipcDir);
        	
        	// start path explorations
        	startExploreTesting(checker, numNode, workingDir, pauseEveryPathExploration, ipcDir);
	        
    	} catch (Exception e){
    		e.printStackTrace();
    	}
    }
	
	protected static ModelCheckingServerAbstract createModelCheckerFromConf(String confFile, 
            String workingDir, WorkloadDriver ensembleController, String ipcDir) {
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
            
            @SuppressWarnings("unchecked")
            Class<? extends SpecVerifier> verifierClass = (Class<? extends SpecVerifier>) Class.forName(verifierName);
            Constructor<? extends SpecVerifier> verifierConstructor = verifierClass.getConstructor();
            SpecVerifier verifier = verifierConstructor.newInstance();
            ensembleController.setVerifier(verifier);
            LOG.info("State exploration strategy is " + strategy);
            
            @SuppressWarnings("unchecked")
			Class<? extends ModelCheckingServerAbstract> modelCheckerClass = (Class<? extends ModelCheckingServerAbstract>) Class.forName(strategy);
            
            if(GuideModelChecker.class.isAssignableFrom(modelCheckerClass)){
            	String programFileName = prop.getProperty("program");
                if (programFileName == null) {
                    throw new RuntimeException("No program file specified");
                }
                System.out.println("Inspect potential bug in: " + programFileName);
                File program = new File(programFileName);
                Constructor<? extends ModelCheckingServerAbstract> programmableCheckerConstructor = modelCheckerClass.getConstructor(
                		String.class, String.class, Integer.TYPE, String.class, File.class, 
                		String.class, WorkloadDriver.class, String.class);
                modelCheckingServerAbstract = programmableCheckerConstructor.newInstance(interceptorName, ackName, numNode,
                		testRecordDir, program, workingDir, ensembleController, ipcDir);
            } else {
            	Constructor<? extends ModelCheckingServerAbstract> modelCheckerConstructor = modelCheckerClass.getConstructor(String.class, 
	                    String.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, String.class, String.class, 
	                    String.class, WorkloadDriver.class, String.class);
	            modelCheckingServerAbstract = modelCheckerConstructor.newInstance(interceptorName, ackName, 
	                    numNode, numCrash, numReboot, testRecordDir, traversalRecordDir, workingDir, 
	                    ensembleController, ipcDir);
            }
            modelCheckingServerAbstract.setInitialPath(initialPath);
            verifier.modelCheckingServer = modelCheckingServerAbstract;
		} catch (Exception e) {
            e.printStackTrace();
        }
        return (ModelCheckingServerAbstract) modelCheckingServerAbstract;
	}
	
	protected static void startExploreTesting(final ModelCheckingServerAbstract checker, int numNode, String workingDir,
			boolean pauseEveryPathExploration, String ipcDir) throws IOException {
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
    		
	    	// activate Directory Watcher
	        FileWatcher dirWatcher = new FileWatcher(ipcDir, checker);
	        Thread watcherThread = new Thread(dirWatcher);
	        watcherThread.start();
	    	Thread.sleep(300);
	        
            for (; !finishedFlag.exists(); ++testId) {
            	waitingFlag.delete();
            	System.out.println("*** Path Exploration No-" + testId + " ***");
                checker.setTestId(testId);
                workloadDriver.resetTest(testId);
                dirWatcher.resetPacketCount();
                checker.runEnsemble();
                workloadDriver.runWorkload();
                checker.waitOnSteadyStatesByTimeout(); // wait on first steady state timeout
                while (!waitingFlag.exists()) {
                    Thread.sleep(30);
                }
                checker.stopEnsemble();
                if (pauseEveryPathExploration) {
                    System.out.println("Press enter to continue...");
                    System.in.read();
                } else {
	                // pause the next iteration for a while
	    	    	Thread.sleep(200);
                }
            }
            
            System.exit(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
