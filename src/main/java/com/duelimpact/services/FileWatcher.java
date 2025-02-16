package com.duelimpact.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.duelimpact.entities.AcsCompiler;

import me.michael4797.util.ArchiveEntry;
import me.michael4797.util.EntrySource;
import me.michael4797.util.ZipArchive;

public class FileWatcher {
	
	private static Map<File, Long> fileMap;
	private static ZipArchive zip;
	private static Collection<Path> acsPaths;
	private static PrintWriter out;
	private static File outFile;
	private static boolean watchMode;
	
	private static final int WARN_ACS = 0;
	private static final int WARN_COUNT = 1;
	
	private static int warnCount[];
	
	
	enum ACSCommand {
		ACSC_COPY,
		ACSC_DELETE
	}
	
	static {
		fileMap = new HashMap<>();
		acsPaths = new ArrayList<>();
		warnCount = new int[WARN_COUNT];
		warnCount[WARN_ACS] = 0;
	}
	
	public static void observeFiles(String sourceDir, String destDir) {
		watchMode = true;
		
		try {
			File destFile = new File(destDir);
			if(destFile.exists()) {
				destFile.delete();
			}
			zip = new ZipArchive(destFile);
			File source = new File(sourceDir);
			
			cleanAcs(sourceDir);

			while (true) {
				Collection<File> updates = updateMap(source, fileMap);
				updates.addAll(checkDeletions(fileMap));
				if (updates.size() > 0) {
					String timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
					System.out.printf("\n%s - Updating archive %s...\n", timeStamp, destFile.getName());
					checkAcs(updates);
					
					zip.removeAll(findEntries(updates));
					zip.addAll(findSources(updates));
				}

				Thread.sleep(2000);
			}
		} catch (Exception e) {
			System.err.println("Exception occurred during processing of file changes");
			e.printStackTrace();
		}
	}
	
	private static Collection<ArchiveEntry> findEntries(Collection<File> files) {
		Collection<ArchiveEntry> result = new ArrayDeque<>();
		
		for(File file : files) {
			String path = file.getPath().replaceAll("\\\\", "/");
			path = path.substring(path.indexOf("/")+1, path.length());
			if(zip.containsEntry(path)) {
				result.add(zip.getEntry(path));
			}
		}
		
		return result;
	}
	
	private static Collection<EntrySource> findSources(Collection<File> files) {
		Collection<EntrySource> result = new ArrayDeque<>();
		
		for(File file : files) {
			if(!file.isDirectory() && file.exists()) {
				String path = file.getPath().replaceAll("\\\\", "/");
				path = path.substring(path.indexOf("/")+1, path.length());
				result.add(new EntrySource(path, file));
			}
		}
		
		return result;
	}
	
	private static Collection<File> updateMap(File directory, Map<File, Long> map) {
		Collection<File> result = new ArrayDeque<>();

		for (File file : directory.listFiles()) {
			if (file.isDirectory()) {
				result.addAll(updateMap(file, map));
			}
			if (map.get(file) == null || map.get(file) != file.lastModified()) {
				map.put(file, file.lastModified());
				result.add(file);
			}
		}
		
		return result;
	}
	
	private static Collection<File> checkDeletions(Map<File, Long> map) {
		Collection<File> result = new ArrayDeque<>();
		
		Collection<File> mapRemoval = new ArrayDeque<>();
		for(File key : map.keySet()) {
			if(!key.exists()) {
				System.out.println(key.getName() + " does not exist!!");
				mapRemoval.add(key);
				result.add(key);
			}
		}
		
		for(File key : mapRemoval) {
			map.remove(key);
		}
		
		return result;
	}
	
	public static int countDirs(File directory, int start) {
		for (File file : directory.listFiles()) {
			if (file.isDirectory()) {
				start += countDirs(file, start);
			}
		}
		return start;
	}
	
	public static void compileAndCleanAcs(String sourceDir) {
		cleanAcs(sourceDir);
		compileAcs(sourceDir);
	}
	
	public static void compileAcs(String sourceDir) {
		try
		{
			File source = new File(sourceDir);
			Collection<File> updates = updateMap(source, fileMap);
			checkAcs(updates);
		} catch (Exception e) {
			raiseWarning(WARN_ACS);
			System.err.println("Exception occurred during processing of ACS files.\n");
			e.printStackTrace();
		}
	}
	
	public static void updateAcsPaths(ACSCommand cmd, Collection<File> updates) throws IOException {
		if(cmd == ACSCommand.ACSC_COPY) {
			acsPaths.clear();
			for(File file : updates) {
				if( StringService.endsWithAny(file.getName(), PropertiesService.getACSFileTypes()) ) {
					Path outPath = Paths.get(PropertiesService.getAccLocation(), file.getName());
					acsPaths.add(outPath);
					Files.copy(file.getAbsoluteFile().toPath(), outPath);
				}
			}
		} else if (cmd == ACSCommand.ACSC_DELETE) {
			for(Path outPath : acsPaths) {
				Files.delete(outPath);
			}
		}
	}
	
