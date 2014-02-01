package nz.net.goddard.mcrecompress.ui;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

class LogWindowHandler extends Handler {
	private MainWindow window = null;

	public LogWindowHandler(MainWindow window) {
		LogManager manager = LogManager.getLogManager();
		String className = this.getClass().getName();
		String level = manager.getProperty(className + ".level");
		setLevel(level != null ? Level.parse(level) : Level.INFO);
		this.window = window;
	}

	public synchronized void publish(LogRecord record) {
		String message = null;
		if (!isLoggable(record))
			return;
		message = record.getMessage();
		window.logMessage(message);
	}

	public void close() {
	}

	public void flush() {
	}
}
