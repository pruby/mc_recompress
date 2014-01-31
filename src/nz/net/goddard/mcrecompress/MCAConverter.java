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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class MCAConverter extends SimpleFileVisitor<Path> {
	private Path directory;
	private int threads;
	private ExecutorService workers;
	
	public MCAConverter(Path tempDir, int threads) {
		this.directory = tempDir;
		this.threads = threads;
	}

	public synchronized void convertMCAFiles() throws IOException {
		this.workers = Executors.newFixedThreadPool(threads);
		Files.walkFileTree(directory, this);
		workers.shutdown();
		try {
			workers.awaitTermination(5, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

    @Override
    public FileVisitResult visitFile(Path file,
            BasicFileAttributes attrs) {
        if (file.toString().endsWith(".mca")) {
        	workers.execute(new ConversionTask(file));
        }
        return FileVisitResult.CONTINUE;
    }
    
    private class ConversionTask implements Runnable {

    	private Path file;

		public ConversionTask(Path file) {
    		this.file = file;
    	}
    	
		@Override
		public void run() {
			try {
	        	byte[] fileData = Files.readAllBytes(file);
	        	RegionFile region = RegionFile.parse(fileData);
	        	
	        	File tempFile = File.createTempFile("conversion", ".mri.t", file.getParent().toFile());
	        	String newFileName = file.getFileName().toString().replace(".mca", ".mri.bz2");
	        	region.writeArchive(tempFile, ArchiveCompression.BZIP2);
	        	
	        	Path newPath = file.resolveSibling(newFileName);
	        	
	        	Files.deleteIfExists(newPath);
	        	Files.move(tempFile.toPath(), newPath);
	        	Files.delete(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    	
    }
}
