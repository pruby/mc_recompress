package nz.net.goddard.mcrecompress;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jnbt.ByteArrayTag;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;


public class SectionData {
	private CompoundTag sectionTag;

	public SectionData(CompoundTag sectionTag) {
		this.sectionTag = sectionTag;
	}
	
	public int getY() {
		try {
			int y1 = ((IntTag) sectionTag.getValue().get("Y")).getValue();
			return y1;
		} catch (RuntimeException e) {
			return -1;
		}
	}
	
	public byte[] getBlocks() {
		return getByteBlock("Blocks");
	}
	
	public byte[] getBlockAdd() {
		byte[] bd = getByteBlock("Add");
		if (bd == null) {
			bd = new byte[2048];
		}
		return bd;
	}
	
	public byte[] getData() {
		return getByteBlock("Data");
	}
	
	public byte[] getBlockLight() {
		return getByteBlock("BlockLight");
	}
	
	public byte[] getSkyLight() {
		return getByteBlock("SkyLight");
	}
	
	protected byte[] getByteBlock(String tagName) {
		try {
			byte[] y1 = ((ByteArrayTag) sectionTag.getValue().get(tagName)).getValue();
			return y1;
		} catch (RuntimeException e) {
			return null;
		}
	}
		public CompoundTag getSectionTag() {
		return sectionTag;
	}

	public void setSectionTag(CompoundTag sectionTag) {
		this.sectionTag = sectionTag;
	}
	
	public void extractBlockData(Map<String, Integer> blockSizes, Map<String, OutputStream> combinedOutputs) throws IOException {
		Map<String, Tag> residualFields = new HashMap<String, Tag>(sectionTag.getValue());
		for (String key : new ArrayList<String>(sectionTag.getValue().keySet())) {
			if (combinedOutputs.containsKey(key) && residualFields.containsKey(key)) {
				ByteArrayTag value = (ByteArrayTag) residualFields.get(key);
				if (blockSizes.get(key).equals(value.getValue().length)) {
					combinedOutputs.get(key).write(value.getValue());
					residualFields.remove(key);
					residualFields.put(key + "Storage", new StringTag(key + "Storage", "BlockData"));
				}
			}
		}
		sectionTag = new CompoundTag(sectionTag.getName(), residualFields);
	}
	
	public Map<String, Tag> getTagFields() {
		return sectionTag.getValue();
	}

	public String getTagName() {
		return sectionTag.getName();
	}

	public void regenerateBlockData(Map<String, Integer> blockSizes,
			Map<String, InputStream> combinedStreams) throws IOException {
		Map<String, Tag> residualFields = new HashMap<String, Tag>(sectionTag.getValue());
		for (String key : new ArrayList<String>(combinedStreams.keySet())) {
			if (!residualFields.containsKey(key) && residualFields.containsKey(key + "Storage") && residualFields.get(key + "Storage").getValue().equals("BlockData")) {
				byte[] sectionData = new byte[blockSizes.get(key)];
				combinedStreams.get(key).read(sectionData);
				residualFields.remove(key + "Storage");
				residualFields.put(key, new ByteArrayTag(key, sectionData));
			}
		}
		sectionTag = new CompoundTag(sectionTag.getName(), residualFields);
	}
}
