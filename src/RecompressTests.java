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
    	// Ruby code used to check - 349 chunks in the test.mca region file
    	assertEquals(349, chunkCount);
	}
	
	@Test
	public void testReadChunks() throws IOException {
		RandomAccessFile testFile = new RandomAccessFile("./assets/test.mca", "r");
    	FileChannel inChan = testFile.getChannel();
    	ByteBuffer buf = ByteBuffer.allocate((int) testFile.length());
    	inChan.read(buf);
    	buf.flip();
    	RegionFile region = RegionFile.parse(buf.array());
    	
    	ChunkData testChunk = region.getChunk(16, 27); 
    	assertNotNull(testChunk);
    	SectionData bedrock = testChunk.getSection(0);
    	assertNotNull(bedrock);
    	byte[] blocks = bedrock.getBlocks();
    	assertNotNull(blocks);
    	
    	// Check for bedrock ID
    	assertEquals(7, blocks[0]);
	}
	
	@Test
	public void testWriteArchive() throws IOException {
		RandomAccessFile testFile = new RandomAccessFile("./assets/test.mca", "r");
    	FileChannel inChan = testFile.getChannel();
    	ByteBuffer buf = ByteBuffer.allocate((int) testFile.length());
    	inChan.read(buf);
    	buf.flip();
    	RegionFile region = RegionFile.parse(buf.array());
    	
    	File tempFile = File.createTempFile("test", ".mci.gz");
    	tempFile.deleteOnExit();
    	region.writeArchive(tempFile);
	}

}
