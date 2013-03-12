package bcif;

/**
 * Image viewer for BCIF files
 * @author Stefano Brocchi
 * @version 1.0 beta
 */
 
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;

/**
 * <p> Class used for visualizzation of bcif files. At execution, file is uncompressed and showed in a JPanel. If filesize is
 * bigger than the screen resolution, it will be resized to fit screen. </p>
 * @author Stefano Brocchi
 */

public class bcifViewer extends JPanel
{
    private Image image;
    private Rectangle r;
    private String fileName = "";
    private int width = 0;
    private int height = 0;
    private float scale = 0;
    private int maxResX = 0;
    private int maxResY = 0;
    private boolean ok = false;
    MemoryImageSource source = null;

    /**
     * Crates a bcifViewer object bounded to a maximum dimension from a
     * compressed file. If image is too big, it will be resized
     * @param fileName The file to view
     * @param maxx The maximum width of the window
     * @param maxy The maximum height of the window
     */

    public bcifViewer(String fileName, int maxx, int maxy) {
        this.fileName = fileName;
		maxResX = maxx;
		maxResY = maxy;
        init();
		loadImage();
    }

    /**
     * Crates a bcifViewer object bounded to a maximum dimension from a
     * compressed image. If image is too big, it will be resized
     * @param source The image to view
     * @param maxx The maximum width of the window
     * @param maxy The maximum height of the window
     */

    public bcifViewer(BmpImage source, int maxx, int maxy) {
        this.fileName = "";
        maxResX = maxx;
        maxResY = maxy;
        init();
        loadImage(source);
    }
    
    public bcifViewer(BmpImage source) {
        this.fileName = "";
		Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
		int screenResX = (int)screenDim.getWidth();
		int screenResY = (int)screenDim.getHeight();        
        maxResX = screenResX - 50;
        maxResY = screenResY - 50;
        init();
        loadImage(source);
    }    

    public bcifViewer(int[] source, int w, int h, int maxx, int maxy) {
        this.fileName = "";
        width = w;
        height = h;
        maxResX = maxx;
        maxResY = maxy;
        init();
        loadImage(source, w, h);
    }

