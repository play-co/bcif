package bcif;

/**
 * <p>Applet allowing the visualization of BCIF images on a website</p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */
 
import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.awt.image.*;
import java.net.*;
import java.awt.geom.AffineTransform;
import java.io.*;

/**
 * <p> This class implements an applet for bcif image decompression. This permits
 * the displaying of pcf files in any Java enabled browser without any other
 * client side compatibility. The URL containing the compressed image must be
 * specified in the applet parameter image.</p>
 * @author Stefano Brocchi
 */

public class bcifapplet extends Applet {

  private int timeout = 5000;
  private boolean isStandalone = false;
  private String fileUrl = null;
  private URL source = null;
  private String errorBaseString = "Error during image loading: ";
  private String errorString = null;
  private Image image = null;
  private int imageWidth = 0;
  private int imageHeight = 0;
  private int[] imageArray;
  private String copyright = "Bcif decompressing applet version " 
                             + bcifEncoder.version() + "." + bcifEncoder.subVersion() +
                               (bcifEncoder.beta == 1 ? " beta" : "") +
	                           (bcifEncoder.beta == 2 ? " alpha" : "") +
                               ", author Stefano Brocchi";
   
  public void loadImage(int parts) throws IOException {
  	imageArray = new int[imageWidth * imageHeight];
  	MemoryImageSource misource = new MemoryImageSource(
  		                         imageWidth, imageHeight, imageArray, 0, imageWidth);
  	misource.setAnimated(true);
  	image = createImage(misource);
  	decompress(parts, misource, imageArray);
  }
  
  public void init() {
  	try {
  	  repaint();
	  if (image == null && errorString == null) {
    	int parts = 0;    	
      	System.out.println(copyright);
        fileUrl = getParameter("image");
        imageWidth = Integer.parseInt(getParameter("width"));
        imageHeight = Integer.parseInt(getParameter("height"));
        String partsString = getParameter("parts");
        if (partsString != null) {
          parts = Integer.parseInt(partsString);
        }
        if ( ! fileUrl.startsWith("http:/") & ! fileUrl.startsWith("file:/")) {
          fileUrl = getCodeBase().toString() + getParameter("image");
        }
        source = new URL(fileUrl);
        loadImage(parts);
	  }      
    } catch (MalformedURLException ex) {
      errorString = "Malformed url: " + fileUrl;
    } catch (Exception e) {
      errorString = "There was a problem loading the image:   \n" + e.toString();
      e.printStackTrace();
    } finally {
      repaint();    
      imageArray = null; // For gc    
    }
  }

  public void decompress(int parts, MemoryImageSource misource,
                         int[] imageArray) throws IOException {  
  	  HttpURLConnection ucon = (HttpURLConnection)source.openConnection();
  	  ucon.setConnectTimeout(timeout); // From JDK 1.5
  	  if (ucon.getResponseCode() / 100 != 2) {
  	  	System.out.println("ERROR: server returned response code " + ucon.getResponseCode() +
  	  	                   ": " + ucon.getResponseMessage());
  	  	throw new FileNotFoundException();
  	  } else {
  	  	System.out.println("Image found on server, decompressing...");
  	  }
      InputStream in = new BufferedInputStream(ucon.getInputStream());
      bitReader br = new bitReader(in);
      streamableArrayBmpWriter image = new streamableArrayBmpWriter();
      image.setOut(imageArray);
      image.setExternal(misource);
      image.setView(true);
      image.setParts(parts);
      image.setTitle(source.toString());
      bcifEncoder.decompressBcifFromReaderToWriter(br, image);
      in.close();
      image.close();
  }  
  
  /**
   * Paints the image on the applet area itself; if an error occurred a message
   * may be displayed
   * @param g The destination graphic object
   */
     
  public void paint(Graphics g) {
    if (errorString != null) {
      g.drawString(errorBaseString, 40, 80);
      g.drawString(errorString, 40, 120);
    }
    if (image != null) {
      super.paint(g);
      g.drawImage(image, 0, 0, this);
    }
  }	
}