import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

public class Launcher {
	
	private static final String DEFAULT_PATH = "res/img.bmp";
	private static final String NAME = "ImageToSkype";
	private static final int DEFAULT_TEXT_SIZE = 4;
	private static final Font SKYPE_FONT = new Font("MS Gothic", Font.BOLD, DEFAULT_TEXT_SIZE);
	private static final String SKYPE_BACKGROUND_COLOR = "#E6F8FC";
	
	private static JFrame currFrame;
	private final static JFileChooser fileChooser = new JFileChooser();
	
	// Generate fields for the component storing the last used color/size for text. Also give them default values for when you launch the program
	private String lastColor = "";
	private static final int COLOR_TEXT_LENGTH = 7; // # sign and 6 numbers
	
	private String lastSize = String.valueOf(DEFAULT_TEXT_SIZE);
	private static final int SIZE_TEXT_LENGTH = 2; // doubt anybody will set the size more than 2 digits and expect something reasonable
	
	private static boolean useCustomColor = false;
	
	public static void main(String[] args) throws IOException {
		FileFilter filter = new FileFilter() {
			
			@Override
			public boolean accept(File f) {
				return (f.isDirectory() || f.getName().toLowerCase().endsWith("bmp"));
			}
			
			@Override
			public String getDescription() {
				return "BMPs and Directories";
			}
			
		};
		fileChooser.setFileFilter(filter);
		fileChooser.setCurrentDirectory(new File("res/"));
		fileChooser.setSelectedFile(new File(DEFAULT_PATH));
		new Launcher();
	}
	
	public Launcher() {
		JFrame frame = new JFrame(NAME);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel mainGrid = new JPanel();
		mainGrid.setLayout(new BoxLayout(mainGrid, BoxLayout.Y_AXIS));
		
		JPanel optionsPanel = new JPanel(new GridLayout(4, 1));
		
		JPanel colorCheckboxPanel = new JPanel(new FlowLayout());
		JCheckBox customColor = new JCheckBox("Use Custom Color", useCustomColor);
		customColor.addActionListener(l -> useCustomColor = !useCustomColor);
		colorCheckboxPanel.add(customColor);
		
		JPanel fontColorPanel = new JPanel(new GridLayout(1, 2));
		JLabel optionsColorLabel = new JLabel("Text color");
		JTextField optionsColor = new JTextField(lastColor, COLOR_TEXT_LENGTH);
		optionsColor.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				lastColor = optionsColor.getText();
			}
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				lastColor = optionsColor.getText();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				lastColor = optionsColor.getText();
			}
		});
		fontColorPanel.add(optionsColorLabel);
		fontColorPanel.add(optionsColor);
		
		JPanel fontSizePanel = new JPanel(new GridLayout(1, 2));
		JLabel optionsSizeLabel = new JLabel("Text color");
		JTextField optionsSize = new JTextField(lastSize, SIZE_TEXT_LENGTH);
		optionsSize.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				lastSize = optionsColor.getText();
			}
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				lastSize = optionsColor.getText();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				lastSize = optionsColor.getText();
			}
		});
		fontSizePanel.add(optionsSizeLabel);
		fontSizePanel.add(optionsSize);
		
		JPanel loadImagePanel = new JPanel(new GridLayout(1, 2));
		JButton filePicker = new JButton("New File");
		filePicker.addActionListener(l -> {
			// If we select a new file to render
				if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					// Render it in a background thread and then swap them when it's done
					SwingUtilities.invokeLater(() -> new Launcher());
				}
			});
		JButton refresh = new JButton("Refresh");
		refresh.addActionListener(l -> new Launcher());
		loadImagePanel.add(filePicker);
		loadImagePanel.add(refresh);
		
		optionsPanel.add(colorCheckboxPanel);
		optionsPanel.add(fontColorPanel);
		optionsPanel.add(fontSizePanel);
		optionsPanel.add(loadImagePanel);
		
		mainGrid.add(optionsPanel);
		
		JEditorPane preview = new JEditorPane();
		preview.setFont(SKYPE_FONT);
		
		//JEditorPanel 
		BufferedImage image;
		try {
			image = ImageRender.getImage(fileChooser.getSelectedFile());
			String content = ImageRender.render(image);
			
			String color = null;
			if (useCustomColor) {
				
				/* If string is of format:
				 * Can start with optional # sign
				 * Followed by 6 digits/letters from Hexadecimal
				 * All of this must be on its own line with nothing preceding/following
				 */
				if (lastColor.matches("^#?([0-9]|[A-F]){6}$")) {
					if (!lastColor.startsWith("#")) {
						color = "#" + lastColor;
					} else {
						color = lastColor;
					}
				}
			}
			if (color == null) {
				color = ImageRender.getBestFitColorCode(image);
			}
			String textSize = lastSize;
			if (textSize.matches("^[0-9]+$")) {} else {
				textSize = String.valueOf(DEFAULT_TEXT_SIZE);
			}
			
			preview.setText(ImageRender.getFormattedHTML(content, color, textSize));
			preview.setForeground(Color.decode(color));
			
			Dimension stringDimension = getMinimumStringDimension(image.getGraphics(), content, SKYPE_FONT);
			preview.setMinimumSize(stringDimension);
			
		} catch (IOException e) {
			preview.setText("Failed to load image");
		}
		preview.setBackground(Color.decode(SKYPE_BACKGROUND_COLOR));
		preview.setEditable(false);
		
		mainGrid.add(preview);
		
		frame.getContentPane().add(mainGrid);
		
		frame.setMinimumSize(addDimension(optionsPanel.getMinimumSize(), preview.getMinimumSize()));
		
		frame.pack();
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);
		
		// Swap the current frame with the new one once it's done being created
		if (currFrame != null) {
			currFrame.dispose();
		}
		currFrame = frame;
	}
	
	private Dimension addDimension(Dimension a, Dimension b) {
		final int EDGE_BUFFER = 20; // A buffer because the window can't be EXACTLY the size of the text
		
		int maxWidth = a.width > b.width ? a.width : b.width;
		return new Dimension(maxWidth + EDGE_BUFFER, a.height + b.height);
	}
	
	private Dimension getMinimumStringDimension(Graphics g, String content, Font f) {
		String longestString = "";
		String[] splitString = content.split("\n");
		for (String s : splitString) {
			if (s.length() > longestString.length()) {
				longestString = s;
			}
		}
		int containerWidth = g.getFontMetrics(SKYPE_FONT).stringWidth(longestString);
		int containerHeight = g.getFontMetrics(SKYPE_FONT).getHeight() * splitString.length;
		
		return new Dimension(containerWidth, containerHeight);
	}
}
