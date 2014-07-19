import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

public class Launcher {
	
	private static final String DEFAULT_PATH = "res/img.bmp";
	private static final String NAME = "ImageToSkype";
	private static final Font SKYPE_FONT = new Font("MS Gothic", Font.BOLD, 6);
	private static final String SKYPE_BACKGROUND_COLOR = "#E6F8FC";
	
	private static JFrame currFrame;
	private final static JFileChooser fileChooser = new JFileChooser();
	
	// Generate fields for the component storing the last used color/size for text. Also give them default values for when you launch the program
	private String lastColor = "#";
	private String lastSize = "4";
	
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
		new Launcher(DEFAULT_PATH);
	}
	
	public Launcher(String path) throws IOException {
		JFrame frame = new JFrame(NAME);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel mainGrid = new JPanel();
		mainGrid.setLayout(new BoxLayout(mainGrid, BoxLayout.Y_AXIS));
		
		JPanel optionsPanel = new JPanel(new FlowLayout());
		
		JTextField optionsColor = new JTextField(lastColor);
		JTextField optionsSize = new JTextField(lastSize);
		JButton filePicker = new JButton("Render File");
		filePicker.addActionListener(l -> {
			if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
				try {
					new Launcher(fileChooser.getSelectedFile().toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		optionsPanel.add(optionsColor);
		optionsPanel.add(optionsSize);
		optionsPanel.add(filePicker);
		
		mainGrid.add(optionsPanel);
		
		JEditorPane preview = new JEditorPane();
		preview.setFont(SKYPE_FONT);
		
		//JEditorPanel 
		BufferedImage image;
		try {
			image = ImageRender.getImage(path);
			String content = ImageRender.render(image);
			
			Dimension stringDimension = getMinimumStringDimension(image.getGraphics(), content, SKYPE_FONT);
			preview.setMinimumSize(stringDimension);
			
			String optionsColorText = optionsColor.getText();
			
			String color;
			if (optionsColorText.matches("^#([0-9]|[A-F])+$")) {
				color = optionsColorText;
			} else {
				color = ImageRender.getBestFitColorCode(image);
			}
			
			preview.setText(ImageRender.getFormattedHTML(content, color, optionsSize.getText()));
			preview.setForeground(Color.decode(color));
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
