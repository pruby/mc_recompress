package nz.net.goddard.mcrecompress.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import nz.net.goddard.mcrecompress.MCAConverter;
import nz.net.goddard.mcrecompress.MCARegenerator;

public class MainWindow extends JFrame {
	private static final long serialVersionUID = 1L;
	private JTextArea logArea;
	private List<String> logMessages;
	private Logger logger = null;
	private List<Component> actionControls;
	private JTextField pathField;
	
	public MainWindow() {
		super("MC Recompressor");
		
		logMessages = new ArrayList<String>();
		actionControls = new ArrayList<Component>();
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setupLoggers();
		buildUI();
		
		pack();
	}
	
	private void setupLoggers() {
		LogWindowHandler handler = new LogWindowHandler(this);
		this.logger = Logger.getLogger("main");
		logger.addHandler(handler);
	}
	
	private void buildUI() {
		// Text area
		logArea = new JTextArea();
		logArea.setPreferredSize(new Dimension(600, 500));
		
		getContentPane().add(logArea, BorderLayout.NORTH);
		
		// Path field
		
		JPanel pathPanel = new JPanel();
		pathPanel.setLayout(new FlowLayout());
		
		pathPanel.add(new JLabel("World to pack: "));
		
		pathField = new JTextField();
		Path defaultPath =  FileSystems.getDefault().getPath("");
		pathField.setText(defaultPath.toAbsolutePath().toString());
		pathPanel.add(pathField);
		pathField.setPreferredSize(new Dimension(800, 40));
		
		final MainWindow window = this;
		
		JButton choosePath = new JButton("...");
		choosePath.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileSelect = new JFileChooser();
				fileSelect.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int result = fileSelect.showOpenDialog(window);
				if (result == JFileChooser.APPROVE_OPTION) {
					pathField.setText(fileSelect.getSelectedFile().toString());
				}
			}
		});
		pathPanel.add(choosePath);
		
		getContentPane().add(pathPanel, BorderLayout.CENTER);
		
		// Buttons
		JPanel actionButtons = new JPanel();
		actionButtons.setLayout(new FlowLayout(FlowLayout.TRAILING));
		
		JButton packButton = new JButton("Pack MCAs");
		actionControls.add(packButton);
		packButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				convertMCAs();
			}
		});
		actionButtons.add(packButton , BorderLayout.WEST);
		
		JButton unpackButton = new JButton("Unpack MRIs");
		actionControls.add(unpackButton);
		unpackButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				convertMRIs();
			}
		});
		actionButtons.add(unpackButton , BorderLayout.EAST);
		
		getContentPane().add(actionButtons , BorderLayout.SOUTH);
	}

	public void logMessage(String message) {
		if (logMessages.size() > 30) {
			logMessages.remove(0);
		}
		logMessages.add(message);
		
		StringBuilder log = new StringBuilder();
		for (String entry : logMessages) {
			log.append(entry);
			log.append("\n");
		}
		logArea.setText(log.toString());
	}
	
	private void convertMCAs() {
		Runnable task = new Runnable() {
			public void run() {
				Path dir = FileSystems.getDefault().getPath(pathField.getText());
				MCAConverter converter = new MCAConverter(dir, 2);
				try {
					converter.convertMCAFiles();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				enableActions();			}
		};
		disableActions();
		new Thread(task).start();
	}
	
	private void disableActions() {
		for (Component control : actionControls) {
			control.setEnabled(false);
		}
	}
	
	private void enableActions() {
		for (Component control : actionControls) {
			control.setEnabled(true);
		}
	}

	private void convertMRIs() {
		Runnable task = new Runnable() {
			public void run() {
				Path dir = FileSystems.getDefault().getPath(pathField.getText());
				MCARegenerator regen = new MCARegenerator(dir, 2);
				try {
					regen.regenerateMCAFiles();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				enableActions();
			}
		};
		disableActions();
		new Thread(task).start();
	}
	
	public static void main(String[] args) {
		if (args.length == 0) {
			MainWindow window = new MainWindow();
			window.setVisible(true);
		}
	}
}
