package me.michael4797.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.PriorityQueue;

import com.duelimpact.services.ZipPrinter; // [RCS] Used for updating the progress meter.

public class ZipArchive implements Iterable<ArchiveEntry>, AutoCloseable{
	
	public static final short COMPRESSION_TYPE_NONE = 0;
	public static final short COMPRESSION_TYPE_DEFLATE = 8;

	protected static final int LOCF_SIGNATURE = 67324752;
	protected static final int CD_SIGNATURE = 33639248;
	private static final int EOCD_SIGNATURE = 101010256;
	
	private static final byte MINIMUM_BLOCK_SIZE = 5;
	private static final int EOCD_SIZE = 22;
	private final BinaryRandomAccess source;

	private final LinkedHashMap<String, ArchiveEntry> entries = new LinkedHashMap<>();
	private final ArrayList<StorageList> storage = new ArrayList<>();
	
	private final boolean readOnly;
	
	private long cdPos;
	private long eocdPos;
	private long totalSize;
	
	
	public ZipArchive(RandomAccessFile zipFile) throws IOException{
		
		this(zipFile, false);
	}
	
	
	public ZipArchive(RandomAccessFile zipFile, boolean readOnly) throws IOException{
		
		source = new BinaryRandomAccess(zipFile);
		this.readOnly = readOnly;
		init();
	}
	
	
	public ZipArchive(File zipFile) throws IOException {		

		this(zipFile, false);
	}
	
	
	public ZipArchive(File zipFile, boolean readOnly) throws IOException {
		
		if(readOnly)
			source = new BinaryRandomAccess(new RandomAccessFile(zipFile, "r"));
		else
			source = new BinaryRandomAccess(new RandomAccessFile(zipFile, "rw"));
		
		this.readOnly = readOnly;
		
		try {
			
			init();			
		}catch(IOException e) {
			
			source.close();			
			throw e;
		}
	}
	
	
	public ZipArchive(String zipFile) throws IOException {
		
		this(zipFile, false);
	}
	
	
	public ZipArchive(String zipFile, boolean readOnly) throws IOException {
		
		if(readOnly)
			source = new BinaryRandomAccess(new RandomAccessFile(zipFile, "r"));
		else
			source = new BinaryRandomAccess(new RandomAccessFile(zipFile, "rw"));
		
		this.readOnly = readOnly;
		
		try {
			
			init();			
		}catch(IOException e) {
			
			source.close();			
			throw e;
		}
	}
	
	
	private void init() throws IOException {
		
		if(source.length() == 0) {
			
			if(readOnly)
				return;
			
			writeEOCD();
			source.flush();
			return;
		}

		source.seek(source.length() - EOCD_SIZE);		
		readEOCD();
		
		if(readOnly)
			return;
		
		PriorityQueue<ArchiveEntry> assignOrder = new PriorityQueue<>((a, b) -> (int) (a.getHeaderPosition() - b.getHeaderPosition()));
		for(ArchiveEntry entry: entries.values())
			assignOrder.add(entry);

		long newCdPos = 0;
		long currentPosition = 0;
		PriorityQueue<EntryLayout> archiveLayout = new PriorityQueue<>();
		while(!assignOrder.isEmpty()) {

			ArchiveEntry entry = assignOrder.poll();

			if(storage.isEmpty()) {
				
				byte b = getSize(entry.getTotalSize());
				byte listSize = (byte) (b - MINIMUM_BLOCK_SIZE);
				while(storage.size() < listSize)
					storage.add(new StorageList());
				
				StorageList list = new StorageList();
				list.add(new StorageBlock(0, b));
				totalSize += 1 << b;
				list.add(new StorageBlock(totalSize, b));
				totalSize += 1 << b;
				storage.add(list);
			}
			
			if(entry.getHeaderPosition() >= currentPosition) {
				int positionMask = (1 << getSize(entry.getTotalSize())) - 1;
				if((entry.getHeaderPosition() & positionMask) == 0) {
					
					getBlock(entry.getTotalSize(), entry.getHeaderPosition());
					archiveLayout.add(new EntryLayout(entry, entry.getHeaderPosition()));
					long position = entry.getHeaderPosition() + entry.getTotalSize();
					currentPosition = position;
					if(position > newCdPos)
						newCdPos = position;
					
					continue;
				}
			}
			
			long position = getBlockAfter(entry.getTotalSize(), currentPosition);

			archiveLayout.add(new EntryLayout(entry, position));
			position += entry.getTotalSize();
			currentPosition = position;
			if(position > newCdPos)
				newCdPos = position;
		}
		
		boolean moveCD = newCdPos != cdPos;
		while(!archiveLayout.isEmpty()) {
			
			EntryLayout layout = archiveLayout.poll();
			layout.entry.moveTo(layout.position, source);
		}
		
		writeCD(newCdPos);
		if(moveCD)
			writeEOCD();
		
		source.flush();
	}
	
	
	private void increaseStorage(byte b) throws IOException {
		
		while(storage.size() <= b) {
			
			StorageList list = new StorageList();
			
			if(!entries.isEmpty() || storage.size() == b) {
				
				long size = 1 << (MINIMUM_BLOCK_SIZE + storage.size());
				list.add(new StorageBlock(totalSize, (byte) (storage.size() + MINIMUM_BLOCK_SIZE)));
				
				if(entries.isEmpty()) {

					list.add(new StorageBlock(totalSize + size, (byte) (storage.size() + MINIMUM_BLOCK_SIZE)));
					size += size;
				}
				
				totalSize += size;
			}
			
			storage.add(list);
		}
	}
	
	
	private long getBlock(long size) throws IOException {
		
		byte b = (byte) (getSize(size) - MINIMUM_BLOCK_SIZE);
		
		if(b >= storage.size())
			increaseStorage(b);
		
		StorageList list = storage.get(b);
		if(list.isEmpty())
			splitBlock(b);
		
		StorageBlock block = list.remove();		
		return block.position;
	}
	
	
	private void getBlock(long size, long position) throws IOException {
		
		byte b = (byte) (getSize(size) - MINIMUM_BLOCK_SIZE);
		
		if(b >= storage.size())
			increaseStorage(b);
		
		StorageList list = storage.get(b);
		if(list.remove(position) != null)
			return;
		
		splitBlock(b, position);		
		list.remove(position);
	}
	
	
	private long getBlockAfter(long size, long after) throws IOException {
		
		byte b = (byte) (getSize(size) - MINIMUM_BLOCK_SIZE);
		
		if(b >= storage.size())
			increaseStorage(b);
		
		StorageList list = storage.get(b);
		StorageBlock block = list.removeAfter(after);
		if(block == null)
			splitBlockAfter(b, after);
		else
			return block.position;
		
		return list.removeAfter(after).position;
	}
	
	
	private void freeBlock(long position, long size) {
				
		byte b = getSize(size);		
		StorageBlock block = new StorageBlock(position, b);
		
		b -= MINIMUM_BLOCK_SIZE;		
		while(b >= storage.size())
			storage.add(new StorageList());
		
		while(true) {
			
			StorageList list = storage.get(b);
			StorageBlock buddy = list.getBuddy(block);
			
			if(buddy == null) {
				
				list.add(block);
				return;
			}
			
			if(buddy.position < block.position)
				block = buddy;
			
			++block.size;
			++b;
			
			if(b >= storage.size())
				storage.add(new StorageList());
		}
	}
	
	
	private void splitBlock(byte targetSize) throws IOException {
		
		byte i = (byte) (targetSize + 1);
				
		while(i < storage.size() && storage.get(i).isEmpty())
			++i;
		
		if(i >= storage.size()) 
			increaseStorage(i);
		
		while(i > targetSize) {

			StorageBlock block = storage.get(i--).remove();
			storage.get(i).add(block.split());
			storage.get(i).add(block);
		}
	}
	
	
	private void splitBlock(byte targetSize, long position) throws IOException {
		
		byte i = (byte) (targetSize + 1);

		byte bit = (byte) (i + MINIMUM_BLOCK_SIZE);
		while(i < storage.size() && !storage.get(i).hasBlock(position & ~((1 << bit) - 1))) {
			++i;
			++bit;
		}
		
		if(i == storage.size())
			increaseStorage(i);
		
		while(i > targetSize) {

			StorageBlock block = storage.get(i--).remove(position & ~((1 << bit) - 1));
			storage.get(i).add(block.split());
			storage.get(i).add(block);
			bit--;
		}
	}
	
	
	private void splitBlockAfter(byte targetSize, long after) throws IOException {
		
		byte i = (byte) (targetSize + 1);
				
		while(i < storage.size() && !storage.get(i).hasAfter(after))
			++i;
		
		if(i >= storage.size()) 
			increaseStorage(i);
		
		while(i > targetSize) {

			StorageBlock block = storage.get(i--).removeAfter(after);
			storage.get(i).add(block.split());
			storage.get(i).add(block);
		}
	}
	
	
	public int entryCount() {
		
		return entries.size();
	}
	
	
	public boolean containsEntry(String name) {
		
		return entries.containsKey(name);
	}
	
	
	public ArchiveEntry getEntry(String name) {
		
		return entries.get(name);
	}
	
	
	public byte[] extract(ArchiveEntry entry) throws IOException {
		
		return entry.extract(source);
	}
	
	
	public byte[] getCompressedData(ArchiveEntry entry) throws IOException {
		
		return entry.getCompressedData(source);
	}
	
	
	public void remove(String name) throws IOException {
		
		if(readOnly)
			throw new UnsupportedOperationException("Cannot modify zip entry in read only mode!");
		
		remove(entries.get(name));
	}
	
	
	public void remove(ArchiveEntry entry) throws IOException {
		
		if(readOnly)
			throw new UnsupportedOperationException("Cannot modify zip entry in read only mode!");

		if(!entries.remove(entry.name, entry))
			throw new IOException("Entry \"" + entry.name + "\" does not exist in this archive");
		
		freeBlock(entry.getHeaderPosition(), entry.getTotalSize());
		long newCDPos = removeFromCD(entry);
		
		writeCD(newCDPos);
		writeEOCD();
		
		source.flush();
	}
	
	
	public void removeAll(Collection<ArchiveEntry> entries) throws IOException {
		
		if(readOnly)
			throw new UnsupportedOperationException("Cannot modify zip entry in read only mode!");
		
		long finalPosition = cdPos;
		
		for(ArchiveEntry entry: entries) {
	
			if(!this.entries.remove(entry.name, entry))
				throw new IOException("Entry does not exist in this archive");
			
			freeBlock(entry.getHeaderPosition(), entry.getTotalSize());
			long newCDPos = removeFromCD(entry);
			if(newCDPos < finalPosition)
				finalPosition = newCDPos;
		}
	
		writeCD(finalPosition);
		writeEOCD();
		
		source.flush();
	}
	
	
	private long removeFromCD(ArchiveEntry entry) throws IOException {
		
		long newCDPos = 0;
		long directoryOffset = entry.getDirectoryOffset();
		long directorySize = 46 + entry.name.length();
		
		for(ArchiveEntry e: entries.values()) {
			
			if(e.getDirectoryOffset() > directoryOffset)
				e.moveDirectoryOffset(-directorySize);
			
			long endOfEntry = e.getHeaderPosition() + e.getTotalSize();
			if(endOfEntry > newCDPos)
				newCDPos = endOfEntry;
		}
		
			
		eocdPos -= directorySize;
		return newCDPos;
	}
	
	
	public ArchiveEntry add(String entryName, File data) throws IOException {
		
		if(readOnly)
			throw new UnsupportedOperationException("Cannot modify zip entry in read only mode!");
		
		return add(new EntrySource(entryName, data));
	}
	
	
	public ArchiveEntry add(String entryName, byte[] data) throws IOException {
		
		if(readOnly)
			throw new UnsupportedOperationException("Cannot modify zip entry in read only mode!");
		
		return add(new EntrySource(entryName, data));
	}
	
	
	private ArchiveEntry createEntry(EntrySource entrySource) throws IOException{
		
		int uncompressedSize = entrySource.uncompressedSize;
		CompressionInfo compress = entrySource.compress();
		
		source.seek(getBlock(compress.compressedSize + 30 + entrySource.entryName.length()));
		
		ArchiveEntry entry = new ArchiveEntry(entrySource.entryName, compress.compressedData, entrySource.lastModifiedTime, entrySource.lastModifiedDate, compress.compressionType, eocdPos - cdPos, compress.crc32, compress.compressedSize, uncompressedSize, source);
		entries.put(entry.name, entry);
		return entry;
	}
	
	
	private void writeCD(long position) throws IOException {
		
		long dif = position - cdPos;
		if(dif != 0) {
			
			cdPos += dif;
			eocdPos += dif;
		}
		
		for(ArchiveEntry entry: entries.values())
			entry.writeDirectoryListing(cdPos, source);
	}
	
	
	public ArchiveEntry add(EntrySource entrySource) throws IOException {
		
		if(readOnly)
			throw new UnsupportedOperationException("Cannot modify zip entry in read only mode!");
		
		ArchiveEntry entry = createEntry(entrySource);

		long endOfEntry = entry.getHeaderPosition() + entry.getTotalSize();
		if(cdPos < endOfEntry)
			writeCD(endOfEntry);
		else
			entry.writeDirectoryListing(cdPos, source);
		
		eocdPos += 46 + entry.name.length();
		writeEOCD();		
		source.flush();	
		
		return entry;
	}


