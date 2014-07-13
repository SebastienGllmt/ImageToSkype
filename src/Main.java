import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

public class Main {
	
	private static final String PATH = "res/img.bmp";
	private static final String prefix = "<b><font size=\"4\">";
	private static final String suffix = "</font></b>";
	
	private static final Supplier<Pixel> blackPixelSupplier = BlackPixel::new;
	private static final Supplier<Pixel> whitePixelSupplier = WhitePixel::new;
	private static final Supplier<Pixel> unknownPixelSupplier = UnknownPixel::new;
	
	public static void main(String[] args) throws IOException {
		File f = new File(PATH);
		if (!f.exists()) {
			throw new FileNotFoundException(f.getAbsolutePath() + " could not be found");
		}
		
		BufferedImage img = ImageIO.read(f);
		
		Supplier<Pixel> pixelCreator = null;
		
		System.out.println(prefix);
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int color = img.getRGB(x, y);
				
				if (color == Color.BLACK.getRGB()) {
					pixelCreator = blackPixelSupplier;
				} else if (color == Color.WHITE.getRGB()) {
					pixelCreator = whitePixelSupplier;
				} else {
					pixelCreator = unknownPixelSupplier;
				}
				
				System.out.print(pixelCreator.get().getPixel());
			}
			System.out.println();
		}
		System.out.print(suffix);
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