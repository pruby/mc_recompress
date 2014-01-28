import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jnbt.ByteArrayTag;
import org.jnbt.CompoundTag;
import org.jnbt.ListTag;
import org.jnbt.NBTOutputStream;
import org.jnbt.StringTag;
import org.jnbt.Tag;


public class RegionFile {
	private ChunkData[] chunks;
	
	public RegionFile() {
		chunks = new ChunkData[1024];
	}
	
	public void setChunk(int index, ChunkData chunk) {
		chunks[index] = chunk;
	}
	
	public ChunkData getChunk(int rx, int rz) {
		int offset = rz * 32 + rx;
		return chunks[offset];
	}
	
	public static RegionFile parse(byte[] data) {
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
	
	private static final String[] combineKeys = {"Blocks", "Add", "Data", "BlockLight", "SkyLight"};
	private static final Integer[] blockLengths = {4096, 2048, 2048, 2048, 2048};
	
	public Tag makeArchive() {
		Map<String, Tag> rootNodes = new HashMap<String, Tag>();
		rootNodes.put("MRI Version", new StringTag("MRI Version", "1.0"));

		// Output streams
		Map<String, OutputStream> combinedStreams = new HashMap<String, OutputStream>();
		Map<String, Integer> combineBlockLengths = new HashMap<String, Integer>();
		
		for (int i = 0; i < combineKeys.length; i++) {
			String key = combineKeys[i];
			Integer blockLength = blockLengths[i];
			
			combinedStreams.put(key, new ByteArrayOutputStream());
			combineBlockLengths.put(key, blockLength);
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
		FileOutputStream fileOut = new FileOutputStream(file);
		NBTOutputStream nbtOut = new NBTOutputStream(fileOut);
		nbtOut.writeTag(makeArchive());
		nbtOut.close();
	}
}
