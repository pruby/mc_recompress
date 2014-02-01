package nz.net.goddard.mcrecompress;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.itadaki.bzip2.BZip2InputStream;
import org.itadaki.bzip2.BZip2OutputStream;
import org.jnbt.ByteArrayTag;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTOutputStream;
import org.jnbt.StringTag;
import org.jnbt.Tag;


public class RegionFile {
	private static final String[] combineKeys = {"Blocks", "Add", "Data", "BlockLight", "SkyLight"};
	private static final Integer[] blockLengths = {4096, 2048, 2048, 2048, 2048};
	public static Map<String, Integer> combineBlockLengths;
	static {
		combineBlockLengths = new HashMap<String, Integer>();
		for (int i = 0; i < combineKeys.length; i++) {
			String key = combineKeys[i];
			Integer blockLength = blockLengths[i];
			
			combineBlockLengths.put(key, blockLength);
		}
		combineBlockLengths = Collections.unmodifiableMap(combineBlockLengths);
	}
	
	private ChunkData[] chunks;
	
	private RegionFile() {
		chunks = new ChunkData[1024];
	}
	
	public void setChunk(int index, ChunkData chunk) {
		chunks[index] = chunk;
	}
	
	public ChunkData getChunk(int rx, int rz) {
		int offset = rz * 32 + rx;
		return chunks[offset];
	}
	
	public static RegionFile parse(byte[] data) throws IOException {
		RegionFile region = new RegionFile();
		
		byte[] locationData = Arrays.copyOfRange(data, 0, 4096);
		byte[] timestampData = Arrays.copyOfRange(data, 4096, 4096 + 4096);
		
		ByteBuffer locations = ByteBuffer.wrap(locationData);
		locations.order(ByteOrder.BIG_ENDIAN);
		ByteBuffer timestamps = ByteBuffer.wrap(timestampData);
		timestamps.order(ByteOrder.BIG_ENDIAN);
		
		for (int i = 0; i < 1024; i++) {
			int location = (locations.getInt() >> 8);
			int timestamp = timestamps.getInt();
			if (location != 0) {
				region.setChunk(i, ChunkData.parse(data, location, timestamp));
			}
		}
		
		return region;
	}
	
	public static RegionFile fromArchive(Tag rootTag) {
		assert(rootTag instanceof CompoundTag);
		
		CompoundTag root = (CompoundTag) rootTag;
		Tag versionTag = root.getValue().get("MRI Version");
		assert(versionTag != null);
		assert(versionTag instanceof StringTag);
		assert(((StringTag) versionTag).getValue().equals("1.0"));
		
		RegionFile region = new RegionFile();
		
		ListTag chunkList = (ListTag) root.getValue().get("Chunks");
		for (Tag chunkTag : chunkList.getValue()) {
			CompoundTag chunkRoot = (CompoundTag) chunkTag;
			
			// Remove timestamp field, pass separately
			Map<String, Tag> chunkFields = new HashMap<String, Tag>(chunkRoot.getValue());
			IntTag timestamp = (IntTag) chunkFields.remove("Last Modified");
			chunkRoot = new CompoundTag(chunkRoot.getName(), chunkFields);
			
			ChunkData chunk;
			if (timestamp == null) {
				chunk = new ChunkData(0, chunkTag);
			} else {
				chunk = new ChunkData(timestamp.getValue(), chunkTag);
			}
			
			region.setChunk(chunk.getZ() * 32 + chunk.getX(), chunk);
		}
		
		CompoundTag blockData = (CompoundTag) root.getValue().get("BlockData");
		Map<String, Tag> combinedValues = blockData.getValue();
		Map<String, InputStream> restoreInputs = new HashMap<String, InputStream>();
		for (String key : combineKeys) {
			if (combinedValues.containsKey(key)) {
				restoreInputs.put(key, new ByteArrayInputStream(((ByteArrayTag) combinedValues.get(key)).getValue()));
			}
		}
		
		region.restoreBlockData(restoreInputs);
		
		return region;
	}
	
