/*
 * Load GPS Tracks
 */

package gps;

import interfaces.StatusUpdate;

import java.io.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import cartesian.Coordinates;
import java.awt.geom.Point2D;
import java.util.*;

/**
 * @author Daniel Sathees Elmo
 * @author Adrian Skuballa
 * 
 *         this class imports GPS traces from text based or GPX-XML files and
 *         exports matched GPS traces/Points to text based files
 */

public class GPSTraceStreamer {

	// pattern for GPS point: timestamp, latitude, longitude ->
	// digit(s),digit(s).digit(s),digit(s).digit(s)
	static private final Pattern gpsPattern = Pattern.compile("-?\\d+(,-?\\d+.\\d+){2}");
	static private final Pattern gpsPattern2 = Pattern.compile("-?\\d+(,-?\\d+.\\d+){4}");
	static private final Pattern gpsSplitPattern = Pattern.compile(",");

	// pattern for date strings in GPX files (e.g. "2012-10-02T16:17:16Z"), we
	// have to split at '-', 'T' and 'Z' Position
	static private final Pattern gpxDateSplitPattern = Pattern.compile("[-TZ]");
	static private final int GPX_STRING_DATE_PARTS = 4; // we must have 4 parts
														// after splitting:
														// 1.Year 2.Month 3.Day
														// 4.Time(HH:MM:ss)

	// create date formatter for parsing date string
	static private DateFormat dateFormatter = DateFormat.getDateTimeInstance();

	// for GPX parsing (XML)
	static private XMLInputFactory xmlInputfactory = XMLInputFactory.newInstance();

	// for longitude/latitude double formating, set '.' as separator
	static private DecimalFormatSymbols dfS = DecimalFormatSymbols.getInstance();
	static {
		dfS.setDecimalSeparator('.');
	}
	
	// create formations for latitude (+-90) and longitude (+-180)
	static private DecimalFormat latFormat = new DecimalFormat("##.000000000", dfS);
	static private DecimalFormat lonFormat = new DecimalFormat("###.000000000", dfS);

	/** 
	 * converts Text or GPX formated files including a trace to a GPSTrace 
	 * 
	 * @param filePath 
	 * @param statusUpdate 
	 * @return 
	 * @throws Exception 
	 */ 
	public static GPSTrace convertToGPSPath(String filePath, StatusUpdate statusUpdate) throws Exception { 
		GPSTrace gpsTrace; // store parsed GPS trace from file
		File gpsTraceFile = new File(filePath); // connect to given file

		// TEXT file
		if (filePath.toLowerCase().endsWith(".txt") || filePath.toLowerCase().endsWith(".log")) {
			gpsTrace = convertToGPSPathFromTextFile(filePath, statusUpdate);
		}
		// GPX XML file
		else if (filePath.toLowerCase().endsWith(".gpx")) {
			gpsTrace = convertToGPSPathFromGPXFile(filePath, statusUpdate);
		}
		// otherwise throw exception
		else {
			throw new Exception("Not valid GPS file extension!");
		}

		// update status, work finished!
		statusUpdate.finished("GPS trace file \"" + gpsTraceFile.getName() + "\" with " + gpsTrace.getNrOfNodes()
				+ " GPS points loaded! Boundary min(lon/lat) max (lon/lat): (" + lonFormat.format(gpsTrace.getMinLon())
				+ ", " + latFormat.format(gpsTrace.getMinLat()) + ") (" + lonFormat.format(gpsTrace.getMaxLon()) + ", "
				+ latFormat.format(gpsTrace.getMaxLat()) + ")");
		/*
		 * System.out.println("GPS trace file \"" + gpsTraceFile.getName() +
		 * "\" with " + gpsTrace.getNrOfNodes() +
		 * " GPS points loaded! Boundary min(lon/lat) max (lon/lat): (" +
		 * lonFormat.format(gpsTrace.getMinLon()) + ", " +
		 * latFormat.format(gpsTrace.getMinLat()) + ") (" +
		 * lonFormat.format(gpsTrace.getMaxLon()) + ", " +
		 * latFormat.format(gpsTrace.getMaxLat()) + ")");
		 */

		// return parsed GPS trace
		return gpsTrace;
	}

