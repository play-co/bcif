package bcif;

/**
 * <p> Class that converts image formats to and from 24-bit per pixel BMPs. It searches
 * the 'convert' command of the imagemagick toolkit (www.imagemagick.org) and delegates
 * the task to this command. </p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */
 
import java.io.*;
import java.util.StringTokenizer;

public class converter {

	public static int canConvert = -1;
	
	public static boolean canConvert() {
		if (canConvert == -1) {
			verifyConvert();
		}
		return canConvert == 1;		
	}
	
	public static String getExtension(String filename) {
		StringTokenizer st = new StringTokenizer(filename, ".");
		String next = "";
		while (st.hasMoreTokens()) {
			next = st.nextToken();
		}
		return next.toLowerCase();
	}
	
	private static void verifyConvert() {
		try {			
			processRunner pc = new processRunner("convert -version ", new NullOutputStream());
			pc.run();				               
			pc.waitFor();
			int exitv = pc.exitVal();
			if (exitv == 0) {
				canConvert = 1;
			} else {
				canConvert = 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
			canConvert = 0;
		}
	}
	
	public static boolean convert(String initFile, String finalFile) {
		boolean ok = false;
		try {
			System.out.println("Attempting to convert file through ImageMagick's convert tool (www.imagemagick.org)");
			System.out.flush();
			String options = "";
			if (finalFile.toLowerCase().endsWith(".bmp")) {
				options = " -type truecolor -alpha off BMP3:";
			}
			processRunner pc = null;
			if (File.separatorChar == '\\') {
			  pc = new processRunner("convert \"" + initFile + "\" " + options + "\"" + finalFile + "\"");
			} else {
              String[] l = null;
			  if (finalFile.toLowerCase().endsWith(".bmp")) {
                                l = new String[] {"convert", initFile, "-type", "truecolor", "-alpha", "off", "BMP3:" + finalFile};
			  } else {
				l = new String[] {"convert", initFile, finalFile};
                          }
			  pc = new processRunner(l);
			}
			pc.run();
			pc.waitFor();	
			if (pc.exitVal() != 0) {
				System.out.println("Conversion to " + finalFile + " failed !");
				System.out.println("Convert returned error exit code " + pc.exitVal());
				throw new RuntimeException("Conversion with convert tool failed, converted returned error code " + pc.exitVal());
			} else {
				System.out.println("Conversion successfull !");
				ok = true;
			}
		} catch (Exception e) {
			System.out.println("Conversion to " + finalFile + " failed !");
			System.out.println("Launch of convert caused exception " + e);
			throw new RuntimeException("Conversion with convert tool failed: " + e.getMessage());		
		}
		return ok;
	}
	
	
	public static void main(String[] args) {
		convert("lena.png", "lena.bmp");
	}

	public static class NullOutputStream extends OutputStream {
		public void write(int b) { }
		public void write(byte[] b, int s, int o) { }
		public void write(byte[] b) { }
	}
}

