package route;

/*
import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import myOSM.myOSMMap;
import hash.Hash;
import interfaces.StatusUpdate;
*/

public class NRouteStreamer {
	
	/**
	 * this exception can be used to indicate that the SHA-256 hash value does not
	 * match to a given street map file
	 */
	/*
	public static class SHA256CheckSumException extends Exception {
		private static final long serialVersionUID = 1L;

		public SHA256CheckSumException() {
			super();
		}
	
		public SHA256CheckSumException(String message) {
			super(message);
		}

	}
	*/
	
	/*
	public static class NRouteFileNotFoundException extends FileNotFoundException {

		private static final long serialVersionUID = 1L;

		public NRouteFileNotFoundException() {
			super();
		}

		public NRouteFileNotFoundException(String s) {
			super(s);
		}
	}
	*/
	
	/*
	public static class MapFileNotFoundException extends FileNotFoundException {

		private static final long serialVersionUID = 1L;

		public MapFileNotFoundException() {
			super();
		}

		public MapFileNotFoundException(String s) {
			super(s);
		}
	}
*/
	
	/*
	public String getMapFileFromNRouteFile(String nRouteFilePath, StatusUpdate statusUpdate) 
																throws SHA256CheckSumException,
																	   NRouteFileNotFoundException,
																	   MapFileNotFoundException,
																	   IOException,
																	   NoSuchAlgorithmException {
		return getMapFileFromNRouteFile(nRouteFilePath, "", statusUpdate);
		
	}
	*/

	/*
	public static String getMapFileFromNRouteFile(String nRouteFilePath, String altMapFilePath, StatusUpdate statusUpdate) 
																					throws SHA256CheckSumException,
																						   NRouteFileNotFoundException,
																						   MapFileNotFoundException,
																						   IOException,
																						   NoSuchAlgorithmException {
		// check if n route file exists
		File nRouteFile = new File(nRouteFilePath);
		if (!nRouteFile.exists()) {
			throw new NRouteFileNotFoundException();
		}
		
		// open file using File-, Buffered- and DataInputStream
		// status update 
		  statusUpdate.updateStatus("Creating file stream...", 5);
		
		FileInputStream fis = new FileInputStream(nRouteFile);
		BufferedInputStream bis = new BufferedInputStream(fis);
		DataInputStream dis = new DataInputStream(bis);				
		
		// 1.) read street map file path & its SHA-256 checksum
		// status update 
		statusUpdate.updateStatus("Reading map info...", 10);
		
		String mapFilePath = dis.readUTF();
		String sha256Checksum = dis.readUTF();
		
		// set alternative file path if given by caller
		// status update  
		statusUpdate.updateStatus("Checking alternative map...", 15);
		
		if (!altMapFilePath.isEmpty()) {
			mapFilePath = altMapFilePath;
		}
		
		// 2.) check if street map file exists
		// status update 
		statusUpdate.updateStatus("Checking map file...", 20);
		
		File mapFile = new File(mapFilePath);
		if (!mapFile.exists()) {
			dis.close();
			throw new MapFileNotFoundException();
		}
			
		// 3.) check if SHA-256 hash value of street map file matched given hash value
		String calcSHA256Checksum = Hash.getSHA256FileChecksumString(mapFile, statusUpdate, 20, 95);
		
		// 4.) compare SHA-256 checksums
		// status update 
		statusUpdate.updateStatus("Comparing SHA-256 checksum...", 100);
		
		if (!sha256Checksum.equalsIgnoreCase(calcSHA256Checksum)) {
			dis.close();
			throw new SHA256CheckSumException();
		}
		
		dis.close();
		
		// everything's fine
		// status update 
		statusUpdate.finished("Map info valid!");
		
		return mapFilePath;
	}
	*/
	
	/*
	public static SelectedNRoute getNRouteFromFile(String nRouteFilePath, myOSMMap myMap, Component drawComponent, StatusUpdate statusUpdate) throws NRouteFileNotFoundException,
																												   					   IOException{
		// check if n route file exists
		File nRouteFile = new File(nRouteFilePath);
		if (!nRouteFile.exists()) {
			throw new NRouteFileNotFoundException();
		}
		
		// open file using File-, Buffered- and DataInputStream
		// status update 
		statusUpdate.updateStatus("Creating file stream...", 5);
		
		FileInputStream fis = new FileInputStream(nRouteFile);
		BufferedInputStream bis = new BufferedInputStream(fis);
		DataInputStream dis = new DataInputStream(bis);
				
		// 1.) first two values not needed (file path & its SHA-256 checksum)
		// status update 
		statusUpdate.updateStatus("Skipping map info...", 10);
		
		dis.readUTF();
		dis.readUTF();
		
		SelectedNRoute selectedNRoute = new SelectedNRoute(myMap, drawComponent);
		
		// 2.) read length of start links
		// status update 
		statusUpdate.updateStatus("Reading start links size...", 15);
		
		int startLinksSize = dis.readInt();
		
		// 3.) read length of end links
		// status update 
		statusUpdate.updateStatus("Reading end links size...", 20);
		
		int endLinksSize = dis.readInt();
		
		int sumLinks = startLinksSize + endLinksSize;
		
		System.out.println("\n\nGetting selected N route from file:");
		System.out.println("=====================================");
		
		System.out.println("\nstart links size: " + startLinksSize);
		System.out.print("StartLinks: ");
		
		// 4.) read id's of start links and add to selected N route
		for (int i=1; i <= startLinksSize; i++) {
			long id = dis.readLong();
			System.out.print(id + ",");
			
//			selectedNRoute.addStartLink(streetMap.getLink(id));
			selectedNRoute.addStartLink(myMap.getLink(id));
			
			double progress = i * (80.0f / sumLinks) + 20;
			//status update 
			statusUpdate.updateStatus("reading street link (start links) nr. " + i, (int) progress);
			
		}
		
		System.out.println("\n\nend links size: " + endLinksSize);
		System.out.print("EndLinks: ");
		
		// 5.) read id's of end links
		for (int i=1; i <= endLinksSize; i++) {
			long id = dis.readLong();
			System.out.print(id + ",");
			
			selectedNRoute.addEndLink(myMap.getLink(id));
			
			double progress = (startLinksSize + i) * (80.0f / sumLinks) + 20;
			// status update 
			statusUpdate.updateStatus("reading street link (end links) nr. " + i, (int) progress);
			
		}
		
		// close stream
		dis.close();
		
		// status update 
		statusUpdate.finished("Selected N Route successfully imported!");
		
		return selectedNRoute;
	}
	*/
	