	/**
	 * parses text file and converts it to a GPS Path an GPS Path
	 * 
	 * @param filePath
	 * @return GPSPath
	 * @exception FileNotFoundException
	 *                if GPS trace file can't be found
	 * @exception IOException
	 *                if reading file occurs an error
	 * @exception NumberFormatException
	 *                if a number can't be read
	 */
	public static GPSTrace convertToGPSPathFromTextFile(String filePath, StatusUpdate statusUpdate) throws Exception {
		try {
			// variables
			GPSTrace gpsTrace;
			int nrOfGPSPoints = 0;
			long refTimeStamp = 0;

			// access file and save name
			File gpsTraceFile = new File(filePath);

			// read file via buffered Reader due to better performance
			FileReader fReader = new FileReader(gpsTraceFile);
			BufferedReader bReader = new BufferedReader(fReader);

			// read first line
			String line = bReader.readLine();

			// line must be "#n" with n = Number Of GPS Points in file
			if (line.matches("#\\d+")) {
				nrOfGPSPoints = Integer.parseInt(line.substring(1));
				
				// read second line
				line = bReader.readLine();

				// line must contain reference time stamp, ignore case sensitivity
				if (line.matches("(?i)#all Tstamps substracted by \\d+"))
					refTimeStamp = Long.parseLong(line.substring(28));
				else
					System.out.println("Numbers of GPS Point information couldn't be read");

				// read third line, ignore though it contains information about GPS
				// information syntax
				bReader.readLine();

				// initialize GPS path
				gpsTrace = new GPSTrace(nrOfGPSPoints, refTimeStamp);

				// store read data
				long timeStamp = 0;
				long prevTime = Long.MIN_VALUE;
				double latitude = 0.0;
				double longitude = 0.0;

				// store read data from file
				String[] gpsData;

				// current read line
				int currentLineNr = 0;
				float currentProgress = 0;

				int count = 0;

				while ((line = bReader.readLine()) != null) {
					count++;

					// read line must confirm to pattern
					if (gpsPattern.matcher(line).matches() || gpsPattern2.matcher(line).matches()
							|| line.startsWith("2014-")) {
						gpsData = gpsSplitPattern.split(line);

						// read time, read latitude/longitude
						if (line.startsWith("2014-")) {
							Calendar c = Calendar.getInstance();

							String[] sdatetime = line.split(" ");
							String[] sdate = sdatetime[0].split("-");
							String[] stime = sdatetime[1].split(".0000000,");
							stime = stime[0].split(":");

							c.set(Integer.parseInt(sdate[0]), Integer.parseInt(sdate[1]), Integer.parseInt(sdate[2]),
									Integer.parseInt(stime[0]), Integer.parseInt(stime[1]), Integer.parseInt(stime[2]));

							timeStamp = c.getTimeInMillis();
						} else {
							timeStamp = Long.parseLong(gpsData[0]);
						}

						latitude = Double.parseDouble(gpsData[1]);
						longitude = Double.parseDouble(gpsData[2]);

						// check if its time is greater then previous GPS point's
						// time
						if (timeStamp > prevTime) {
							// add node to GPS Path
							Point2D p = Coordinates.getCartesianXY(longitude, latitude);
							gpsTrace.addNode(p.getX(), p.getY(), timeStamp, longitude, latitude);
							prevTime = timeStamp;
						}
					}
					// ignore comments
					else if (line.startsWith("#"))
						continue;
					else
						System.out.println(line + " doesn't match gps information pattern!");

					// update status
					currentLineNr++;
					currentProgress = ((float) currentLineNr / nrOfGPSPoints * 100);
					statusUpdate.updateStatus("reading line Nr." + currentLineNr + "...", currentProgress);
				}

				nrOfGPSPoints = count;

				// close reader
				bReader.close();
				fReader.close();

				// return created GPS path
				return gpsTrace;

			} if (line.matches("\\d+:(.*)$") && line.matches("(.*)\"class\":(.*)")) {
				Vector<GPSNode> vTemp = new Vector<GPSNode>();

				long prevTime = Long.MIN_VALUE;
				
				line = bReader.readLine();

				while (line != null) {
					if (line.matches("\\d+:(.*)$") && line.contains("\"lat\":") && line.contains("\"lon\":")) {
						String[] lines = line.split(":", 2);

						long timeStamp = Long.parseLong(lines[0]);

						lines = line.split("\"lat\":", 2);
						
						lines = lines[1].split(",\"lon\":", 2);
						
						double lat = Double.parseDouble(lines[0]); 
						
						lines = lines[1].split(",", 2);
						
						double lon = Double.parseDouble(lines[0]); 
						
						Point2D p = Coordinates.getCartesianXY(lon, lat);
						
						GPSNode gpsNode = new GPSNode(p.getX(), p.getY(), timeStamp, lon, lat);
						
						if (timeStamp > prevTime) {
							vTemp.addElement(gpsNode);	
							prevTime = timeStamp;
						}
					}
					
					line = bReader.readLine();
				}
				
				// close reader
				bReader.close();
				fReader.close();

				gpsTrace = new GPSTrace(vTemp.size(), 0);
				
				for (int i = 0; i < vTemp.size(); i++) {
					gpsTrace.addNode(vTemp.get(i));
				}
				
				// return created GPS path
				return gpsTrace;
				
				
			} else {
				System.out.println("Numbers of GPS Point information couldn't be read");
				bReader.close();
				throw new Exception("Numbers of GPS Point information couldn't be read");
			}

		} catch (FileNotFoundException e) {
			System.out.println("GPS-trace file not found!");
			throw e;
		} catch (IOException e) {
			System.out.println("Error while reading GPS-trace file!");
			throw e;
		} catch (NumberFormatException e) {
			System.out.println("Error reading number!");
			throw e;
		}
	}

