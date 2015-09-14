package algorithm;

import java.awt.Color;
import java.util.Vector;

import myClasses.myCellInfo;
import myClasses.myDataset;
import myClasses.myOSMWayPart;

public class MatchedNLink {
	
	public static int objCount = 0;
	public int objID = 0;
	
	private myOSMWayPart streetLink;
	private MatchedRange matchedRange;		// reference to matched range
	
	public Vector<MatchedGPSNode> matchedGPSNodes = new Vector<MatchedGPSNode>();
	public Vector<myDataset> matchedDownDatasets = new Vector<myDataset>();
	public Vector<myDataset> matchedDownDatasetsRouteDistribution = new Vector<myDataset>();
	public Vector<myDataset> matchedUpDatasets = new Vector<myDataset>();
	public Vector<myDataset> matchedUpDatasetsRouteDistribution = new Vector<myDataset>();
	public Vector<myCellInfo> matchedCellInfos = new Vector<myCellInfo>();
	
	private Color color;
	
	public double lengthPosStart = 0;
	public double lengthPosEnd = 0;	
	
	public static void setLengthPos(Vector<MatchedNLink> matchedNLinks) {
		
		// first set lengthPos of MatchedNLinks		
		if (matchedNLinks.size() > 0) {	
			MatchedNLink matchedNLinkPrevious = null; 
			matchedNLinkPrevious = matchedNLinks.get(0); 

			matchedNLinkPrevious.lengthPosStart = 0; 
			matchedNLinkPrevious.lengthPosEnd = matchedNLinkPrevious.getStreetLink().length; 

			for (int i = 1; i < matchedNLinks.size(); i++) { 

				MatchedNLink matchedNLink = matchedNLinks.get(i); 
				
				matchedNLink.lengthPosStart = matchedNLinkPrevious.lengthPosEnd; 
				
				matchedNLink.lengthPosEnd = matchedNLink.lengthPosStart + matchedNLink.getStreetLink().length; 
				
				matchedNLinkPrevious = matchedNLink;
			}
		}
		
		// then set lengthPos of MatchedGPSNode
		for (int i = 0; i < matchedNLinks.size(); i++) {
			MatchedNLink matchedNLink = matchedNLinks.get(i);
			
			for (int j=0; j < matchedNLink.matchedGPSNodes.size(); j++) {
				MatchedGPSNode n = matchedNLink.matchedGPSNodes.get(j);
				
				double d = n.matched_distribution_in_WayParty;
				d = d * matchedNLink.getStreetLink().length;
				d = d + matchedNLink.lengthPosStart;
				
				n.lengthPos = d;
			}
		}
	}
	
	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public MatchedNLink(myOSMWayPart streetLink, MatchedRange matchedRange, Color color) {
		super();
		
		this.streetLink = streetLink;
		this.matchedRange = matchedRange;
		this.color = color;
		
		this.objID = objCount;
		objCount++;
	}
	
	public MatchedNLink(myOSMWayPart streetLink, Color color) {
		this(streetLink, new MatchedRange(0, 0, false), color);
	}

	public myOSMWayPart getStreetLink() {
		return streetLink;
	}

	public void setStreetLink(myOSMWayPart streetLink) {
		this.streetLink = streetLink;
	}

	public boolean isMatched() {
		return matchedRange.getMatched();
	}

	public void setMatched(boolean matched) {		
		this.matchedRange.setMatched(matched);
	}
	
	/**
	 * set start index of matched range
	 * @param rangeStartIndex
	 */
	public void setRangeStartIndex(int rangeStartIndex) {
		matchedRange.setRangeStartIndex(rangeStartIndex);
	}
	
	/**
	 * get start index of matched range
	 * @return int
	 */
	public int getRangeStartIndex() {
		return matchedRange.getRangeStartIndex();
	}
	
	/**
	 * set end index of matched range 
	 * @param rangeEndIndex
	 */
	public void setRangeEndIndex(int rangeEndIndex) {
		matchedRange.setRangeEndIndex(rangeEndIndex);
	}
	
	/**
	 * get end index of matched range
	 * @param int
	 */
	public int getRangeEndIndex() {
		return matchedRange.getRangeEndIndex();
	}
	
	/**
	 * get range size
	 * @return int
	 */
	public int getRangeSize() {
		return matchedRange.getRangeSize();
	}
}
