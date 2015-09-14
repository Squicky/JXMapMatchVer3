package myClasses;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import javax.swing.JOptionPane;

import cartesian.Coordinates;
import algorithm.MatchedGPSNode;
import algorithm.MatchedNLink;

public class myDataset {

	@SuppressWarnings("unused")
	private long timestampOrginal = 0;
    private long timestampInNanoSec = 0;
    public int datarate = -1;
	public double delay = -1;
	public double loss_rate = -1;
	public double lengthPos = -1;
	public double lengthPosInLink = -1;
	public MatchedNLink matchedNLink = null;
	public double matched_distribution_in_WayPart = -1;
	public double X = 0;
	public double Y = 0;
	public double Xunmatched = 0;
	public double Yunmatched = 0;
	public boolean isMatched = false;
	
	private double lengthPosRouteDistributionUnmatched = -1;
	private double lengthPosRouteDistributionMatched = -1;
	private MatchedNLink matchedNLinkRouteDistribution = null;
	private double matched_distribution_in_WayPart_RouteDistribution = -1;
	public double X_RouteDistribution = 0;
	public double Y_RouteDistribution = 0;
	
	@SuppressWarnings("unused")
	private long objID = 0;
	private static long objCount = 0;
	
	public myCellInfo cellInfo = null;
	
	/*
	 * constructor
	 */
	public myDataset() {
		objID = objCount;
		objCount++;		
	}
	
    /**
     * match all myDataset of Datasets
     * 
     * @param Datasets: Vector of all myDataset
     * @param isDatasetDown: info if datasets are from downstream
     * @param gpsNodesToMatch: Vector of all matched gps nodes
     * @param matchedNLinks: Vector of all matched wayParts (MatchedNLink)
     * @param CellInfos: Vector of all myCellInfos
     * @param onlyUniqueMatchedGPS: info if to use ""Unique GPS function"
     */
	public static void matchMatchedGPSNode(Vector<myDataset> Datasets, boolean isDatasetDown, Vector<MatchedGPSNode> gpsNodesToMatch,  Vector<MatchedNLink> matchedNLinks, Vector<myCellInfo> CellInfos, boolean onlyUniqueMatchedGPS) {
		for (myDataset d : Datasets) {
			d.match(gpsNodesToMatch, matchedNLinks, isDatasetDown, CellInfos, onlyUniqueMatchedGPS);
		}

		// set var for RouteDistribution
		myDataset firstMatchedDs = null;
		int firstMatchedIndex = 0;
		for (int i = 0; i < Datasets.size(); i++) {
			if (Datasets.get(i).isMatched) {
				firstMatchedDs = Datasets.get(i);
				firstMatchedIndex = i;
				break;
			}
		}
		myDataset lastMatchedDs = null;
		int lastMatchedIndex = 0;
		for (int i = Datasets.size() - 1; i >= 0; i--) {
			if (Datasets.get(i).isMatched) {
				lastMatchedDs = Datasets.get(i);
				lastMatchedIndex = i;
				break;
			}
		}
		
		if (firstMatchedDs != null && lastMatchedDs != null) {
			double lengthPosUnmatched = 0;
			double dis = 0;
			myDataset lastDs = null;
			for (int i = firstMatchedIndex; i <= lastMatchedIndex; i++) {
				myDataset Ds = Datasets.get(i);
				
				if (Ds.isMatched) {
					if (lastDs != null) {
						dis = Coordinates.getDistance(lastDs.Xunmatched, lastDs.Yunmatched, Ds.Xunmatched, Ds.Yunmatched);
					}
					lengthPosUnmatched += dis;
					
					Ds.lengthPosRouteDistributionUnmatched = lengthPosUnmatched;
					lastDs = Ds;
				}
			}
			
			double TotalRouteLenMatched = lastMatchedDs.lengthPos - firstMatchedDs.lengthPos;

			for (int i = firstMatchedIndex; i <= lastMatchedIndex; i++) {
				myDataset Ds = Datasets.get(i);
				
				Ds.lengthPosRouteDistributionMatched = firstMatchedDs.lengthPos;
				double distri = Ds.lengthPosRouteDistributionUnmatched / lengthPosUnmatched;
				Ds.lengthPosRouteDistributionMatched += TotalRouteLenMatched * distri;
				
				for (MatchedNLink link : matchedNLinks) {
					if (link.lengthPosStart <= Ds.lengthPosRouteDistributionMatched && Ds.lengthPosRouteDistributionMatched <= link.lengthPosEnd) {
						Ds.matchedNLinkRouteDistribution = link;
						if (isDatasetDown) {
							link.matchedDownDatasetsRouteDistribution.add(Ds);							
						} else {
							link.matchedUpDatasetsRouteDistribution.add(Ds);
						}
						break;
					}
				}
				
				if (Ds.matchedNLinkRouteDistribution != null) {
					double lengthPosInLink = Ds.lengthPosRouteDistributionMatched - Ds.matchedNLinkRouteDistribution.lengthPosStart;
					
					Ds.matched_distribution_in_WayPart_RouteDistribution = lengthPosInLink / Ds.matchedNLinkRouteDistribution.getStreetLink().length;

					// set X Y RouteDistribution matched
					double xLen = Ds.matchedNLinkRouteDistribution.getStreetLink().endNode.x - Ds.matchedNLinkRouteDistribution.getStreetLink().startNode.x;
					xLen = xLen * Ds.matched_distribution_in_WayPart_RouteDistribution;	
					Ds.X_RouteDistribution = Ds.matchedNLinkRouteDistribution.getStreetLink().startNode.x + xLen;
					double yLen = Ds.matchedNLinkRouteDistribution.getStreetLink().endNode.y - Ds.matchedNLinkRouteDistribution.getStreetLink().startNode.y;
					yLen = yLen * Ds.matched_distribution_in_WayPart_RouteDistribution;	
					Ds.Y_RouteDistribution = Ds.matchedNLinkRouteDistribution.getStreetLink().startNode.y + yLen;
				}

			}
		}
		

	}
	