	/**
	 * parses GPX XML file and converts it to a GPS Path
	 * 
	 * @param filePath
	 * @param statusUpdate
	 * @return
	 * @throws Exception
	 */
	public static GPSTrace convertToGPSPathFromGPXFile(String filePath, StatusUpdate statusUpdate) throws Exception {
		boolean isInsideMetadata = false; // flag to check if are we inside a
											// meta data block
		long refTimeStamp = 0; // save reference time stamp of GPS trace
		int nrOfGPSPoints = 0; // sum of all GPS Points

		// try initialize stream reader with XML file
		XMLStreamReader parser = createXMLStreamReader(filePath);

		// update status to an undefined status, cause we don't know at this
		// time
		// how many track points we have to read
		statusUpdate.updateUndefinedStatus("parsing...");

		// get time stamp and bounds
		loop_count: while (parser.hasNext()) {
			switch (parser.getEventType()) {
			case XMLStreamConstants.START_ELEMENT:

				// notice that we entered metadata info
				if (parser.getLocalName().equals("metadata")) {
					// update status
					statusUpdate.updateStatus("reading metadata...");
					// notice we're inside meta data block
					isInsideMetadata = true;
				}
				// read reference time stamp inside metadata
				else if (parser.getLocalName().equals("time") && isInsideMetadata) {
					// update status
					statusUpdate.updateStatus("reading reference timestamp...");
					// get reference time stamp
					refTimeStamp = readGPXTimeStamp(parser);
				}
				// count GPS Points
				else if (parser.getLocalName().equals("trkpt")) {
					// update status
					statusUpdate.updateStatus("counting trackpoints..." + nrOfGPSPoints);
					// increase nr of read GPS points
					nrOfGPSPoints++;
				}
				break;

			// leave while loop if metadata info ends
			case XMLStreamConstants.END_ELEMENT:
				if (parser.getLocalName().equals("trk"))
					break loop_count;
			}
			// get next event
			parser.next();
		}

		// read XML Stream from Beginning, but this read each GPS Point and add
		// to GPSPath
		parser.close();
		parser = createXMLStreamReader(filePath);

		// create new GPS path
		GPSTrace gpsTrace = new GPSTrace(nrOfGPSPoints, refTimeStamp);

		// read each track point
		double lat = 0;
		double lon = 0;
		long timeStamp = 0;

		// flags for parsing
		boolean isInsideTrackPointBlock = false;

		// current read line
		int currentTrackPoint = 0;
		float currentProgress = 0;

		// go through file again
		loop_reader: while (parser.hasNext()) {

			switch (parser.getEventType()) {

			case XMLStreamConstants.START_ELEMENT:

				// track point tag reached, set flag
				if (parser.getLocalName().equals("trkpt")) {
					isInsideTrackPointBlock = true;
					// read latitude and longitude
					for (int i = 0; i < parser.getAttributeCount(); i++) {
						if (parser.getAttributeLocalName(i).equals("lat"))
							lat = Double.parseDouble(parser.getAttributeValue(i));
						else if (parser.getAttributeLocalName(i).equals("lon"))
							lon = Double.parseDouble(parser.getAttributeValue(i));
					}
				}

				// read time stamp inside, add GPS Point data to GPS trace,
				// reset track point flag
				else if (parser.getLocalName().equals("time") && isInsideTrackPointBlock) {
					timeStamp = readGPXTimeStamp(parser);
					Point2D p = Coordinates.getCartesianXY(lon, lat);
					gpsTrace.addNode(p.getX(), p.getY(), timeStamp, lon, lat);
					isInsideTrackPointBlock = false;

					// calculate progress
					currentTrackPoint++;
					currentProgress = ((float) currentTrackPoint / nrOfGPSPoints * 100);
					statusUpdate.updateStatus("reading track point " + currentTrackPoint + "/" + nrOfGPSPoints,
							currentProgress);
				}
				break;

			// leave while loop if first track ends
			case XMLStreamConstants.END_ELEMENT:
				if (parser.getLocalName().equals("trk")) {
					break loop_reader;
				}
			}

			// get next event
			parser.next();
		}

		// GPS trace with parsed position/time values
		return gpsTrace;
	}

