package me.michael4797.util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class ArchiveEntry {

	private static final CRC32 crc = new CRC32();
	private static final short COMPLIANT_VERSION_NUMBER = 20;

	public final String name;
	public final short compressionType;
	public final short lastModifiedTime;
	public final short lastModifiedDate;
	public final int crc32;
	public final int compressedSize;
	public final int uncompressedSize;
	private long position;
	private long directoryOffset;

	
	protected ArchiveEntry(short compressionType, short lastModifiedTime, short lastModifiedDate, int crc32, int compressedSize, int uncompressedSize, String name, long position, long directoryOffset) throws IOException{
		
		this.compressionType = compressionType;
		this.lastModifiedTime = lastModifiedTime;
		this.lastModifiedDate = lastModifiedDate;					
		this.crc32 = crc32;
		this.compressedSize = compressedSize;
		this.uncompressedSize = uncompressedSize;
		this.name = name;
		this.position = position;
		this.directoryOffset = directoryOffset;
	}

	
	protected ArchiveEntry(String name, byte[] compressedData, short lastModifiedTime, short lastModifiedDate, short compressionType, long directoryOffset, int crc32, int compressedSize, int uncompressedSize, BinaryRandomAccess output) throws IOException{
		
		if(name.length() > 65535)
			throw new IOException("Entry name too long");
		
		this.name = name;
		this.compressionType = compressionType;
		this.lastModifiedTime = lastModifiedTime;
		this.lastModifiedDate = lastModifiedDate;
		this.crc32 = crc32;
		this.compressedSize = compressedSize;
		this.uncompressedSize = uncompressedSize;
		this.directoryOffset = directoryOffset;

		position = output.getFilePointer();
		output.writeInt(ZipArchive.LOCF_SIGNATURE);
		output.writeShort(COMPLIANT_VERSION_NUMBER);
		output.writeShort((short) 0);
		output.writeShort(compressionType);
		output.writeShort(lastModifiedTime);
		output.writeShort(lastModifiedDate);
		output.writeInt(crc32);
		output.writeInt(compressedSize);
		output.writeInt(uncompressedSize);
		output.writeShort((short) name.length());
		output.writeShort((short) 0);
		output.writeString(name, name.length());
		
		output.writeByteArray(compressedData, 0, compressedSize);
	}
	
	
	protected long getHeaderPosition() {
		
		return position;
	}
	
	
	protected long getFilePosition() {
		
		return position + 30 + name.length();
	}
	
	
	protected long getDirectoryOffset() {
		
		return directoryOffset;
	}
	
	
	protected long getHeaderSize() {
		
		return 30 + name.length();
	}
	
	
	protected long getFileSize() {
		
		return compressedSize;
	}
	
	
	protected long getTotalSize() {
		
		return compressedSize + 30 + name.length();
	}


	protected void moveTo(long position, BinaryRandomAccess output) throws IOException {
		
		if(position != this.position) {
			
			output.seek(this.position);
			byte[] compressedData = output.readByteArray((int) getTotalSize());
			output.seek(position);
			output.writeByteArray(compressedData);
			this.position = position;
		}			
	}
	
	
	public long getLastModified() {
		
		return LocalDateTime.of(((lastModifiedDate >>> 9) & 127) + 1980, (lastModifiedDate >>> 5) & 15, lastModifiedDate & 31,
			(lastModifiedTime >>> 11) & 31, (lastModifiedTime >>> 5) & 63, (lastModifiedTime & 31) << 1).atZone(ZoneOffset.systemDefault()).toEpochSecond() * 1000;
	}
	
	
	protected void moveDirectoryOffset(long shift) {
		
		directoryOffset += shift;
	}
	
	
	protected void writeDirectoryListing(long directoryPosition, BinaryRandomAccess output) throws IOException {
		
		output.seek(directoryPosition + directoryOffset);
		output.writeInt(ZipArchive.CD_SIGNATURE);
		output.writeShort(COMPLIANT_VERSION_NUMBER);
		output.writeShort(COMPLIANT_VERSION_NUMBER);
		output.writeShort((short) 0);
		output.writeShort(compressionType);
		output.writeShort(lastModifiedTime);
		output.writeShort(lastModifiedDate);
		output.writeInt(crc32);
		output.writeInt(compressedSize);
		output.writeInt(uncompressedSize);
		output.writeShort((short) name.length());
		output.writeShort((short) 0);
		output.writeShort((short) 0);
		output.writeShort((short) 0);
		output.writeShort((short) 1);
		output.writeInt(32);
		output.writeInt((int) position);
		output.writeString(name, name.length());
	}
	
	
	protected byte[] extract(BinaryRandomAccess from) throws IOException {
		
		from.seek(getFilePosition());
		byte[] compressedData = from.readByteArray(compressedSize);
		
		if(compressionType == ZipArchive.COMPRESSION_TYPE_NONE)
			return checkCRC32(compressedData, crc32);
		else if(compressionType != ZipArchive.COMPRESSION_TYPE_DEFLATE)
			throw new IOException("Unsupported compression type " + compressionType);
		
		Inflater decompresser = new Inflater(true);
		decompresser.setInput(compressedData, 0, compressedSize);
		byte[] uncompressedData = new byte[uncompressedSize];
		
		try {
			
			if(decompresser.inflate(uncompressedData) != uncompressedSize)
				throw new IOException("Corrupted zip archive");
		} catch (DataFormatException e) {
			
			throw new IOException(e);
		}
		
		decompresser.end();
		return checkCRC32(uncompressedData, crc32);
	}


	public byte[] getCompressedData(BinaryRandomAccess source) throws IOException {

		source.seek(getFilePosition());
		return source.readByteArray(compressedSize);
	}
	
	
	protected synchronized static int getCRC32(byte[] data) {
		
		crc.update(data);
		int value = (int) crc.getValue();
		crc.reset();
		
		return value;
	}
	
	
	private static byte[] checkCRC32(byte[] data, int crc32) throws IOException{
		
		if(getCRC32(data) != crc32)
			throw new IOException("Corrupted zip archive");
		
		return data;
	}
}
