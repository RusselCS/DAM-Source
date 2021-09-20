package me.michael4797.util;

import java.io.IOException;
import java.io.OutputStream;

public class BinaryOutputStream extends OutputStream implements BinaryOutput{
	
	private final OutputStream out;
	
	
	public BinaryOutputStream(OutputStream out){
		
		this.out = out;
	}
	
	
	@Override
	public void write(int i) throws IOException{
		
		out.write(i);
	}
	
	
	@Override
	public void writeByte(byte b) throws IOException{
		
		out.write(b&255);
	}
	
	
	@Override
	public void close() throws IOException {
		
		out.close();
	}
}
