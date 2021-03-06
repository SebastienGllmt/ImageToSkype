import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import javax.imageio.ImageIO;

public class ImageRender {
	
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
	
	public static BufferedImage getImage(File f) throws IOException {
		BufferedImage bufferedImage = null;
		try {
			bufferedImage = ImageIO.read(f);
		} catch (IOException e) {
			throw new IOException("IO error while trying to read image at " + f.getAbsolutePath());
		}
		
		return bufferedImage;
	}
	
	public static String render(BufferedImage bufferedImage) {
		// Set up image array
		int count = 0;
		int[][] pixels = getPixelArray(bufferedImage);
		// The size of our text is width*height of the image as every pixel is a char.
		// We also add a height again since there will be *height* amount of \n
		// We also add an extra one for the last line
		StringBuilder imageAsChars = new StringBuilder((1 + bufferedImage.getHeight()) + (bufferedImage.getWidth() * bufferedImage.getHeight()));
		
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
				
				// Increment count
				count++;
				if (count >= MAX_MSG_LENGTH) {
					break imageRender;
				}
			}
		}
		
		imageAsChars.append('\n');
		
		return imageAsChars.toString();
	}
	
	public static String getBestFitColorCode(BufferedImage bufferedImage) {
		// Set up average color
		BigDecimal[] averageHSB = new BigDecimal[3];
		for (int i = 0; i < averageHSB.length; i++) {
			averageHSB[i] = BigDecimal.ZERO;
		}
		long colorPixelCount = 0;
		
		// Calcualte average HSB
		for (int y = 0; y < bufferedImage.getHeight(); y++) {
			for (int x = 0; x < bufferedImage.getWidth(); x++) {
				
				// Avoid pure black/pure white by checking if the pixel is at least some distance away from them
				int[] imageColorPixel = getRgbArray(bufferedImage.getRGB(x, y));
				int[] blackDiff = getColorDifference(imageColorPixel, BLACK);
				int[] whiteDiff = getColorDifference(imageColorPixel, WHITE);
				if ((blackDiff[0] + blackDiff[1] + blackDiff[2]) >= MIN_DIST_FROM_EDGE && (whiteDiff[0] + whiteDiff[1] + whiteDiff[2]) >= MIN_DIST_FROM_EDGE) {
					float[] hsb = new float[3];
					Color.RGBtoHSB(imageColorPixel[0], imageColorPixel[1], imageColorPixel[2], hsb);
					for (int i = 0; i < averageHSB.length; i++) {
						averageHSB[i] = averageHSB[i].add(BigDecimal.valueOf(hsb[i]));
					}
					colorPixelCount++;
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
		return "#" + Integer.toHexString(averageColors[0]) + Integer.toHexString(averageColors[1]) + Integer.toHexString(averageColors[2]);
	}
	
	public static String getFormattedHTML(String content, String colorCode, String size) {
		return String.format(PREFIX, colorCode, size) + content + SUFFIX;
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
		for (int i = 0; i < rgb.length; i++) {
			rgb[i] += colorError[i] * quantError;
			rgb[i] = rgb[i] > 255 ? 255 : rgb[i];
		}
		pixels[y][x] = (rgb[0] << 16) + (rgb[1] << 8) + rgb[2];
		
	}
	
	private static int[] getColorDifference(int[] rgb1, int[] rgb2) {
		int[] rgbResult = new int[3];
		for (int i = 0; i < rgbResult.length; i++) {
			rgbResult[i] = rgb1[i] - rgb2[i];
			rgbResult[i] = rgbResult[i] > 0 ? rgbResult[i] : -rgbResult[i];
		}
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