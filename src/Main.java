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
	
	// Quant errors based on Floyd-Steinberg dithering ( http://en.wikipedia.org/wiki/Floyd–Steinberg_dithering ) 
	private static final double QUANT_ERROR_RIGHT = 7.0 / 16;
	private static final double QUANT_ERROR_DOWN = 5.0 / 16;
	private static final double QUANT_ERROR_DOWN_LEFT = 3.0 / 16;
	private static final double QUANT_ERROR_DOWN_RIGHT = 1.0 / 16;
	
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
		
		BufferedImage bufferedImage = ImageIO.read(f);
		
		int count = 0;
		int[][] pixels = getPixelArray(bufferedImage);
		
		System.out.println(PREFIX);
		for (int y = 0; y < bufferedImage.getHeight(); y++) {
			for (int x = 0; x < bufferedImage.getWidth(); x++) {
				
				// Get pixel and luminance
				int originalPixel = pixels[y][x];
				int luminance = getLuminanceFromRGB(originalPixel);
				
				// Calculate error caused from palette reduction
				int colorReducedPixel = colorFromRGB(luminance, luminance, luminance);
				int[] colorError = getColorDifference(originalPixel, colorReducedPixel);
				
				// Adjust next pixels based on Floyd-Steinberg dithering ( http://en.wikipedia.org/wiki/Floyd–Steinberg_dithering )
				if (x < bufferedImage.getWidth() - 1)
					FixQuantError(pixels, x + 1, y, colorError, QUANT_ERROR_RIGHT);
				if (y < bufferedImage.getHeight() - 1)
					FixQuantError(pixels, x, y + 1, colorError, QUANT_ERROR_DOWN);
				if (x > 0 && y < bufferedImage.getHeight() - 1)
					FixQuantError(pixels, x - 1, y + 1, colorError, QUANT_ERROR_DOWN_LEFT);
				if (x < bufferedImage.getWidth() - 1 && y < bufferedImage.getHeight() - 1)
					FixQuantError(pixels, x + 1, y + 1, colorError, QUANT_ERROR_DOWN_RIGHT);
				
				// Print char
				char pixel = getCharFromLuminance(luminance / LUMINANCE_FLATTEN);
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
	
	private static int[][] getPixelArray(BufferedImage bufferedImage) {
		int[][] pixels = new int[bufferedImage.getHeight()][bufferedImage.getWidth()];
		
		for (int y = 0; y < bufferedImage.getHeight(); y++) {
			for (int x = 0; x < bufferedImage.getWidth(); x++) {
				pixels[y][x] = bufferedImage.getRGB(x, y);
			}
		}
		
		return pixels;
	}
	
	private static void FixQuantError(int[][] pixels, int x, int y, int[] colorError, double quantError) {
		int[] rgb = getRgbArray(pixels[y][x]);
		
		rgb[0] += colorError[0] * quantError;
		rgb[0] = rgb[0] > 255 ? 255 : rgb[0];
		
		rgb[1] += colorError[1] * quantError;
		rgb[1] = rgb[1] > 255 ? 255 : rgb[1];
		
		rgb[2] += colorError[2] * quantError;
		rgb[2] = rgb[2] > 255 ? 255 : rgb[2];
		
		pixels[y][x] = colorFromRGB(rgb[0], rgb[1], rgb[2]);
		
	}
	
	private static int[] getColorDifference(int rgb1, int rgb2) {
		int[] rgbArray1 = getRgbArray(rgb1);
		int[] rgbArray2 = getRgbArray(rgb2);
		
		int[] rgbResult = new int[3];
		rgbResult[0] = rgbArray1[0] - rgbArray2[0];
		rgbResult[0] = rgbResult[0] > 0 ? rgbResult[0] : -rgbResult[0];
		
		rgbResult[1] = rgbArray1[1] - rgbArray2[1];
		rgbResult[1] = rgbResult[1] > 0 ? rgbResult[1] : -rgbResult[1];
		
		rgbResult[2] = rgbArray1[2] - rgbArray2[2];
		rgbResult[1] = rgbResult[1] > 0 ? rgbResult[1] : -rgbResult[1];
		
		return rgbResult;
	}
	
	private static int colorFromRGB(int r, int g, int b) {
		return b + (g << 8) + (r << 16);
	}
	
	private static int[] getRgbArray(int rgb) {
		int[] rgbArray = new int[3];
		rgbArray[0] = (rgb >> 16) & 0xFF;
		rgbArray[1] = (rgb >> 8) & 0xFF;
		rgbArray[2] = rgb & 0xFF;
		
		return rgbArray;
	}
	
	private static int getLuminanceFromRGB(int rgb) {
		// First extract colors
		int[] rgbArray = getRgbArray(rgb);
		
		// Now multiply by weights
		rgbArray[0] *= WEIGHT_RED;
		rgbArray[1] *= WEIGHT_GREEN;
		rgbArray[2] *= WEIGHT_BLUE;
		
		return (rgbArray[0] + rgbArray[1] + rgbArray[2]);
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