	private void restoreBlockData(Map<String, InputStream> restoreInputs) {
		// Restore grouped data
		for (int y = 0; y < 16; y++) {
			for (int i = 0; i < 1024; ++i) {
				if (chunks[i] != null) {
					try {
						chunks[i].regenerateYLayerData(y, combineBlockLengths, restoreInputs);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public Tag makeArchive() {
		Map<String, Tag> rootNodes = new HashMap<String, Tag>();
		rootNodes.put("MRI Version", new StringTag("MRI Version", "1.0"));

		// Output streams
		Map<String, OutputStream> combinedStreams = new HashMap<String, OutputStream>();
		
		for (int i = 0; i < combineKeys.length; i++) {
			String key = combineKeys[i];
			Integer blockLength = blockLengths[i];
			
			combinedStreams.put(key, new ByteArrayOutputStream());
		}
		
		// Extract grouped data
		for (int y = 0; y < 16; y++) {
			for (int i = 0; i < 1024; ++i) {
				if (chunks[i] != null) {
					try {
						chunks[i].extractYLayerData(y, combineBlockLengths, combinedStreams);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
		// Output chunk tags
		List<Tag> chunkTags = new ArrayList<Tag>();
		for (int i = 0; i < 1024; ++i) {
			if (chunks[i] != null) {
				chunkTags.add(chunks[i].generateChunkTag());
			}
		}
		Tag chunkList = new ListTag("Chunks", CompoundTag.class, chunkTags);
		rootNodes.put("Chunks", chunkList);
		
		// Add block data
		Map<String, Tag> blockDataNodes = new HashMap<String, Tag>();
		blockDataNodes.put("Order", new StringTag("Order", "Section YZX"));
		for (String key : combineKeys) {
			ByteArrayOutputStream stream = (ByteArrayOutputStream) combinedStreams.get(key);
			blockDataNodes.put(key, new ByteArrayTag(key, stream.toByteArray()));
		}
		rootNodes.put("BlockData", new CompoundTag("BlockData", blockDataNodes));
		
		// Root tag
		CompoundTag root = new CompoundTag("Region", rootNodes);
		return root;
	}
	
	public void writeArchive(File file) throws IOException {
		writeArchive(file, deduceCompression(file));
	}
	
	private static ArchiveCompression deduceCompression(File file) {
		if (file.toString().endsWith(".bz2")) {
			return ArchiveCompression.BZIP2;
		} else if (file.toString().endsWith(".gz")) {
			return ArchiveCompression.GZIP;
		} else {
			return ArchiveCompression.NONE;
		}
	}

	public void writeArchive(File file, ArchiveCompression compression) throws IOException {
		OutputStream fileOut = new FileOutputStream(file);
		if (compression == ArchiveCompression.BZIP2) {
			fileOut = new BZip2OutputStream(fileOut);
		}
		NBTOutputStream nbtOut = new NBTOutputStream(fileOut, compression == ArchiveCompression.GZIP);
		nbtOut.writeTag(makeArchive());
		nbtOut.close();
	}
	
	public static RegionFile readArchive(File file) throws IOException {
		return readArchive(file, deduceCompression(file));
	}
		
	public static RegionFile readArchive(File file, ArchiveCompression compression) throws IOException {
    	byte[] fileData = Files.readAllBytes(file.toPath());
    	NBTInputStream reparser;
    	if (compression == ArchiveCompression.BZIP2) {
    		reparser = new NBTInputStream(new BZip2InputStream(new ByteArrayInputStream(fileData), false), false);
    	} else {
    		reparser = new NBTInputStream(new ByteArrayInputStream(fileData), compression == ArchiveCompression.GZIP);
    	}
    	Tag root = reparser.readTag();
    	RegionFile region = RegionFile.fromArchive(root);
    	return region;
	}
	
	public static RegionFile readMCA(File file) throws IOException {
    	byte[] fileData = Files.readAllBytes(file.toPath());
    	return RegionFile.parse(fileData);
	}

	public void writeRegionFile(File file) throws IOException {
		byte[][] chunkDataBlocks = new byte[1024][];
		int[] chunkOffsets = new int[1024];
		int[] chunkPositionHeaders = new int[1024];
		
		for (int i = 0; i < 1024; ++i) {
			if (chunks[i] != null) {
				try {
					chunkDataBlocks[i] = chunks[i].generateChunkBlock();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		int position = 2;
		for (int i = 0; i < 1024; ++i) {
			
			if (chunkDataBlocks[i] != null) {
				int size = chunkDataBlocks[i].length;
				int chunks = size / 4096;
				chunkOffsets[i] = position * 4096;
				chunkPositionHeaders[i] = (position << 8) + (chunks & 255);
				position += chunks;
			} else {
				chunkOffsets[i] = 0;
			}
		}
		int finalLength = position * 4096;
		
		FileOutputStream fileOut = new FileOutputStream(file);
		DataOutputStream out = new DataOutputStream(fileOut);
		
		// Chunk positions
		for (int i = 0; i < 1024; ++i) {
			out.writeInt(chunkPositionHeaders[i]);
		}
		
		// Timestamps
		for (int i = 0; i < 1024; ++i) {
			if (chunks[i] != null) {
				out.writeInt(chunks[i].getTimestamp());
			} else {
				out.writeInt(0);
			}
		}
		
		int outAmount = 1024 * 8;
		// Chunk data
		for (int i = 0; i < 1024; ++i) {
			if (chunkDataBlocks[i] != null) {
				assert(outAmount == chunkOffsets[i]);
				out.write(chunkDataBlocks[i]);
				outAmount += chunkDataBlocks[i].length;
			}
		}
		
		assert(outAmount == finalLength);
		
		out.close();
	}
}
