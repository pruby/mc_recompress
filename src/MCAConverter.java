import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;


public class MCAConverter extends SimpleFileVisitor<Path> {
	private String directory;
	
	public MCAConverter(String directory) {
		this.directory = directory;
	}

	public void convertMCAFiles() throws IOException {
		Files.walkFileTree(FileSystems.getDefault().getPath(directory), this);
	}

    @Override
    public FileVisitResult visitFile(Path file,
            BasicFileAttributes attrs) {
    	try {
	        if (file.toString().endsWith(".mca")) {
	        	byte[] fileData = Files.readAllBytes(file);
	        	RegionFile region = RegionFile.parse(fileData);
	        	
	        	File tempFile = File.createTempFile("test", ".mci.gz");
	        	tempFile.deleteOnExit();
	        	region.writeArchive(tempFile);
	        }
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
        return FileVisitResult.CONTINUE;
    }
}
