import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.IntSupplier;

import javax.imageio.ImageIO;

public class Main {
	
	private static final String PATH = "res/img.bmp";
	private static final String PREFIX = "<b><font size=\"4\">";
	private static final String SUFFIX = "</font></b>";
	
	/**
	 * Char that looked the most like a black box while being monosized w/ other Chinese/Japanese symbols
	 */
	private static final IntSupplier BLACK_PIXEL_SUPPLIER = () -> '麤';
	/**
	 * Char that looked the most empty while being monosized w/ other Chinese/Japanese symbols
	 */
	private static final IntSupplier WHITE_PIXEL_SUPPLIER = () -> '一';
	/**
	 * Char representing a non-black/white pixel
	 */
	private static final IntSupplier UNKNOWN_PIXEL_SUPPLIER = () -> '何';
	
	public static void main(String[] args) throws IOException {
		File f = new File(PATH);
		if (!f.exists()) {
			throw new FileNotFoundException(f.getAbsolutePath() + " could not be found");
		}
		
		BufferedImage img = ImageIO.read(f);
		
		IntSupplier pixelCreator = null;
		
		System.out.println(PREFIX);
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int color = img.getRGB(x, y);
				
				if (color == Color.BLACK.getRGB()) {
					pixelCreator = BLACK_PIXEL_SUPPLIER;
				} else if (color == Color.WHITE.getRGB()) {
					pixelCreator = WHITE_PIXEL_SUPPLIER;
				} else {
					pixelCreator = UNKNOWN_PIXEL_SUPPLIER;
				}
				
				System.out.print((char) pixelCreator.getAsInt());
			}
			System.out.println();
		}
		System.out.print(SUFFIX);
	}
}