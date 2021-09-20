package me.michael4797.util;

import java.io.IOException;

public interface BinaryOutput extends AutoCloseable{
	
	@Override
	void close() throws IOException;
	
	/**
	 * Writes a single byte.
	 * @param b The byte to be written.
	 * @throws IOException If an error is encountered while writing.
	 */
	void writeByte(byte b) throws IOException;
	
	/**
	 * Writes each byte of the array individually.
	 * @param data The array to be written.
	 * @throws IOException If an error is encountered while writing.
	 */
	default void writeByteArray(byte[] data) throws IOException{
		
		for(byte b: data)
			writeByte(b);
	}
	
	/**
	 * Writes each of the specified bytes of the array individually.
	 * @param data The array to be written.
	 * @param offset The offset into the array to start writing.
	 * @param length The number of bytes to be written.
	 * @throws IOException If an error is encountered while writing.
	 */
	default void writeByteArray(byte[] data, int offset, int length) throws IOException{
		
		for(int i = 0; i < length; ++i)
			writeByte(data[i + offset]);
	}
	
	/**
	 * Writes a single boolean as a byte with the value 1 for true, 0 for false.
	 * @param b The boolean to be written.
	 * @throws IOException If an error is encountered while writing.
	 */
	default void writeBoolean(boolean b) throws IOException{
		
		writeByte(b ? (byte) 1 : (byte) 0);
	}
	
	/**
	 * Writes a character as a single byte.
	 * @param c The character to be written.
	 * @throws IOException If an error is encountered while writing.
	 */
	default void writeChar(char c) throws IOException{
		
		writeByte((byte) (c&255));
	}
	
	/**
	 * Writes a short as two bytes in little endian format.
	 * @param s The short to be written.
	 * @throws IOException If an error is encountered while writing.
	 */
	default void writeShort(short s) throws IOException{
		
		writeByte((byte) (s&255));
		writeByte((byte) (s>>>8));
	}
	
	/**
	 * Writes an integer as four bytes in little endian format.
	 * @param i The integer to be written.
	 * @throws IOException If an error is encountered while writing.
	 */
	default void writeInt(int i) throws IOException{
		
		writeShort((short) (i&65535));
		writeShort((short) (i>>>16));
	}

	/**
	 * Writes a long as eight bytes in little endian format.
	 * @param i The long to be written.
	 * @throws IOException If an error is encountered while writing.
	 */
	default void writeLong(long l) throws IOException {

		writeInt((int)(l&4294967295L));
		writeInt((int)(l>>>32));
	}
	
	/**
	 * Writes a float as an integer in its raw int format.
	 * @param f The float to be written.
	 * @throws IOException If an error is encountered while writing.
	 * @see Float#floatToRawIntBits(float)
	 */
	default void writeFloat(float f) throws IOException{
		
		writeInt(Float.floatToIntBits(f));
	}
	
	/**
	 * Writes a String as a series of chars truncated by a null character.
	 * @param s The String to be written.
	 * @throws IOException If an error is encountered while writing.
	 */
	default void writeString(String s) throws IOException{
		
		if(s == null)
			s = "";
		
		for(char c: s.toCharArray())
			writeByte((byte) (c&255));
		
		writeByte((byte) 0);
	}
	
	/**
	 * Writes a String as a series of chars truncated by a null character.
	 * If the String is longer than the specified width, the String will be
	 * truncated, and no null terminator will be appended. If the String is
	 * shorter than the specified width, then the String will be padded by
	 * appending additional null characters.
	 * @param s The String to be written.
	 * @param width The number of bytes used to represent this String.
	 * @throws IOException If an error is encountered while writing.
	 */
	default void writeString(String s, int width) throws IOException{
		
		for(int i = 0; i < width; i++){
			
			if(s.length() <= i)
				writeByte((byte)0);
			else
				writeByte((byte)(s.charAt(i)&255));
		}
	}
}
