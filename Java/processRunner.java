package bcif;
import java.io.*;

/**
 * <p> Class that handles the execution of external processes; used in BCIF to call the
 * 'convert' tool (from www.imagemagick.org) to convert from/to non BMP formats</p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */
 
public class processRunner extends Thread {

  private OutputStream out;
  private InputStream processOut;
  private InputStream processErr;
  private streamer outListener;
  private streamer errListener;
  private String command;
  private Process p;
  private int waitTime = 100;
  private boolean win = File.separatorChar == '\\';
  private String[] args = null;
  private Exception runException = null;

  public processRunner(String c, OutputStream o) {
    command = c;
    out = new BufferedOutputStream(o);
  }

  public processRunner(String c) {
    command = c;
    out = new BufferedOutputStream(System.out);
  }

  public processRunner(String[] a) {
    command = null;
    args = a;
    out = new BufferedOutputStream(System.out);
  }

  public Exception getRunException() {
    return runException;
  }

  public static void exec(String c, OutputStream o) {
    processRunner pr = new processRunner(c, o);
    pr.run();
  }

  public static void exec(String c) {
    processRunner pr = new processRunner(c);
    pr.run();
  }
  
  public int exitVal() {
      if (p != null) {
  	return p.exitValue();
      } else {
        return 2;
      }
  }    

  public void waitFor() {
  	try {
  	    p.waitFor();
  	    p.exitValue();
  	} catch (Exception e) {
            runException = e;
  	}
  }
  
  public void run() {
    int exitVal = 0;
    try {
      if (command != null) { 
	String ecom = getCommand(command);
	p = Runtime.getRuntime().exec(ecom);
      } else if (args != null) {
        p = Runtime.getRuntime().exec(args);
      } else {
        throw new RuntimeException("Command not specified !");
      }
      processOut = p.getInputStream();
      processErr = p.getErrorStream();
      outListener = new streamer(processOut, out);
      errListener = new streamer(processErr, out);
      outListener.start();
      errListener.start();
      boolean exitf = false;
      while (! exitf) {
        try {
          exitVal = p.exitValue();
          exitf = true;
        } catch (Exception e) {
          Thread.sleep(waitTime);
          outListener.flush();
          errListener.flush();
        }
      }
      outListener.requestStop();
      errListener.requestStop();
    } catch (Exception e) {
      runException = e;
    }
    if (exitVal != 0) {

	}
  }

  public class ticker extends Thread {

    private Thread[] threads;
    private boolean exitf;
    private long sleepTime = 1000;

    ticker(Thread[] t) {
      threads = t;
    }

    public void run() {
      try {
        while (!exitf) {
          Thread.sleep(sleepTime);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void requestStop() {
      exitf = true;
    }
  }

  public class streamer extends Thread {

    private InputStream in;
    private OutputStream out;
    private boolean exitf = false;

    public streamer (InputStream i, OutputStream o) {
      in = new BufferedInputStream(i);
      out = new BufferedOutputStream(o);
    }

    public void run() {
      while (! exitf) {
        try {
          int readByte = in.read();
          while (readByte >= 0) {
            out.write(readByte);
            readByte = in.read();
          }
          exitf = true;
        } catch (InterruptedIOException e2) {
          //System.out.println("Streamer tick");
        } catch (IOException e) {
          e.printStackTrace();
          exitf = true;
        }
        flush();
      }
      //System.out.println("Streamer terminating...");
    }

    public void flush() {
      try {
        out.flush();
      }
      catch (IOException e) {
        //e.printStackTrace();
      }
    }


    public void requestStop() {
      exitf = true;
      interrupt();
    }
  }

  public static String getUnixCommand() {
	  String launcher = "";
	  if (new File("/bin/sh").exists()) {            
		launcher = "/bin/sh";
	  } else if (new File("/bin/ksh").exists()) {    
		launcher = "/bin/ksh";
          } else if (new File("/bin/bash").exists()) {   
		launcher = "/bin/bash";
          } else if (new File("/bin/tcsh").exists()) {   
		launcher = "/bin/tcsh";
          } else if (new File("/bin/xterm").exists()) {  
		launcher = "/bin/xterm";
          } else {
                launcher = ""; // this is a problem
	  }
          return launcher;
  }

  public static String[] getUnixArgs(String com) {
	  String[] args = new String[2];
          args[0] = "-c";
          args[1] = com;
          return args;
  }

  public static String getCommand(String com) {
      String launcher = "";
      if(File.separatorChar == '\\') {
          launcher = "cmd /c " + com;
      } else {
		  com = com.replaceAll("\"", "");
          launcher = com;
      } 
      return launcher;
  }
    
  public static void pipeExe(String ps1, String ps2) {
    try {
      Process p1 = Runtime.getRuntime().exec(ps1);
      Process p2 = Runtime.getRuntime().exec(ps2);
      InputStream in = new BufferedInputStream(p1.getInputStream());
      OutputStream out = new BufferedOutputStream(p2.getOutputStream());
      int readByte = in.read();
      int bytenum = 1;
      while (readByte > -1) {
        out.write(readByte);
        readByte = in.read();
        bytenum ++;
      }
      out.flush();
      System.out.println("Input bytes read: " + bytenum);
      System.out.println("Process 2 outputstream: ");
      InputStream in2 = new BufferedInputStream(p2.getInputStream());
      if (in2.available() > 0) { readByte = in2.read(); }
      while (in2.available() > 0) {
        System.out.write(readByte);
        readByte = in2.read();
      }
      System.out.println("Process 2 errorstream: ");
      InputStream in3 = new BufferedInputStream(p2.getErrorStream());
      if (in3.available() > 0) { readByte = in3.read(); }
      while (in3.available() > 0) {
        System.out.write(readByte);
        readByte = in3.read();
      }
      p2.waitFor();
      in.close();
      out.close();
      in2.close();
      System.out.println("Exit code for process 2: " + p2.exitValue());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws IOException {
    Process p = Runtime.getRuntime().exec("C:\\bistred mandorla.png");
    
  }
}