    /**
     * match "this" to a wayPart of matchedNLinks
     * 
     * @param gpsNodesToMatch: Vector of all matched gps nodes
     * @param matchedNLinks: Vector of all matched wayParts (MatchedNLink)
     * @param isDatasetDown: info if datasets are from downstream
     * @param CellInfos: Vector of all myCellInfos
     * @param onlyUniqueMatchedGPS: info if to use ""Unique GPS function"
     */
	public void match(Vector<MatchedGPSNode> gpsNodesToMatch,  Vector<MatchedNLink> matchedNLinks, boolean isDatasetDown, Vector<myCellInfo> CellInfos, boolean onlyUniqueMatchedGPS) {
		
		for (int i = CellInfos.size() - 1; i >= 0 ; i--) {
			myCellInfo ci = CellInfos.get(i);
			if (ci.getTimestamp() <= this.getTimestamp()) {
				this.cellInfo = ci;
				break;
			}
		}
		
		MatchedGPSNode lastNode = null;
		MatchedGPSNode lastNodeUnique = null;
		for (int i = gpsNodesToMatch.size()-1; i >= 0; i--) {
			MatchedGPSNode n = gpsNodesToMatch.get(i);
			if (n.getTimestamp() <= this.getTimestamp()) {
				if (lastNode == null) {
					lastNode = n;
					if (onlyUniqueMatchedGPS == false) {
						break;
					}
				}
				if (n.isUniqueMatchedXY) {
					lastNodeUnique = n;
					break;
				}
			}
		}

		if (lastNode == null) {
			isMatched = false;
			return;
		}
		
		if (onlyUniqueMatchedGPS && lastNodeUnique == null) {
			isMatched = false;
			return;			
		}

		MatchedGPSNode nextNode = null;
		MatchedGPSNode nextNodeUnique = null;
		for (int i = 0; i < gpsNodesToMatch.size(); i++) {
			MatchedGPSNode n = gpsNodesToMatch.get(i);
			if (this.getTimestamp() <= n.getTimestamp()) {
				if (nextNode == null) {
					nextNode = n;
					if (onlyUniqueMatchedGPS == false) {
						break;
					}
				}
				if (n.isUniqueMatchedXY) {
					nextNodeUnique = n;
					break;
				}
			}
		}
		
		if (nextNode == null) {
			isMatched = false;
			return;
		}
		
		if (onlyUniqueMatchedGPS && nextNodeUnique == null) {
			isMatched = false;
			return;			
		}

		if (onlyUniqueMatchedGPS) {
			double timeTotal = nextNodeUnique.getTimestamp() - lastNodeUnique.getTimestamp();
			double timeNode = this.getTimestamp() - lastNodeUnique.getTimestamp();
			double timeDistribution = timeNode / timeTotal;
			double lenPosTotal;
			if (nextNodeUnique.isReordered) {
				lenPosTotal = nextNodeUnique.lengthPosReordered;
			} else {
				lenPosTotal = nextNodeUnique.lengthPos;
			}
			if (lastNodeUnique.isReordered) {
				lenPosTotal -= lastNodeUnique.lengthPosReordered;
			} else {
				lenPosTotal -= lastNodeUnique.lengthPos;
			}
			
			this.lengthPos = lenPosTotal * timeDistribution;
			
			if (lastNodeUnique.isReordered) {
				this.lengthPos += lastNodeUnique.lengthPosReordered;
			} else {
				this.lengthPos += lastNodeUnique.lengthPos;
			}
		} else {
			double timeTotal = nextNode.getTimestamp() - lastNode.getTimestamp();
			double timeNode = this.getTimestamp() - lastNode.getTimestamp();
			double timeDistribution = timeNode / timeTotal;
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
		}
		

		for (MatchedNLink link : matchedNLinks) {
			if (link.lengthPosStart <= this.lengthPos && this.lengthPos <= link.lengthPosEnd) {
				this.matchedNLink = link;
				if (isDatasetDown) {
					link.matchedDownDatasets.add(this);					
				} else {
					link.matchedUpDatasets.add(this);
				}
				break;
			}
		}
		
		if (this.matchedNLink == null) {
			isMatched = false;
			return;
		}
		
		this.lengthPosInLink = this.lengthPos - this.matchedNLink.lengthPosStart;
		
		this.matched_distribution_in_WayPart = lengthPosInLink / this.matchedNLink.getStreetLink().length;

		// set X Y unmatched
		double timeTotal = nextNode.getTimestamp() - lastNode.getTimestamp();
		double timeNode = this.getTimestamp() - lastNode.getTimestamp();
		double timeDistribution = timeNode / timeTotal;
		
		double xLen = nextNode.getX() - lastNode.getX();
		xLen = xLen * timeDistribution;	
		this.Xunmatched = lastNode.getX() + xLen;
		double yLen = nextNode.getY() - lastNode.getY();
		yLen = yLen * timeDistribution;	
		this.Yunmatched = lastNode.getY() + yLen;			
		
		// set X Y matched
		xLen = this.matchedNLink.getStreetLink().endNode.x - this.matchedNLink.getStreetLink().startNode.x;
		xLen = xLen * this.matched_distribution_in_WayPart;	
		this.X = this.matchedNLink.getStreetLink().startNode.x + xLen;
		yLen = this.matchedNLink.getStreetLink().endNode.y - this.matchedNLink.getStreetLink().startNode.y;
		yLen = yLen * this.matched_distribution_in_WayPart;	
		this.Y = this.matchedNLink.getStreetLink().startNode.y + yLen;
		
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
     * load the datasets from "upstream-data.csv"
     * 
     * @param FilePath: Path of the file
     * @return Vector of all datasets from file 
     */
	public static Vector<myDataset> loadDatasetsUp(String FilePath) {
		
		Vector<myDataset> datasets = new Vector<myDataset>();
		
		String line = "";
		try {
			BufferedReader bReader = new BufferedReader( new InputStreamReader( new FileInputStream( new File( FilePath ) ), "UTF-8" ));
			
			line = bReader.readLine();
			
			int columnNrDataRate = -1;
			int columnNrDelay = -1;
			int columnNrTimestamp = -1;
			int columnNrLossRate = -1;
			
			if (line != null) {
				String [] lines = line.split(",");
				
				for (int i = 0; i < lines.length; i++) {
					
					if (lines[i].equals("data rate [Byte/s]")) {
						
						columnNrDataRate = i;
					
					} else if (lines[i].equals("delay [s]")) {
					
						columnNrDelay = i;
					
					} else if (lines[i].equals("first ttx [ns]")) {
					
						columnNrTimestamp = i;
					
					} else if (lines[i].equals("loss rate")) {
					
						columnNrLossRate = i;
					
					} 
					
				}
				
				datasets = loadDatasets(bReader, columnNrDataRate, columnNrDelay, columnNrTimestamp, columnNrLossRate);
				
			}
			
			bReader.close();
			
			return datasets;
			
		} catch (java.io.FileNotFoundException e) {
			System.out.println("Error: " + e.toString());
			JOptionPane.showMessageDialog(null, "File nocht Found: \n" + FilePath, "Error", JOptionPane.CANCEL_OPTION);
		} catch (Exception e) {			
			System.out.println("Error: loadGetEdges: \n" + line + "\n" + e.toString());
		}		
		
		return datasets;
		
	}
	
	/**
     * load the datasets from "downstream-data.csv"
     * 
     * @param FilePath: Path of the file
     * @return Vector of all datasets from file 
     */
	public static Vector<myDataset> loadDatasetsDown(String FilePath) {
		
		Vector<myDataset> datasets = new Vector<myDataset>();
		
		String line = "";
		try {
			BufferedReader bReader = new BufferedReader( new InputStreamReader( new FileInputStream( new File( FilePath ) ), "UTF-8" ));
			
			line = bReader.readLine();
			
			int columnNrDataRate = -1;
			int columnNrDelay = -1;
			int columnNrTimestamp = -1;
			int columnNrLossRate = -1;
			
			if (line != null) {
				String [] lines = line.split(",");
				
				for (int i = 0; i < lines.length; i++) {
					
					if (lines[i].equals("data rate [Byte/s]")) {
						
						columnNrDataRate = i;
					
					} else if (lines[i].equals("delay [s]")) {
					
						columnNrDelay = i;
					
					} else if (lines[i].equals("first trx [ns]")) {
					
						columnNrTimestamp = i;
					
					} else if (lines[i].equals("loss rate")) {
					
						columnNrLossRate = i;
					
					} 
					
				}
				
				datasets = loadDatasets(bReader, columnNrDataRate, columnNrDelay, columnNrTimestamp, columnNrLossRate);
				
			}
			
			bReader.close();
			
			return datasets;
			
		} catch (Exception e) {			
			System.out.println("Error: loadGetEdges: \n" + line + "\n" + e.toString());
		}		
		
		return datasets;
		
	}
	
	/**
     * load the datasets from "upstream-data.csv"
     * 
     * @param bReader: BufferedReader of of file
     * @param columnNrDataRate: No of column of datarate in csv
     * @param columnNrDelay: No of column of delay in csv
     * @param columnNrTimestamp: No of column of timestamp in csv
     * @param columnNrLossRate: No of column of loss_rate in csv
     * @return Vector of all datasets from file 
     */
	private static Vector<myDataset> loadDatasets(BufferedReader bReader, int columnNrDataRate, int columnNrDelay, int columnNrTimestamp, int columnNrLossRate) {
		
		Vector<myDataset> datasets = new Vector<myDataset>();
		
		String line = "";
		
		try {
			line = bReader.readLine();	
			
			while (line != null) {
				
				myDataset d = new myDataset();
				
				String[] lines = line.split(",");
				
				try {
					d.datarate = Integer.parseInt(lines[columnNrDataRate]);					
				} catch (Exception e) {
					d.datarate = -1;
				}
				
				try {
					d.delay = Double.parseDouble(lines[columnNrDelay]);		
				} catch (Exception e) {
					d.delay = -1;
				}
				
				try {
					d.setTimestamp(Long.parseLong(lines[columnNrTimestamp]) );
				} catch (Exception e) {
					d.setTimestamp(-1);
				}
				
				try {
					d.loss_rate = Double.parseDouble(lines[columnNrLossRate]);				
				} catch (Exception e) {
					d.loss_rate = -1;
				}
				
				datasets.add(d);
				
				line = bReader.readLine();	
			}		
		} catch(Exception e) {
			System.out.println("Error: loadGetEdges: \n" + line + "\n" + e.toString());
		}
		
		return datasets;
	}

}
