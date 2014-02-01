package nz.net.goddard.mcrecompress.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextPane;

import nz.net.goddard.mcrecompress.MCAConverter;
import nz.net.goddard.mcrecompress.MCARegenerator;

public class MainWindow extends JFrame {
	private JTextArea logArea;
	private List<String> logMessages;
	private Logger logger = null;
	
	public MainWindow() {
		super("MC Recompressor");
		
		logMessages = new ArrayList<String>();
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		buildUI();
		
		pack();
	}
	
	private void setupLoggers() {
		WindowHandler handler = new WindowHandler(this);
		this.logger = Logger.getLogger("main");
		logger.addHandler(handler);
	}
	
	private void buildUI() {
		// Text area
		logArea = new JTextArea();
		logArea.setEnabled(false);
		logArea.setPreferredSize(new Dimension(600, 500));
		
		getContentPane().add(logArea, BorderLayout.NORTH);
		
		// Buttons
		JPanel actionButtons = new JPanel();
		actionButtons.setLayout(new FlowLayout(FlowLayout.TRAILING));
		
		JButton packButton = new JButton("Pack MCAs");
		packButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				convertMCAs();
			}
		});
		
		actionButtons.add(packButton , BorderLayout.WEST);
		packButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				convertMRIs();
			}
		});
		
		JButton unpackButton = new JButton("Unpack MRIs");
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
		if (logMessages.size() > 12) {
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
		Path dir = FileSystems.getDefault().getPath(".");
		MCAConverter converter = new MCAConverter(dir, 4);
		try {
			converter.convertMCAFiles();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void convertMRIs() {
		Path dir = FileSystems.getDefault().getPath(".");
		MCARegenerator regen = new MCARegenerator(dir, 4);
		try {
			regen.regenerateMCAFiles();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		if (args.length == 0) {
			MainWindow window = new MainWindow();
			window.setVisible(true);
		}
	}
}