	public void addAll(Collection<EntrySource> entrySources) throws IOException {
		
		if(readOnly)
			throw new UnsupportedOperationException("Cannot modify zip entry in read only mode!");
		
		if(entrySources.isEmpty())
			return;
		
		int max = entrySources.size();
		ZipPrinter.setMaxFiles(max); // [RCS] Update the zip progress printer data.
		ZipPrinter.updateSubject(0); // [RCS] Update the zip progress printer data.
		
		PriorityQueue<EntrySource> queue;
		if(entrySources instanceof PriorityQueue)
			queue = (PriorityQueue<EntrySource>) entrySources;
		else {

			queue = new PriorityQueue<EntrySource>(entrySources.size());
			queue.addAll(entrySources);
		}
		
		long endOfEntries = cdPos;
		while(!queue.isEmpty()) {
			ArchiveEntry entry = createEntry(queue.poll());
			eocdPos += 46 + entry.name.length();
			
			ZipPrinter.updateSubject(max - queue.size()); // [RCS] Update the zip progress printer data.
			ZipPrinter.setFileName(entry.name);
			
			long endOfEntry = entry.getHeaderPosition() + entry.getTotalSize();
			if(endOfEntries < endOfEntry)
				endOfEntries = endOfEntry;
		}
		
		writeCD(endOfEntries);
		writeEOCD();
		source.flush();
	}
	
	
	private void readEOCD() throws IOException {
		
		short entries;
		int sizeOfCD;
		
		while(true) {
			
			eocdPos = source.getFilePointer();
			int signature;
			while((signature = source.readInt()) != EOCD_SIGNATURE) {
				
				if(source.getFilePointer() >= 5 && (signature << 8) == (EOCD_SIGNATURE << 8))
					source.seek(source.getFilePointer() - 5);
				else if(source.getFilePointer() >= 6 && (signature << 16) == (EOCD_SIGNATURE << 16))
					source.seek(source.getFilePointer() - 6);
				else if(source.getFilePointer() >= 7 && (signature << 24) == (EOCD_SIGNATURE << 24))
					source.seek(source.getFilePointer() - 7);
				else if(source.getFilePointer() >= 8)
					source.seek(source.getFilePointer() - 8);
				else
					throw new IOException("Invalid zip file");
			}
			
			short diskNum = source.readShort();
			short directoryDiskNum = source.readShort();
			short recordsOnDisk = source.readShort();
			entries = source.readShort();
			
			if(diskNum != directoryDiskNum || recordsOnDisk != entries)
				throw new IOException("Unsupported zip archive"); //We don't support zips split into multiple files
			
			sizeOfCD = source.readInt();
			cdPos = source.readInt();
			short commentLength = source.readShort();
			if(commentLength == source.length() - source.getFilePointer())
				break;
			else if(source.getFilePointer() <= EOCD_SIZE)
				throw new IOException("Invalid zip file");
			else
				source.seek(source.getFilePointer() - EOCD_SIZE - 1);
		}
		
		readCD(entries&65535, sizeOfCD);
	}
	
	
	private void readCD(int entries, int sizeOfCD) throws IOException{
		
		source.seek(cdPos);
		BinaryReader reader = new BinaryReader(source.readByteArray(sizeOfCD));
		reader.close(); //Doesn't do anything anyways and warnings are annoying
		
		long directoryOffset = 0;
		for(int i = 0; i < entries; ++i) {
			
			if(reader.readInt() != CD_SIGNATURE)
				throw new IOException("Corrupt zip archive");
			
			reader.skip(6); //Skip version info & Flags
			short compressionType = reader.readShort();
			short lastModifiedTime = reader.readShort();
			short lastModifiedDate = reader.readShort();
			
			int crc32 = reader.readInt();
			int compressedSize = reader.readInt();
			int uncompressedSize = reader.readInt();
			
			short fileNameLength = reader.readShort();
			short fileExtraLength = reader.readShort();
			short fileCommentLength = reader.readShort();
			
			reader.skip(8); //Disk number & File attributes
						
			int position = reader.readInt();
			String name = reader.readString(fileNameLength);
			reader.skip(fileExtraLength + fileCommentLength);
			
			if(this.entries.containsKey(name))
				continue;
			
			ArchiveEntry entry = new ArchiveEntry(compressionType, lastModifiedTime, lastModifiedDate, crc32, compressedSize, uncompressedSize, name, position, directoryOffset);
			this.entries.put(entry.name, entry);
			directoryOffset += 46 + fileNameLength + fileExtraLength + fileCommentLength;
		}
		
		eocdPos = cdPos + directoryOffset;
		if(reader.hasMoreData())
			throw new IOException("Corrupt zip archive");
	}
	
	
	private void writeEOCD() throws IOException {
		
		source.seek(eocdPos);
		source.writeInt(EOCD_SIGNATURE);
		source.writeShort((short) 0);
		source.writeShort((short) 0);
		source.writeShort((short) entries.size());
		source.writeShort((short) entries.size());
		source.writeInt((int) (eocdPos - cdPos));
		source.writeInt((int) cdPos);
		source.writeShort((short) 0);
		
		source.setLength(source.getFilePointer());
	}


