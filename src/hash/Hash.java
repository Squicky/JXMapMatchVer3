package hash;

/*
import interfaces.StatusUpdate;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
*/

public class Hash {
	
	/*
	private static final int BUFFER_LENGTH = 1024;
	
	public static byte[] getSHA256FileChecksum(File file, StatusUpdate statusUpdate) throws FileNotFoundException,
																						    IOException,
																					 	    NoSuchAlgorithmException{
		return getSHA256FileChecksum(file, statusUpdate, 0, 100);
	}
	
	public static byte[] getSHA256FileChecksum(File file, StatusUpdate statusUpdate, int minPercent, int maxPercent) throws FileNotFoundException,
																															IOException,
																															NoSuchAlgorithmException {
		
		// check if file exists
		if (!file.exists()) {
			throw new FileNotFoundException("File: " + file.getAbsolutePath() + " doesn't exist to procedure SHA-256 algorithm!");
		}
		
		// get SHA-256 instance
		MessageDigest md = MessageDigest.getInstance("SHA-256");

		statusUpdate.updateStatus("calculating SHA-256 checksum for file \"" + file.getAbsolutePath() + "\": ", 0);
		
		int maxOffsetPercent = maxPercent - minPercent;
		
		// create input stream
		FileInputStream fis = new FileInputStream(file);
		
		// reading buffer
        byte[] dataBytes = new byte[BUFFER_LENGTH];
 
        // update SHA-256 algorithm while reading
        int nread = 0;
        int it = 0;
        double maxIt = file.length() / BUFFER_LENGTH; 
        while ((nread = fis.read(dataBytes)) != -1) {
          md.update(dataBytes, 0, nread);
          it++;
          
          // calculate progress and update
          double progress =  minPercent + (it * (maxOffsetPercent / maxIt));
          long readByte = BUFFER_LENGTH * it;
          statusUpdate.updateStatus("calculating SHA-256 checksum for file \"" + file.getAbsolutePath() + "\": " + readByte + " byted read", (int) progress);
        };
        
        fis.close();
        
        return md.digest();
	}
	
	public static String getSHA256FileChecksumString(File file, StatusUpdate statusUpdate) throws FileNotFoundException,
																										 IOException,
																										 NoSuchAlgorithmException {
		return getSHA256FileChecksumString(file, statusUpdate, 0, 100);
	}
		
	public static String getSHA256FileChecksumString(File file, StatusUpdate statusUpdate, int minPercent, int maxPercent) throws FileNotFoundException,
																																  IOException,
																																  NoSuchAlgorithmException{
		// get SHA-256 checksum as byte array
		byte[] mdBytes = getSHA256FileChecksum(file, statusUpdate, minPercent, maxPercent);
		
	      //convert the byte to hex format method 1
        StringBuffer checksum = new StringBuffer();
        for (int i = 0; i < mdBytes.length; i++) {
          checksum.append(Integer.toString((mdBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
 
        
        //convert the byte to hex format method 2
        //StringBuffer hexString = new StringBuffer();
    	//for (int i=0;i<mdbytes.length;i++) {
    	//	String hex=Integer.toHexString(0xff & mdbytes[i]);
   	    // 	if(hex.length()==1) hexString.append('0');
   	    // 	hexString.append(hex);
    	//}

        // return checksum as string
        return checksum.toString();
	}
	
	*/

}
