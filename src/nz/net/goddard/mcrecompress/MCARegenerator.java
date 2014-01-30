package nz.net.goddard.mcrecompress;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.itadaki.bzip2.BZip2InputStream;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;


public class MCARegenerator extends SimpleFileVisitor<Path> {
	private Path directory;
	
	public MCARegenerator(Path tempDir) {
		this.directory = tempDir;
	}

	public void convertMCAFiles() throws IOException {
		Files.walkFileTree(directory, this);
	}

    @Override
    public FileVisitResult visitFile(Path file,
            BasicFileAttributes attrs) {
    	try {
	        if (file.toString().endsWith(".mri") || file.toString().endsWith(".mri.gz") || file.toString().endsWith(".mri.bz2")) {
	        	byte[] fileData = Files.readAllBytes(file);
	        	NBTInputStream reparser;
	        	if (file.toString().endsWith(".bz2")) {
	        		reparser = new NBTInputStream(new BZip2InputStream(new ByteArrayInputStream(fileData), false), false);
	        	} else {
	        		reparser = new NBTInputStream(new ByteArrayInputStream(fileData), file.toString().endsWith(".gz"));
	        	}
	        	Tag root = reparser.readTag();
	        	RegionFile region = RegionFile.fromArchive(root);
	        	
	        	File tempFile = File.createTempFile("conversion", ".mca.t", file.getParent().toFile());
	        	region.writeRegionFile(tempFile);
	        	
	        	String newFileName = file.getFileName().toString().replace(".mri.gz", ".mca");
	        	Path newPath = file.resolveSibling(newFileName);
	        	
	        	Files.deleteIfExists(newPath);
	        	Files.move(tempFile.toPath(), newPath);
	        	Files.delete(file);
	        }
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
        return FileVisitResult.CONTINUE;
    }
}
