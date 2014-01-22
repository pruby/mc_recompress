import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;


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
}
