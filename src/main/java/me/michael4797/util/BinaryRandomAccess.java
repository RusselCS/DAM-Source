package me.michael4797.util;

import java.io.IOException;
import java.io.RandomAccessFile;

public class BinaryRandomAccess implements BinaryInput, BinaryOutput{

	private final RandomAccessFile source;
	private final byte[] outBuffer;
	private int index = 0;
	private long position;
	
	
	public BinaryRandomAccess(RandomAccessFile source) throws IOException {
		
		this.source = source;
		this.position = source.getFilePointer();
		outBuffer = new byte[8192];
	}
	
	
	public void seek(long position) throws IOException{
		
		if(position != this.position) {
		
			flush();
			this.position = position;
			source.seek(position);
		}
	}
	
	
	public long length() throws IOException{
		
		return source.length();
	}
	
	
	public long getFilePointer() throws IOException{
		
		return position;
	}


	@Override
	public void writeByte(byte b) throws IOException {
		
		if(index >= outBuffer.length)
			flush();
		
		++position;
		outBuffer[index++] = b;
	}


	@Override
	public void close() throws IOException {
		
		flush();		
		source.close();
	}


	@Override
	public boolean hasMoreData() throws IOException {

		return position < source.length();
	}


	@Override
	public byte readNext() throws IOException {
		
		flush();
		++position;
		return source.readByte();
	}


	@Override
	public void readByteArray(byte[] data, int offset, int length) throws IOException {
		
		flush();
		position += length;
		source.read(data, offset, length);
	}


	public void setLength(long newLength) throws IOException {
		
		if(newLength < position)
			flush();
		
		source.setLength(newLength);
	}


	public void flush() throws IOException {

		if(index > 0) {
			
			source.write(outBuffer, 0, index);
			index = 0;
		}
	}
}
