package nz.net.goddard.mcrecompress;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.itadaki.bzip2.BZip2InputStream;
import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;
import org.junit.Rule;
import org.junit.Test;


public class RecompressTests {
	@Test
	public void testParseMCA() throws IOException {
    	RegionFile region = RegionFile.readMCA(new File("./assets/test.mca"));
    	
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
    	RegionFile region = RegionFile.readMCA(new File("./assets/test.mca"));
    	
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
	public void testRegenerateRegionFile() throws IOException {
		// Read test.mca as master copy
    	RegionFile region = RegionFile.readMCA(new File("./assets/test.mca"));
    	
    	// Read test_converted.mri.gz - should read to the same data
		File archiveFile = new File("./assets/test_converted.mri.gz");
		byte[] fileData = Files.readAllBytes(archiveFile.toPath());
    	NBTInputStream reparser = new NBTInputStream(new ByteArrayInputStream(fileData));
    	Tag root = reparser.readTag();

    	assertEquals("Region", root.getName());
    	assertEquals("1.0", ((CompoundTag) root).getValue().get("MRI Version").getValue());

    	RegionFile reregion = RegionFile.fromArchive(root);
    	
    	compareRegions(region, reregion);
	}
	
	@Test
	public void testRegenerateBZ2RegionFile() throws IOException {
		// Read test.mca as master copy
    	RegionFile region = RegionFile.readMCA(new File("./assets/test.mca"));
    	
    	// Read test_converted.mri.gz - should read to the same data
		File archiveFile = new File("./assets/test_converted.mri.bz2");
    	RegionFile reregion = RegionFile.readArchive(archiveFile);
    	
    	compareRegions(region, reregion);
	}
	
	@Test
	public void testConvertReadBack() throws IOException {
    	RegionFile region = RegionFile.readMCA(new File("./assets/test.mca"));
    	
    	File tempFile = File.createTempFile("test", ".mri.bz2");
    	tempFile.deleteOnExit();
    	region.writeArchive(tempFile);
    	
    	byte[] fileData = Files.readAllBytes(tempFile.toPath());
    	NBTInputStream reparser = new NBTInputStream(new BZip2InputStream(new ByteArrayInputStream(fileData), false), false);
    	Tag root = reparser.readTag();

    	assertEquals("Region", root.getName());
    	assertEquals("1.0", ((CompoundTag) root).getValue().get("MRI Version").getValue());

    	RegionFile reregion = RegionFile.fromArchive(root);
    	// Reload original region
    	region = RegionFile.readMCA(new File("./assets/test.mca"));
    	
    	compareRegions(region, reregion);
	}
	
	@Test
	public void testChunkDataBlock() throws IOException {
    	RegionFile region = RegionFile.readMCA(new File("./assets/test.mca"));
    	
    	ChunkData chunk = null;
    	outer:
    	for (int x = 0; x < 32; ++x) {
    		for (int z = 0; z < 32; ++z) {
    			if (region.getChunk(x,  z) != null) {
    				chunk = region.getChunk(x,  z);
    				break outer;
    			}
    		}
    	}
    	
    	byte[] chunkBlock = chunk.generateChunkBlock();
    	DataInputStream blockReader = new DataInputStream(new ByteArrayInputStream(chunkBlock));
    	int dataLength = blockReader.readInt();
    	byte compressionMethod = blockReader.readByte();
    	assertEquals(2, compressionMethod);
    	byte[] compressedData = new byte[dataLength - 1];
    	blockReader.read(compressedData);
    	
    	ByteArrayInputStream rereader = new ByteArrayInputStream(compressedData);
    	InflaterInputStream reinflater = new InflaterInputStream(rereader);
    	NBTInputStream reparser = new NBTInputStream(reinflater, false);
    	reparser.readTag();
	}
	
	@Test
	public void testTranscribeAssets() throws IOException {
    	Path tempDir = Files.createTempDirectory("temp_mca");
    	
    	Path assetPath = FileSystems.getDefault().getPath("assets/test.mca");
    	Files.copy(assetPath, tempDir.resolve("test.mca"));
    	
		MCAConverter converter = new MCAConverter(tempDir, 1);
		converter.convertMCAFiles();

		// File should have been converted and original deleted
		assertTrue(Files.exists(tempDir.resolve("test.mri.bz2")));
		assertFalse(Files.exists(tempDir.resolve("test.mca")));
    	
		MCARegenerator regen = new MCARegenerator(tempDir, 1);
		regen.regenerateMCAFiles();

		// File should have been converted and original deleted
		assertFalse(Files.exists(tempDir.resolve("test.mri.bz2")));
		assertTrue(Files.exists(tempDir.resolve("test.mca")));
		
		// Load test region
    	RegionFile region = RegionFile.readMCA(new File("./assets/test.mca"));
    	RegionFile reregion = RegionFile.readMCA(tempDir.resolve("test.mca").toFile());
    	
    	compareRegions(region, reregion);
		
		// Clean up
		Files.deleteIfExists(tempDir.resolve("test.mca"));
		Files.delete(tempDir);
	}
	
	private void compareRegions(RegionFile region, RegionFile reregion) {
    	for (int x = 0; x < 32; x++) {
    		for (int z = 0; z < 32; z++) {
    			ChunkData c1 = region.getChunk(x, z);
    			ChunkData c2 = reregion.getChunk(x, z);
    			assertEquals(c1 == null, c2 == null);
    			
    			if (c1 != null) {
	    			for (int y = 0; y < 16; ++y) {
	    				boolean c1HasSection = (c1.getSection(y) != null);
	    				boolean c2HasSection = (c2.getSection(y) != null);
	    				assertEquals(c1HasSection, c2HasSection);
	    				
	    				if (c1HasSection) {
		    				for (String combineKey: RegionFile.combineBlockLengths.keySet()) {
			    				// Pick a pseudo-random byte to look at
			    				Random random = new Random(x);
			    				random = new Random(random.nextInt() + z);
			    				random = new Random(random.nextInt() + y);
			    				
			    				// Sample 50 random bytes, check identical
			    				for (int rep = 0; rep < 50; ++rep) {
				    				int selection = random.nextInt(RegionFile.combineBlockLengths.get(combineKey));
				    				byte[] c1data = c1.getSection(y).getByteBlock(combineKey);
				    				byte[] c2data = c2.getSection(y).getByteBlock(combineKey);
				    				assertEquals(c1data == null, c2data == null);
				    				if (c1data != null) {
				    					assertEquals(c1data[selection], c2data[selection]);
				    				}
			    				}
		    				}
	    				}
	    			}
    			}
    		}
    	}
	}
}
