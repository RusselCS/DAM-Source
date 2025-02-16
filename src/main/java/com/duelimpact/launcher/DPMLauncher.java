package com.duelimpact.launcher;

import java.io.IOException;

import com.duelimpact.services.FileWatcher;
import com.duelimpact.services.PropertiesService;
import com.duelimpact.services.StringService;
import com.duelimpact.services.ZipPrinter;

public class DPMLauncher {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		System.setProperty("line.separator", "\n");
		
		if(StringService.stringIs(args, 0, "build")) {
			ZipPrinter.subscribe();
			FileWatcher.compileArchive(PropertiesService.getArchive().getSource(), PropertiesService.getArchive().getBuildDestination());
		} else if(StringService.stringIs(args, 0, "start")) {
			ZipPrinter.subscribe();
			System.out.println("Watching files in " + PropertiesService.getArchive().getSource());
			FileWatcher.observeFiles(PropertiesService.getArchive().getSource(), PropertiesService.getArchive().getDestination());
		} else if(StringService.stringIs(args, 0, "acc")) {
			ZipPrinter.subscribe();
			System.out.printf("Compiling ACS in %s...\n", PropertiesService.getArchive().getSource());
			FileWatcher.compileAndCleanAcs(PropertiesService.getArchive().getSource());
		}
		
		else {
			System.out.println("Thank you for using Doom Archive Manager!\n"
							 + "In order to use this application, please enter a command after running.\n\n"
							 + "start - Observes a directory specified in archive.properties for file changes.\n"
							 + "        When a change is noted, it will be added to the pk3.\n"
							 + "build - Constructs a pk3 from the directory specified in archive.properties.\n"
							 + "acc   - Compiles all available ACS files in the source folder.\n"
							 + "\n"
							 + "For additional options, check archive.properties for details.");
		}
	}
}