  /**
   * Repaints the specified image
   * @param g The target graphic
   */

    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                          RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      AffineTransform at = AffineTransform.getTranslateInstance(r.x, r.y);
      at.scale(scale, scale);
      g2.drawImage(image, at, this);
    }

    private void init()
    {
        int w = getWidth();
        int h = getHeight();
        r = new Rectangle(width, height);
        r.x = 0;
        r.y = 0;
    }

    /**
     * Converts an image represented as a byteMatrix in an image object.
     * @param im The source image
     * @return The output image
     */

    public Image getJavaImage(byteMatrix im) {

      int [] is = new int[width * height];
      int c = 0;
      //im.firstVal();
      for (int i2 = height - 1; i2  >= 0; i2 --) {
          for (int i = 0; i < width; i ++) {
            im.setPoint(i, i2, 0);
            int red = im.getCurVal();
            int green = im.getCurVal(1);
            int blue = im.getCurVal(2);
            if (red < 0) { red += 256; }
            if (green < 0) { green += 256; }
            if (blue < 0) { blue += 256; }
            is[c] = (255 << 24) | (blue << 16) | (green << 8) | (red);
            c++;
        }
      }
      return createImage( new MemoryImageSource(width, height, is, 0, width));
    }

    private void loadImage() {
      Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
      try {
        BmpImage bimage = bcifEncoder.decompressBcifToBmpObj(fileName);
        loadImage(bimage);
        ok = (bimage != null);
      } catch (Exception e) {
        System.err.println("Error occured during image loading: ");
        System.err.println(e.getMessage());
      }
    }

    private void loadImage(BmpImage bimage) {
      if (bimage != null) {
        try {
          width = (int) bimage.getWidth();
          height = (int) bimage.getHeight(); ;
          scale = 1;
          while ( (width * scale > maxResX && maxResX > 0) ||
                 (maxResY > 0 && height * scale > maxResY)) {
            scale = scale * .9F;
          }
          if (scale < 1) {
            System.out.println("Image scaled to " + (int) (scale * 100) +
                               "%, original resolution is " + width + " x " +
                               height);
          }
          else {
            System.out.println("Image fully represented, resolution is " + width +
                               " x " +
                               height);
          }
          image = getJavaImage(bimage.getImage());
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    private void loadImage(int[] imageArray, int width, int height) {
      if (imageArray != null) {
        try {
          scale = 1;
          while ( (width * scale > maxResX && maxResX > 0) ||
                 (maxResY > 0 && height * scale > maxResY)) {
            scale = scale * .9F;
          }
          if (scale < 1) {
            System.out.println("Image scaled to " + (int) (scale * 100) +
                               "%, original resolution is " + width + " x " + height);
          } else {
            System.out.println("Image fully represented, resolution is " + width + " x " + height);
          }
          source = new MemoryImageSource(width, height, imageArray, 0, width);
          source.setAnimated(true);
          image = createImage(source);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    private JPanel getUIPanel()
    {
        JPanel panel = new JPanel();
        return panel;
    }

    /**
     * Returns the original image width
     * @return The original image width
     */

    public int getImageWidth() {
      return width;
    }

    public MemoryImageSource getSource() {
      return source;
    }

    public Image getImage() {
      return image;
    }

    /**
     * Returns the original image height
     * @return The original image height
     */

    public int getImageHeight() {
      return height;
    }

    /**
     * Return the scale used to display the image
     * @return The image scale
     */

    public float getScale() {
	return scale;
    }

    /**
     * Returns the filename of the image
     * @return The filename of the image
     */

    public String getFileName() {
      return fileName;
    }

    private static int getXBarDim() {
	if (File.separator.equals("\\")) { return 6; }
	else { return 6; }
    }

    private static int getYBarDim() {
	if (File.separator.equals("\\")) { return 32; }
	else { return 24; }
    }

    /**
     * Opens a window to view the image compressed in the bcif file given as argument
     * @param args The first argument must be the file to be viewed, and must bge a bcif file. Other arguments
     * will be ignored
     */

    public static void main(String[] args)
    {
        int yBarDim = getYBarDim();
        int xBarDim = getXBarDim();
        int yMargin = 60;
		Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
		int screenResX = (int)screenDim.getWidth();
		int screenResY = (int)screenDim.getHeight();
		int maxx = screenResX - xBarDim;
		int maxy = screenResY - yBarDim - yMargin;
        bcifViewer pv = new bcifViewer(args[0], maxx, maxy);
        if (pv.ok) {
          JFrame f = new JFrame();
          f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          f.setTitle("Bcif image viewer - " + pv.getFileName());
          f.getContentPane().add(pv);
          float scale = pv.getScale();
          int dimx = (int) Math.ceil(scale * pv.getImageWidth() + xBarDim);
          int dimy = (int) Math.ceil(scale * pv.getImageHeight() + yBarDim);
          f.setSize(dimx, dimy);
          f.setLocation( (screenResX - dimx) / 2, (screenResY - dimy) / 2);
          f.setVisible(true);
        }
    }

    /**
     * Opens a window to view the image given as argument
     * @param source The image to view
     */

    public static void main(BmpImage source) {
      main(source, "Untitled");
    }

    /**
     * Opens a window to view the image given as argument specifying
     * an image title
     * @param source The image to view
     * @param title The name of the image
     */

    public static void main(BmpImage source, String title) {
        int yBarDim = getYBarDim(); //32;
        int xBarDim = getXBarDim(); //6;
        int yMargin = 60;
        Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        int screenResX = (int)screenDim.getWidth();
        int screenResY = (int)screenDim.getHeight();
        int maxx = screenResX - xBarDim;
        int maxy = screenResY - yBarDim - yMargin;
        bcifViewer pv = new bcifViewer(source, maxx, maxy);
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setTitle("Bcif image viewer - " + title);
        f.getContentPane().add(pv);
        float scale = pv.getScale();
        int dimx = (int)Math.ceil(scale * pv.getImageWidth() + xBarDim);
        int dimy = (int)Math.ceil(scale * pv.getImageHeight() + yBarDim);
        f.setSize(dimx, dimy);
        f.setLocation((screenResX - dimx) / 2, (screenResY - dimy) / 2);
        f.setVisible(true);
      }

      public static bcifViewer view(int[] source, int w, int h) {
        return view(source, w, h, "Untitled");
      }

      public static bcifViewer view(int[] source, int w, int h, String title) {
          int yBarDim = getYBarDim();
          int xBarDim = getXBarDim();
          int yMargin = 60;
          Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
          int screenResX = (int)screenDim.getWidth();
          int screenResY = (int)screenDim.getHeight();
          int maxx = screenResX - xBarDim;
          int maxy = screenResY - yBarDim - yMargin;
          bcifViewer pv = new bcifViewer(source, w, h, maxx, maxy);
          JFrame f = new JFrame();
          f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          f.setTitle("Bcif image viewer - " + title);
          f.getContentPane().add(pv);
          float scale = pv.getScale();
          int dimx = (int)Math.ceil(scale * pv.getImageWidth() + xBarDim);
          int dimy = (int)Math.ceil(scale * pv.getImageHeight() + yBarDim);
          f.setSize(dimx, dimy);
          f.setLocation((screenResX - dimx) / 2, (screenResY - dimy) / 2);
          f.setVisible(true);
          return pv;
        }
}
