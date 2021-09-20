package me.michael4797.util;

import java.util.Arrays;

public class BinaryWriter implements BinaryOutput{

	private int index;
	private byte[] data;
	
	
	public BinaryWriter(){
		
		data = new byte[1024];
	}
	
	
	public BinaryWriter(int size){
		
		if(size < 0)
			throw new IllegalArgumentException("Size must be postive");
		
		data = new byte[size];
	}
	
	
	public BinaryWriter(byte[] data, int offset){
		
		this.data = data;
		this.index = offset;
	}
	
	
	public boolean hasData(){
		
		return index != 0;
	}
	
	
	public int getPosition(){
		
		return index;
	}
	
	
	public void setPosition(int index){
		
		this.index = index;
	}
	
	
	public void clear(){
		
		data = new byte[1024];
		index = 0;
	}
	
	
	public byte[] getData(){
		
		return Arrays.copyOf(data, index);
	}
	
	
	public byte[] getRawData(){
		
		return data;
	}
	
	
	private void incrementSize(){
	
		data = Arrays.copyOf(data, data.length<<1);
	}
	

	@Override
	public void writeByte(byte b){
		
		if(index >= data.length)
			incrementSize();
		data[index++] = b;
	}
	
	@Override
	public void writeByteArray(byte[] data) {
		
		for(byte b: data)
			writeByte(b);
	}
	
	@Override
	public void writeByteArray(byte[] data, int offset, int length) {
		
		for(int i = 0; i < length; ++i)
			writeByte(data[i + offset]);
	}
	
	@Override
	public void writeBoolean(boolean b) {
		
		writeByte(b ? (byte) 1 : (byte) 0);
	}
	
	@Override
	public void writeChar(char c) {
		
		writeByte((byte) (c&255));
	}
	
	@Override
	public void writeShort(short s) {
		
		writeByte((byte) (s&255));
		writeByte((byte) (s>>>8));
	}
	
	@Override
	public void writeInt(int i) {
		
		writeShort((short) (i&65535));
		writeShort((short) (i>>>16));
	}

	@Override
	public void writeLong(long l)  {

		writeInt((int)(l&4294967295L));
		writeInt((int)(l>>>32));
	}
	
	@Override
	public void writeFloat(float f) {
		
		writeInt(Float.floatToIntBits(f));
	}
	
	@Override
	public void writeString(String s) {
		
		if(s == null)
			s = "";
		
		for(char c: s.toCharArray())
			writeByte((byte) (c&255));
		
		writeByte((byte) 0);
	}
	
	@Override
	public void writeString(String s, int width) {
		
		for(int i = 0; i < width; i++){
			
			if(s.length() <= i)
				writeByte((byte)0);
			else
				writeByte((byte)(s.charAt(i)&255));
		}
	}

	
	@Override
	public void close() {}
}
