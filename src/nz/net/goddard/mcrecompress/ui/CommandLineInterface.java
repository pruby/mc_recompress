package nz.net.goddard.mcrecompress.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

import nz.net.goddard.mcrecompress.MCAConverter;
import nz.net.goddard.mcrecompress.MCARegenerator;

public class CommandLineInterface {
	public static void main(String[] args) {
		if (args.length > 1) {
			switch (args[0]) {
			case "--compress":
				System.err.println("Compressing " + args[1]);
				MCAConverter converter = new MCAConverter(FileSystems.getDefault().getPath("."), 1);
				try {
					converter.convertSingleMCA(FileSystems.getDefault().getPath(args[1]));
				} catch (IOException e) {
					System.err.println(e.getMessage());
					System.exit(1);
				}
				break;
			case "--decompress":
				System.err.println("Unpacking " + args[1]);
				MCARegenerator regen = new MCARegenerator(FileSystems.getDefault().getPath("."), 1);
				try {
					regen.regenerateSingleMCA(FileSystems.getDefault().getPath(args[1]));
				} catch (IOException e) {
					System.err.println(e.getMessage());
					System.exit(1);
				}
				break;
			}
		} else {
			usage();
		}
	}
	
	private static void usage() {
		System.err.println("Usage: java -jar mcrecompress.jar [options]");
		System.err.println("Options:");
		System.err.println("  --compress MCA");
		System.err.println("  --decompress MRI");
		System.exit(1);
	}

}
