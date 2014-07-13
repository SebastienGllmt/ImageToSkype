import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

public class Main {
	
	private static final String PATH = "res/img.bmp";
	private static final String PREFIX = "<b><font size=\"4\">";
	private static final String SUFFIX = "</font></b>";
	
	private static final Supplier<BlackPixel> BLACK_PIXEL_SUPPLIER = BlackPixel::new;
	private static final Supplier<WhitePixel> WHITE_PIXEL_SUPPLIER = WhitePixel::new;
	private static final Supplier<UnknownPixel> UNKNOWN_PIXEL_SUPPLIER = UnknownPixel::new;
	
	public static void main(String[] args) throws IOException {
		File f = new File(PATH);
		if (!f.exists()) {
			throw new FileNotFoundException(f.getAbsolutePath() + " could not be found");
		}
		
		BufferedImage img = ImageIO.read(f);
		
		Supplier<? extends Pixel> pixelCreator = null;
		
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
				
				System.out.print(pixelCreator.get().getPixel());
			}
			System.out.println();
		}
		System.out.print(SUFFIX);
	}
}

abstract class Pixel {
	public abstract char getPixel();
}

class BlackPixel extends Pixel {
	public char getPixel() {
		// the char that looked the most like a black box while being monosized w/ other Chinese/Japanese symbols
		return '麤';
	}
}

class WhitePixel extends Pixel {
	public char getPixel() {
		// the char that looked the most empty while being monosized w/ other Chinese/Japanese symbols
		return '一';
	}
}

class UnknownPixel extends Pixel {
	public char getPixel() {
		// the char representing a non-black/white pixel
		return '何';
	}
}