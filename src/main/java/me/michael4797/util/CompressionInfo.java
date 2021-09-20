package me.michael4797.util;

import java.util.zip.Deflater;

public class CompressionInfo {

	protected final short compressionType;
	protected final int crc32;
	protected final byte[] compressedData;
	protected final int compressedSize;
	
	
	protected CompressionInfo(byte[] input) {
		
		crc32 = ArchiveEntry.getCRC32(input);
		
		short compressionType = ZipArchive.COMPRESSION_TYPE_DEFLATE;
		Deflater compressor = new Deflater(7, true);
		compressor.setInput(input, 0, input.length);
		compressor.finish();
		byte[] compressedData = new byte[input.length];
		int compressedSize = compressor.deflate(compressedData);	
		if(!compressor.finished()) {
			
			compressedData = input;
			compressedSize = input.length;
			compressionType = ZipArchive.COMPRESSION_TYPE_NONE;
		}
			
		compressor.end();
		
		this.compressionType = compressionType;
		this.compressedData = compressedData;
		this.compressedSize = compressedSize;
	}
	
	
	protected CompressionInfo(byte[] compressedData, short compressionType,  int crc32) {
				
		this.crc32 = crc32;
		this.compressionType = compressionType;
		this.compressedData = compressedData;
		this.compressedSize = compressedData.length;
	}
}
