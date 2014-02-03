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
import java.util.logging.Logger;


public class MCARegenerator extends SimpleFileVisitor<Path> {
	private Path directory;
	private int threads;
	private ExecutorService workers;
	private Logger logger;
	
	public MCARegenerator(Path tempDir, int threads) {
		this.directory = tempDir;
		this.threads = threads;
		this.logger = Logger.getLogger("main");
	}

	public synchronized void regenerateMCAFiles() throws IOException {
		this.workers = Executors.newFixedThreadPool(threads);
		logger.info("Regenerating MCAs");
		Files.walkFileTree(directory, this);
		workers.shutdown();
		try {
			workers.awaitTermination(5, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		logger.info("Finished regenerating MCAs");
	}
	
	public synchronized void regenerateSingleMCA(Path file) throws IOException {
		new ConversionTask(file).run();
	}
	
	static final String[] matchExtensions = {".mri", ".mri.gz", ".mri.bz2"};

    @Override
    public FileVisitResult visitFile(Path file,
            BasicFileAttributes attrs) {
    	for (String extension : matchExtensions) {
	        if (file.toString().endsWith(extension)) {
	        	workers.execute(new ConversionTask(file));
	        }
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
				logger.info("Unpacking " + file.toString());
				
	        	RegionFile region = RegionFile.readArchive(file.toFile());
	        	
	        	File tempFile = File.createTempFile("conversion", ".mca.t", file.toAbsolutePath().getParent().toFile());
	        	region.writeRegionFile(tempFile);
	        	
	        	String oldFileName = file.getFileName().toString();
	        	String newFileName = oldFileName + ".out";
	        	for (String extension : matchExtensions) {
	        		if (oldFileName.endsWith(extension)) {
	        			newFileName = oldFileName.replace(extension, ".mca");
	        			break;
	        		}
	        	}
	        	
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
