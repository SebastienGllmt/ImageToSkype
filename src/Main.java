import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Main {
	
	private static final String PATH = "res/img.bmp";
	private static final String PREFIX = "<b><font color=\"#AAAAAA\" size=\"4\">";
	private static final String SUFFIX = "</font></b>";
	
	// These are weights on the rgb scale to turn them into luminance. These seemingly magic numbers apparently make the result "account for human perception" based off how our eyes perceive colors differntly.
	private static final double WEIGHT_RED = 0.299;
	private static final double WEIGHT_GREEN = 0.587;
	private static final double WEIGHT_BLUE = 0.114;
	
	/**
	 * From trials, this seems to be around the max message length
	 */
	private static final int MAX_MSG_LENGTH = 2650;
	
	private final static int NUM_SHADES = 8;
	/**
	 * The number we have to divide luminance by to get a number between 0-NUM_SHADES
	 * For example, if we pick <code>NUM_SHADES</code> as 1, we get
	 * LUMINANCE_FLATTED = 256;
	 * 
	 * With this, any luminance / 256 we get will return 0, which is our single possible number.
	 */
	private final static int LUMINANCE_FLATTEN = (256 / NUM_SHADES);
	
	public static void main(String[] args) throws IOException {
		File f = new File(PATH);
		if (!f.exists()) {
			throw new FileNotFoundException(f.getAbsolutePath() + " could not be found");
		}
		
		BufferedImage img = ImageIO.read(f);
		
		int count = 0;
		
		System.out.println(PREFIX);
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int rgb = img.getRGB(x, y);
				int luminance = getLuminanceFromRGB(rgb);
				char pixel = getCharFromLuminance(luminance);
				
				System.out.print(pixel);
				count++;
				if (count >= MAX_MSG_LENGTH) {
					return;
				}
			}
			System.out.println();
		}
		System.out.print(SUFFIX);
	}
	
	private static int getLuminanceFromRGB(int rgb) {
		// First extract colors
		int red = (rgb >> 16) & 0xFF;
		int green = (rgb >> 8) & 0xFF;
		int blue = rgb & 0xFF;
		
		// Now multiply by weights
		red *= WEIGHT_RED;
		green *= WEIGHT_GREEN;
		blue *= WEIGHT_BLUE;
		
		return (red + green + blue) / LUMINANCE_FLATTEN;
	}
	
	private static char getCharFromLuminance(int luminance) {
		switch (luminance) {
			case 0:
				return '麤';
			case 1:
				return '璽';
			case 2:
				return '目';
			case 3:
				return '王';
			case 4:
				return '干';
			case 5:
				return '三';
			case 6:
				return '二';
			case 7:
				return '一';
			default:
				return '何';
		}
	}
}