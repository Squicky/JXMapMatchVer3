package myClasses;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import javax.swing.JOptionPane;

import algorithm.MatchedGPSNode;
import algorithm.MatchedNLink;

/**
 * @author Adrian Skuballa
 *
 *         This class represents the mobile cell of JXMapMatcher 
 */

public class myCellInfo {

	@SuppressWarnings("unused")
	private long timestampOrginal = 0;
    private long timestampInNanoSec = 0;

	public String w1_ch = "-";
	public String w1_sc = "-";

	public String g1_cellid = "-";
	public String g1_lac = "-";

	public boolean isMatched = false;

    /**
     * 
     * @param CellInfos: Vector of all myCellInfos
     * @param matchedNLinks: Vector of all matched wayParts (MatchedNLink)
     * @param gpsNodesToMatch: Vector of all matched gps nodes
     */
	public static void matchMatchedGPSNode(Vector<myCellInfo> CellInfos, Vector<MatchedNLink> matchedNLinks, Vector<MatchedGPSNode> gpsNodesToMatch) {		
		for (myCellInfo ci : CellInfos) {
			ci.match(gpsNodesToMatch, matchedNLinks);
		}
	}

	/**
     * match "this" to a wayPart of matchedNLinks
     * 
     * @param matchedNLinks: Vector of all matched wayParts (MatchedNLink)
     * @param gpsNodesToMatch: Vector of all matched gps nodes
     */
	private void match(Vector<MatchedGPSNode> gpsNodesToMatch, Vector<MatchedNLink> matchedNLinks) {
		MatchedGPSNode lastNode = null;
		for (int i = gpsNodesToMatch.size()-1; i >= 0; i--) {
			MatchedGPSNode n = gpsNodesToMatch.get(i);
			if (n.getTimestamp() <= this.getTimestamp()) {
				lastNode = n;
				break;
			}
		}

		if (lastNode == null) {
			isMatched = false;
			return;
		}

		MatchedGPSNode nextNode = null;
		for (int i = 0; i < gpsNodesToMatch.size(); i++) {
			MatchedGPSNode n = gpsNodesToMatch.get(i);
			if (this.getTimestamp() <= n.getTimestamp()) {
				nextNode = n;
				break;
			}
		}

		if (nextNode == null) {
			isMatched = false;
			return;
		}

		double timeTotal = nextNode.getTimestamp() - lastNode.getTimestamp();
		double timeCI = this.getTimestamp() - lastNode.getTimestamp();
		double timeDistribution = timeCI / timeTotal;
		double lenPosTotal;
		if (nextNode.isReordered) {
			lenPosTotal = nextNode.lengthPosReordered;
		} else {
			lenPosTotal = nextNode.lengthPos;
		}
		if (lastNode.isReordered) {
			lenPosTotal -= lastNode.lengthPosReordered;
		} else {
			lenPosTotal -= lastNode.lengthPos;
		}

		double lengthPos = -1;
		lengthPos = lenPosTotal * timeDistribution;

		if (lastNode.isReordered) {
			lengthPos += lastNode.lengthPosReordered;
		} else {
			lengthPos += lastNode.lengthPos;
		}

		MatchedNLink matchedNLink = null;
		for (MatchedNLink link : matchedNLinks) {
			if (link.lengthPosStart <= lengthPos && lengthPos <= link.lengthPosEnd) {
				matchedNLink = link;
				link.matchedCellInfos.add(this);
				break;
			}
		}

		if (matchedNLink == null) {
			isMatched = false;
			return;
		}

		isMatched = true;
	}
	
    /**
     * set and save the timestamp in nanosec 
     * 
     * @param timestamp
     */
    public void setTimestamp(long timestamp){

		this.timestampOrginal = timestamp;

    	if (timestamp <= 0) {
    		this.timestampInNanoSec = -1;
    		return;
    	}

        if (1000000000000000000L < timestamp) { // Nanosec
        	timestampInNanoSec = timestamp;
        } else if (1000000000000000L < timestamp) { // Microsec
        	timestampInNanoSec = timestamp * 1000L;
        } else if (1000000000000L < timestamp) { // Millisec
        	timestampInNanoSec = timestamp * 1000000L;
        } else if (1000000000000L < timestamp) { // Sec
        	timestampInNanoSec = timestamp * 1000000000L;
        } else {
        	this.timestampInNanoSec = -1;
        }
    }

