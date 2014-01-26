import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.management.RuntimeErrorException;

import org.jnbt.CompoundTag;
import org.jnbt.ByteTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;


public class ChunkData {
	private int timestamp;
	private CompoundTag chunkRootTag;
	private Map<Integer, SectionData> sections;
	
	public ChunkData(int timestamp, Tag chunkRootTag) {
		this.setTimestamp(timestamp);
		this.setChunkRootTag((CompoundTag) chunkRootTag);
		extractSections();
	}
	
	private void extractSections() {
		this.sections = new HashMap<Integer, SectionData>();
		try {
			CompoundTag level = (CompoundTag) chunkRootTag.getValue().get("Level");
			ListTag sections = (ListTag) level.getValue().get("Sections");
			for (Tag entry : sections.getValue()) {
				CompoundTag section = (CompoundTag) entry;
				byte y = ((ByteTag) section.getValue().get("Y")).getValue();
				this.sections.put((int) y, new SectionData(section));
			}
		} catch (Exception e) {
			
		}
	}

	public static ChunkData parse(byte[] data, int location, int timestamp) {
		NBTInputStream nbtInput;
		ChunkData chunk = null;
		
		try {
			int offset_sectors = location * 4096;
			byte[] header = Arrays.copyOfRange(data, offset_sectors, offset_sectors + 5);
			ByteBuffer headerStream = ByteBuffer.wrap(header);
			headerStream.order(ByteOrder.BIG_ENDIAN);
			
			int length = headerStream.getInt();
			byte compression = headerStream.get();
			
			byte[] chunkData = Arrays.copyOfRange(data, offset_sectors + 5, offset_sectors + 5 + length);
			
			InputStream chunkReader = new ByteArrayInputStream(chunkData);
			if (compression == 1) {
				// Gzip compression
				chunkReader = new BufferedInputStream(new GZIPInputStream(chunkReader));
			} else if (compression == 2) {
				// Deflate compression
				chunkReader = new InflaterInputStream(chunkReader, new Inflater());
			}
			
			nbtInput = new NBTInputStream(chunkReader, false);
			chunk = new ChunkData(timestamp, nbtInput.readTag());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return chunk;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public CompoundTag getChunkRootTag() {
		return chunkRootTag;
	}

	public void setChunkRootTag(CompoundTag chunkRootTag) {
		this.chunkRootTag = chunkRootTag;
	}
	
	public int getX() {
		CompoundTag level = (CompoundTag) chunkRootTag.getValue().get("Level");
		IntTag xPos = (IntTag) level.getValue().get("xPos");
		return xPos.getValue();
	}
	
	public int getZ() {
		CompoundTag level = (CompoundTag) chunkRootTag.getValue().get("Level");
		IntTag zPos = (IntTag) level.getValue().get("zPos");
		return zPos.getValue();
	}
	
	public SectionData getSection(int y) {
		return this.sections.get(y);
	}
	
	public void extractYLayerData(int y, Map<String, Integer> blockSizes, Map<String, OutputStream> combinedStreams) throws IOException {
		SectionData section = getSection(y);
		if (section != null) {
			section.extractBlockData(blockSizes, combinedStreams);
		}
	}
	
	public void regenerateYLayerData(int y, Map<String, Integer> blockSizes, Map<String, InputStream> combinedStreams) throws IOException {
		SectionData section = getSection(y);
		if (section != null) {
			section.regenerateBlockData(blockSizes, combinedStreams);
		}
	}
	
	public Tag generateChunkTag() {
		CompoundTag levelTag = (CompoundTag) chunkRootTag.getValue().get("Level");
		Map<String, Tag> chunkFields = new HashMap<String, Tag>(levelTag.getValue());
		Tag origSectionTag = chunkFields.remove("Sections");
		
		List<Tag> sections = new ArrayList<Tag>();
		
		for (int i = 0; i < 16; ++i) {
			SectionData section = getSection(i);
			if (section != null) {
				sections.add(new CompoundTag(section.getTagName(), section.getResidualFields()));
			}
		}
		
		chunkFields.put("Sections", new ListTag(origSectionTag.getName(), CompoundTag.class, sections));
		
		return new CompoundTag(chunkRootTag.getName(), chunkFields);
	}
}
