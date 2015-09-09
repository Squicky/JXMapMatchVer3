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

import myOSM.myDataset;
import myOSM.myOSMMap;
import myOSM.myOSMNode;
import myOSM.myOSMWay;
import myOSM.myOSMWayPart;

import org.jdesktop.swingx.mapviewer.GeoPosition;

import algorithm.MatchedGPSNode;
import algorithm.MatchedNLink;
//import algorithm.ReorderedMatchedGPSNode;
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

	static private final double MIN_DATARATE_DOWN = 0;
	static private final double MAX_DATARATE_DOWN = 7200000 / 8;
	static private final double MIN_DELAY_DOWN = 0;
	static private final double MAX_DELAY_DOWN = 0.5;
	static private final double MIN_LOSS_RATE_DOWN = 0;
	static private final double MAX_LOSS_RATE_DOWN = 100;

	static private final double MIN_DATARATE_UP = 0;
	static private final double MAX_DATARATE_UP = 4000000 / 8;
	static private final double MIN_DELAY_UP = 0;
	static private final double MAX_DELAY_UP = 0.5;
	static private final double MIN_LOSS_RATE_UP = 0;
	static private final double MAX_LOSS_RATE_UP = 100;

	
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

	private static void createOsmKml(Vector<MatchedGPSNode> gpsNodesToMatch, String filePath) {

		try {
			File kmlOsm = new File(filePath + ".osm.kml");
			BufferedWriter bKmlWriterOsm = new BufferedWriter(new FileWriter(kmlOsm));
			
			bKmlWriterOsm.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
			bKmlWriterOsm.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">" + System.lineSeparator());
			bKmlWriterOsm.write("	<Document>" + System.lineSeparator());
			bKmlWriterOsm.write("		<name>" + kmlOsm.getName() + "</name>" + System.lineSeparator());
			bKmlWriterOsm.write("" + System.lineSeparator());
	
			myOSMMap myMap = gpsNodesToMatch.get(0).matchtedWayPart.parentWay.map;
			for (int i = 0; i < myMap.ways.size(); i++) {
				myOSMWay w = myMap.ways.get(i);
	
				for (int j = 0; j < w.WayParts.length; j++) {
					myOSMWayPart wp = w.WayParts[j];
					myOSMNode n1 = wp.startNode;
					myOSMNode n2 = wp.endNode;
	
					bKmlWriterOsm.write("		<Placemark>" + System.lineSeparator());
					bKmlWriterOsm.write("			<name>" + wp.parentWay.id + "</name>" + System.lineSeparator());
					bKmlWriterOsm.write("			<LineString>" + System.lineSeparator());
					bKmlWriterOsm.write("				<coordinates>" + System.lineSeparator());
	
					bKmlWriterOsm.write(n1.lon + "," + n1.lat + ",0" + System.lineSeparator());
					bKmlWriterOsm.write(n2.lon + "," + n2.lat + ",0" + System.lineSeparator());
	
					bKmlWriterOsm.write("				</coordinates>" + System.lineSeparator());
					bKmlWriterOsm.write("			</LineString>" + System.lineSeparator());
					bKmlWriterOsm.write("			<Style> " + System.lineSeparator());
					bKmlWriterOsm.write("				<LineStyle>  " + System.lineSeparator());
					bKmlWriterOsm.write("					<color>#ff000000</color>" + System.lineSeparator());
					bKmlWriterOsm.write("					<width>5</width>" + System.lineSeparator());
					bKmlWriterOsm.write("				</LineStyle> " + System.lineSeparator());
					bKmlWriterOsm.write("			</Style>" + System.lineSeparator());
					bKmlWriterOsm.write("		</Placemark>" + System.lineSeparator());
					bKmlWriterOsm.write("" + System.lineSeparator());
				}
			}
	
			bKmlWriterOsm.write("	</Document>" + System.lineSeparator());
			bKmlWriterOsm.write("</kml>" + System.lineSeparator());
			bKmlWriterOsm.write("" + System.lineSeparator());
			
			bKmlWriterOsm.close();
		}
		catch (Exception e) {
			System.out.println("Error: createKmlUnmatchedMatched \n" + e.toString());
		}
		
	}
	
	private static void createKmlUnmatchedMatched(Vector<MatchedGPSNode> gpsNodesToMatch, String filePath) {
		try {
			File kmlUnmatchedMatched = new File(filePath + ".unmatched.matched.kml");
			BufferedWriter bKmlWriterUnmatchedMatched = new BufferedWriter(new FileWriter(kmlUnmatchedMatched));
			
			bKmlWriterUnmatchedMatched.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">" + System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("	<Document>" + System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("		<name>" + kmlUnmatchedMatched.getName() + "</name>" + System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("" + System.lineSeparator());
			
			int matchedGPSNodeIndex = 0;
			
			for (MatchedGPSNode matchedGPSNode : gpsNodesToMatch) {
				// get just matched GPS Points
				if (matchedGPSNode.isMatched()) {
					
					Calendar c = Calendar.getInstance();
					//long t = matchedGPSNode.getTimestamp() + timeStampOffSet;
					long t = matchedGPSNode.getTimestamp();
					t = t / 1000000L;
					c.setTimeInMillis(t);
					
					GeoPosition matchedGeoPos = Coordinates.getGeoPos(matchedGPSNode.getMatchedX(),	matchedGPSNode.getMatchedY());

					if (matchedGPSNode.isReordered) {
						matchedGeoPos = Coordinates.getGeoPos(matchedGPSNode.matchedXreordered,	matchedGPSNode.matchedYreordered);
					}
					
					GeoPosition unmatchedGeoPos = Coordinates.getGeoPos(matchedGPSNode.getX(), matchedGPSNode.getY());
					
					bKmlWriterUnmatchedMatched.write("		<Placemark>" + System.lineSeparator());
					bKmlWriterUnmatchedMatched.write("			<name>" + (matchedGPSNode.getTimestamp())
							+ " " + matchedGPSNodeIndex + " " + c.getTime().toString() + "</name>" + System.lineSeparator());
					bKmlWriterUnmatchedMatched.write("			<description>" + (matchedGPSNode.getTimestamp())
							+ " " + matchedGPSNodeIndex + " " + c.getTime().toString() + "</description>" + System.lineSeparator());

					
					bKmlWriterUnmatchedMatched.write("			<LineString>" + System.lineSeparator());
					bKmlWriterUnmatchedMatched.write("				<coordinates>" + System.lineSeparator());
					bKmlWriterUnmatchedMatched.write(lonFormat.format(unmatchedGeoPos.getLongitude()) + ","
							+ latFormat.format(unmatchedGeoPos.getLatitude()) + ",0" + System.lineSeparator());
					
					bKmlWriterUnmatchedMatched.write(lonFormat.format(matchedGeoPos.getLongitude()) + ","
							+ latFormat.format(matchedGeoPos.getLatitude()) + ",0" + System.lineSeparator());
					
					bKmlWriterUnmatchedMatched.write("				</coordinates>" + System.lineSeparator());
					bKmlWriterUnmatchedMatched.write("			</LineString>" + System.lineSeparator());
					bKmlWriterUnmatchedMatched.write("			<Style> " + System.lineSeparator());
					bKmlWriterUnmatchedMatched.write("				<LineStyle>  " + System.lineSeparator());
					bKmlWriterUnmatchedMatched.write("					<color>#ff00FFFF</color>" + System.lineSeparator());
					bKmlWriterUnmatchedMatched.write("					<width>5</width>" + System.lineSeparator());
					bKmlWriterUnmatchedMatched.write("				</LineStyle> " + System.lineSeparator());
					bKmlWriterUnmatchedMatched.write("			</Style>" + System.lineSeparator());
					bKmlWriterUnmatchedMatched.write("		</Placemark>" + System.lineSeparator());
					bKmlWriterUnmatchedMatched.write("" + System.lineSeparator());
					
				}
				
				matchedGPSNodeIndex++;
			}
			
			bKmlWriterUnmatchedMatched.write("	</Document>" + System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("</kml>" + System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("" + System.lineSeparator());
	
			bKmlWriterUnmatchedMatched.close();
		}
		catch (Exception e) {
			System.out.println("Error: createKmlUnmatchedMatched \n" + e.toString());
		}
		
	}
	
	private static void createKmlMatched(Vector<MatchedGPSNode> gpsNodesToMatch, String filePath) {

		try{
			File kmlMatched = new File(filePath + ".matched.kml");
			BufferedWriter bKmlWriterMatched = new BufferedWriter(new FileWriter(kmlMatched));
			
			bKmlWriterMatched.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
			bKmlWriterMatched.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">" + System.lineSeparator());
			bKmlWriterMatched.write("	<Document>" + System.lineSeparator());
			bKmlWriterMatched.write("		<name>" + kmlMatched.getName() + "</name>" + System.lineSeparator());
			bKmlWriterMatched.write("" + System.lineSeparator());
			bKmlWriterMatched.write("		<Placemark>" + System.lineSeparator());
			bKmlWriterMatched.write("			<name>matched</name>" + System.lineSeparator());
			bKmlWriterMatched.write("			<LineString>" + System.lineSeparator());
			bKmlWriterMatched.write("				<coordinates>" + System.lineSeparator());
			
			for (MatchedGPSNode matchedGPSNode : gpsNodesToMatch) {
	
				if (matchedGPSNode.isMatched()) {
					
					GeoPosition matchedGeoPos = Coordinates.getGeoPos(matchedGPSNode.getMatchedX(),	matchedGPSNode.getMatchedY());
					
					if (matchedGPSNode.isReordered) {
						matchedGeoPos = Coordinates.getGeoPos(matchedGPSNode.matchedXreordered,	matchedGPSNode.matchedYreordered);
					}
					
					bKmlWriterMatched.write(lonFormat.format(matchedGeoPos.getLongitude()) + ","
							+ latFormat.format(matchedGeoPos.getLatitude()) + ",0" + System.lineSeparator());
					
				}
			
			}
			
			bKmlWriterMatched.write("				</coordinates>" + System.lineSeparator());
			bKmlWriterMatched.write("			</LineString>" + System.lineSeparator());
			bKmlWriterMatched.write("			<Style> " + System.lineSeparator());
			bKmlWriterMatched.write("				<LineStyle>  " + System.lineSeparator());
			bKmlWriterMatched.write("					<color>#ff00FF00</color>" + System.lineSeparator());
			bKmlWriterMatched.write("					<width>5</width>" + System.lineSeparator());
			bKmlWriterMatched.write("				</LineStyle> " + System.lineSeparator());
			bKmlWriterMatched.write("			</Style>" + System.lineSeparator());
			bKmlWriterMatched.write("		</Placemark>" + System.lineSeparator());
			bKmlWriterMatched.write("" + System.lineSeparator());
			bKmlWriterMatched.write("	</Document>" + System.lineSeparator());
			bKmlWriterMatched.write("</kml>" + System.lineSeparator());
			bKmlWriterMatched.write("" + System.lineSeparator());
			
			bKmlWriterMatched.close();
		}
		catch (Exception e) {
			System.out.println("Error: createKmlUnmatched \n" + e.toString());
		}
	}

	private static void createKmlUnmatched(Vector<MatchedGPSNode> gpsNodesToMatch, String filePath)  {
		
		try {
			
			File kmlUnmatched = new File(filePath + ".unmatched.kml");
			BufferedWriter bKmlWriterUnmatched = new BufferedWriter(new FileWriter(kmlUnmatched));
			
			bKmlWriterUnmatched.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
			bKmlWriterUnmatched.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">" + System.lineSeparator());
			bKmlWriterUnmatched.write("	<Document>" + System.lineSeparator());
			bKmlWriterUnmatched.write("		<name>" + kmlUnmatched.getName() + "</name>" + System.lineSeparator());
			bKmlWriterUnmatched.write("" + System.lineSeparator());
			bKmlWriterUnmatched.write("		<Placemark>" + System.lineSeparator());
			bKmlWriterUnmatched.write("			<name>unmatched</name>" + System.lineSeparator());
			bKmlWriterUnmatched.write("			<LineString>" + System.lineSeparator());
			bKmlWriterUnmatched.write("				<coordinates>" + System.lineSeparator());
			
			for (MatchedGPSNode matchedGPSNode : gpsNodesToMatch) {
				
				if (matchedGPSNode.isMatched()) {
					
					GeoPosition unmatchedGeoPos = Coordinates.getGeoPos(matchedGPSNode.getX(), matchedGPSNode.getY());
					
					bKmlWriterUnmatched.write(lonFormat.format(unmatchedGeoPos.getLongitude()) + ","
							+ latFormat.format(unmatchedGeoPos.getLatitude()) + ",0" + System.lineSeparator());
					
				}
				
			}
			
			bKmlWriterUnmatched.write("				</coordinates>" + System.lineSeparator());
			bKmlWriterUnmatched.write("			</LineString>" + System.lineSeparator());
			bKmlWriterUnmatched.write("			<Style> " + System.lineSeparator());
			bKmlWriterUnmatched.write("				<LineStyle>  " + System.lineSeparator());
			bKmlWriterUnmatched.write("					<color>#ff0000FF</color>" + System.lineSeparator());
			bKmlWriterUnmatched.write("					<width>5</width>" + System.lineSeparator());
			bKmlWriterUnmatched.write("				</LineStyle> " + System.lineSeparator());
			bKmlWriterUnmatched.write("			</Style>" + System.lineSeparator());
			bKmlWriterUnmatched.write("		</Placemark>" + System.lineSeparator());
			bKmlWriterUnmatched.write("" + System.lineSeparator());
			bKmlWriterUnmatched.write("	</Document>" + System.lineSeparator());
			bKmlWriterUnmatched.write("</kml>" + System.lineSeparator());
			bKmlWriterUnmatched.write("" + System.lineSeparator());
			
	
			bKmlWriterUnmatched.close();
		}
		catch (Exception e) {
			System.out.println("Error: createKmlUnmatched \n" + e.toString());
		}
	}

	private static void createOsm(Vector<MatchedNLink> matchedNLinks, File osmFile, String filePath) {

		String FilePath = osmFile.getPath();

		TreeSet<Long> TreeSetWayIds = new TreeSet<Long>();
		TreeSet<Long> TreeSetNodeIds = new TreeSet<Long>();

		double minlat = Double.MAX_VALUE;
		double maxlat = Double.MIN_VALUE;
		double minlon = Double.MAX_VALUE;
		double maxlon = Double.MIN_VALUE;

		for (int i = 0; i < matchedNLinks.size(); i++) {

			myOSMWayPart wp = matchedNLinks.get(i).getStreetLink();

			if (TreeSetWayIds.contains(wp.parentWay.id) == false) {
				TreeSetWayIds.add(wp.parentWay.id);
			}

			if (TreeSetNodeIds.contains(wp.startNode.id) == false) {
				TreeSetNodeIds.add(wp.startNode.id);
			}

			if (TreeSetNodeIds.contains(wp.endNode.id) == false) {
				TreeSetNodeIds.add(wp.endNode.id);
			}

			if ( wp.startNode.lat < minlat ) { minlat = wp.startNode.lat; } 
			if ( maxlat < wp.startNode.lat ) { maxlat = wp.startNode.lat; } 
			if ( wp.startNode.lon < minlon ) { minlon = wp.startNode.lon; } 
			if ( maxlon < wp.startNode.lon ) { maxlon = wp.startNode.lon; } 
			if ( wp.endNode.lat < minlat ) { minlat = wp.endNode.lat; } 
			if ( maxlat < wp.endNode.lat ) { maxlat = wp.endNode.lat; } 
			if ( wp.endNode.lon < minlon ) { minlon = wp.endNode.lon; } 
			if ( maxlon < wp.endNode.lon ) { maxlon = wp.endNode.lon; } 

		}

		try {
			BufferedReader bReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(FilePath)),"UTF-8"));

			String FilePathNew = filePath + ".route.osm.xml";
			
			BufferedWriter bWriter;
			bWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(FilePathNew)), "UTF-8"));

			String line = bReader.readLine();

			String tline = line.trim();

			while (line != null) {
				if (tline.startsWith("<node id=\"")) {

					String s = tline.replace("<node id=\"", "").split("\"")[0];

					long id = Long.parseLong(s);

					boolean write = TreeSetNodeIds.contains(id);

					if (tline.endsWith("/>")) {
						if (write) {
							bWriter.write(line + System.lineSeparator());
						}
					} else if (tline.endsWith(">")) {

						if (write) {
							bWriter.write(line + System.lineSeparator());
						}

						line = bReader.readLine();
						tline = line.trim();

						while (tline.equals("</node>") == false) {
							if (write) {
								bWriter.write(line + System.lineSeparator());
							}
							line = bReader.readLine();
							tline = line.trim();
						}

						if (write) {
							bWriter.write(line + System.lineSeparator());
						}

					} else {
						System.out.println("Error: End of node: " + id);
					}

				} else if (tline.startsWith("<way id=\"")) {

					String s = tline.replace("<way id=\"", "").split("\"", 2)[0];
					long id = Long.parseLong(s);
					boolean write = TreeSetWayIds.contains(id);

					if (write) {
						bWriter.write(line + System.lineSeparator());
					}

					line = bReader.readLine();
					tline = line.trim();

					while (tline.startsWith("</way>") == false) {
						if (write) {
							bWriter.write(line + System.lineSeparator());
						}

						line = bReader.readLine();
						tline = line.trim();
					}

					if (write) {
						bWriter.write(line + System.lineSeparator());
					}

				} else if (tline.startsWith("<bounds minlat=")) {					
					bWriter.write("	<bounds minlat=\"" + minlat + "\" minlon=\"" + minlon + "\" maxlat=\"" + maxlat + "\" maxlon=\"" + maxlon + "\"/>" + System.lineSeparator());
				} else {
					bWriter.write(line + System.lineSeparator());
				}

				line = bReader.readLine();
				if (line != null) {
					tline = line.trim();
				}
			}

			bReader.close();
			bWriter.close();

		} catch (Exception e) {
			System.out.println("Error: createKmlOsm " + System.lineSeparator() + e.toString());
		}

	}

	private static void createKmlRoute(Vector<MatchedNLink> matchedNLinks, String filePath) {
		
		try {

			File kmlRoute = new File(filePath + ".route.kml");
			BufferedWriter bKmlWriterRoute = new BufferedWriter(new FileWriter(kmlRoute));

			bKmlWriterRoute.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
			bKmlWriterRoute.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">" + System.lineSeparator());
			bKmlWriterRoute.write("	<Document>" + System.lineSeparator());
			bKmlWriterRoute.write("		<name>" + kmlRoute.getName() + "</name>" + System.lineSeparator());
			bKmlWriterRoute.write("" + System.lineSeparator());
			
			for (int i = 0; i < matchedNLinks.size(); i++) {

				MatchedNLink m = matchedNLinks.get(i);
				myOSMWayPart wp = m.getStreetLink();

				myOSMNode n1 = wp.startNode;
				myOSMNode n2 = wp.endNode;
				String edgeID = "null";
				
				if (wp.edge != null) {
					edgeID = wp.edge.id_str;
				}
				

				bKmlWriterRoute.write("		<Placemark>" + System.lineSeparator());
				bKmlWriterRoute.write("			<name>" + wp.parentWay.id + " " + m.matchedGPSNodes.size() + " | " + m.objID
						+ "</name>" + System.lineSeparator());
				bKmlWriterRoute.write("			<description>" + edgeID + " | " + wp.parentWay.id + " " + m.matchedGPSNodes.size() + " | " + m.objID
						+ "</description>" + System.lineSeparator());
				bKmlWriterRoute.write("			<LineString>" + System.lineSeparator());
				bKmlWriterRoute.write("				<coordinates>" + System.lineSeparator());

				bKmlWriterRoute.write(n1.lon + "," + n1.lat + ",0" + System.lineSeparator());
				bKmlWriterRoute.write(n2.lon + "," + n2.lat + ",0" + System.lineSeparator());

				bKmlWriterRoute.write("				</coordinates>" + System.lineSeparator());
				bKmlWriterRoute.write("			</LineString>" + System.lineSeparator());
				bKmlWriterRoute.write("			<Style> " + System.lineSeparator());
				bKmlWriterRoute.write("				<LineStyle>  " + System.lineSeparator());

				if (m.isMatched()) {
					bKmlWriterRoute.write("					<color>#ff000000</color>" + System.lineSeparator());
				} else {
					bKmlWriterRoute.write("					<color>#ffffffff</color>" + System.lineSeparator());
				}

				bKmlWriterRoute.write("					<width>5</width>" + System.lineSeparator());
				bKmlWriterRoute.write("				</LineStyle> " + System.lineSeparator());
				bKmlWriterRoute.write("			</Style>" + System.lineSeparator());
				bKmlWriterRoute.write("		</Placemark>" + System.lineSeparator());
				bKmlWriterRoute.write("" + System.lineSeparator());

			}

			bKmlWriterRoute.write("	</Document>" + System.lineSeparator());
			bKmlWriterRoute.write("</kml>" + System.lineSeparator());
			bKmlWriterRoute.write("" + System.lineSeparator());
			
			bKmlWriterRoute.close();
			
		}
		catch (Exception e) {
			System.out.println("Error: createKmlRoute \n" + e.toString());
		}

	}
	
	private static void createKmlReordered(Vector<MatchedGPSNode> gpsNodesToMatch, String filePath) {
		
		try {
			File kmlUnmatchedMatched = new File(filePath + ".reordered.kml");
			BufferedWriter bKmlWriterUnmatchedMatched = new BufferedWriter(new FileWriter(kmlUnmatchedMatched));
			
			bKmlWriterUnmatchedMatched.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">" + System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("	<Document>" + System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("		<name>" + kmlUnmatchedMatched.getName() + "</name>" + System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("" + System.lineSeparator());
			
			int matchedGPSNodeIndex = 0;
			
			for (MatchedGPSNode matchedGPSNode : gpsNodesToMatch) {
				// get just matched GPS Points
				if (matchedGPSNode.isMatched()) {
					
					if (matchedGPSNode.isReordered) {

						Calendar c = Calendar.getInstance();
						//long t = matchedGPSNode.getTimestamp() + timeStampOffSet;
						long t = matchedGPSNode.getTimestamp();
						t = t / 1000000L;
						c.setTimeInMillis(t);
						
						GeoPosition matchedGeoPos = Coordinates.getGeoPos(matchedGPSNode.getMatchedX(),	matchedGPSNode.getMatchedY());

						GeoPosition	matchedGeoPosReordered = Coordinates.getGeoPos(matchedGPSNode.matchedXreordered,	matchedGPSNode.matchedYreordered);
						
						bKmlWriterUnmatchedMatched.write("		<Placemark>" + System.lineSeparator());
						bKmlWriterUnmatchedMatched.write("			<name>" + (matchedGPSNode.getTimestamp())
								+ " " + matchedGPSNodeIndex + " " + c.getTime().toString() + "</name>" + System.lineSeparator());
						
						bKmlWriterUnmatchedMatched.write("			<LineString>" + System.lineSeparator());
						bKmlWriterUnmatchedMatched.write("				<coordinates>" + System.lineSeparator());
						
						bKmlWriterUnmatchedMatched.write(lonFormat.format(matchedGeoPos.getLongitude()) + ","
								+ latFormat.format(matchedGeoPos.getLatitude()) + ",0" + System.lineSeparator());

						bKmlWriterUnmatchedMatched.write(lonFormat.format(matchedGeoPosReordered.getLongitude()) + ","
								+ latFormat.format(matchedGeoPosReordered.getLatitude()) + ",0" + System.lineSeparator());

						bKmlWriterUnmatchedMatched.write("				</coordinates>" + System.lineSeparator());
						bKmlWriterUnmatchedMatched.write("			</LineString>" + System.lineSeparator());
						bKmlWriterUnmatchedMatched.write("			<Style> " + System.lineSeparator());
						bKmlWriterUnmatchedMatched.write("				<LineStyle>  " + System.lineSeparator());
						bKmlWriterUnmatchedMatched.write("					<color>#ff00FFFF</color>" + System.lineSeparator());
						bKmlWriterUnmatchedMatched.write("					<width>5</width>" + System.lineSeparator());
						bKmlWriterUnmatchedMatched.write("				</LineStyle> " + System.lineSeparator());
						bKmlWriterUnmatchedMatched.write("			</Style>" + System.lineSeparator());
						bKmlWriterUnmatchedMatched.write("		</Placemark>" + System.lineSeparator());
						bKmlWriterUnmatchedMatched.newLine();
					}

				}
				
				matchedGPSNodeIndex++;
			}
			
			bKmlWriterUnmatchedMatched.write("	</Document>" + System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("</kml>" + System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("" + System.lineSeparator());
	
			bKmlWriterUnmatchedMatched.close();
		}
		catch (Exception e) {
			System.out.println("Error: createKmlUnmatchedMatched \n" + e.toString());
		}
		
	}

	private static void createKmlDatasetsUp(Vector<myDataset> DatasetsUp, String filePath)  {
		filePath = filePath + ".DatasetsUp.kml";
		createKmlDatasets(DatasetsUp, filePath);
	}
	
	private static void createKmlDatasetsDown(Vector<myDataset> DatasetsDown, String filePath)  {
		filePath = filePath + ".DatasetsDown.kml";
		createKmlDatasets(DatasetsDown, filePath);
	}
	
	private static void createKmlDatasets(Vector<myDataset> DatasetsDown, String filePath)  {
		try {
			File kmlDatasetsUp = new File(filePath);
			BufferedWriter bKmlWriterDatasetsUp = new BufferedWriter(new FileWriter(kmlDatasetsUp));

			bKmlWriterDatasetsUp.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("	<Document>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("		<name>" + kmlDatasetsUp.getName() + "</name>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("" + System.lineSeparator());

			myDataset ds1 = null;

			int i;
			for (i = 0; i < DatasetsDown.size() - 1; i++) {
				ds1 = DatasetsDown.get(i);
				if (ds1.isMatched) {
					break;
				}
			}

			for (i = (i + 1); i < DatasetsDown.size(); i++) {

				myDataset ds2 = DatasetsDown.get(i);

				if (ds2.isMatched) {

					bKmlWriterDatasetsUp.write("		<Placemark>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			<name>" + ds1.getTimestamp() + "</name>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			<description>" + ds1.getTimestamp() + System.lineSeparator() +
					"datarate: " + ds1.datarate + System.lineSeparator() + 
					"delay: " + ds1.delay + System.lineSeparator() + 
					"loss rate: " + ds1.loss_rate + "</description>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			<LineString>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("				<coordinates>" + System.lineSeparator());
					
					GeoPosition GeoPos1 = Coordinates.getGeoPos(ds1.X, ds1.Y);
					
					bKmlWriterDatasetsUp.write(lonFormat.format(GeoPos1.getLongitude()) + ","
							+ latFormat.format(GeoPos1.getLatitude()) + ",0" + System.lineSeparator());
					
					GeoPosition GeoPos2 = Coordinates.getGeoPos(ds2.X, ds2.Y);
					
					bKmlWriterDatasetsUp.write(lonFormat.format(GeoPos2.getLongitude()) + ","
							+ latFormat.format(GeoPos2.getLatitude()) + ",0" + System.lineSeparator());
					
					bKmlWriterDatasetsUp.write("				</coordinates>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			</LineString>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			<Style> " + System.lineSeparator());
					bKmlWriterDatasetsUp.write("				<LineStyle>  " + System.lineSeparator());
					bKmlWriterDatasetsUp.write("					<color>#ffFF00FF</color>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("					<width>5</width>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("				</LineStyle> " + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			</Style>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("		</Placemark>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("" + System.lineSeparator());
					
					ds1 = ds2;
				}
			}

			bKmlWriterDatasetsUp.write("	</Document>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("</kml>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("" + System.lineSeparator());

			bKmlWriterDatasetsUp.close();
		}
		catch (Exception e) {
			System.out.println("Error: createKmlDatasets \n" + e.toString());
		}
	}

	private static void createKmlDatasetsDatarateColor(Vector<myDataset> DatasetsDown, String filePath, boolean kmlNorm, double _minData, double _maxData) {
		filePath = filePath + ".ColorDatarate.kml";
		createKmlDatasetsDataColor(DatasetsDown, filePath, "datarate", kmlNorm, _minData, _maxData);
	}
	
	private static void createKmlDatasetsDelayColor(Vector<myDataset> DatasetsDown, String filePath, boolean kmlNorm, double _minData, double _maxData) {
		filePath = filePath + ".ColorDelay.kml";
		createKmlDatasetsDataColor(DatasetsDown, filePath, "delay", kmlNorm, _minData, _maxData);
	}
	
	private static void createKmlDatasetsLossRateColor(Vector<myDataset> DatasetsDown, String filePath, boolean kmlNorm, double _minData, double _maxData) {
		filePath = filePath + ".ColorLoss_rate.kml";
		createKmlDatasetsDataColor(DatasetsDown, filePath, "loss_rate", kmlNorm, _minData, _maxData);
	}

	private static void createKmlDatasetsDataColor(Vector<myDataset> Datasets, String filePath, String dataType, boolean kmlNorm, double _minData, double _maxData)  {
		try {
			double minData = Double.MAX_VALUE;
			double maxData = -Double.MAX_VALUE;
			
			if (kmlNorm) {
				if (dataType.equals("datarate")) {
					for (myDataset ds : Datasets) {
						if (ds.isMatched && ds.datarate != -1) {
							if (ds.datarate < minData) {
								minData = ds.datarate;
							}
							if (maxData < ds.datarate) {
								maxData = ds.datarate;
							}
						}
					}
				} else if (dataType.equals("delay")) {
					for (myDataset ds : Datasets) {
						if (ds.isMatched && ds.delay != -1) {
							if (ds.delay < minData) {
								minData = ds.delay;
							}
							if (maxData < ds.delay) {
								maxData = ds.delay;
							}
						}
					}
				} else {
					for (myDataset ds : Datasets) {
						if (ds.isMatched && ds.loss_rate != -1) {
							if (ds.loss_rate < minData) {
								minData = ds.loss_rate;
							}
							if (maxData < ds.loss_rate) {
								maxData = ds.loss_rate;
							}
						}
					}
				}
			} else {
				minData = _minData;
				maxData = _maxData;
			}

			
			double devData = maxData - minData;
			
			File kmlDatasetsUp = new File(filePath);
			BufferedWriter bKmlWriterDatasetsUp = new BufferedWriter(new FileWriter(kmlDatasetsUp));

			bKmlWriterDatasetsUp.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("	<Document>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("		<name>" + kmlDatasetsUp.getName() + "</name>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("		<description> " + dataType + " : " + minData + " to " + maxData + "</description>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("" + System.lineSeparator());

			myDataset ds1 = null;

			int i;
			for (i = 0; i < Datasets.size() - 1; i++) {
				ds1 = Datasets.get(i);
				if (ds1.isMatched) {
					if (dataType.equals("datarate")) {
						if (ds1.datarate != -1) {
							break;
						}
					} else if (dataType.equals("delay")) {
						if (ds1.delay != -1) {
							break;
						}
					} else {
						if (ds1.loss_rate != -1) {
							break;
						}
					}
				}
			}

			for (i = (i + 1); i < Datasets.size(); i++) {

				myDataset ds2 = Datasets.get(i);

				if (ds2.isMatched && (
					(dataType.equals("datarate") && ds2.datarate != -1)
					||
					(dataType.equals("delay") && ds2.delay != -1)
					||
					(dataType.equals("loss_rate") && ds2.loss_rate != -1)
				)) {
					
					String color = "ff000000";
					
					double f;
					
					if (dataType.equals("datarate")) {
						if (kmlNorm) {
							f = ds1.datarate - minData;
						} else if (ds1.datarate < minData) {
							f = minData;
						} else if (maxData < ds1.datarate) {
							f = maxData;
						} else {
							f = ds1.datarate - minData;
						}
					} else if (dataType.equals("delay")) {
						if (kmlNorm) {
							f = ds1.delay - minData;
						} else if (ds1.delay < minData) {
							f = minData;
						} else if (maxData < ds1.delay) {
							f = maxData;
						} else {
							f = ds1.delay - minData;
						}
					} else {
						if (kmlNorm) {
							f = ds1.loss_rate - minData;
						} else if (ds1.loss_rate < minData) {
							f = minData;
						} else if (maxData < ds1.loss_rate) {
							f = maxData;
						} else {
							f = ds1.loss_rate - minData;
						}
					}
					
					f = f / devData;
					
					if (devData == 0) {
						f = 0;
					}
					
					if (dataType.equals("datarate") == false) {
						f = 1.0F - f;
					}
					
	                // gelb bis gr�n

	                f = f * 2.5F;

	                if (f <= 0.5) {
	                    // gelb bis rot

	                    f = f + f;
	                    f = 255.0F * f;
	                    int ig = 255 - (int)f;

	                    String g = Integer.toHexString(ig);
	                    
	                    if (g.length() == 1) { g = "0" + g; }

	                    color = "ff00" + g + "ff";
	                }
	                else if (0.5 < f && f <= 1.5) {
	                    // rot bis blau

	                    f = f - 0.5F;
	                    f = 255.0F * f;

	                    int ir = 255 - (int)f;
	                    int ib = (int)f;

	                    String r = Integer.toHexString(ir);
	                    if (r.length() == 1) { r = "0" + r; }
	                    String b = Integer.toHexString(ib);
	                    if (b.length() == 1) { b = "0" + b; }

	                    color = "ff" + b + "00" + r;
	                }
	                else if (1.5 < f) {
	                    // blau bis gr�n

	                    f = f - 1.5F;
	                    f = 255.0F * f;

	                    int ib = 255 - (int)f;
	                    int ig = (int)f;

	                    String g = Integer.toHexString(ig);
	                    if (g.length() == 1) { g = "0" + g; }
	                    String b = Integer.toHexString(ib);
	                    if (b.length() == 1) { b = "0" + b; }

	                    color = "ff" + b + g + "00";
	                }
	                
					bKmlWriterDatasetsUp.write("		<Placemark>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			<name>" + ds1.getTimestamp() + "</name>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			<description>" +
					"datarate: " + ds1.datarate + System.lineSeparator() + 
					"delay: " + ds1.delay + System.lineSeparator() + 
					"loss rate: " + ds1.loss_rate + "</description>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			<LineString>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("				<coordinates>" + System.lineSeparator());
					
					GeoPosition GeoPos1 = Coordinates.getGeoPos(ds1.X, ds1.Y);
					
					bKmlWriterDatasetsUp.write(lonFormat.format(GeoPos1.getLongitude()) + ","
							+ latFormat.format(GeoPos1.getLatitude()) + ",0" + System.lineSeparator());
					
					GeoPosition GeoPos2 = Coordinates.getGeoPos(ds2.X, ds2.Y);
					
					bKmlWriterDatasetsUp.write(lonFormat.format(GeoPos2.getLongitude()) + ","
							+ latFormat.format(GeoPos2.getLatitude()) + ",0" + System.lineSeparator());
					
					bKmlWriterDatasetsUp.write("				</coordinates>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			</LineString>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			<Style> " + System.lineSeparator());
					bKmlWriterDatasetsUp.write("				<LineStyle>  " + System.lineSeparator());
					bKmlWriterDatasetsUp.write("					<color>#" + color + "</color>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("					<width>5</width>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("				</LineStyle> " + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			</Style>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("		</Placemark>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("" + System.lineSeparator());
					
					ds1 = ds2;
				}
			}

			bKmlWriterDatasetsUp.write("	</Document>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("</kml>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("" + System.lineSeparator());

			bKmlWriterDatasetsUp.close();
		}
		catch (Exception e) {
			System.out.println("Error: createKmlDatasets \n" + e.toString());
		}
	}

	
	private static void createKmlDatasetsUpUnMatched(Vector<myDataset> DatasetsUp, String filePath)  {
		filePath = filePath + ".DatasetsUpUnMatched.kml";
		createKmlDatasetsUnMatched(DatasetsUp, filePath, "FF88FF00");
	}
	
	private static void createKmlDatasetsDownUnMatched(Vector<myDataset> DatasetsDown, String filePath)  {
		filePath = filePath + ".DatasetsDownUnMatched.kml";
		createKmlDatasetsUnMatched(DatasetsDown, filePath, "FFFF8800");
	}
	
	private static void createKmlDatasetsUnMatched(Vector<myDataset> DatasetsDown, String filePath, String color)  {
		try {
			File kmlDatasetsUp = new File(filePath);
			BufferedWriter bKmlWriterDatasetsUp = new BufferedWriter(new FileWriter(kmlDatasetsUp));

			bKmlWriterDatasetsUp.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("	<Document>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("		<name>" + kmlDatasetsUp.getName() + "</name>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("" + System.lineSeparator());

			myDataset ds1 = null;

			for (int i = 0; i < DatasetsDown.size(); i++) {

				ds1 = DatasetsDown.get(i);

				if (ds1.isMatched) {
					bKmlWriterDatasetsUp.write("		<Placemark>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			<name>" + ds1.getTimestamp() + "</name>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			<description>" + ds1.getTimestamp() + System.lineSeparator() +
							"datarate: " + ds1.datarate + System.lineSeparator() + 
							"delay: " + ds1.delay + System.lineSeparator() + 
							"loss rate: " + ds1.loss_rate + "</description>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			<LineString>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("				<coordinates>" + System.lineSeparator());
					
					GeoPosition GeoPos1 = Coordinates.getGeoPos(ds1.Xunmatched, ds1.Yunmatched);
					
					bKmlWriterDatasetsUp.write(lonFormat.format(GeoPos1.getLongitude()) + ","
							+ latFormat.format(GeoPos1.getLatitude()) + ",0" + System.lineSeparator());
					
					GeoPosition GeoPos2 = Coordinates.getGeoPos(ds1.X, ds1.Y);
					
					bKmlWriterDatasetsUp.write(lonFormat.format(GeoPos2.getLongitude()) + ","
							+ latFormat.format(GeoPos2.getLatitude()) + ",0" + System.lineSeparator());
					
					bKmlWriterDatasetsUp.write("				</coordinates>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			</LineString>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			<Style> " + System.lineSeparator());
					bKmlWriterDatasetsUp.write("				<LineStyle>  " + System.lineSeparator());
					bKmlWriterDatasetsUp.write("					<color>#" + color + "</color>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("					<width>5</width>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("				</LineStyle> " + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			</Style>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("		</Placemark>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("" + System.lineSeparator());
				}
			}

			bKmlWriterDatasetsUp.write("	</Document>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("</kml>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("" + System.lineSeparator());

			bKmlWriterDatasetsUp.close();
		}
		catch (Exception e) {
			System.out.println("Error: createKmlDatasetsUp \n" + e.toString());
		}
	}

	private static void createKmlDatasetsUnMatchedRouteDistribution(Vector<myDataset> DatasetsDown, String filePath, String color)  {

		try {
			File kmlDatasetsUp = new File(filePath);
			BufferedWriter bKmlWriterDatasetsUp = new BufferedWriter(new FileWriter(kmlDatasetsUp));

			bKmlWriterDatasetsUp.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("	<Document>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("		<name>" + kmlDatasetsUp.getName() + "</name>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("" + System.lineSeparator());

			myDataset ds1 = null;

			for (int i = 0; i < DatasetsDown.size(); i++) {

				ds1 = DatasetsDown.get(i);

				if (ds1.isMatched) {
					bKmlWriterDatasetsUp.write("		<Placemark>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			<name>" + ds1.getTimestamp() + "</name>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			<LineString>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("				<coordinates>" + System.lineSeparator());

					GeoPosition GeoPos1 = Coordinates.getGeoPos(ds1.Xunmatched, ds1.Yunmatched);

					bKmlWriterDatasetsUp.write(lonFormat.format(GeoPos1.getLongitude()) + ","
							+ latFormat.format(GeoPos1.getLatitude()) + ",0" + System.lineSeparator());

					GeoPosition GeoPos2 = Coordinates.getGeoPos(ds1.X_RouteDistribution, ds1.Y_RouteDistribution);

					bKmlWriterDatasetsUp.write(lonFormat.format(GeoPos2.getLongitude()) + ","
							+ latFormat.format(GeoPos2.getLatitude()) + ",0" + System.lineSeparator());

					bKmlWriterDatasetsUp.write("				</coordinates>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			</LineString>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			<Style> " + System.lineSeparator());
					bKmlWriterDatasetsUp.write("				<LineStyle>  " + System.lineSeparator());
					bKmlWriterDatasetsUp.write("					<color>#" + color + "</color>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("					<width>5</width>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("				</LineStyle> " + System.lineSeparator());
					bKmlWriterDatasetsUp.write("			</Style>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("		</Placemark>" + System.lineSeparator());
					bKmlWriterDatasetsUp.write("" + System.lineSeparator());
				}
			}

			bKmlWriterDatasetsUp.write("	</Document>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("</kml>" + System.lineSeparator());
			bKmlWriterDatasetsUp.write("" + System.lineSeparator());

			bKmlWriterDatasetsUp.close();
		}
		catch (Exception e) {
			System.out.println("Error: createKmlDatasetsUp \n" + e.toString());
		}
	}

	private static void createCsvFromGPS(myOSMMap myMap, Vector<MatchedNLink> matchedNLinks, String filePath) {

		filePath = filePath.replace(".", "_GPS.");

		try {
			// wrap with buffered writer
			//BufferedWriter bWriter_old = new BufferedWriter(new FileWriter(gpsTracefile_old));

			File gpsTracefile = new File(filePath);
			
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(gpsTracefile));

			bWriter.write("type,timestamp,matched_latitude,matched_longitude,unmatched_latitude,unmatched_longitude,unMatched_distance,matched_distribution_in_WayParty,startNode_id,endNode_id,edge_id_str,length_in_edge,length_of_edge,Dataset_Up_Timestamp,Dataset_Up_datarate,Dataset_Up_delay,Dataset_Up_loss rate,Dataset_Down_Timestamp,Dataset_Down_datarate,Dataset_Down_delay,Dataset_Down_loss_rate,reordered");
			bWriter.newLine();

			for (int i = 0; i < matchedNLinks.size(); i++) {

				MatchedNLink matchedNLink = matchedNLinks.get(i);

				if (matchedNLink.matchedGPSNodes.size() == 0 && matchedNLink.getStreetLink().edge != null) {

					if (matchedNLink.getStreetLink().CountMatchedGPSNodes == 0) {

						myOSMWayPart WayPartBackDirektion = matchedNLink.getStreetLink().WayPartBackDirektion;

						if (WayPartBackDirektion == null || WayPartBackDirektion.CountMatchedGPSNodes == 0) {

							long lastTimeStamp = -1;
							MatchedNLink lastMatchedNLink = null;
							MatchedGPSNode lastMatchedGPSNode = null;

							double disFromLast = 0;

							for (int j = i - 1; j >= 0; j--) {
								if (matchedNLinks.get(j).matchedGPSNodes.size() == 0) {
									disFromLast += matchedNLinks.get(j).getStreetLink().length;
								} else {
									lastMatchedNLink = matchedNLinks.get(j);
									lastMatchedGPSNode = lastMatchedNLink.matchedGPSNodes.lastElement();
									lastTimeStamp = lastMatchedGPSNode.getTimestamp();
									j = -1;
								}
							}
							
							double lastLength = lastMatchedNLink.getStreetLink().length;
							double lastDistribution = 1.0 - lastMatchedGPSNode.matched_distribution_in_WayParty;
							
							if (lastMatchedGPSNode.isReordered) {
								lastDistribution = 1.0 - lastMatchedGPSNode.matched_distribution_in_WayPartyReordered;
							}
							
							lastLength = lastLength * lastDistribution;
							
							if (lastMatchedGPSNode.isReordered) {
								disFromLast += lastMatchedNLink.getStreetLink().length
										* (1.0 - lastMatchedGPSNode.matched_distribution_in_WayPartyReordered);
							} else {
								disFromLast += lastMatchedNLink.getStreetLink().length
										* (1.0 - lastMatchedGPSNode.matched_distribution_in_WayParty);
							}

							disFromLast += matchedNLink.getStreetLink().length * 0.5;
							
							long nextTimeStamp = -1;
							MatchedNLink nextMatchedNLink = null;
							MatchedGPSNode nextMatchedGPSNode = null;

							double disToNext = 0;

							int j;
							for (j = i + 1; j < matchedNLinks.size(); j++) {
								if (matchedNLinks.get(j).matchedGPSNodes.size() == 0) {
									disToNext += matchedNLinks.get(j).getStreetLink().length;
								} else {
									nextMatchedNLink = matchedNLinks.get(j);
									nextMatchedGPSNode = nextMatchedNLink.matchedGPSNodes.firstElement();
									nextTimeStamp = nextMatchedGPSNode.getTimestamp();
									j = matchedNLinks.size();
								}
							}

							if (nextMatchedNLink == null) {
								continue;
							}
							
							double nextLength = nextMatchedNLink.getStreetLink().length;
							double nextDistribution = nextMatchedGPSNode.matched_distribution_in_WayParty;
							
							if (nextMatchedGPSNode.isReordered) {
								nextDistribution = nextMatchedGPSNode.matched_distribution_in_WayPartyReordered;
							}
							
							nextLength = nextLength * nextDistribution;
							
							try {
								if (nextMatchedGPSNode.isReordered) {
									disToNext += nextMatchedNLink.getStreetLink().length
											* nextMatchedGPSNode.matched_distribution_in_WayPartyReordered;
								} else {
									disToNext += nextMatchedNLink.getStreetLink().length
											* nextMatchedGPSNode.matched_distribution_in_WayParty;
								}
							} catch (Exception e) {
								System.out.println(e.toString());
								e.printStackTrace();
							}

							double totalDistance = disFromLast + matchedNLink.getStreetLink().length + disToNext;

							long totalTime = nextTimeStamp - lastTimeStamp;

							double percent = disFromLast / totalDistance;

							long newStartTimeStamp = (long) (((double) totalTime) * percent);
							newStartTimeStamp += lastTimeStamp;

							percent = disToNext / totalDistance;

							long newEndTimeStamp = (long) (((double) totalTime) * percent);
							newEndTimeStamp += lastTimeStamp;

							long newTimeStamp = newEndTimeStamp - newStartTimeStamp;
							newTimeStamp = newTimeStamp / 2;
							newTimeStamp = newStartTimeStamp + newTimeStamp;

							myOSMWayPart wp = matchedNLink.getStreetLink();

							bWriter.write("Calc," + (newTimeStamp));

							bWriter.write(",-,-"); // matched lat lon
							bWriter.write(",-,-"); // unmatched lat lon
							bWriter.write(",-"); // distance

							bWriter.write(",50.0");

							bWriter.write("," + wp.startNode.id + "," + wp.endNode.id);

							if (wp.edge == null) {
								bWriter.write(",null");
							} else {
								bWriter.write("," + wp.edge.id_str);
							}

							double EdgeLength = wp.endEdgeLength - wp.startEdgeLength;

							bWriter.write("," + (EdgeLength / 2.0));

							bWriter.write("," + EdgeLength);

							myDataset DatasetUp = myMap.getDatasetUp((newTimeStamp));
							myDataset DatasetDown = myMap.getDatasetDown((newTimeStamp));

							if (DatasetUp != null) {
								bWriter.write("," + DatasetUp.getTimestamp());
								bWriter.write("," + DatasetUp.datarate);
								bWriter.write("," + DatasetUp.delay);
								bWriter.write("," + DatasetUp.loss_rate);
							} else {
								bWriter.write(",-,-,-,-");
							}

							if (DatasetDown != null) {
								bWriter.write("," + DatasetDown.getTimestamp());
								bWriter.write("," + DatasetDown.datarate);
								bWriter.write("," + DatasetDown.delay);
								bWriter.write("," + DatasetDown.loss_rate);
							} else {
								bWriter.write(",-,-,-,-");
							}

							if (nextMatchedGPSNode.isReordered || lastMatchedGPSNode.isReordered) {
								bWriter.write(",true");
							} else {
								bWriter.write(",false");
							}
							
							bWriter.newLine();
						}

					}

				}

				for (MatchedGPSNode matchedGPSNode : matchedNLink.matchedGPSNodes) {

					myOSMWayPart wp = matchedGPSNode.matchtedWayPart;

					// convert to geographic position
					GeoPosition matchedGeoPos = Coordinates.getGeoPos(matchedGPSNode.getMatchedX(),	matchedGPSNode.getMatchedY());
					
					if (matchedGPSNode.isReordered) {
						matchedGeoPos = Coordinates.getGeoPos(matchedGPSNode.matchedXreordered,	matchedGPSNode.matchedYreordered);
					}
					
					GeoPosition unmatchedGeoPos = Coordinates.getGeoPos(matchedGPSNode.getX(), matchedGPSNode.getY());

					//bWriter2.write("Real," + (matchedGPSNode.getTimestamp() + timeStampOffSet));
					bWriter.write("Real," + (matchedGPSNode.getTimestamp()));

					bWriter.write("," + latFormat.format(matchedGeoPos.getLatitude()) + ","
							+ lonFormat.format(matchedGeoPos.getLongitude()));
					bWriter.write("," + latFormat.format(unmatchedGeoPos.getLatitude()) + ","
							+ lonFormat.format(unmatchedGeoPos.getLongitude()));

					double distance = getDistance(matchedGeoPos.getLatitude(), matchedGeoPos.getLongitude(), unmatchedGeoPos.getLatitude(), unmatchedGeoPos.getLongitude());
					
					bWriter.write("," + distance); // distance
					
					if (matchedGPSNode.isReordered) {
						bWriter.write("," + matchedGPSNode.matched_distribution_in_WayPartyReordered);
					} else {
						bWriter.write("," + matchedGPSNode.matched_distribution_in_WayParty);
					}

					bWriter.write("," + wp.startNode.id + "," + wp.endNode.id);

					if (wp.edge == null) {
						bWriter.write(",null");
					} else {
						bWriter.write("," + wp.edge.id_str);
					}

					double EdgeLength = wp.endEdgeLength - wp.startEdgeLength;

					double lpos = EdgeLength * matchedGPSNode.matched_distribution_in_WayParty;
					
					if (matchedGPSNode.isReordered) {
						lpos = EdgeLength * matchedGPSNode.matched_distribution_in_WayPartyReordered;
					}

					//lpos = lpos / 100;

					lpos = wp.startEdgeLength + lpos;

					bWriter.write("," + lpos);

					bWriter.write("," + EdgeLength);

					myDataset DatasetUp = myMap.getDatasetUp((matchedGPSNode.getTimestamp()));
					myDataset DatasetDown = myMap.getDatasetDown((matchedGPSNode.getTimestamp()));

					if (DatasetUp != null) {
						bWriter.write("," + DatasetUp.getTimestamp());
						bWriter.write("," + DatasetUp.datarate);
						bWriter.write("," + DatasetUp.delay);
						bWriter.write("," + DatasetUp.loss_rate);
					} else {
						bWriter.write(",-,-,-,-");
					}

					if (DatasetDown != null) {
						bWriter.write("," + DatasetDown.getTimestamp());
						bWriter.write("," + DatasetDown.datarate);
						bWriter.write("," + DatasetDown.delay);
						bWriter.write("," + DatasetDown.loss_rate);
					} else {
						bWriter.write(",-,-,-,-");
					}

					if (matchedGPSNode.isReordered) {
						bWriter.write(",true");
					} else {
						bWriter.write(",false");
					}
					
					bWriter.newLine();

					if (wp.WayPartBackDirektion != null) {

						if (wp.WayPartBackDirektion.CountMatchedGPSNodes == 0) {

							wp = wp.WayPartBackDirektion;

							//bWriter2.write("BackDirektion," + (matchedGPSNode.getTimestamp() + timeStampOffSet));
							bWriter.write("BackDirektion," + (matchedGPSNode.getTimestamp()));

							bWriter.write("," + latFormat.format(matchedGeoPos.getLatitude()) + ","
									+ lonFormat.format(matchedGeoPos.getLongitude()));
							bWriter.write("," + latFormat.format(unmatchedGeoPos.getLatitude()) + ","
									+ lonFormat.format(unmatchedGeoPos.getLongitude()));

							distance = getDistance(matchedGeoPos.getLatitude(), matchedGeoPos.getLongitude(), unmatchedGeoPos.getLatitude(), unmatchedGeoPos.getLongitude());
							
							bWriter.write("," + distance); // distance
							
							if (matchedGPSNode.isReordered) {
								bWriter.write("," + (1.0 - matchedGPSNode.matched_distribution_in_WayPartyReordered));
							} else {
								bWriter.write("," + (1.0 - matchedGPSNode.matched_distribution_in_WayParty));
							}

							bWriter.write("," + wp.startNode.id + "," + wp.endNode.id);

							if (wp.edge == null) {
								bWriter.write(",null");
							} else {
								bWriter.write("," + wp.edge.id_str);
							}

							EdgeLength = wp.endEdgeLength - wp.startEdgeLength;

							lpos = EdgeLength * (1.0 - matchedGPSNode.matched_distribution_in_WayParty);
							
							if (matchedGPSNode.isReordered) {
								lpos = EdgeLength * (1.0 - matchedGPSNode.matched_distribution_in_WayPartyReordered);
							}

							lpos = lpos / 100;

							lpos = wp.startEdgeLength + lpos;

							bWriter.write("," + lpos);

							bWriter.write("," + EdgeLength);

							DatasetUp = myMap.getDatasetUp((matchedGPSNode.getTimestamp()));
							DatasetDown = myMap.getDatasetDown((matchedGPSNode.getTimestamp()));

							if (DatasetUp != null) {
								bWriter.write("," + DatasetUp.getTimestamp());
								bWriter.write("," + DatasetUp.datarate);
								bWriter.write("," + DatasetUp.delay);
								bWriter.write("," + DatasetUp.loss_rate);
							} else {
								bWriter.write(",-,-,-,-");
							}

							if (DatasetDown != null) {
								bWriter.write("," + DatasetDown.getTimestamp());
								bWriter.write("," + DatasetDown.datarate);
								bWriter.write("," + DatasetDown.delay);
								bWriter.write("," + DatasetDown.loss_rate);
							} else {
								bWriter.write(",-,-,-,-");
							}
							
							if (matchedGPSNode.isReordered) {
								bWriter.write(",true");
							} else {
								bWriter.write(",false");
							}

							bWriter.newLine();
						}

					}

				}

			}

			bWriter.close();
		}
		// handle I/O error during writing operation
		catch (IOException e) {
			System.out.println("Error during exporting matched GPS points! \n" + e.toString());
			return;
		}
		
	}
	
	private static void writeDataset(BufferedWriter bWriter, myDataset dataset, int matchedLinkNr, MatchedNLink matchedNLink, boolean isDownStream, Double disFromLastDatasetDown, Double disToNextDatasetDown) {
		try {
			double matched_distribution_in_WayPart;
			double matched_distribution_in_WayPartBD;
			
			StringBuilder bd = new StringBuilder("");
			Double disFromLastDatasetDownBD = disToNextDatasetDown;
			Double disToNextDatasetDownBD = disFromLastDatasetDown;
			
			if (matchedNLink == null) {
				bWriter.write("Real");
				bd.append("BackDirektion");
				matchedNLink = dataset.matchedNLink;
				matched_distribution_in_WayPart = dataset.matched_distribution_in_WayPart;
				matched_distribution_in_WayPartBD = 1.0 - matched_distribution_in_WayPart;
			} else if (disFromLastDatasetDown != null) {
				bWriter.write("Start");
				bd.append("EndBackDirektion");
				matched_distribution_in_WayPart = 0.0;
				matched_distribution_in_WayPartBD = 1.0 - matched_distribution_in_WayPart;
			} else if (disToNextDatasetDown != null) {
				bWriter.write("End");
				bd.append("StartBackDirektion");
				matched_distribution_in_WayPart = 1.0;
				matched_distribution_in_WayPartBD = 1.0 - matched_distribution_in_WayPart;
			} else {
				System.out.println("Error: writeDataset: type not defined");
				return;
			}

			if (isDownStream) {
				bWriter.write(",down");
				bd.append(",down");
			} else {
				bWriter.write(",up");
				bd.append(",up");
			}

			bWriter.write("," + dataset.getTimestamp());
			bd.append("," + dataset.getTimestamp());

			GeoPosition matchedGeoPos = Coordinates.getGeoPos(dataset.X, dataset.Y);

			GeoPosition unmatchedGeoPos = Coordinates.getGeoPos(dataset.Xunmatched, dataset.Yunmatched);

			bWriter.write("," + latFormat.format(matchedGeoPos.getLatitude()) + ","
					+ lonFormat.format(matchedGeoPos.getLongitude()));
			bd.append("," + latFormat.format(matchedGeoPos.getLatitude()) + ","
					+ lonFormat.format(matchedGeoPos.getLongitude()));
			bWriter.write("," + latFormat.format(unmatchedGeoPos.getLatitude()) + ","
					+ lonFormat.format(unmatchedGeoPos.getLongitude()));
			bd.append("," + latFormat.format(unmatchedGeoPos.getLatitude()) + ","
					+ lonFormat.format(unmatchedGeoPos.getLongitude()));

			double distance = getDistance(matchedGeoPos.getLatitude(), matchedGeoPos.getLongitude(), unmatchedGeoPos.getLatitude(), unmatchedGeoPos.getLongitude());
			
			bWriter.write("," + distance); // distance
			bd.append("," + distance); // distance
					
			myOSMWayPart wp = matchedNLink.getStreetLink();
			myOSMWayPart wpBD = matchedNLink.getStreetLink().WayPartBackDirektion;			

			bWriter.write("," + wp.startNode.id + "," + wp.endNode.id);
			if (wpBD != null) {
				bd.append("," + wpBD.startNode.id + "," + wpBD.endNode.id);
			}
			
			if (wp.edge == null) {
				bWriter.write(",null");
				bWriter.write(",0");
				bWriter.write(",0" + wp.edge.length);
				bWriter.write(",0");
			} else {
				if (disFromLastDatasetDown != null) {
					bWriter.write("," + wp.edge.id_str);
					double length_in_edge = -1.0 * disFromLastDatasetDown;
					bWriter.write("," + length_in_edge);
					bWriter.write("," + wp.edge.length);
					bWriter.write(",0");
				} else if (disToNextDatasetDown != null) {
					bWriter.write("," + wp.edge.id_str);
					bWriter.write("," + disToNextDatasetDown);
					bWriter.write("," + wp.edge.length);
					bWriter.write("," + (disToNextDatasetDown / wp.edge.length));
				} else {
					bWriter.write("," + wp.edge.id_str);
					double length_in_edge = wp.edge.length * matched_distribution_in_WayPart;
					bWriter.write("," + length_in_edge);
					bWriter.write("," + wp.edge.length);
					bWriter.write("," + matched_distribution_in_WayPart);
				}
			}
			if (wpBD != null) {
				if (wpBD.edge == null) {
					bd.append(",null");
					bd.append(",0");
					bd.append(",0" + wpBD.edge.length);
					bd.append(",0");
				} else {
					if (disFromLastDatasetDownBD != null) {
						
						disFromLastDatasetDownBD = wp.edge.length - disFromLastDatasetDownBD;
						
						bd.append("," + wpBD.edge.id_str);
						double length_in_edge = disFromLastDatasetDownBD;
						bd.append("," + length_in_edge);
						bd.append("," + wpBD.edge.length);
						bd.append(",0");
					} else if (disToNextDatasetDownBD != null) {
						
						disToNextDatasetDownBD += wpBD.edge.length;
						
						bd.append("," + wpBD.edge.id_str);
						bd.append("," + disToNextDatasetDownBD);
						bd.append("," + wpBD.edge.length);
						bd.append("," + (disToNextDatasetDownBD / wpBD.edge.length));
					} else {
						bd.append("," + wpBD.edge.id_str);
						double length_in_edge = wpBD.edge.length * matched_distribution_in_WayPartBD;
						bd.append("," + length_in_edge);
						bd.append("," + wpBD.edge.length);
						bd.append("," + matched_distribution_in_WayPartBD);
					}
				}
			}

			bWriter.write("," + dataset.datarate);
			bWriter.write("," + dataset.delay);
			bWriter.write("," + dataset.loss_rate);
			
			bd.append("," + dataset.datarate);
			bd.append("," + dataset.delay);
			bd.append("," + dataset.loss_rate);
			
			bWriter.write("," + matchedLinkNr);
	
			bd.append("," + matchedLinkNr);
			
			if (dataset.cellInfo == null) {
				bWriter.write(",null,null,null,null");
				
				bd.append(",null,null,null,null");
			} else {
				bWriter.write("," + dataset.cellInfo.w1_ch);
				bWriter.write("," + dataset.cellInfo.w1_sc);
				bWriter.write("," + dataset.cellInfo.g1_cellid);
				bWriter.write("," + dataset.cellInfo.g1_lac);

				bd.append("," + dataset.cellInfo.w1_ch);
				bd.append("," + dataset.cellInfo.w1_sc);
				bd.append("," + dataset.cellInfo.g1_cellid);
				bd.append("," + dataset.cellInfo.g1_lac);
			}
			
			bWriter.newLine();
			
			if (wpBD != null) {
				bWriter.write(bd.toString());
				bWriter.newLine();				
			}
			
		} catch (IOException e) {
			System.out.println("Error during exporting matched GPS points! \n" + e.toString());
			return;
		}
	}

	private static void createCsvFromDatasets(myOSMMap myMap, Vector<MatchedNLink> matchedNLinks, String filePath) {
		
		//filePath = filePath.replace(".", "_DS.");
		
		try {
			// wrap with buffered writer
			//BufferedWriter bWriter_old = new BufferedWriter(new FileWriter(gpsTracefile_old));

			File gpsTracefile = new File(filePath);
			
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(gpsTracefile));

			String s = "type,down_up,timestamp,"
					+ "matched_latitude,matched_longitude,unmatched_latitude,unmatched_longitude,unMatched_distance,"
					+ "startNode_id,endNode_id,edge_id_str,length_in_edge,length_of_edge,"
					+ "matched_distribution_in_WayPart,"
					+ "datarate,delay,loss_rate,matchedLinkNr,"
					+ "WCDMA_Ch,WCDMA_SC,GSM_CellId,GSM_LAC";
			
			bWriter.write(s);
			bWriter.newLine();
			
			myDataset lastDsDown = null;
			double disFromLastDatasetDown = 0;
			myDataset lastDsUp = null;
			double disFromDatasetUp = 0;
			
			for (int i = 0; i < matchedNLinks.size(); i++) {

				MatchedNLink matchedNLink = matchedNLinks.get(i);
				
				if (matchedNLink.getStreetLink().edge != null) {
					if (matchedNLink.matchedDownDatasets.size() == 0 ) {
						if (lastDsDown != null) {
							writeDataset(bWriter, lastDsDown, i, matchedNLink, true, new Double(disFromLastDatasetDown), null);							
							disFromLastDatasetDown += matchedNLink.getStreetLink().edge.length;
						}
					} else if (matchedNLink.matchedDownDatasets.firstElement().matched_distribution_in_WayPart != 0.0) {
						if (lastDsDown != null) {
							writeDataset(bWriter, lastDsDown, i, matchedNLink, true, new Double(disFromLastDatasetDown), null);
						}
					}
					for(myDataset ds : matchedNLink.matchedDownDatasets) {
						writeDataset(bWriter, ds, i, null, true, null, null);
						lastDsDown = ds;
						disFromLastDatasetDown = matchedNLink.getStreetLink().edge.length;
						disFromLastDatasetDown -= (disFromLastDatasetDown * ds.matched_distribution_in_WayPart);
					}
					if (matchedNLink.matchedDownDatasets.size() == 0 ||  matchedNLink.matchedDownDatasets.lastElement().matched_distribution_in_WayPart != 1.0) {
						double disToNextDataset = matchedNLink.getStreetLink().edge.length;
						for (int j = (i + 1); j < matchedNLinks.size(); j++) {
							MatchedNLink nextMatchedNLink = matchedNLinks.get(j);
							if (nextMatchedNLink.matchedDownDatasets.size() == 0) {
								if (nextMatchedNLink.getStreetLink().edge != null) {
									disToNextDataset += nextMatchedNLink.getStreetLink().edge.length;									
								}
							} else {
								if (nextMatchedNLink.getStreetLink().edge != null) {
									disToNextDataset += (nextMatchedNLink.getStreetLink().edge.length * 
											nextMatchedNLink.matchedDownDatasets.firstElement().matched_distribution_in_WayPart);
								}
								writeDataset(bWriter, nextMatchedNLink.matchedDownDatasets.firstElement(), i, matchedNLink, true, null, new Double(disToNextDataset));
								break;
							}
						}
					}
					
					if (matchedNLink.matchedUpDatasets.size() == 0 ) {
						if (lastDsUp != null) {
							writeDataset(bWriter, lastDsUp, i, matchedNLink, false, new Double(disFromDatasetUp), null);
							disFromDatasetUp += matchedNLink.getStreetLink().edge.length;
						}
					} else if (matchedNLink.matchedUpDatasets.firstElement().matched_distribution_in_WayPart != 0.0) {
						if (lastDsUp != null) {
							writeDataset(bWriter, lastDsUp, i, matchedNLink, false, new Double(disFromDatasetUp), null);
						}
					}
					for(myDataset ds : matchedNLink.matchedUpDatasets) {
						writeDataset(bWriter, ds, i, null, false, null, null);
						lastDsUp = ds;
						disFromDatasetUp = matchedNLink.getStreetLink().edge.length;
						disFromDatasetUp -= (disFromDatasetUp * ds.matched_distribution_in_WayPart);
					}
					if (matchedNLink.matchedUpDatasets.size() == 0 || matchedNLink.matchedUpDatasets.lastElement().matched_distribution_in_WayPart != 1.0) {
						double disToNextDataset = matchedNLink.getStreetLink().edge.length;
						for (int j = (i + 1); j < matchedNLinks.size(); j++) {
							MatchedNLink nextMatchedNLink = matchedNLinks.get(j);
							if (nextMatchedNLink.matchedUpDatasets.size() == 0) {
								if (nextMatchedNLink.getStreetLink().edge != null) {
									disToNextDataset += nextMatchedNLink.getStreetLink().edge.length;									
								}
							} else {
								if (nextMatchedNLink.getStreetLink().edge != null) {
									disToNextDataset += (nextMatchedNLink.getStreetLink().edge.length * 
											nextMatchedNLink.matchedUpDatasets.firstElement().matched_distribution_in_WayPart);
								}
								writeDataset(bWriter, nextMatchedNLink.matchedUpDatasets.firstElement(), i, matchedNLink, false, null, new Double(disToNextDataset));
								break;
							}
						}
					}
				}
			}

			bWriter.close();
		}
		// handle I/O error during writing operation
		catch (IOException e) {
			System.out.println("Error during exporting matched GPS points! \n" + e.toString());
			return;
		}
		
	}

	/**
	 * saves all matched GPS points to text file. time stamps can be normalized
	 * if desired
	 * 
	 * @param gpsTrace
	 * @param normalizeTimeStamp
	 * @param filePath
	 * @param statusUpdate
	 * @return was writing progress successful?
	 */
	public static boolean saveMatchedGPSTraceToFile(myOSMMap myMap, Vector<MatchedGPSNode> gpsNodesToMatch, long refTimeStamp,
			boolean normalizeTimeStamp, String filePath, StatusUpdate statusUpdate, Vector<MatchedNLink> matchedNLinks, boolean kmlNorm, boolean onlyUniqueMatchedGPS) {

		MatchedNLink.reorderMatchedGPSNodes(matchedNLinks, gpsNodesToMatch);

		myDataset.matchMatchedGPSNode(myMap.DatasetsDown, true, gpsNodesToMatch, matchedNLinks, myMap.CellInfos, onlyUniqueMatchedGPS);
		myDataset.matchMatchedGPSNode(myMap.DatasetsUp, false, gpsNodesToMatch, matchedNLinks, myMap.CellInfos, onlyUniqueMatchedGPS);

		// finished
		statusUpdate.finished(" saved datasets to " + filePath);

		GPSTraceStreamer.createCsvFromGPS(myMap, matchedNLinks, filePath);

		GPSTraceStreamer.createCsvFromDatasets(myMap, matchedNLinks, filePath);

		GPSTraceStreamer.createKmlMatched(gpsNodesToMatch, filePath);
		GPSTraceStreamer.createOsm(matchedNLinks, myMap.osmFile, filePath);
		GPSTraceStreamer.createKmlRoute(matchedNLinks, filePath);
		GPSTraceStreamer.createKmlUnmatched(gpsNodesToMatch, filePath);
		GPSTraceStreamer.createKmlUnmatchedMatched(gpsNodesToMatch, filePath);
		GPSTraceStreamer.createOsmKml(gpsNodesToMatch, filePath);
		GPSTraceStreamer.createKmlReordered(gpsNodesToMatch, filePath);
		GPSTraceStreamer.createKmlDatasetsUp(myMap.DatasetsUp, filePath);
		GPSTraceStreamer.createKmlDatasetsDown(myMap.DatasetsDown, filePath);
		GPSTraceStreamer.createKmlDatasetsUpUnMatched(myMap.DatasetsUp, filePath);
		GPSTraceStreamer.createKmlDatasetsDownUnMatched(myMap.DatasetsDown, filePath);

		GPSTraceStreamer.createKmlDatasetsDatarateColor(myMap.DatasetsDown, filePath + ".Down", kmlNorm, MIN_DATARATE_DOWN, MAX_DATARATE_DOWN);
		GPSTraceStreamer.createKmlDatasetsDelayColor(myMap.DatasetsDown, filePath + ".Down", kmlNorm, MIN_DELAY_DOWN, MAX_DELAY_DOWN);
		GPSTraceStreamer.createKmlDatasetsLossRateColor(myMap.DatasetsDown, filePath + ".Down", kmlNorm, MIN_LOSS_RATE_DOWN, MAX_LOSS_RATE_DOWN);
		
		GPSTraceStreamer.createKmlDatasetsDatarateColor(myMap.DatasetsUp, filePath + ".Up", kmlNorm, MIN_DATARATE_UP, MAX_DATARATE_UP);
		GPSTraceStreamer.createKmlDatasetsDelayColor(myMap.DatasetsUp, filePath + ".Up", kmlNorm, MIN_DELAY_UP, MAX_DELAY_UP);
		GPSTraceStreamer.createKmlDatasetsLossRateColor(myMap.DatasetsUp, filePath + ".Up", kmlNorm, MIN_LOSS_RATE_UP, MAX_LOSS_RATE_UP);
		
		GPSTraceStreamer.createKmlDatasetsUnMatchedRouteDistribution(myMap.DatasetsDown, filePath + ".RouteDistribution.kml", "FF888888");

		// successful!
		return true;
	}

	public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
	    double earthRadius = 6371000; //meters
	    double dLat = Math.toRadians(lat2-lat1);
	    double dLon = Math.toRadians(lon2-lon1);
	    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
	               Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
	               Math.sin(dLon/2) * Math.sin(dLon/2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    double dist = (earthRadius * c);

	    return dist;
	}
}
