import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.ByteArrayTag;
import org.jnbt.Tag;


public class SectionData {
	private CompoundTag sectionTag;
	private Map<String, Tag> residualFields;

	public SectionData(CompoundTag sectionTag) {
		this.sectionTag = sectionTag;
		resetResidualFields();
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
		for (String key : new ArrayList<String>(residualFields.keySet())) {
			if (combinedOutputs.containsKey(key)) {
				ByteArrayTag value = (ByteArrayTag) residualFields.get(key);
				if (blockSizes.get(key).equals(value.getValue().length)) {
					combinedOutputs.get(key).write(value.getValue());
					residualFields.remove(key);
					residualFields.put(key, new ByteArrayTag(key, new byte[0]));
				}
			}
		}
	}
	
	public void resetResidualFields() {
		this.residualFields = new HashMap<String, Tag>(sectionTag.getValue());
	}
	
	public Map<String, Tag> getResidualFields() {
		return this.residualFields;
	}

	public String getTagName() {
		return sectionTag.getName();
	}

	public void regenerateBlockData(Map<String, Integer> blockSizes,
			Map<String, InputStream> combinedStreams) throws IOException {
		for (String key : new ArrayList<String>(combinedStreams.keySet())) {
			if (residualFields.containsKey(key) && ((ByteArrayTag) residualFields.get(key)).getValue().length == 0) {
				byte[] sectionData = new byte[blockSizes.get(key)];
				combinedStreams.get(key).read(sectionData);
				residualFields.put(key, new ByteArrayTag(key, sectionData));
			}
		}
	}
}
