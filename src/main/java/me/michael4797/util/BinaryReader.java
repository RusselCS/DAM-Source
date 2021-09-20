package me.michael4797.util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;

public class BinaryReader implements BinaryInput{

	private byte[] data;
	private int index;
	private int offset;
	private int limit;
	
	
	public BinaryReader(DatagramPacket recieved){
		
		data = recieved.getData();
		offset = recieved.getOffset();
		limit = offset+recieved.getLength();
	}
	
	
	public BinaryReader(byte[] data){
		
		this.data = data;
		offset = 0;
		limit = this.data.length;
	}
	
	
	public BinaryReader(byte[] data, int offset, int length){
		
		this.data = data;		
		limit = offset+length;
		this.offset = offset;
	}
	
	
	public boolean hasMoreData(){
		
		return index+offset < limit;
	}
	
	
	public int getPosition(){
		
		return index;
	}
	
	
	public int getOffset(){
		
		return offset;
	}
	
	
	public int getLimit(){
		
		return limit;
	}
	
	
	public void setPosition(int index){
		
		this.index = index;
	}
	
	
	public void rewind(int bytes){
		
		index -= bytes;
	}
	
	
	public void skip(int bytes){
		
		index += bytes;
	}
	
	
	public String toString(){
		
		String toRet = "";
		
		if(index+offset < limit)
			toRet += "Index: " + (index+offset) + "  Value: " + data[index+offset] + "\n";
		toRet += "[";
		for(int i = 0; i < limit; i++){
			
			if(i == index+offset)
				toRet += "***";

			toRet += data[i];
			if(i == index+offset)
				toRet += "***";
			if(i != limit-1)
				toRet += ", ";
		}
		toRet += "]";
		
		return toRet;
	}
	
	
	public byte[] getData(){
		
		return Arrays.copyOfRange(data, offset, limit);
	}
	
	
	public byte[] getRawData(){
		
		return data;
	}
	
	
	public void setData(byte[] data) {

		this.data = data;
		this.index = 0;
		this.limit = data.length;
		this.offset = 0;		
	}
	
	
	public void setData(byte[] data, int offset, int length) {
		
		this.data = data;
		this.index = 0;
		this.limit = offset+length;
		this.offset = offset;
	}
	
	
	public byte readNext() throws IOException{

		return data[offset+index++];
	}


	@Override
	public void close() throws IOException {}
}
