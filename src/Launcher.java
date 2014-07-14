import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Launcher {
	
	private static final String DEFAULT_PATH = "res/img.bmp";
	private static final String NAME = "ImageToSkype";
	private static final Dimension SIZE = new Dimension(1280, 960);
	private static final Font SKYPE_FONT = new Font("LucidaSans", Font.BOLD, 6);
	
	public static void main(String[] args) throws IOException {
		new Launcher();
	}
	
	public Launcher() throws IOException {
		JFrame frame = new JFrame(NAME);
		frame.setPreferredSize(SIZE);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		
		JPanel mainGrid = new JPanel();
		mainGrid.setLayout(new BoxLayout(mainGrid, BoxLayout.Y_AXIS));
		
		JPanel optionsPanel = new JPanel(new FlowLayout());
		
		JTextField optionsColor = new JTextField("#AAAAAA");
		JTextField optionsSize = new JTextField("4");
		
		optionsPanel.add(optionsColor);
		optionsPanel.add(optionsSize);
		
		mainGrid.add(optionsPanel);
		
		JPanel imageContentGrid = new JPanel(new GridLayout(1, 2));
		
		JEditorPane preview = new JEditorPane();
		preview.setContentType("text/html");
		preview.setFont(SKYPE_FONT);
		
		JTextArea rawText = new JTextArea();
		rawText.setFont(SKYPE_FONT);
		
		//JEditorPanel 
		BufferedImage image;
		try {
			image = ImageRender.getImage(DEFAULT_PATH);
			
			String content = ImageRender.render(image);
			String optionsColorText = optionsColor.getText();
			
			String color;
			if (optionsColorText.matches("^#([0-9]|[A-F])+$")) {
				color = optionsColorText;
			} else {
				color = ImageRender.getBestFitColorCode(image);
			}
			
			String diplay = content.replaceAll("\n", "<br/>");
			rawText.setText(ImageRender.getFormattedHTML(content, color, optionsSize.getText()));
			preview.setText(ImageRender.getFormattedHTML(diplay, color, "1"));
		} catch (IOException e) {
			rawText.setText("Failed to load image");
			preview.setText("Failed to load image");
		}
		imageContentGrid.add(rawText);
		imageContentGrid.add(preview);
		
		mainGrid.add(imageContentGrid);
		
		frame.getContentPane().add(mainGrid);
		
		frame.pack();
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);
		
	}
}
