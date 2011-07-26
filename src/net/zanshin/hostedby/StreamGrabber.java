package net.zanshin.hostedby;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.InputStream;
/**
 * A Thread (Runable) to capture a Stream.
 * 
 * @author Mark H. Nichols
 * Jun 11, 2008
 *
 */
public class StreamGrabber implements Runnable {
    private InputStream is;
    private String type;
    private boolean verbose;
    private String result;
	
    StreamGrabber(InputStream is, String type, boolean verbose) {
        this.is = is;
        this.type = type;
        this.verbose = verbose;
    }
	
    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ( (line = br.readLine()) != null) {
//              result = line;
                int startIndex = line.indexOf(":");
                result = line.substring(startIndex + 1).trim();
                if (verbose) System.out.println("SG " + type + " > " + line);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * @return the result
     */
    public String getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(String result) {
        this.result = result;
    }

}