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
 * 
 *         this class imports GPS traces from text based or GPX-XML files and
 *         exports matched GPS traces/Points to text based files
 */

// TODO: improve moment where progress calculation is shown (for GPX)
// TODO: bugfix normalized timestamp

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
		if (filePath.toLowerCase().endsWith(".txt")) {
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
			if (line.matches("#\\d+"))
				nrOfGPSPoints = Integer.parseInt(line.substring(1));
			else {
				System.out.println("Numbers of GPS Point information couldn't be read");
				bReader.close();
				throw new Exception("Numbers of GPS Point information couldn't be read");
			}

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
			long prevTime = -Long.MAX_VALUE;
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
						timeStamp = timeStamp * 1000000L;

					} else {
						timeStamp = Long.parseLong(gpsData[0]);
					}

					latitude = Double.parseDouble(gpsData[1]);
					longitude = Double.parseDouble(gpsData[2]);

					// check if its time is greater then previous GPS point's
					// time
					if (timeStamp > prevTime) {
						// add node to GPS Path
						if (count % 1 == 0) {
							Point2D p = Coordinates.getCartesianXY(longitude, latitude);
							gpsTrace.addNode(p.getX(), p.getY(), timeStamp, longitude, latitude);
							prevTime = timeStamp;
						}
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
	public static boolean saveMatchedGPSTraceToFile(Vector<MatchedGPSNode> gpsNodesToMatch, long refTimeStamp,
			boolean normalizeTimeStamp, String filePath, StatusUpdate statusUpdate, Vector<MatchedNLink> matchedNLinks) {

		// access file and save name
		File gpsTracefile = new File(filePath);

		File gpsTracefile2 = new File(filePath.replace(".", "2."));

		File kmlUnmatchedMatched = new File(filePath + ".unmatched.matched.kml");
		File kmlMatched = new File(filePath + ".matched.kml");
		File kmlUnmatched = new File(filePath + ".unmatched.kml");
		File kmlOsm = new File(filePath + ".osm.kml");
		File kmlRoute = new File(filePath + ".route.kml");

		// create offSet if user wishes to normalize exported time stamp
		long timeStampOffSet = (normalizeTimeStamp) ? refTimeStamp : 0;

		int nrOfMatchedNodes = 0;
		// count numbers of matched gps nodes
		for (MatchedGPSNode matchedGPSNode : gpsNodesToMatch) {
			if (matchedGPSNode.isMatched())
				++nrOfMatchedNodes;
		}

		try {
			// wrap with buffered writer
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(gpsTracefile));

			BufferedWriter bWriter2 = new BufferedWriter(new FileWriter(gpsTracefile2));

			BufferedWriter bKmlWriterUnmatchedMatched = new BufferedWriter(new FileWriter(kmlUnmatchedMatched));
			BufferedWriter bKmlWriterMatched = new BufferedWriter(new FileWriter(kmlMatched));
			BufferedWriter bKmlWriterUnmatched = new BufferedWriter(new FileWriter(kmlUnmatched));
			BufferedWriter bKmlWriterOsm = new BufferedWriter(new FileWriter(kmlOsm));
			BufferedWriter bKmlWriterRoute = new BufferedWriter(new FileWriter(kmlRoute));

			bKmlWriterUnmatchedMatched.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
			bKmlWriterUnmatchedMatched
					.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">" + System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("	<Document>" + System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("		<name>" + kmlUnmatchedMatched.getName() + "</name>"
					+ System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("" + System.lineSeparator());

			bKmlWriterMatched.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
			bKmlWriterMatched.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">" + System.lineSeparator());
			bKmlWriterMatched.write("	<Document>" + System.lineSeparator());
			bKmlWriterMatched.write("		<name>" + kmlMatched.getName() + "</name>" + System.lineSeparator());
			bKmlWriterMatched.write("" + System.lineSeparator());
			bKmlWriterMatched.write("		<Placemark>" + System.lineSeparator());
			bKmlWriterMatched.write("			<name>matched</name>" + System.lineSeparator());
			bKmlWriterMatched.write("			<LineString>" + System.lineSeparator());
			bKmlWriterMatched.write("				<coordinates>" + System.lineSeparator());

			bKmlWriterUnmatched.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
			bKmlWriterUnmatched.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">" + System.lineSeparator());
			bKmlWriterUnmatched.write("	<Document>" + System.lineSeparator());
			bKmlWriterUnmatched.write("		<name>" + kmlUnmatched.getName() + "</name>" + System.lineSeparator());
			bKmlWriterUnmatched.write("" + System.lineSeparator());
			bKmlWriterUnmatched.write("		<Placemark>" + System.lineSeparator());
			bKmlWriterUnmatched.write("			<name>unmatched</name>" + System.lineSeparator());
			bKmlWriterUnmatched.write("			<LineString>" + System.lineSeparator());
			bKmlWriterUnmatched.write("				<coordinates>" + System.lineSeparator());

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

			bKmlWriterRoute.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
			bKmlWriterRoute.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">" + System.lineSeparator());
			bKmlWriterRoute.write("	<Document>" + System.lineSeparator());
			bKmlWriterRoute.write("		<name>" + kmlRoute.getName() + "</name>" + System.lineSeparator());
			bKmlWriterRoute.write("" + System.lineSeparator());

			bWriter2.write("type,timestamp,matched latitude,matchedlongitude,unmatched latitude,unmatched longitude,matched_percent_in_WayParty,startNode.id,endNode.id,edge.id_str,length_in_edge,length_of_edge,Dataset Up Timestamp, Dataset Up datarate,Dataset Up delay,Dataset Up loss rate,Dataset Down Timestamp, Dataset Down datarate,Dataset Down delay,Dataset Down loss rate");
			bWriter2.newLine();
			
			for (int i = 0; i < matchedNLinks.size(); i++) {

				MatchedNLink matchedNLink = matchedNLinks.get(i);
				
				// calculate virtual TimeStamps for Edges without gps
				// MatchPoints
				// calculate TimeStamps from Distance of last and next gps
				// MatchPoints
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
							double lastPercent = 100.0 - lastMatchedGPSNode.matched_percent_in_WayParty;
							lastPercent = lastPercent / 100.0;
							
							lastLength = lastLength * lastPercent;
							
							disFromLast += lastMatchedNLink.getStreetLink().length
									* ((100.0 - lastMatchedGPSNode.matched_percent_in_WayParty) / 100.0);

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

							double nextLength = nextMatchedNLink.getStreetLink().length;
							double nextPercent = nextMatchedGPSNode.matched_percent_in_WayParty;
							nextPercent = nextPercent / 100.0;
							
							nextLength = nextLength * nextPercent;
							
							try {
								disToNext += nextMatchedNLink.getStreetLink().length
										* (nextMatchedGPSNode.matched_percent_in_WayParty / 100.0);
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

							bWriter2.write("Calc," + (newTimeStamp + timeStampOffSet));

							bWriter2.write(",-,-"); // matched lat lon
							bWriter2.write(",-,-"); // unmatched lat lon

							bWriter2.write(",50.0");

							bWriter2.write("," + wp.startNode.id + "," + wp.endNode.id);

							if (wp.edge == null) {
								bWriter2.write(",null");
							} else {
								bWriter2.write("," + wp.edge.id_str);
							}

							double EdgeLength = wp.endEdgeLength - wp.startEdgeLength;

							bWriter2.write("," + (EdgeLength / 2.0));

							bWriter2.write("," + EdgeLength);

							myDataset DatasetUp = myMap.getDatasetUp((newTimeStamp + timeStampOffSet));
							myDataset DatasetDown = myMap.getDatasetDown((newTimeStamp + timeStampOffSet));

							if (DatasetUp != null) {
								bWriter2.write("," + DatasetUp.timestamp);
								bWriter2.write("," + DatasetUp.datarate);
								bWriter2.write("," + DatasetUp.delay);
								bWriter2.write("," + DatasetUp.loss_rate);
							} else {
								bWriter2.write(",-,-,-,-");
							}

							if (DatasetDown != null) {
								bWriter2.write("," + DatasetDown.timestamp);
								bWriter2.write("," + DatasetDown.datarate);
								bWriter2.write("," + DatasetDown.delay);
								bWriter2.write("," + DatasetDown.loss_rate);
							} else {
								bWriter2.write(",-,-,-,-");
							}

							bWriter2.newLine();

						}

					}

				}

				for (MatchedGPSNode matchedGPSNode : matchedNLink.matchedGPSNodes) {

					myOSMWayPart wp = matchedGPSNode.matchtedWayPart;

					// convert to geographic position
					GeoPosition matchedGeoPos = Coordinates.getGeoPos(matchedGPSNode.getMatchedX(),
							matchedGPSNode.getMatchedY());
					GeoPosition unmatchedGeoPos = Coordinates.getGeoPos(matchedGPSNode.getX(), matchedGPSNode.getY());

					bWriter2.write("Real," + (matchedGPSNode.getTimestamp() + timeStampOffSet));

					bWriter2.write("," + latFormat.format(matchedGeoPos.getLatitude()) + ","
							+ lonFormat.format(matchedGeoPos.getLongitude()));
					bWriter2.write("," + latFormat.format(unmatchedGeoPos.getLatitude()) + ","
							+ lonFormat.format(unmatchedGeoPos.getLongitude()));

					bWriter2.write("," + matchedGPSNode.matched_percent_in_WayParty);

					bWriter2.write("," + wp.startNode.id + "," + wp.endNode.id);

					if (wp.edge == null) {
						bWriter2.write(",null");
					} else {
						bWriter2.write("," + wp.edge.id_str);
					}

					double EdgeLength = wp.endEdgeLength - wp.startEdgeLength;

					double lpos = EdgeLength * matchedGPSNode.matched_percent_in_WayParty;

					lpos = lpos / 100;

					lpos = wp.startEdgeLength + lpos;

					bWriter2.write("," + lpos);

					bWriter2.write("," + EdgeLength);

					myDataset DatasetUp = myMap.getDatasetUp((matchedGPSNode.getTimestamp() + timeStampOffSet));
					myDataset DatasetDown = myMap.getDatasetDown((matchedGPSNode.getTimestamp() + timeStampOffSet));

					if (DatasetUp != null) {
						bWriter2.write("," + DatasetUp.timestamp);
						bWriter2.write("," + DatasetUp.datarate);
						bWriter2.write("," + DatasetUp.delay);
						bWriter2.write("," + DatasetUp.loss_rate);
					} else {
						bWriter2.write(",-,-,-,-");
					}

					if (DatasetDown != null) {
						bWriter2.write("," + DatasetDown.timestamp);
						bWriter2.write("," + DatasetDown.datarate);
						bWriter2.write("," + DatasetDown.delay);
						bWriter2.write("," + DatasetDown.loss_rate);
					} else {
						bWriter2.write(",-,-,-,-");
					}

					bWriter2.newLine();

					if (wp.WayPartBackDirektion != null) {

						if (wp.WayPartBackDirektion.CountMatchedGPSNodes == 0) {

							wp = wp.WayPartBackDirektion;

							bWriter2.write("BackDirektion," + (matchedGPSNode.getTimestamp() + timeStampOffSet));

							bWriter2.write("," + latFormat.format(matchedGeoPos.getLatitude()) + ","
									+ lonFormat.format(matchedGeoPos.getLongitude()));
							bWriter2.write("," + latFormat.format(unmatchedGeoPos.getLatitude()) + ","
									+ lonFormat.format(unmatchedGeoPos.getLongitude()));

							bWriter2.write("," + (100.0 - matchedGPSNode.matched_percent_in_WayParty));

							bWriter2.write("," + wp.startNode.id + "," + wp.endNode.id);

							if (wp.edge == null) {
								bWriter2.write(",null");
							} else {
								bWriter2.write("," + wp.edge.id_str);
							}

							EdgeLength = wp.endEdgeLength - wp.startEdgeLength;

							lpos = EdgeLength * (100.0 - matchedGPSNode.matched_percent_in_WayParty);

							lpos = lpos / 100;

							lpos = wp.startEdgeLength + lpos;

							bWriter2.write("," + lpos);

							bWriter2.write("," + EdgeLength);

							DatasetUp = myMap.getDatasetUp((matchedGPSNode.getTimestamp() + timeStampOffSet));
							DatasetDown = myMap.getDatasetDown((matchedGPSNode.getTimestamp() + timeStampOffSet));

							if (DatasetUp != null) {
								bWriter2.write("," + DatasetUp.timestamp);
								bWriter2.write("," + DatasetUp.datarate);
								bWriter2.write("," + DatasetUp.delay);
								bWriter2.write("," + DatasetUp.loss_rate);
							} else {
								bWriter2.write(",-,-,-,-");
							}

							if (DatasetDown != null) {
								bWriter2.write("," + DatasetDown.timestamp);
								bWriter2.write("," + DatasetDown.datarate);
								bWriter2.write("," + DatasetDown.delay);
								bWriter2.write("," + DatasetDown.loss_rate);
							} else {
								bWriter2.write(",-,-,-,-");
							}

							bWriter2.newLine();
						}

					}

				}

			}

			bWriter2.close();

			for (int i = 0; i < matchedNLinks.size(); i++) {

				MatchedNLink m = matchedNLinks.get(i);
				myOSMWayPart wp = m.getStreetLink();

				myOSMNode n1 = wp.startNode;
				myOSMNode n2 = wp.endNode;

				bKmlWriterRoute.write("		<Placemark>" + System.lineSeparator());
				bKmlWriterRoute.write("			<name>" + wp.parentWay.id + " " + m.matchedGPSNodes.size() + " | " + m.objID
						+ "</name>" + System.lineSeparator());
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

			// write numbers of (matched) GPS nodes
			bWriter.write("#" + nrOfMatchedNodes);
			bWriter.newLine();

			// write structural info in the form of comments
			bWriter.write("#all Tstamps substracted by " + (refTimeStamp - timeStampOffSet)); // +
																								// gpsTrace.getTimestamp());
			bWriter.newLine();
			bWriter.write("#NormTstamp [ns], matched latitude, matched longitude, unmatched latitude, unmatched longitude, matched_percent_in_WayParty, startNode.id, endNode.id,Dataset Up Timestamp, Dataset Up datarate,Dataset Up delay,Dataset Down loss rate,Dataset Down Timestamp, Dataset Down datarate,Dataset Down delay,Dataset Down loss rate");

			bWriter.write(", edge.id_str, length_in_edge");

			bWriter.newLine();

			// for calculating current progress
			float currentProgress = 0;
			int nodeCounter = 0;

			int matchedGPSNodeIndex = 0;
			
			// write matched GPS Points to file
			for (MatchedGPSNode matchedGPSNode : gpsNodesToMatch) {
				// get just matched GPS Points
				if (matchedGPSNode.isMatched()) {
					// convert to geographic position
					GeoPosition matchedGeoPos = Coordinates.getGeoPos(matchedGPSNode.getMatchedX(),
							matchedGPSNode.getMatchedY());
					GeoPosition unmatchedGeoPos = Coordinates.getGeoPos(matchedGPSNode.getX(), matchedGPSNode.getY());
					// write line to file
					bWriter.write((matchedGPSNode.getTimestamp() + timeStampOffSet) + "");

					bWriter.write("," + latFormat.format(matchedGeoPos.getLatitude()) + ","
							+ lonFormat.format(matchedGeoPos.getLongitude()));
					bWriter.write("," + latFormat.format(unmatchedGeoPos.getLatitude()) + ","
							+ lonFormat.format(unmatchedGeoPos.getLongitude()));

					bWriter.write("," + matchedGPSNode.matched_percent_in_WayParty);

					bWriter.write("," + matchedGPSNode.matchtedWayPart.startNode.id + ","
							+ matchedGPSNode.matchtedWayPart.endNode.id);

					if (matchedGPSNode.matchtedWayPart.edge == null) {
						bWriter.write(",null");
					} else {
						bWriter.write("," + matchedGPSNode.matchtedWayPart.edge.id_str);
					}

					double lpos = matchedGPSNode.matchtedWayPart.endEdgeLength
							- matchedGPSNode.matchtedWayPart.startEdgeLength;

					lpos = lpos * matchedGPSNode.matched_percent_in_WayParty;

					lpos = lpos / 100.0;

					lpos = matchedGPSNode.matchtedWayPart.startEdgeLength + lpos;

					bWriter.write("," + lpos);

					myDataset DatasetUp = myMap.getDatasetUp((matchedGPSNode.getTimestamp() + timeStampOffSet));
					myDataset DatasetDown = myMap.getDatasetDown((matchedGPSNode.getTimestamp() + timeStampOffSet));

					if (DatasetUp != null) {
						bWriter.write("," + DatasetUp.timestamp);
						bWriter.write("," + DatasetUp.datarate);
						bWriter.write("," + DatasetUp.delay);
						bWriter.write("," + DatasetUp.loss_rate);
					} else {
						bWriter.write(",-,-,-");
					}

					if (DatasetDown != null) {
						bWriter.write("," + DatasetDown.timestamp);
						bWriter.write("," + DatasetDown.datarate);
						bWriter.write("," + DatasetDown.delay);
						bWriter.write("," + DatasetDown.loss_rate);
					} else {
						bWriter.write(",-,-,-");
					}

					bWriter.newLine();

					Calendar c = Calendar.getInstance();
					long t = matchedGPSNode.getTimestamp() + timeStampOffSet;
					t = t / 1000000L;
					c.setTimeInMillis(t);

					bKmlWriterUnmatchedMatched.write("		<Placemark>" + System.lineSeparator());
					bKmlWriterUnmatchedMatched.write("			<name>" + (matchedGPSNode.getTimestamp() + timeStampOffSet)
							+ " " + matchedGPSNodeIndex + " " + c.getTime().toString() + "</name>" + System.lineSeparator());
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

					bKmlWriterMatched.write(lonFormat.format(matchedGeoPos.getLongitude()) + ","
							+ latFormat.format(matchedGeoPos.getLatitude()) + ",0" + System.lineSeparator());

					bKmlWriterUnmatched.write(lonFormat.format(unmatchedGeoPos.getLongitude()) + ","
							+ latFormat.format(unmatchedGeoPos.getLatitude()) + ",0" + System.lineSeparator());

					// increase counter
					nodeCounter++;
					// update current status of exporting progress
					statusUpdate.updateStatus("Writing matched GPS node nr." + nodeCounter);
				}

				// calculate progress and update
				currentProgress = ((float) nodeCounter / nrOfMatchedNodes * 100);
				statusUpdate.updateStatus(currentProgress);
				
				matchedGPSNodeIndex++;
			}

			bKmlWriterUnmatchedMatched.write("	</Document>" + System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("</kml>" + System.lineSeparator());
			bKmlWriterUnmatchedMatched.write("" + System.lineSeparator());

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

			// close writer

			bKmlWriterUnmatchedMatched.close();
			bKmlWriterMatched.close();
			bKmlWriterUnmatched.close();
			bKmlWriterOsm.close();
			bKmlWriterRoute.close();

			bWriter.close();

			// finished
			statusUpdate.finished(nrOfMatchedNodes + " matched GPS nodes saved to " + gpsTracefile.getName());

			TreeSet<Long> TreeSetWayIds = new TreeSet<Long>();
			TreeSet<Long> TreeSetNodeIds = new TreeSet<Long>();

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
			}

			creatNewOsm(TreeSetWayIds, TreeSetNodeIds, myMap.osmFile);

		}
		// handle I/O error during writing operation
		catch (IOException e) {
			System.out.println("Error during exporting matched GPS points! \n" + e.toString());
			return false;
		}

		// successful!
		return true;
	}

	public static void creatNewOsm(TreeSet<Long> TreeSetWayIds, TreeSet<Long> TreeSetNodeIds, File osmFile) {

		String FilePath = osmFile.getPath();

		try {
			BufferedReader bReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(FilePath)),
					"UTF-8"));

			FilePath = FilePath + ".2.osm";
			BufferedWriter bWriter;
			bWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(FilePath)), "UTF-8"));

			String line = bReader.readLine();

			String tline = line.trim();

			while (line != null) {
				if (tline.startsWith("<node id=\"")) {

					String s = tline.replace("<node id=\"", "").split("\"")[0];

					long id = Long.parseLong(s);

					boolean write = TreeSetNodeIds.contains(id);

					if (tline.endsWith("/>")) {
						if (write) {
							bWriter.write(line + "\n");
						}
					} else if (tline.endsWith(">")) {

						if (write) {
							bWriter.write(line + "\n");
						}

						line = bReader.readLine();
						tline = line.trim();

						while (tline.equals("</node>") == false) {
							if (write) {
								bWriter.write(line + "\n");
							}
							line = bReader.readLine();
							tline = line.trim();
						}

						if (write) {
							bWriter.write(line + "\n");
						}

					} else {
						System.out.println("Error: Ende von Note: " + id);
					}

				} else if (tline.startsWith("<way id=\"")) {

					String s = tline.replace("<way id=\"", "").split("\"", 2)[0];
					long id = Long.parseLong(s);
					boolean write = TreeSetWayIds.contains(id);

					if (write) {
						bWriter.write(line + "\n");
					}

					line = bReader.readLine();
					tline = line.trim();

					while (tline.startsWith("</way>") == false) {
						if (write) {
							bWriter.write(line + "\n");
						}

						line = bReader.readLine();
						tline = line.trim();
					}

					if (write) {
						bWriter.write(line + "\n");
					}

				} else {
					bWriter.write(line + "\n");
				}

				line = bReader.readLine();
				if (line != null) {
					tline = line.trim();
				}
			}

			bReader.close();
			bWriter.close();

		} catch (Exception e) {
			System.out.println("Error: creatNewOsm \n" + e.toString());
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
	/*
	public static boolean saveNMatchedGPSTraceToFile(Vector<ReorderedMatchedGPSNode> matchedGPSNodes,
			GPSTrace gpsTrace, boolean normalizeTimeStamp, String filePath, StatusUpdate statusUpdate) {

		// access file and save name
		File gpsTracefile = new File(filePath);

		// get starting time of measurement
		long refTimeStamp = gpsTrace.getRefTimeStamp();

		// create offSet if user wishes to normalize exported time stamp
		long timeStampOffSet = (normalizeTimeStamp) ? refTimeStamp : 0;

		// get count of matched GPS nodes
		int nrOfMatchedNodes = matchedGPSNodes.size();

		try {
			// wrap with buffered writer
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(gpsTracefile));

			// write numbers of (matched) GPS nodes
			bWriter.write("#" + nrOfMatchedNodes);
			bWriter.newLine();

			// write structural info in the form of comments
			bWriter.write("#all Tstamps substracted by " + (refTimeStamp - timeStampOffSet)); // +
																								// gpsTrace.getTimestamp());
			bWriter.newLine();
			bWriter.write("#NormTstamp [ns], matched latitude, matched longitude, unmatched latitude, unmatched longitude");
			bWriter.newLine();

			// for calculating current progress
			float currentProgress = 0;

			// write matched GPS Points to file
			for (int i = 0; i < matchedGPSNodes.size(); i++) {
				// get matched GPS Node
				MatchedGPSNode matchedGPSNode = matchedGPSNodes.get(i);

				// get just matched GPS Points
				if (matchedGPSNode.isMatched()) {
					// convert to geographic position
					GeoPosition matchedGeoPos = Coordinates.getGeoPos(matchedGPSNode.getMatchedX(),
							matchedGPSNode.getMatchedY());
					GeoPosition unmatchedGeoPos = Coordinates.getGeoPos(matchedGPSNode.getX(), matchedGPSNode.getY());
					// write line to file
					bWriter.write((gpsTrace.getNodeTimestamp(i) + timeStampOffSet) + ",");
					bWriter.write(latFormat.format(matchedGeoPos.getLatitude()) + ","
							+ lonFormat.format(matchedGeoPos.getLongitude()) + ",");
					bWriter.write(latFormat.format(unmatchedGeoPos.getLatitude()) + ","
							+ lonFormat.format(unmatchedGeoPos.getLongitude()));
					bWriter.newLine();

					// update current status of exporting progress
					statusUpdate.updateStatus("Writing matched GPS node nr." + i);
				}

				// calculate progress and update
				currentProgress = ((float) i / gpsTrace.getNrOfNodes() * 100);
				statusUpdate.updateStatus(currentProgress);
			}

			// close writer
			bWriter.close();

			// finished
			statusUpdate.finished(nrOfMatchedNodes + " matched GPS nodes saved to " + gpsTracefile.getName());
		}
		// handle I/O error during writing operation
		catch (IOException e) {
			System.out.println("Error during exporting matched GPS points!");
			return false;
		}

		// successful!
		return true;
	}
	*/
}
