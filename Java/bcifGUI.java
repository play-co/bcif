package bcif;

/**
 * GUI for the comrpession and decompression of BCIF files, and for their visualization
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
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.MouseInputAdapter;


public class bcifGUI extends JFrame {
	
	private boolean canConvert = false;
	private JLabel foot = new JLabel("BCIF encoder - version " +
	                                 bcifEncoder.version + "." +
	                                 bcifEncoder.subVersion + 
	                                 (bcifEncoder.beta == 1 ? " beta" : "") +
	                                 (bcifEncoder.beta == 2 ? " alpha" : "") +
	                                 ". Created by Stefano Brocchi.", SwingConstants.CENTER);
	private JLabel foot1 = new JLabel("Submit feedback, bug reports, requests or opinions to", SwingConstants.CENTER);
	private JLabel foot2 = new JLabel("stefano.brocchi@researchandtechnology.net", SwingConstants.CENTER);
	private JLabel foot3 = new JLabel("Website: http://www.researchandtechnology.net/bcif/", SwingConstants.CENTER);
	private String curPath = ".";
	private myThread mt = null;
	private ButtonGroup actionButtons = new ButtonGroup();
	private ButtonGroup streamButtons = new ButtonGroup();
	private Container actions = new Container();
	private JLabel actionlab = new JLabel("Action");
	private JRadioButton actionguess = new JRadioButton("Guess action from input file", true);
	private JRadioButton actioncomp = new JRadioButton("Encode (ex. BMP to BCIF)", false);
	private JRadioButton actiondecomp = new JRadioButton("Decode (ex. BCIF to BMP)", false);
	private myOutput my;
	private JLabel op = new JLabel("", SwingConstants.CENTER);
	private JTextArea txtout = new JTextArea(25,60);
	private JScrollPane txtoutscroll;
	private int dimx = 800;
	private int dimy = 650;
	private JButton encode = new JButton("Encode");
	private JTextArea inputfiletxt = new JTextArea(2, 20);
	private JLabel inputfilelab = new JLabel("Input file");
	private JButton openfile = new JButton("Browse");
	private JButton viewfile = new JButton("View image");
	private JLabel draganddrop = new JLabel("or drag and drop file");
	private Container outputdir = new Container();
	private JTextArea outputdirtext = new JTextArea(2, 20);
	private JScrollPane outputfilescroller = new JScrollPane(outputdirtext);
	private JScrollPane inputfilescroller = new JScrollPane(inputfiletxt);	
	private JButton openoutdir = new JButton("Browse");
	private JLabel outputdirlab = new JLabel("Output file");
	private JCheckBox outputdircb = new JCheckBox("Use the same directory of the input file");
	private Container decodingOptions = new Container();
	private JLabel decoptlab = new JLabel("Decoding options");
	private JRadioButton nostream = new JRadioButton("Do not stream output", false);	
	private JRadioButton stream = new JRadioButton("Stream output ", true);
	
	bcifGUI() {
		Font mainfont = new Font("Dialog", Font.BOLD, 12); 
		encode.setFont(mainfont);
		outputdirlab.setFont(mainfont);
		inputfilelab.setFont(mainfont);
		openfile.setFont(mainfont);
		viewfile.setFont(mainfont);
		openoutdir.setFont(mainfont);
		Container cp = getContentPane();
		FlowLayout fl = new FlowLayout();
		cp.setLayout(null);//fl);							
		setSize(dimx, dimy);
		cp.add(inputfilelab);		
		cp.add(inputfilescroller);
		openfile.setSize(20, 70);		
		cp.add(openfile);
		cp.add(viewfile);
		//cp.add(draganddrop);	
		outputdir.setLayout(new FlowLayout());
		cp.add(outputdirlab);		
		cp.add(outputfilescroller);		
		//outputdirtext.setText("c:\\");
		cp.add(openoutdir);
		//outputdir.add(outputdircb);
		outputdir.setSize(300, 200);		
		add(outputdir);
		//outputdirtext.setLineWrap(true);
		decodingOptions.setLayout(null);
		decodingOptions.add(decoptlab);
		decodingOptions.add(nostream);
		decodingOptions.add(stream);
		actions.add(actionlab);
		actions.add(actionguess);		
		actions.add(actioncomp);
		actions.add(actiondecomp);
		actionButtons.add(actionguess);
		actionButtons.add(actioncomp);
		actionButtons.add(actiondecomp);
		streamButtons.add(stream);
		streamButtons.add(nostream);
		encode.addMouseListener(new encodePressed());
		viewfile.addMouseListener(new viewPressed());
		cp.add(decodingOptions);		
		cp.add(op);
		cp.add(encode);		
		cp.add(actions);
		cp.add(op);
		my = new myOutput(txtout);
		System.setOut(new PrintStream(my));
		System.setErr(new PrintStream(my));	
		txtoutscroll = new JScrollPane(txtout);	
		txtout.setEditable(false);		
		cp.add(txtoutscroll);
		cp.add(inputfilescroller);	
		cp.add(foot);	
		cp.add(foot1);	
		cp.add(foot2);
		cp.add(foot3);		
		Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
		int screenResX = (int)screenDim.getWidth();
		int screenResY = (int)screenDim.getHeight();         
        setLocation( (screenResX - dimx) / 2, (screenResY - dimy) / 2);			
		setTitle("BCIF encoder version " + bcifEncoder.version() + "." 
		                                 + bcifEncoder.subversion() 
		                                 + (bcifEncoder.beta == 1 ? " beta" : "")
		                                 + (bcifEncoder.beta == 2 ? " alpha" : ""));
		inputfilelab.setBounds(90, 20, 110, 18);
		//inputfiletxt.setBounds(200, 10, 300, 36);
		openfile.setBounds(490, 15, 100, 25);
		viewfile.setBounds(610, 15, 120, 25);
		outputdirlab.setBounds(90, 65, 110, 18);
		//outputdirtext.setBounds(200, 50, 300, 18);
		outputfilescroller.setBounds(170, 55, 300, 36);		
		outputfilescroller.setHorizontalScrollBarPolicy(
			 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		outputfilescroller.setVerticalScrollBarPolicy(
			 JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		inputfilescroller.setBounds(170, 10, 300, 36);
		inputfilescroller.setHorizontalScrollBarPolicy(
			 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		inputfilescroller.setVerticalScrollBarPolicy(
			 JScrollPane.VERTICAL_SCROLLBAR_NEVER);			 
		openoutdir.setBounds(490, 60, 100, 25);		
		actions.setBounds(20, 90, 300, 300);
		actionlab.setBounds(10, 10, 300, 20);
		actionguess.setBounds(10, 30, 300, 20);
		actioncomp.setBounds(10, 50, 300, 20);
		actiondecomp.setBounds(10, 70, 500, 20);
		decodingOptions.setBounds(350, 90, 520, 300);
		//decodingOptions.setBackground(Color.red);
		decoptlab.setBounds(10, 10, 500, 20);
		stream.setBounds(10, 30, 500, 20);
		nostream.setBounds(10, 50, 500, 20);
		encode.setBounds(650, 180, 100, 25);
		txtoutscroll.setBounds(20, 240, 760, 300);
		foot.setBounds(50, 540, 700, 20);
		foot1.setBounds(50, 555, 700, 20);
		foot2.setBounds(50, 570, 700, 20);		
		foot3.setBounds(50, 585, 700, 20);		
		op.setBounds(200, 210, 400, 30);
		op.setFont(new Font("Times new roman", 0, 20));
		openfile.addMouseListener(new browseInput());
		openoutdir.addMouseListener(new browseOutput());
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setVisible(true);
		System.out.println("BCIF graphical user interface started.");
		if (converter.canConvert()) {
			System.out.println("Detected ImageMagick's convert command.");
			System.out.println("Conversion from image formats different than BMP will be attempted through this tool.");
		} else {
			System.out.println("ImageMagick's convert command not found.");
			System.out.println("Download the ImageMagick software (www.imagemagick.org) to be able to convert also form other formats than BMP.");
		}
	}
	
	public void browseInput() {
		try {
			JFileChooser chooser = new JFileChooser(curPath);
			if (converter.canConvert()) {
				chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
					public boolean accept(File f) {
						return (f.getName().toLowerCase().matches(
							    ".*\\.(bmp|png|ppm|pgm|jp2|raw|tiff|tif|pbm|pnm)") ||
						        f.getName().toLowerCase().matches(".*\\.bcif[0-9]*") ||
						        f.isDirectory());
					}
					public String getDescription() {
						return "Main lossless image formats";
					}
				});				
			} else {
				chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
					public boolean accept(File f) {
						return (f.getName().toLowerCase().matches(".*\\.bmp") ||
						        f.getName().toLowerCase().matches(".*\\.bcif[0-9]*") ||
						        f.isDirectory());
					}
					public String getDescription() {
						return "BCIF or BMP files";
					}
				});
			}
    		int returnVal = chooser.showOpenDialog(this);
    		if(returnVal == JFileChooser.APPROVE_OPTION) {
				inputfiletxt.setText(chooser.getSelectedFile().
			                      getCanonicalFile().getPath());
			    String sfile = inputfiletxt.getText();
			    inputfiletxt.setCaretPosition(sfile.length()); 
			    if (! sfile.matches(".*\\.bcif[0-9]*")) {
			    	outputdirtext.setText(sfile.substring(
			    		                  0, sfile.length() - 
			    		                  converter.getExtension(sfile).length() - 1) + 
			    	                      ".bcif" /*+ bcifEncoder.version() + 
			    	                      bcifEncoder.subversion()*/);         
					outputdirtext.setCaretPosition(outputdirtext.getText().length());			    	                      
			    } else {
			    	outputdirtext.setText(sfile + ".bmp");    			    	
					outputdirtext.setCaretPosition(outputdirtext.getText().length());			    	
			    }
    			curPath = chooser.getSelectedFile().getPath();			    
    		}	
    	} catch (IOException e) {
    		reportError(e);
    	}	
	}
	
	public void reportError(Exception e) {
		JOptionPane.showMessageDialog(this, "An error has occurred: \n" + e.getMessage());
		System.err.println("Showing error details: ");
		e.printStackTrace();
		System.err.println("End of error details. ");
	}
	
	public void browseOutput() {
		try {
			JFileChooser chooser = new JFileChooser(curPath);
    		int returnVal = chooser.showOpenDialog(this);
    		if(returnVal == JFileChooser.APPROVE_OPTION) {
				outputdirtext.setText(chooser.getSelectedFile().
			                      getCanonicalFile().getPath());
			    outputdirtext.setCaretPosition(outputdirtext.getText().length());                  
    			curPath = chooser.getSelectedFile().getPath();			    
    		}	
    	} catch (IOException e) {
    		reportError(e);
    	}	
	}	
	
	public void encode() {
		printCommandSeparator();
		String[] options = new String[4];
		boolean encode = false;
		String inputFile = inputfiletxt.getText().trim();
		String outputFile = outputdirtext.getText().trim();
		boolean ok = true;
		boolean streamfile = false;
		String errorMessage = "";
		File tempFile = null;
		myConverter myconv = null;
		if (actionguess.isSelected()) {
			if (outputFile.toLowerCase().matches(".*\\.bcif[0-9]*")) {
				encode = true;
			} else if (inputFile.toLowerCase().matches(".*\\.bcif[0-9]*")) {
				encode = false;
			} else {
				ok = false;
				errorMessage += "Cannot guess action for input file " + inputFile;
				errorMessage += "\nThere seems to be no specified BCIF file in input or output";
			}
		} else if (actioncomp.isSelected()) {
			encode = true;
		} else {
			encode = false;
		}
		if (encode) {
			String inext = converter.getExtension(inputFile);
			if (! inext.equals("bmp")) {
				if (! converter.canConvert()) {
					ok = false;
					errorMessage += "Input file extension " + inext + " unrecognized !";
					errorMessage += "\nBCIF encoder supports basically only BMP and BCIF files";
					errorMessage += "\nYou can installing ImageMagick (www.imagemagick.org) ";
					errorMessage += "\nand make the converty command available to enable the support";
					errorMessage += "\nof many other image formats.";
				} else {
					try {
						tempFile = File.createTempFile("bcifconverter", ".bmp");
						op.setText("Converting file to BMP");
						repaint();
						myconv = new myConverter(inputFile, tempFile.getCanonicalFile().toString());
						//converter.convert(inputFile, tempFile.getCanonicalFile().toString());
						inputFile = tempFile.getCanonicalFile().toString();
					} catch (Exception e) {
						ok = false;
						errorMessage += "Error during the attempt to create temporary file";
						errorMessage += "\nfor intermediate conversion to bmp.";
					}
				}
			}
		}
		String finalFile = null;
		if (! encode) {
			String inext = converter.getExtension(outputFile);
			if (! inext.equals("bmp")) {
				if (! converter.canConvert()) {
					ok = false;
					errorMessage += "Output file extension " + inext + " unrecognized !";
					errorMessage += "\nBCIF encoder supports basically only BMP and BCIF files";
					errorMessage += "\nYou can installing ImageMagick (www.imagemagick.org) ";
					errorMessage += "\nand make the converty command available to enable the support";
					errorMessage += "\nof many other image formats.";
				} else {
					try {
						tempFile = File.createTempFile("bcifconverter", ".bmp");
						finalFile = outputFile;
						outputFile = tempFile.getCanonicalFile().toString();
					} catch (Exception e) {
						ok = false;
						errorMessage += "Error during the attempt to create temporary file";
						errorMessage += "\nfor intermediate conversion to bmp.";
					}
				}
			}
		}		
		if (stream.isSelected()) {
			streamfile = true;
		}
		if (mt != null && ! mt.done()) {
			ok = false;
			errorMessage += "Already processing a file, please wait for the computation to finish";
		}
		options[0] = encode ? "-c" : "-d";
		if (! encode) {
			options[1] = streamfile ? "-stream" : "-nostream";
		} else {
			options[1] = "";
		}
		options[2] = inputFile;
		options[3] = outputFile;
		if (! ok) {
			JOptionPane.showMessageDialog(this, errorMessage);
			if (mt == null || mt.done()) {
				op.setText("There were errors during encoding");
			}
		} else {
			if (myconv != null) {
				myconv.start();
			}
			mt = new myThread(options, op);			
			mt.setGUI(this);
			mt.setFinalFile(finalFile);	
			mt.setTempFile(tempFile);
			mt.setActiveConverter(myconv);				
			mt.start();
		}
		
	}
	
	public void repaint() {
		System.out.flush();
		super.repaint();
	}
	
	public void viewImage() {
	  try {
		printCommandSeparator();
		String infile = inputfiletxt.getText();
		if (infile.toLowerCase().endsWith(".bmp")) {
			BmpImage img = new BmpImage(infile);
			bcifViewer.main(img, (new File(infile)).getName());
		} else if (infile.toLowerCase().matches(".*\\.bcif[0-9]*")) {
			if (stream.isSelected()) {
				bcifEncoder.decompressBcifToStreamableViewer(infile);
			} else {
				bcifEncoder.decompressBcifToViewer(infile);
			}
		} else if (infile == null || infile.equals("")) {
			JOptionPane.showMessageDialog(this, "No input file specified !");
		} else if (! converter.canConvert()) {
			JOptionPane.showMessageDialog(this, "Unrecognized filetype for file: \n" + infile);
		} else {
			try {
				File tf = File.createTempFile("bcifconverter", ".bmp");
				repaint();
				boolean cres = converter.convert(infile, tf.getCanonicalFile().toString());
				if (cres) {
					BmpImage img = new BmpImage(tf.getCanonicalFile().toString());
					bcifViewer.main(img, (new File(infile)).getName());			
				} else {
					String ermsg = "Error: \nConversion from non-BMP file failed.";
					ermsg += "\nPossible cause: imagemagick must be at least of version 6.3.6-9";
					JOptionPane.showMessageDialog(this, ermsg);
				}
			} catch	(Exception e) {
				String ermsg = "Error in opening file " + infile + ": \n" + e.getMessage();				
				JOptionPane.showMessageDialog(this, ermsg);				
			}
		}	
	  }	catch (Exception e) {
		String ermsg = "Error in image visualization: \n" + e.getMessage();				
		JOptionPane.showMessageDialog(this, ermsg);			  	
	  }
	}
	
	public void printCommandSeparator() {
		System.out.println();
		for (int i = 0; i < 100; i++) {
			System.out.print("_");
		}
		System.out.println("\n");
	}
	
	public static void main(String[] args) {
		bcifGUI bg = new bcifGUI();
	}
	
	public class myThread extends Thread {
		
		private JLabel msg = null;
		private String[] options;
		private boolean done = false;
		private String finalFile = null;
		private File tempFile = null;
		private bcifGUI gui;
		private myConverter conv;
		private String convertErrorMsg = "Conversion with convert tool failed.";
		
		myThread(String[] opt, JLabel m) {
			msg = m;
			options = opt;
		}
		
		public void setActiveConverter(myConverter myconv) {
			this.conv = myconv;
		}
		
		public void setGUI(bcifGUI g) {
			gui = g;
		}
		
		public boolean done() {
			return done;
		}
		
		public void setTempFile(File tf) {
			tempFile = tf;
		}
				
		public void setFinalFile(String ff) {
			finalFile = ff;
		}
		
		public void run() {
			try {
				if (conv != null) { 					
					conv.join(); 
					if (! conv.ok()) {
						throw new Exception(convertErrorMsg);
					}
				}
				if (options[0].equals("-c")) {
					msg.setText("Compressing image to BCIF file");
				} else {
					msg.setText("Decompressing image from BCIF file");
				}
				System.out.println("\nLaunching encoder command: ");
				System.out.println("bcif " + options[0] + " "
								    + options[1] + " "
								    + options[2] + " "
								    + options[3]);
				if (gui != null) { gui.repaint(); }								    
				bcif.nonStandaloneMain(options);
				done = true;
				if (finalFile != null) {
					op.setText("Converting file from decompressed BMP");
					if (gui != null) { gui.repaint(); }								    
					converter.convert(options[3], finalFile);				
				}
				if (tempFile != null) {
					tempFile.delete();
				}
				msg.setText("Done");
			} catch (Throwable e) {
				if (gui != null) {
					String ermsg = "Error during encoder execution: \n";
					String exmsg = e.getMessage();
					if (exmsg.equalsIgnoreCase("Java heap space")) {
						exmsg += " finished.\n";
						exmsg += "Avoid this allowing more memory to be used in the JVM\n";
						exmsg += "launching Java with the option -Xmx[space].\n";
						exmsg += "To use up to 1 GB for example execute \n";
						exmsg += "java -Xmx1024M -jar bcifGUI.jar";
					}
					if (exmsg.equals(convertErrorMsg)) {
						exmsg += "\nPossible cause: Imagemagick must be at least of version 6.3.6-9";
					}
					JOptionPane.showMessageDialog(gui, ermsg + exmsg);
				} else {
					System.out.println("-----------------------------------------------------");
					System.out.println("" + e);
					System.out.println("-----------------------------------------------------");				
				}
				msg.setText("There were errors during encoding");
				done = true;
			}
		}
	}
	
	public class myOutput extends OutputStream {
		
		private String text = "";
		private JTextArea target = null;
		private int maxlength = 30000;
		
		myOutput (JTextArea t) {
			target = t;
		}
		
		public void write(int i) {
			text = text + (char)(i & 255);
			if (text.length() > maxlength) {
				text = text.substring(text.length() - maxlength, text.length());
			}
			if (i < 32) {
				flush();
			}
		}
		
		public void write(int[] b, int start, int offset) {
			for (int i = start; i < start + offset; i++) {
				write(b[i]);
			}
		}
		
		public void write(int[] b) {
			write(b, 0, b.length);
		}
		
		public void flush() {
			target.setText(text);
			target.setCaretPosition(text.length() - 1);			
		}
		
		public void setText(String t) {
			text = t;
			target.setText(text);
			target.setCaretPosition(text.length() - 1);
		}
	}
		
	public static class myConverter extends Thread {
		private String initFile;
		private String finalFile;
		private boolean ok = false;
		
		myConverter(String in, String out) {
			initFile = in;
			finalFile = out;
		}
		
		public void run() {
			ok = converter.convert(initFile, finalFile);
		}

		public boolean ok() {
			return ok;
		}
	}
	
	public class browseInput extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			browseInput();
		}		
	}

	public class browseOutput extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			browseOutput();
		}			
	}
	

	public class encodePressed extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			encode();
		}			
	}	
	
	public class viewPressed extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			viewImage();
		}			
	}	
}

