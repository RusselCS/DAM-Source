package com.duelimpact.entities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

import com.duelimpact.services.PropertiesService;
import com.duelimpact.services.StringService;

public class AcsCompiler {
	
	private String acsPath;
	private int exitValue;
	private String errorMessage;
	private String outputFile;
	
	public AcsCompiler() {
		acsPath = PropertiesService.getAccLocation();
		exitValue = -1;
		errorMessage = "Process not run.";
	}
	
	public void compileAcs(String source) throws IOException, InterruptedException {
		outputFile = null;
		
		String baseName = source.substring(source.lastIndexOf("/")+1, source.indexOf("."));
		String directory = source.substring(0, source.indexOf("/"));
		
		String prog = StringService.buildString(acsPath, "/acc");
		String dest = StringService.buildString(directory, "/acs/", baseName, ".o");
		String err = StringService.buildString(baseName, ".err");
		String errArg = StringService.buildString("-d./", err);
		
		Process process = new ProcessBuilder(prog, source, dest, errArg).start();
		process.waitFor();
		
		exitValue = process.exitValue();
		if(exitValue == 0) {
			errorMessage = StringService.buildString("\r", baseName, " compiled successfully!");
			outputFile = dest;
		} else {
			errorMessage = buildErrorMessage(baseName, process);
		}
		File errFile = new File(err);
		errFile.delete();
		while(errFile.exists()) {
			Thread.sleep(10l);
		}
	}
	
	private String buildErrorMessage(String fileName, Process process) {
		StringBuilder bdr = new StringBuilder();
		bdr.append("\n");
		bdr.append(StringService.lineDelimiter('=', StringService.buildString("FAILED TO COMPILE ", fileName)));
		bdr.append("\n");
		
		InputStream is = process.getErrorStream();
		InputStreamReader isr = new InputStreamReader(is);
		Scanner scan = new Scanner(isr);
		
		while(scan.nextLine().indexOf("**") == -1);
		scan.nextLine();
	    while (scan.hasNext()) {
			bdr.append(scan.nextLine());
	    	bdr.append("\n");
	    }
	    scan.close();
	    bdr.append(StringService.lineDelimiter('='));
	    return bdr.toString();
	}
	
	public String getOutputFile() {
		return outputFile;
	}
	
	public int getExitValue() {
		return exitValue;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
}
