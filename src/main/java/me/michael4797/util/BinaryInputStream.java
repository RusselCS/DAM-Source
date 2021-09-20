package me.michael4797.util;

import java.io.IOException;
import java.io.InputStream;

public class BinaryInputStream extends InputStream implements BinaryInput{

	private final InputStream source;
	private final byte[] data;
	private int index;
	private int limit;
	
	
	public BinaryInputStream(InputStream source){
		
		this.source = source;
		data = new byte[65535];
	}
	
	
	private boolean readBlock() throws IOException {
		
		if(limit == -1)
			return false;
		
		limit = source.read(data, 0, data.length);
		index = 0;
		
		return limit != -1;
	}
	
	
	public boolean hasMoreData() throws IOException{
				
		if(index < limit)
			return true;
		
		return readBlock();
	}


	@Override
	public byte readNext() throws IOException {

		return data[index++];
	}


	@Override
	public int read() throws IOException {

		if(hasMoreData())
			return readNext()&255;
		
		return -1;
	}
	
	
	@Override
	public void close() throws IOException {
		
		source.close();
	}
}
