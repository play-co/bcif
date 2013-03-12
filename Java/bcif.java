package bcif;

/**
 * <p>Main interface for bcif viewing, compression and decompression. 
 * Works by command line</p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class bcif {

  private String sourceFile = "";
  private String destFile = "";
  private String errString = "";
  private int filterModule = 8;
  private boolean ok = true;
  private boolean console = false;
  private boolean layerXor = false;
  private int filterStep = 1;
  private int compressing = 0;
  private boolean bmpInfo = false;
  private boolean help = false;
  private boolean version = false;
  private boolean fileVersion = false;
  private boolean hashCode = false;
  private boolean zigzag = true;
  private int filterNum = 0;
  private int colorFilterNum = 0;
  private int filterZoneDim = 0;
  private int colorFilterZoneDim = 0;
  private int lossy = 0;
  private String unUseless = "";
  private String compUseless = "";
  private boolean imageCheck = false;
  private boolean layerCheck = false;
  private boolean view = false;
  private boolean compress = false;
  private boolean decompress = false;
  private boolean stream = true;
  private boolean streamDef = false;
  private boolean actionDef = false;

  /**
   * Builds a new bcif object analizing the list of parameters in input.
   * @param args The list of parameters called by command line
   */

  public bcif(String[] args) {
    String cur = "";
    for (int i = 0; i < args.length; i++) {
      cur = args[i];
      if ( ! cur.startsWith("-")) {
        if (sourceFile.equals("")) { sourceFile = cur; } else
          if (destFile.equals("")) { destFile = cur; } else {
            ok = false; errString += "Overabundant parameter " + cur + "\n";
          }
      } else {
        cur = cur.substring(1, cur.length());
        if (cur.toLowerCase().equals("v") || cur.toLowerCase().equals("view")) {
          view = true;
          actionDef = true;
        } else if (cur.toLowerCase().equals("c") || cur.toLowerCase().equals("compress")) {
          compress = true;
          actionDef = true;
        } else if (cur.toLowerCase().equals("d") || cur.toLowerCase().equals("decompress")) {
          decompress = true;
          actionDef = true;
        } else if (cur.toLowerCase().equals("s") || cur.toLowerCase().equals("stream")) {
          stream = true;
          streamDef = true;
        } else if (cur.toLowerCase().equals("ns") || cur.toLowerCase().equals("nostream")) {
          stream = false;
          streamDef = true;
        } else if (cur.toLowerCase().equals("hc") || cur.toLowerCase().equals("hashcode")) {
          hashCode = true;
        } else if (cur.toLowerCase().equals("h") || cur.toLowerCase().equals("help")) {
          help = true;
        } else {
          ok = false;
          errString += "Unrecognized option: " + cur + "\n";
        }
      }
    }
    if (sourceFile.equals("") && help == false && version == false) {
      ok = false;
      errString += "No source file specified ! Use -h for help \n";
      errString += "This error also occurs if your Java Virtual Machine is too old\n";
      errString += "If you have in fact entered a filename, download last version\n";
      errString += "of Java at www.java.com\n";
    }
  }

  /**
   *  Prints on standard output BCIF information: creator, website and current version
   */
   
  public void displayBcifInt() {
    System.out.println();
    System.out.print  ("    BCIF lossless image compression algorithm version " + bcifEncoder.version() + "." + bcifEncoder.subVersion());
    if (bcifEncoder.beta == 0) { System.out.println(); }
    if (bcifEncoder.beta == 1) { System.out.println(" beta."); }
    if (bcifEncoder.beta == 2) { System.out.println(" alpha."); }
    if (bcifEncoder.beta > 2) { System.out.println(" unstable pre-alpha, level " + bcifEncoder.beta); }
    System.out.println("    By Stefano Brocchi (stefano.brocchi@researchandtechnology.net) ");
    System.out.println("    Website: www.researchandtechnology.net/bcif ");
    System.out.println();
  }

  /**
   * Displays on standard output help information
   */

  public void displayBcifHelp() {
    System.out.println();
	System.out.println("BCIF syntax: \n ");
	System.out.println("  bcif inputfile [-c|-d|-v] [outputfile] [-h] [-s|-ns] \n");
	System.out.println("Parameters: \n");
	System.out.println("-c         : Compress   (BMP -> BCIF). Default if input extension is BMP.");
	System.out.println("-d         : Decompress (BCIF -> BMP)");
	System.out.println("-v         : View       (BCIF -> viewer). Default if input extension is BCIF.");
	System.out.println("-s         : Stream output when decompressing (faster, takes more memory)");
	System.out.println("-ns        : Don't stream output when decompressing (requires very low memory)");
	System.out.println("-h         : Print help information");
	System.out.println("inputfile  : The file to be compressed, decompressed or viewed");
	System.out.println("outputfile : The destination file. If not specified, default is the same ");
	System.out.println("             filename of inputfile with an appropriate extension. If already ");
	System.out.println("             existing, the outputfile is overwritten without prompting.");
    System.out.println();
  }

  /**
   * Executes the commands given to the constructor. If an error occurred in processing options, an error
   * message is printed on standard output and no other operation is done.
   */

  public void exe() {
    displayBcifInt();
    if (help) { displayBcifHelp(); }
    if (hashCode) {
      bcifEncoder.setHashPrint(true);
      if (stream || view) {
        System.out.println("Hash code is only avaliable during compression or non-streamed decompression.");
      }
    }
    if (! ok) {
      System.err.println(errString);
    } else {
      if (sourceFile.equals("")) {
        if (! help) {
          errString += "No source file defined !\n";
          errString += "This error can also be caused by an unappropriate JVM.\n";
          errString += "In this case, please update your JVM from www.java.com.\n";
        }
        ok = false;
      }
      if (actionDef) {
        if ((compress && decompress) ||
            (compress && view) ||
            (decompress && view)) {
         errString += "Actions defined in contradiction !\n";
         ok = false;
        }
      } else if ( ! sourceFile.equals("")) {
        if (sourceFile.toLowerCase().endsWith(".bmp")) {
          compress = true;
          System.out.println("Action not defined, guessing: compression");
        } else if (sourceFile.toLowerCase().endsWith(".bcif")) {
          view = true;
          System.out.println("Action not defined, guessing: view");
        }
      }
      if (! ok) {
        System.err.println(errString);
      } else {
        if (view) {
          if (stream) {
            bcifEncoder.decompressBcifToStreamableViewer(sourceFile);
          } else {
            bcifEncoder.decompressBcifToViewer(sourceFile);
          }
        }
        if (decompress) {
          if (destFile.equals("")) {
            destFile = sourceFile + ".bmp";
            System.out.println("Output file not defined, decompressing to " + destFile);
          }
          if (stream) {
            bcifEncoder.decompressBcifToBmpFile(sourceFile, destFile);
          } else {
            BmpImage image = bcifEncoder.decompressBcifToBmpObj(sourceFile);
            image.writeImage(destFile);
          }
        }
        if (compress) {
          if (destFile.equals("")) {
            destFile = sourceFile + ".bcif"; // + bcifEncoder.subversion();
            System.out.println("Output file not defined, compressing to " + destFile);
          }
          if (streamDef) {
            System.out.println("Streaming option cannot be used during compression, and will be ignored.");
          }
          bcifEncoder.compress(sourceFile, destFile);
        }
      }
    }
  }

  private void checkUnc(String opt) {
    unUseless += "Useless option during decompression: -" + opt + "\n";
  }

  private void checkComp(String opt) {
    compUseless += "Useless option during compression: -" + opt + "\n";
  }

  /**
   * Just as the standard main, but does not capture Exceptions
   * @param args The arguments of the pcf command
   */
   
  public static void nonStandaloneMain(String[] args) {
    bcif Pcf = new bcif(args);
   	Pcf.exe();
  }
  
  /**
   * Executes compression or decompression based upon the command options.
   * See class description for complete usage instructions
   * @param args The arguments of the pcf command
   */

  public static void main(String[] args) {
  	try {
	    bcif Pcf = new bcif(args);
    	Pcf.exe();
    } catch (Exception e) {
    	System.err.println("Program terminated abnormally.");
    }
  }
}
