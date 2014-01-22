import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.management.RuntimeErrorException;

import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;


public class ChunkData {
	private int timestamp;
	private CompoundTag chunkRootTag;
	
	public ChunkData(int timestamp, Tag chunkRootTag) {
		this.setTimestamp(timestamp);
		this.setChunkRootTag((CompoundTag) chunkRootTag);
	}

	public static ChunkData parse(byte[] data, int location, int timestamp) {
		FixedNBTInputStream nbtInput;
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
			
			nbtInput = new FixedNBTInputStream(chunkReader);
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
}