	/*
	public static boolean saveSelectedNRouteToFile(SelectedNRoute selectedNRoute, String filePath, StatusUpdate statusUpdate) {
		
		try {
			// get street map file
			File streetMapFile = streetMap.getStreetMapFile();
			
			if (!streetMap.getStreetMapFile().exists()) {
				throw new FileNotFoundException("StreetMap file: " + streetMapFile.getAbsolutePath() + " not found!");
			}
			
			// get SHA-256 hash value for street map file content
			String sha256Checksum = Hash.getSHA256FileChecksumString(streetMapFile, statusUpdate, 0, 20);
			
			// get selected N route parts
			ArrayList<myOSMWayPart> startLinks = selectedNRoute.getNRouteLinksStart();
			ArrayList<myOSMWayPart> endLinks = selectedNRoute.getNRouteLinksEnd();

			int startLinksSize = startLinks.size();
			int endLinksSize = endLinks.size();
			int sumLinks = startLinksSize + endLinksSize;
			
			// create file using File-, Buffered- and DataOutputStream
			FileOutputStream fos = new FileOutputStream(new File(filePath));
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			DataOutputStream dos = new DataOutputStream(bos);
			
			try {
				// 1.) store street map file path & its checksum
				statusUpdate.updateStatus("writing file info and SHA-256 checksum...");
				dos.writeUTF(streetMapFile.getAbsolutePath());
				dos.writeUTF(sha256Checksum);
				
				System.out.println("\n\nSave selected N route to file:");
				System.out.println("================================");
				
				System.out.println("\nStreetMapFilePath: " + streetMapFile.getAbsolutePath());
				System.out.println("SHA256CheckSum: " + sha256Checksum);
				
				// 2.) store length of start & end links
				statusUpdate.updateStatus("writing start links size...");
				dos.writeInt(startLinksSize);
				
				// 3.) store length of end links
				statusUpdate.updateStatus("writing end links size...");
				dos.writeInt(endLinksSize);
				
				System.out.println("\nstart links size: " + startLinks.size());
				System.out.print("StartLinks: ");
				
				// 4.) store id's of start links
				int it = 0;
				for (myOSMWayPart link : startLinks) {
					dos.writeLong(link.getID());
					it++;
					
					// update progress
					double progress = it * (80.0f / sumLinks) + 20;
					statusUpdate.updateStatus("writing street link (start links) nr. " + it, (int) progress);
					
					System.out.print(link.getID() + ",");
				}
				
				System.out.println("\n\nend links size: " + endLinks.size());
				System.out.print("EndLinks: ");
				
				// 5.) store id's of end links
				for (myOSMWayPart link : endLinks) {
					dos.writeLong(link.getID());
					it++;
					
					// update progress
					double progress = it * (80.0f / sumLinks) + 20;
					statusUpdate.updateStatus("writing street link (end links) nr. " + it, (int) progress);
					
					System.out.print(link.getID() + ",");
				}
				
				statusUpdate.finished("N Route file successfully saved");
				
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			} finally {
				dos.close();
			}
			
			testRead(new File(filePath));
			
			// successful
			return true;
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} 
	}
	*/
	
	/*
	public static void testRead(File file) {
		
		try {
			// open file using File-, Buffered- and DataInputStream
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis);
			DataInputStream dis = new DataInputStream(bis);
			
			// 1.) read street map file path & its checksum
			String streetMapFilePath = dis.readUTF();
			String sha256Checksum = dis.readUTF();
			
			System.out.println("\n\nTesting saved N route file:");
			System.out.println("=============================");
			
			System.out.println("\nStreetMapFilePath: " + streetMapFilePath);
			System.out.println("SHA256CheckSum: " + sha256Checksum);
			
			// 2.) read length of start links
			int startLinksSize = dis.readInt();
			
			// 3.) read length of end links
			int endLinksSize = dis.readInt();
			
			System.out.println("\nstart links size: " + startLinksSize);
			System.out.print("StartLinks: ");
			
			// 4.) read id's of start links
			for (int i=0; i < startLinksSize; i++) {
				long id = dis.readLong();
				System.out.print(id + ",");
			}
			
			System.out.println("\n\nend links size: " + endLinksSize);
			System.out.print("EndLinks: ");
			
			// 5.) read id's of end links
			for (int i=0; i < endLinksSize; i++) {
				long id = dis.readLong();
				System.out.print(id + ",");
			}
			
			dis.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	*/
	
}
