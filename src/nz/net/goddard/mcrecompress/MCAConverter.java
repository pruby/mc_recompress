package nz.net.goddard.mcrecompress;
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


public class MCAConverter extends SimpleFileVisitor<Path> {
	private Path directory;
	
	public MCAConverter(Path tempDir) {
		this.directory = tempDir;
	}

	public void convertMCAFiles() throws IOException {
		Files.walkFileTree(directory, this);
	}

    @Override
    public FileVisitResult visitFile(Path file,
            BasicFileAttributes attrs) {
    	try {
	        if (file.toString().endsWith(".mca")) {
	        	byte[] fileData = Files.readAllBytes(file);
	        	RegionFile region = RegionFile.parse(fileData);
	        	
	        	File tempFile = File.createTempFile("conversion", ".mri.t", file.getParent().toFile());
	        	String newFileName = file.getFileName().toString().replace(".mca", ".mri.bz2");
	        	region.writeArchive(tempFile, ArchiveCompression.BZIP2);
	        	
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
