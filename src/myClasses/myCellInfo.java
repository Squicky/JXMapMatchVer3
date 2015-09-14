package myClasses;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import javax.swing.JOptionPane;

import algorithm.MatchedGPSNode;
import algorithm.MatchedNLink;

public class myCellInfo {

	@SuppressWarnings("unused")
	private long timestampOrginal = 0;
    private long timestampInNanoSec = 0;

	public String w1_ch = "-";
	public String w1_sc = "-";

	public String g1_cellid = "-";
	public String g1_lac = "-";

	public double x_matched = 0;
	public double y_machted = 0;

	public double lengthPos = -1;
	public double lengthPosInLink = -1;
	public MatchedNLink matchedNLink = null;
	public double matched_distribution_in_WayPart = -1;

	public boolean isMatched = false;
	
    /*
	public String w1_mcc = "";
	public String w1_mnc = "";
	public String w1_lac = "";
	public String w1_ch = "";
	public String w1_sc = "";
	public String w1_rscp = "";
	public String w1_ecno = "";
	public String w1_rssi = "";
	public String w1_servl = "";
	public String w1_servq = "";
	public String w1_hs = "";
	public String w1_rs = "";

	public String w2_mcc = "";
	public String w2_mnc = "";
	public String w2_lac = "";
	public String w2_ch = "";
	public String w2_sc = "";
	public String w2_rscp = "";
	public String w2_ecno = "";
	public String w2_rssi = "";
	public String w2_servl = "";
	public String w2_servq = "";
	public String w2_hs = "";
	public String w2_rs = "";

	public String w3_mcc = "";
	public String w3_mnc = "";
	public String w3_lac = "";
	public String w3_ch = "";
	public String w3_sc = "";
	public String w3_rscp = "";
	public String w3_ecno = "";
	public String w3_rssi = "";
	public String w3_servl = "";
	public String w3_servq = "";
	public String w3_hs = "";
	public String w3_rs = "";
	
	public String m1_mmc = "";
	public String m1_mnc = "";
	public String m1_lac = "";
	public String m1_cellid = "";
	public String m1_bsic = "";
	public String m1_ch = "";
	public String m1_rxl = "";
	public String m1_c1 = "";
	public String m1_c2 = "";	
	*/

	public static void matchMatchedGPSNode(Vector<myCellInfo> CellInfos, Vector<MatchedNLink> matchedNLinks, Vector<MatchedGPSNode> gpsNodesToMatch) {		
		for (myCellInfo ci : CellInfos) {
			ci.match(gpsNodesToMatch, matchedNLinks);
		}
	}

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

		this.lengthPos = lenPosTotal * timeDistribution;

		if (lastNode.isReordered) {
			this.lengthPos += lastNode.lengthPosReordered;
		} else {
			this.lengthPos += lastNode.lengthPos;
		}

		for (MatchedNLink link : matchedNLinks) {
			if (link.lengthPosStart <= this.lengthPos && this.lengthPos <= link.lengthPosEnd) {
				this.matchedNLink = link;
				link.matchedCellInfos.add(this);
				break;
			}
		}

		if (this.matchedNLink == null) {
			isMatched = false;
			return;
		}

		this.lengthPosInLink = this.lengthPos - this.matchedNLink.lengthPosStart;

		this.matched_distribution_in_WayPart = lengthPosInLink / this.matchedNLink.getStreetLink().length;

		// set X Y matched
		double xLen = this.matchedNLink.getStreetLink().endNode.x - this.matchedNLink.getStreetLink().startNode.x;
		xLen = xLen * this.matched_distribution_in_WayPart;	
		this.x_matched = this.matchedNLink.getStreetLink().startNode.x + xLen;
		double yLen = this.matchedNLink.getStreetLink().endNode.y - this.matchedNLink.getStreetLink().startNode.y;
		yLen = yLen * this.matched_distribution_in_WayPart;	
		this.y_machted = this.matchedNLink.getStreetLink().startNode.y + yLen;

		isMatched = true;
	}
	
    /**
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
     * @return (long) timestamp
     */
    public long getTimestamp(){
        return timestampInNanoSec;
    }
	
	public static Vector<myCellInfo> loadCellInfos(String FilePath) {
		
		Vector<myCellInfo> v = new Vector<myCellInfo>();
		
		//int x = 0;
		
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
