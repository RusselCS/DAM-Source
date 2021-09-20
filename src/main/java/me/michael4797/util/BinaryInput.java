package me.michael4797.util;

import java.io.IOException;

public interface BinaryInput extends AutoCloseable{

	@Override
	void close() throws IOException;
	
	/**
	 * Checks if there is at least one byte available to be read.
	 * @return True if at least one byte can be read.
	 * @throws IOException If an error is encountered while reading.
	 */
	boolean hasMoreData() throws IOException;
	
	/**
	 * Reads a single byte. Assuming {@link #hasMoreData()} has been implemented
	 * correctly, there is guaranteed to be at least one byte of data availble
	 * to be read.
	 * @return The next byte.
	 * @throws IOException If an error is encountered while reading.
	 */
	byte readNext() throws IOException;
	
	/**
	 * Reads a single byte if one is available. If no data is available, an
	 * {@link IOException} is thrown.
	 * @return The next byte.
	 * @throws IOException If an error is encountered while reading or no
	 * data is available to be read.
	 */
	default byte readByte() throws IOException{
		
		if(!hasMoreData())
			throw new IOException("End of stream");

		return readNext();
	}
	
	/**
	 * Reads a char as a single byte.
	 * @return The next char.
	 * @throws IOException If an error is encountered while reading or no
	 * data is available to be read.
	 */
	default char readChar() throws IOException{
		
		return (char) (readByte()&255);
	}
	
	/**
	 * Reads an array as a series of individual bytes.
	 * @param size The number of bytes to be read.
	 * @return A new array of the specified size containing all of the read bytes.
	 * @throws IOException If an error is encountered while reading or no
	 * data is available to be read.
	 */
	default byte[] readByteArray(int size) throws IOException{
		
		byte[] data = new byte[size];
		readByteArray(data, 0, size);		
		return data;
	}
	
	/**
	 * Reads an array as a series of individual bytes. This is equivalent to
	 * calling {@link #readByteArray(byte[], int, int) readByteArray(data, 0, data.length)}.
	 * @param data The array in which the read bytes should be placed.
	 * @throws IOException If an error is encountered while reading or no
	 * data is available to be read.
	 */
	default void readByteArray(byte[] data) throws IOException{
		
		readByteArray(data, 0, data.length);
	}
	
	/**
	 * Reads an array as a series of individual bytes.
	 * @param data The array in which the read bytes should be placed.
	 * @param offset The offset into the array to start placing bytes.
	 * @param length The number of bytes to be read.
	 * @throws IOException If an error is encountered while reading or no
	 * data is available to be read.
	 */
	default void readByteArray(byte[] data, int offset, int length) throws IOException{
		
		for(int i = 0; i < length; ++i)
			data[i + offset] = readByte();
	}
	
	/**
	 * Reads a boolean as a single byte where the value 0 represents false, and anything else is true.
	 * @return The next boolean.
	 * @throws IOException If an error is encountered while reading or no
	 * data is available to be read.
	 */
	default boolean readBoolean() throws IOException{
		
		return readByte() != 0;
	}
	
	/**
	 * Reads a short as two bytes in little endian format.
	 * @return The next short.
	 * @throws IOException If an error is encountered while reading or no
	 * data is available to be read.
	 */
	default short readShort() throws IOException{
		
		return (short) ((readByte()&255)+((readByte()&255)<<8));
	}
	
	/**
	 * Reads an integer as four bytes in little endian format.
	 * @return The next int.
	 * @throws IOException If an error is encountered while reading or no
	 * data is available to be read.
	 */
	default int readInt() throws IOException{
		
		return (readShort()&65535)+(readShort()<<16);
	}
	
	/**
	 * Reads a long as eight bytes in little endian format.
	 * @return The next long.
	 * @throws IOException If an error is encountered while reading or no
	 * data is available to be read.
	 */
	default long readLong() throws IOException {

		return (readInt()&4294967295L)+((readInt()&4294967295L)<<32);
	}
	
	/**
	 * Reads a float as an integer in its raw int format.
	 * @return The next float.
	 * @throws IOException If an error is encountered while reading or no
	 * data is available to be read.
	 * @see Float#intBitsToFloat(int)
	 */
	default float readFloat() throws IOException{
		
		return Float.intBitsToFloat(readInt());
	}
	
	/**
	 * Reads a String of the specified size. If a null terminator is read, the String
	 * is truncated to the first occurrence of the null character.
	 * @param width The number of bytes to read.
	 * @return The next String, no larger than the specified size.
	 * @throws IOException If an error is encountered while reading or no
	 * data is available to be read.
	 */
	default String readString(int width) throws IOException{

		StringBuilder string = new StringBuilder();
		boolean end = false;
		for(int i = 0; i < width; i++){
			
			char c = readChar();
			
			if(end)
				continue;
			
			if(c == 0)
				end = true;
			else
				string.append(c);
		}

		return string.toString();
	}
	
	/**
	 * Reads a string as a series of chars. Chars are read until a null terminator
	 * is encountered.
	 * @return The next String.
	 * @throws IOException If an error is encountered while reading or no
	 * data is available to be read.
	 */
	default String readString() throws IOException{
		
		StringBuilder string = new StringBuilder();
		for(;;){
			
			char c = readChar();
			if(c == 0)
				break;
			
			string.append(c);
		}
		
		return string.toString();
	}
}