	private static void cleanAcs(String sourceDir) {
		try
		{
			File acsDir = new File(sourceDir + "/acs");
			
			if(!acsDir.exists()) {
				acsDir.mkdir();
			} else {
				for(File f : acsDir.listFiles()) {
					if(f.getName().toLowerCase().endsWith(".o")) {
						f.delete();
					}
				}
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception occurred while cleaning ACS folder.");
			e.printStackTrace();
		}
	}
	
	private static void checkAcs(Collection<File> updates) throws IOException, InterruptedException {
		
		boolean compilingACS = false;
		
		Collection<File> acsUps = new ArrayDeque<>();
		for(File file : updates) {
			if( StringService.endsWithAny(file.getName(), PropertiesService.getACSFileTypes()) ) {
				if(!compilingACS) {
					updateAcsPaths(ACSCommand.ACSC_COPY, fileMap.keySet());
					compilingACS = true;
				}
				
				if(!StringService.contains(PropertiesService.getACSExclusions(), file.getName())) {
					if(PropertiesService.doLogAcs()) {
						if(out == null) {
							outFile = new File(PropertiesService.getArchive().getSource() + "/acs/_acslist.txt");
							out = new PrintWriter(outFile);
						}
						
						out.printf("===== %s\n", file.getPath().substring(PropertiesService.getArchive().getSource().length(), file.getPath().length()));
						out.flush();
						
						Scanner fScan = new Scanner(file);
						
						while(fScan.hasNext()) {
							String line = fScan.nextLine();
							Scanner lScan = new Scanner(line);
							
							if(lScan.hasNext() && lScan.next().equalsIgnoreCase("script")) {
								out.printf("%s\n", line);
								out.flush();
							}
							lScan.close();
						}
						
						out.printf("\n");
						out.flush();
						
						fileMap.put(outFile, outFile.lastModified());
						
						fScan.close();
					}
					
					AcsCompiler acs = new AcsCompiler();
					System.out.print("Compiling " + file.getName() + "...");
					acs.compileAcs(file.getPath().replaceAll("\\\\", "/"));
					System.out.println(acs.getErrorMessage());
					
					if(acs.getExitValue() == 0) {
						File acsUp = new File(acs.getOutputFile());
						if(!updates.contains(acsUp)) {
							acsUps.add(acsUp);
						}
						fileMap.put(acsUp, acsUp.lastModified());
					} else {
						raiseWarning(WARN_ACS);
					}
					
					File dir = new File(file.getParent());
					fileMap.put(dir, dir.lastModified());
				}
			}
		}
		
		if(compilingACS) {
			updateAcsPaths(ACSCommand.ACSC_DELETE, null);
		}
		
		if(acsUps.size() > 0) {
			updates.addAll(acsUps);
		}
	}
	
	private static int buildProgress;
	private static int buildMax;
	
	public static void compileArchive(String sourceDir, String destDir) {
		watchMode = false;
		try {
			cleanAcs(sourceDir);
			
			System.out.println("\nCompiling ACS source files...");
			File destFile = new File(destDir);
			if(destFile.exists()) {
				destFile.delete();
			}
			File source = new File(sourceDir);
			
			compileAcs(sourceDir);
			
			System.out.println(StringService.lineDelimiter('-'));
			
			Thread.sleep(500);
			fileMap.clear();
			System.out.printf("Compiling archive to %s...\n", destDir);
			
			buildMax = updateMap(source, fileMap).size();
			ZipPrinter.setMaxFiles(buildMax);
			ZipPrinter.updateSubject(0);
			
			pack(Paths.get(sourceDir), Paths.get(destDir));
			ZipPrinter.updateSubject(buildMax);
			System.out.println(StringService.lineDelimiter('-'));
			
			int exitCode = exitCode();
			if(exitCode == 0) {
				System.out.println("Build completed successfully.\n\n");
			} else {
				System.out.println("Build completed with warnings.\nPlease see above for details.\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void raiseWarning(int type) {
		warnCount[type]++;
		if(!watchMode && PropertiesService.killOnWarn()) {
			System.out.println("Build cancelled due to errors. Please see above.\n\n");
			try {
				updateAcsPaths(ACSCommand.ACSC_DELETE, null);
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.exit(exitCode());
		}
	}
	
	public static int exitCode() {
		int ret = 0;
		int errType = 1;
		for(int w : warnCount) {
			if(w > 0) {
				ret+=errType;
			}
			errType *= 2;
		}
		return ret;
	}
	
	// CODE BY JJST ON STACK OVERFLOW
	//
	
	public static void pack(final Path folder, final Path zipFilePath) throws IOException {
		buildProgress = 0;
	    try (
	            FileOutputStream fos = new FileOutputStream(zipFilePath.toFile());
	            ZipOutputStream zos = new ZipOutputStream(fos)
	        ) {
	        Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
	            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	            	String fileName = folder.relativize(file).toString().replace("\\", "/");
	            	buildProgress++;
	            	ZipPrinter.updateSubject(buildProgress);
	    			ZipPrinter.setFileName(fileName);
	                zos.putNextEntry(new ZipEntry(fileName));
	                Files.copy(file, zos);
	                zos.closeEntry();
	                return FileVisitResult.CONTINUE;
	            }

	            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
	            	String fileName = folder.relativize(dir).toString() + "/";
	            	buildProgress++;
	            	ZipPrinter.updateSubject(buildProgress);
	    			ZipPrinter.setFileName(fileName);
	                zos.putNextEntry(new ZipEntry(fileName));
	                zos.closeEntry();
	                return FileVisitResult.CONTINUE;
	            }
	        });
	    }
	}
	
}
