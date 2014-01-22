import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.junit.Rule;
import org.junit.Test;


public class RecompressTests {
	
	@Test
	public void testParseMCA() throws IOException {
		RandomAccessFile testFile = new RandomAccessFile("./assets/test.mca", "r");
    	FileChannel inChan = testFile.getChannel();
    	ByteBuffer buf = ByteBuffer.allocate((int) testFile.length());
    	inChan.read(buf);
    	buf.flip();
    	RegionFile region = RegionFile.parse(buf.array());
    	int chunkCount = 0;
    	for (int x = 0; x < 32; x++) {
    		for (int z = 0; z < 32; z++) {
    			if (region.getChunk(x, z) != null) {
    				chunkCount++;
    			}
    		}
    	}
    	assertEquals(349, chunkCount);
	}

}