    /**
     * return the timestamp in nanosec
     * 
     * @return (long) timestamp
     */
    public long getTimestamp(){
        return timestampInNanoSec;
    }
	
	/**
     * load the Modemdaten from "cellinfo.txt"
     * 
     * @param FilePath: Path of the file "cellinfo.txt"
     * @return Vector of all Modemdaten from file "cellinfo.txt"
     */
	public static Vector<myCellInfo> loadCellInfos(String FilePath) {
		
		Vector<myCellInfo> v = new Vector<myCellInfo>();
		
		String line = "";
		try {
			BufferedReader bReader = new BufferedReader( new InputStreamReader( new FileInputStream( new File( FilePath ) ), "UTF-8" ));
			
			line = bReader.readLine();
			
			reset_while:
			while (line != null) {
				
				if (line.startsWith("##########") ) {
					myCellInfo ci = new myCellInfo();
					
					line = bReader.readLine();
					if (line == null) {break reset_while;}
					if (line.startsWith("##########") ) {
						continue reset_while;
					}
					
					long l = Long.parseLong(line);
					
					ci.setTimestamp(l);
					
					line = bReader.readLine();
					if (line == null) {break reset_while;}
					if (line.startsWith("##########") ) {
						continue reset_while;
					}
					
					reset_intern_while:
					while (line != null) {
						line = bReader.readLine();
						if (line == null) {break reset_while;}
						if (line.startsWith("##########") ) {
							continue reset_while;
						} else if (line.startsWith(" MCC, MNC,  LAC")) {
							break reset_intern_while;
						} else if (line.startsWith("*EWSCI: ")) {
							break reset_intern_while;
						} else if (line.startsWith("+CREG: ")) {
							break reset_intern_while;
						} 
					}
					
					if (line.equals(" MCC, MNC,  LAC, CellId, BSIC,   Ch,  RxL, RxLF, RxLS, RxQF, RxQS, TA, TN")) {
						line = bReader.readLine();
						if (line == null) {break reset_while;}
						if (line.startsWith("##########") ) {
							continue reset_while;
						}
						String lines[] = line.split(",");

						ci.g1_cellid = lines[3].trim();
						ci.g1_lac = lines[2].trim();
						
					} else if (line.equals(" MCC, MNC,  LAC,   Ch,  SC, RSCP, EcNo, RSSI, ServL, ServQ, Hs, Rs")) {
						line = bReader.readLine();
						if (line == null) {break reset_while;}
						if (line.startsWith("##########") ) {
							continue reset_while;
						}
						if (line.trim().equals("") == false) {
							String lines[] = line.split(",");

							ci.w1_ch = lines[3].trim();
							ci.w1_sc = lines[4].trim();
							
						}
					} else if (line.startsWith("*EWSCI: ")) {
						line = line.replace("*EWSCI: ", "");
						String lines[] = line.split(",");
						
						ci.w1_ch = lines[0].replace("\"", "").trim();
						ci.w1_sc = lines[1].replace("\"", "").trim();
					} else if (line.startsWith("+CREG: ")) {
						line = line.replace("+CREG: ", "");
						String lines[] = line.split(",");

						ci.g1_lac = lines[2].replace("\"", "").trim();
						ci.g1_cellid = lines[3].replace("\"", "").trim();
					} 
					
					if (v.size() == 0 || ci.g1_cellid.equals(v.lastElement().g1_cellid) == false || ci.g1_lac.equals(v.lastElement().g1_lac) == false || 
							ci.w1_ch.equals(v.lastElement().w1_ch) == false || ci.w1_sc.equals(v.lastElement().w1_sc) == false ) {
						
						v.add(ci);
					}
				}
				
				line = bReader.readLine();
			}
			
			bReader.close();
		} catch (java.io.FileNotFoundException e) {
			System.out.println("Error: " + e.toString());
			JOptionPane.showMessageDialog(null, "File nocht Found: \n" + FilePath, "Error", JOptionPane.CANCEL_OPTION);
		} catch (Exception e) {
			System.out.println("Error: loadCellInfos: \n" + line + "\n" + e.toString());
		}
		
		return v;
	}
	
}
