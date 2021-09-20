package me.michael4797.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class EntrySource implements Comparable<EntrySource>{

	public final String entryName;
	public final int uncompressedSize;
	protected final short lastModifiedDate;
	protected final short lastModifiedTime;
	private final EntryData data;
	
	
	public EntrySource(String entryName, byte[] entryData) {
		
		this.entryName = entryName;

		ZonedDateTime dateTime = ZonedDateTime.now(ZoneOffset.systemDefault());
		lastModifiedTime = (short) ((dateTime.getHour() << 11) | (dateTime.getMinute() << 5) | (dateTime.getSecond() >> 1));
		lastModifiedDate = (short) (((dateTime.getYear()-1980) << 9) | (dateTime.getMonthValue() << 5) | dateTime.getDayOfMonth());
		uncompressedSize = entryData.length;
		
		data = new RawEntryData(entryData);
	}
	
	
	public EntrySource(String entryName, int uncompressedSize, short lastModifiedDate, short lastModifiedTime, short compressionType, int crc32, byte[] compressedData) {
		
		this.entryName = entryName;
		this.lastModifiedTime = lastModifiedTime;
		this.lastModifiedDate = lastModifiedDate;
		this.uncompressedSize = uncompressedSize;
		
		data = new CompressedEntryData(compressedData, compressionType, crc32);
	}
	
	
	public EntrySource(String entryName, File entryData) {
		
		this.entryName = entryName;

		ZonedDateTime dateTime = Instant.ofEpochMilli(entryData.lastModified()).atZone(ZoneOffset.systemDefault());
		lastModifiedTime = (short) ((dateTime.getHour() << 11) | (dateTime.getMinute() << 5) | (dateTime.getSecond() >> 1));
		lastModifiedDate = (short) (((dateTime.getYear()-1980) << 9) | (dateTime.getMonthValue() << 5) | dateTime.getDayOfMonth());
		uncompressedSize = (int) entryData.length();

		data = new FileEntryData(entryData);
	}
	
	
	protected CompressionInfo compress() throws IOException{
		
		return data.compress();
	}
	
	
	private static interface EntryData{	
		CompressionInfo compress() throws IOException;
	}
	
	private static class FileEntryData implements EntryData{
		
		private final File data;
		
		
		private FileEntryData(File data) {
			
			this.data = data;
		}
		
		
		public CompressionInfo compress() throws IOException{
			
			byte[] buffer = new byte[(int) data.length()];
			try (FileInputStream in = new FileInputStream(data)){
				
				int remaining = buffer.length;
				while(remaining > 0) {
					
					int len = in.read(buffer, buffer.length - remaining, remaining);
					remaining -= len;			
				}
			}
			
			return new CompressionInfo(buffer);
		}
		
	}
	
	private static class RawEntryData implements EntryData{
		
		private final byte[] data;
		
		
		private RawEntryData(byte[] data) {
			
			this.data = data;
		}
		
		
		public CompressionInfo compress() throws IOException {
			
			return new CompressionInfo(data);
		}
	}
	
	private static class CompressedEntryData implements EntryData{
		
		private final byte[] data;
		private final short compressionType;
		private final int crc32;
		
		
		private CompressedEntryData(byte[] data, short compressionType, int crc32) {
			
			this.data = data;
			this.compressionType = compressionType;
			this.crc32 = crc32;
		}
		
		
		public CompressionInfo compress() throws IOException {
			
			return new CompressionInfo(data, compressionType, crc32);
		}
	}

	@Override
	public int compareTo(EntrySource o) {

		return o.uncompressedSize - uncompressedSize;
	}
}
