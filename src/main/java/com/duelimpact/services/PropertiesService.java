package com.duelimpact.services;

import java.io.FileReader;
import java.util.Properties;

import com.duelimpact.entities.Archive;

public class PropertiesService {
	
	private static Archive archive;
	private static String accLocation;
	private static String acsExtensions[];
	private static String acsExclusions[];
	private static boolean logAcs;
	private static boolean killOnWarn;
	
	static {
		archive = new Archive();
		
		try {
			Properties prop = new Properties();
			prop.load(new FileReader("archive.properties"));
			archive.setDestination(prop.getProperty("archive.destination"));
			archive.setSource(prop.getProperty("archive.source"));
			archive.setBuildDestination(prop.getProperty("archive.build.destination"));
			
			accLocation = prop.getProperty("acc.location");
			acsExtensions = StringService.defaultStringArr(
					StringService.appendAllPre(".", StringService.arrFromString(prop.getProperty("acc.extensions"), "\\|")), 
					new String[] {".acs"}
			);
			acsExclusions = StringService.arrFromString(prop.getProperty("acc.exclusions"), "\\|");
			logAcs = Boolean.parseBoolean(prop.getProperty("acc.logacs"));
			
			killOnWarn = Boolean.parseBoolean(prop.getProperty("archive.build.killonwarn"));
			
		} catch (Exception e) {
			System.err.println("Could not load archive properties. Please ensure archive.properties is in the same file as dam.jar.");
			e.printStackTrace();
		}
			
		
	}
	
	public static Archive getArchive() {
		return archive;
	}

	public static String getAccLocation() {
		return accLocation;
	}
	
	public static String[] getACSFileTypes() {
		return acsExtensions;
	}
	
	public static String[] getACSExclusions() {
		return acsExclusions;
	}
	
	public static boolean doLogAcs() {
		return logAcs;
	}
	
	public static boolean killOnWarn() {
		return killOnWarn;
	}
}