	@Override
	public Iterator<ArchiveEntry> iterator() {

		return new ImmutableIterator<ArchiveEntry>(entries.values().iterator());
	}
	
	
	private static byte getSize(long totalCompressedSize2) {
		
		byte b = MINIMUM_BLOCK_SIZE;
		while((1 << b) < totalCompressedSize2)
			++b;
		
		return b;
	}
	
	
	private static class StorageList{
		
		private StorageBlock head;
		
		
		private void add(StorageBlock block) {
			
			if(head == null)
				head = block;
			else if(block.position <= head.position) {
				
				block.next = head;
				head = block;
			}
			else {
				
				StorageBlock temp = head;
				while(temp.next != null && block.position > temp.next.position)
					temp = temp.next;
				
				block.next = temp.next;
				temp.next = block;
			}
		}
		
		
		private boolean isEmpty() {
			
			return head == null;
		}
		
		
		private StorageBlock remove() {
			
			StorageBlock temp = head;
			head = head.next;
			temp.next = null;
			
			return temp;
		}
		
		
		private StorageBlock remove(long position) {
			
			if(head == null)
				return null;
			
			StorageBlock temp = head;
			if(temp.position == position) {
				
				head = temp.next;
				temp.next = null;
				return temp;
			}
			
			while(temp.next != null && temp.next.position < position)
				temp = temp.next;
			
			if(temp.next == null || temp.next.position != position)
				return null;
			
			StorageBlock toReturn = temp.next;
			temp.next = toReturn.next;
			toReturn.next = null;
			return toReturn;
		}
		
		
		private StorageBlock removeAfter(long after) {
			
			if(head == null)
				return null;
			
			StorageBlock temp = head;
			if(temp.position >= after) {
				
				head = temp.next;
				temp.next = null;
				return temp;
			}
			
			while(temp.next != null && temp.next.position < after)
				temp = temp.next;
			
			if(temp.next == null)
				return null;
			
			StorageBlock toReturn = temp.next;
			temp.next = toReturn.next;
			toReturn.next = null;
			return toReturn;
		}
		
		
		private boolean hasAfter(long after) {
			
			if(head == null)
				return false;
			
			StorageBlock temp = head;
			if(temp.position > after)
				return true;
			
			while(temp.next != null && temp.next.position < after)
				temp = temp.next;
			
			return temp.next != null;
		}
		
		
		private boolean hasBlock(long position) {
			
			if(head == null)
				return false;
			
			StorageBlock temp = head;
			if(temp.position == position)
				return true;
			
			while(temp.next != null && temp.next.position < position)
				temp = temp.next;
			
			return temp.next != null && temp.next.position == position;
		}
		
		
		private StorageBlock getBuddy(StorageBlock block) {

			if(head == null)
				return null;
			
			long buddyPos = block.position ^ (1 << block.size);
			
			StorageBlock temp = head;
			if(head.position == buddyPos) {
				
				head = temp.next;
				temp.next = null;
				return temp;
			}
			else {
				
				while(temp.next != null && temp.next.position < buddyPos)
					temp = temp.next;
				
				if(temp.next == null)
					return null;
				
				if(temp.next.position == buddyPos) {
					
					StorageBlock buddy = temp.next;
					temp.next = buddy.next;
					buddy.next = null;
					return buddy;
				}
				
				return null;
			}
		}
	}
	
	
	private static class EntryLayout implements Comparable<EntryLayout>{
		
		private final ArchiveEntry entry;
		private final long position;
		
		
		private EntryLayout(ArchiveEntry entry, long position) {
			
			this.entry = entry;
			this.position = position;
		}

		
		@Override
		public int compareTo(EntryLayout o) {
			
			return (int) (o.position - position);
		}
	}
	
	
	private static class StorageBlock{

		private long position;
		private byte size;
		private StorageBlock next;
		
		
		private StorageBlock(long position, byte size) {

			this.position = position;
			this.size = size;
		}
		
		
		private StorageBlock split() {
			
			--size;
			return new StorageBlock(position | (1<<size), size);
		}
	}


	@Override
	public void close() throws IOException {
		
		source.close();
	}
}
