package com.duelimpact.launcher;

import java.io.IOException;

import com.duelimpact.entities.AcsCompiler;

public class ACCLaunchTest {
	
	public static void main(String [] args) throws IOException, InterruptedException {
		AcsCompiler acs = new AcsCompiler();
		
		acs.compileAcs("mm8bdm-pk3/acs_source/global.acs");
		System.out.println(acs.getErrorMessage());
		System.out.println("Exit Code: " + acs.getExitValue());
	}
}
