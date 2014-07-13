import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;

import javax.imageio.ImageIO;

public class Main {
	
	private static final String PATH = "res/img.bmp";
	private static final String PREFIX = "<b><font color=\"%s\" size=\"%s\">";
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
	
	// Constant colors
	private static final int[] BLACK = { 0, 0, 0 };
	private static final int[] WHITE = { 255, 255, 255 };
	private static final int MIN_DIST_FROM_EDGE = 20; // minimum distance from either WHITE/BLACK for it to be considered a color
	
	/**
	 * From trials, this seems to be around the max message length
	 */
	private static final int MAX_MSG_LENGTH = 2650;
	
	private final static int NUM_SHADES = 11;
	/**
	 * The number we have to divide luminance by to get a number between 0-NUM_SHADES
	 * For example, if we pick <code>NUM_SHADES</code> as 1, we get
	 * LUMINANCE_FLATTED = 256;
	 * 
	 * With this, any luminance / 256 we get will return 0, which is our single possible number.
	 */
	private final static int LUMINANCE_FLATTEN = 256 % NUM_SHADES == 0 ? (256 / NUM_SHADES) : (256 / NUM_SHADES) + 1;
	
	public static void main(String[] args) throws IOException {
		File f = new File(PATH);
		if (!f.exists()) {
			throw new FileNotFoundException(f.getAbsolutePath() + " could not be found");
		}
		
		BufferedImage bufferedImage = ImageIO.read(f);
		
		// Set up image array
		int count = 0;
		int[][] pixels = getPixelArray(bufferedImage);
		// The size of our text is width*height of the image as every pixel is a char.
		// We also add a height again since there will be *height* amount of \n
		StringBuilder imageAsChars = new StringBuilder(bufferedImage.getHeight() + (bufferedImage.getWidth() * bufferedImage.getHeight()));
		
		// Set up average color
		BigDecimal[] averageHSB = new BigDecimal[3];
		for (int i = 0; i < averageHSB.length; i++) {
			averageHSB[i] = BigDecimal.ZERO;
		}
		long colorPixelCount = 0;
		
		imageRender:
		for (int y = 0; y < bufferedImage.getHeight(); y++) {
			imageAsChars.append('\n');
			for (int x = 0; x < bufferedImage.getWidth(); x++) {
				// Get pixel and luminance
				int originalPixel = pixels[y][x] & 0xFFFFFF; // mask away opacity
				int[] originalPixelArray = getRgbArray(originalPixel);
				int luminance = (int) ((originalPixelArray[0] * WEIGHT_RED) + (originalPixelArray[1] * WEIGHT_GREEN) + (originalPixelArray[2] * WEIGHT_BLUE));
				
				// Calculate error caused from palette reduction by finding the difference between the original pixel and the reduced version
				int[] colorError = getColorDifference(originalPixelArray, new int[] { luminance, luminance, luminance });
				
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
				imageAsChars.append(pixel);
				
				// Calcualte average HSB
				
				// Avoid pure black/pure white
				int[] imageColorPixel = getRgbArray(bufferedImage.getRGB(x, y));
				int[] blackDiff = getColorDifference(imageColorPixel, BLACK);
				int[] whiteDiff = getColorDifference(imageColorPixel, WHITE);
				if ((blackDiff[0] + blackDiff[1] + blackDiff[2]) >= MIN_DIST_FROM_EDGE && (whiteDiff[0] + whiteDiff[1] + whiteDiff[2]) >= MIN_DIST_FROM_EDGE) {
					float[] hsb = new float[3];
					Color.RGBtoHSB(imageColorPixel[0], imageColorPixel[1], imageColorPixel[2], hsb);
					averageHSB[0] = averageHSB[0].add(BigDecimal.valueOf(hsb[0]));
					averageHSB[1] = averageHSB[1].add(BigDecimal.valueOf(hsb[1]));
					averageHSB[2] = averageHSB[2].add(BigDecimal.valueOf(hsb[2]));
					colorPixelCount++;
				}
				
				// Increment count
				count++;
				if (count >= MAX_MSG_LENGTH) {
					break imageRender;
				}
			}
		}
		
		float averageHue = (float) (averageHSB[0].doubleValue() / colorPixelCount);
		float averageSaturation = (float) (averageHSB[1].doubleValue() / colorPixelCount);
		float averageBrightness = (float) (averageHSB[2].doubleValue() / colorPixelCount);
		
		// make sure the picture is visible
		if (averageBrightness < 0.5)
			averageBrightness = 0.5f;
		
		int[] averageColors = getRgbArray(Color.HSBtoRGB(averageHue, averageSaturation, averageBrightness));
		String colorCode = "#" + Integer.toHexString(averageColors[0]) + Integer.toHexString(averageColors[1]) + Integer.toHexString(averageColors[2]);
		
		System.out.printf(PREFIX, colorCode, "4");
		System.out.println(imageAsChars.toString());
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
		
		pixels[y][x] = (rgb[0] << 16) + (rgb[1] << 8) + rgb[2];
		
	}
	
	private static int[] getColorDifference(int[] rgb1, int[] rgb2) {
		int[] rgbResult = new int[3];
		rgbResult[0] = rgb1[0] - rgb2[0];
		rgbResult[0] = rgbResult[0] > 0 ? rgbResult[0] : -rgbResult[0];
		
		rgbResult[1] = rgb1[1] - rgb2[1];
		rgbResult[1] = rgbResult[1] > 0 ? rgbResult[1] : -rgbResult[1];
		
		rgbResult[2] = rgb1[2] - rgb2[2];
		rgbResult[1] = rgbResult[1] > 0 ? rgbResult[1] : -rgbResult[1];
		
		return rgbResult;
	}
	
	private static int[] getRgbArray(int rgb) {
		int[] rgbArray = new int[3];
		rgbArray[0] = (rgb >> 16) & 0xFF;
		rgbArray[1] = (rgb >> 8) & 0xFF;
		rgbArray[2] = rgb & 0xFF;
		
		return rgbArray;
	}
	
	private static char getCharFromLuminance(int luminance) {
		switch (luminance) {
			case 0:
				return '麤';
			case 1:
				return '醤';
			case 2:
				return '目';
			case 3:
				return '区';
			case 4:
				return '王';
			case 5:
				return '干';
			case 6:
				return '三';
			case 7:
				return '十';
			case 8:
				return '二';
			case 9:
				return '一';
			case 10:
				return '＿';
			default:
				return '何';
		}
	}
}