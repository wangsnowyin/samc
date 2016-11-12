package edu.uchicago.cs.ucare.samc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StackTracePrinter {
	
	private static final Logger logger = LoggerFactory.getLogger(StackTracePrinter.class);
	
	public static void print(Logger logger) {
		StringBuilder resultBuilder = new StringBuilder("Print stack trace\n");
    	StackTraceElement[] trace= Thread.currentThread().getStackTrace();
    	resultBuilder.append(trace[2].getClassName());
    	resultBuilder.append('.');
    	resultBuilder.append(trace[2].getMethodName());
    	for (int i = 2; i < trace.length; ++i) {
            resultBuilder.append('\n');
    		resultBuilder.append(trace[i].toString());
    	}
    	logger.info(resultBuilder.toString());
	}
	
	public static void print() {
		StringBuilder resultBuilder = new StringBuilder("Print stack trace\n");
    	StackTraceElement[] trace= Thread.currentThread().getStackTrace();
    	resultBuilder.append(trace[2].getClassName());
    	resultBuilder.append('.');
    	resultBuilder.append(trace[2].getMethodName());
    	for (int i = 2; i < trace.length; ++i) {
            resultBuilder.append('\n');
    		resultBuilder.append(trace[i].toString());
    	}
    	logger.info(resultBuilder.toString());
	}
	
	public static String getStackTrace() {
		StringBuilder resultBuilder = new StringBuilder("Print stack trace\n");
    	StackTraceElement[] trace= Thread.currentThread().getStackTrace();
    	resultBuilder.append(trace[2].getClassName());
    	resultBuilder.append('.');
    	resultBuilder.append(trace[2].getMethodName());
    	for (int i = 2; i < trace.length; ++i) {
            resultBuilder.append('\n');
    		resultBuilder.append(trace[i].toString());
    	}
    	return resultBuilder.toString();
	}

}
