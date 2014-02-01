package nz.net.goddard.mcrecompress;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MCAConverter extends SimpleFileVisitor<Path> {
	private Path directory;
	private int threads;
	private ExecutorService workers;
	private final ArchiveCompression compressionMode;
	private final Logger logger;
	
	public MCAConverter(Path tempDir, int threads) {
		this.directory = tempDir;
		this.threads = threads;
		this.compressionMode = ArchiveCompression.BZIP2;
		this.logger = Logger.getLogger("main");
	}
	
	public MCAConverter(Path tempDir, int threads, ArchiveCompression compressionMode) {
		this.directory = tempDir;
		this.threads = threads;
		this.compressionMode = compressionMode;
		this.logger = Logger.getLogger("main");
	}

	public synchronized void convertMCAFiles() throws IOException {
		this.workers = Executors.newFixedThreadPool(threads);
		logger.info("Packing MCAs");
		Files.walkFileTree(directory, this);
		workers.shutdown();
		try {
			workers.awaitTermination(5, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		logger.info("Done packing MCAs");
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
				logger.log(Level.INFO, "Compressing " + file.toString());
				
	        	byte[] fileData = Files.readAllBytes(file);
	        	RegionFile region = RegionFile.parse(fileData);
	        	
	        	File tempFile = File.createTempFile("conversion", ".mri.t", file.getParent().toFile());
	        	
	        	String newFileName;
	        	if (compressionMode == ArchiveCompression.BZIP2) {
	        		newFileName = file.getFileName().toString().replace(".mca", ".mri.bz2");
	        	} else if (compressionMode == ArchiveCompression.GZIP) {
	        		newFileName = file.getFileName().toString().replace(".mca", ".mri.gz");
	        	} else {
	        		newFileName = file.getFileName().toString().replace(".mca", ".mri");
	        	}
	        	region.writeArchive(tempFile, compressionMode);
	        	
	        	Path newPath = file.resolveSibling(newFileName);
	        	
	        	Files.deleteIfExists(newPath);
	        	Files.move(tempFile.toPath(), newPath);
	        	Files.delete(file);
			} catch (IOException e) {
				logger.log(Level.SEVERE, e.toString());
			}
		}
    	
    }
}