	private static XMLStreamReader createXMLStreamReader(String filePath) throws Exception {
		// try initialize stream reader with XML file
		InputStream inputStream = new FileInputStream(filePath);
		XMLStreamReader parser;
		try {
			parser = xmlInputfactory.createXMLStreamReader(inputStream);
		} catch (XMLStreamException e) {
			System.err.println("XML parser couldn't be created (file: " + filePath + ")");
			throw e;
		}
		// give back instance of StAX stream reader
		return parser;
	}

	/**
	 * if time tag is reached, this method will extract timestamp value in
	 * milliseconds
	 * 
	 * @param parser
	 * @return
	 * @throws Exception
	 */
	private static long readGPXTimeStamp(XMLStreamReader parser) throws Exception {
		// get next tag, ignore white spaces an comments
		while (parser.hasNext()) {
			// next content must be characters
			if (parser.getEventType() == XMLStreamConstants.CHARACTERS)
				return dateInGPXToMilli(parser.getText());
			else if ((parser.getEventType() == XMLStreamConstants.END_ELEMENT)
					&& (parser.getLocalName().equals("time")))
				break;
			// get next element
			parser.next();
		}
		// throw error exception
		throw new Exception("No time character stream available inside time tag");
	}

	/**
	 * convert date string out of GPX files to milliseconds since 1.January.1970
	 * 
	 * @param gpxDateString
	 * @return
	 * @throws Exception
	 */
	private static long dateInGPXToMilli(String gpxDateString) throws Exception {
		// build java date class compatible string for parsing
		String dateString;
		// apply split pattern
		String dateStringParts[] = gpxDateSplitPattern.split(gpxDateString);

		// check correct amount of split parts
		if (dateStringParts.length == GPX_STRING_DATE_PARTS) {
			// rebuild compatible date string for parsing
			dateString = dateStringParts[2] + "." + dateStringParts[1] + "." + dateStringParts[0] + " "
					+ dateStringParts[3];
		}
		// otherwise throw exception cause we've got a wrong formated GPX date
		// string
		else
			throw new Exception("GPX date string doesn't match to format YYYY-MM-DDTHH:MM:ssZ");

		// parse date string
		Date date = dateFormatter.parse(dateString);

		// return date in milliseconds since 1.January.1970
		return date.getTime();
	}

}
