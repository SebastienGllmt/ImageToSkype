import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Launcher {
	
	private static final String DEFAULT_PATH = "res/img.bmp";
	private static final String NAME = "ImageToSkype";
	private static final Font SKYPE_FONT = new Font("LucidaSans", Font.BOLD, 6);
	
	public static void main(String[] args) throws IOException {
		new Launcher();
	}
	
	public Launcher() throws IOException {
		JFrame frame = new JFrame(NAME);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel mainGrid = new JPanel();
		mainGrid.setLayout(new BoxLayout(mainGrid, BoxLayout.Y_AXIS));
		
		JPanel optionsPanel = new JPanel(new FlowLayout());
		
		JTextField optionsColor = new JTextField("#AAAAAA");
		JTextField optionsSize = new JTextField("4");
		
		optionsPanel.add(optionsColor);
		optionsPanel.add(optionsSize);
		
		mainGrid.add(optionsPanel);
		
		JEditorPane preview = new JEditorPane();
		preview.setFont(SKYPE_FONT);
		
		//JEditorPanel 
		BufferedImage image;
		try {
			image = ImageRender.getImage(DEFAULT_PATH);
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
		System.out.println(preview.getMinimumSize());
		
		mainGrid.add(preview);
		
		frame.getContentPane().add(mainGrid);
		
		frame.setMinimumSize(addDimension(optionsPanel.getMinimumSize(), preview.getMinimumSize()));
		
		frame.pack();
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);
		
	}
	
	private Dimension addDimension(Dimension a, Dimension b) {
		return new Dimension(a.width + b.width, a.height + b.height);
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
