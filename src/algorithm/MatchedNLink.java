package algorithm;

import java.awt.Color;
import java.util.Vector;

import myOSM.myCellInfo;
import myOSM.myDataset;
import myOSM.myOSMWayPart;

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
	
	public static void reorderMatchedGPSNodes(Vector<MatchedNLink> matchedNLinks, Vector<MatchedGPSNode> gpsNodesToMatch) {
		
		MatchedNLink.setLengthPos(matchedNLinks);
		
		MatchedGPSNode minNodePos = null;
		int minNodeIndex = -1;
		MatchedGPSNode maxNodePos = null;
		int maxNodeIndex = -1;
		MatchedGPSNode maxNodePos2 = null;
		int maxNodeIndex2 = -1;
		
		boolean inReorderPos = false;
		int countOutOfReorderPos = 0;
		
		double minD = -1;
		double nD = -1;
		double maxD = -1;
		double maxD2 = -1;
		
		MatchedGPSNode previousN = null;
		
		for (int i = 0; i < gpsNodesToMatch.size(); i++) {
			MatchedGPSNode n = gpsNodesToMatch.get(i);

			nD = n.lengthPos;
			
			if ( previousN != null && n.lengthPos < previousN.lengthPos) {
				inReorderPos = true;
				
				if (maxNodePos2 == null) {
					maxNodePos2 = previousN;
					maxD2 = previousN.lengthPos;
					maxNodeIndex2 = i - 1;
				}
			}

			if (inReorderPos) {
				
				if (maxNodePos2 == null || n.lengthPos > maxNodePos2.lengthPos) {
					maxNodePos2 = n;
					maxD2 = maxNodePos2.lengthPos;
					maxNodeIndex2 = i;
				}
				
				if (minNodePos == null || n.lengthPos < minNodePos.lengthPos) {
					minNodePos = n;
					minD = minNodePos.lengthPos;
					minNodeIndex = i;
					
					if (maxNodePos2 != null) {
						maxNodePos = maxNodePos2;
						maxD = maxNodePos.lengthPos;
						maxNodeIndex = maxNodeIndex2;
					}
				}
				
				if ( maxNodePos.lengthPos < n.lengthPos ) {
					countOutOfReorderPos++;
				} else {
					countOutOfReorderPos = 0;
				}
				
				if ( 10 <= countOutOfReorderPos) {
					double disMinMax = maxNodePos.lengthPos - minNodePos.lengthPos;
					double disMinCur = n.lengthPos - minNodePos.lengthPos;
					
					double disMinMaxDouble = disMinMax + disMinMax;
					if (disMinMaxDouble < disMinCur) {
						
						// reorder 
						int FirstReorderIndex = 0;
						for (int j = 0; j < gpsNodesToMatch.size(); j++) {
							if (gpsNodesToMatch.get(j).lengthPos > minNodePos.lengthPos) {
								FirstReorderIndex = j;
								j = gpsNodesToMatch.size();
							}
						}
						
						int LastReorderIndex = FirstReorderIndex - 1;
						for (int j = gpsNodesToMatch.size()-1; j >= 0; j--) {
							if (gpsNodesToMatch.get(j).lengthPos < maxNodePos.lengthPos) {
								LastReorderIndex = j;
								j = 0;
							}
						}

						double CountReorder = LastReorderIndex - FirstReorderIndex;
						double DisReorder = maxNodePos.lengthPos - minNodePos.lengthPos;
						double DisReorderStep = DisReorder / CountReorder;
						double PosReorder = minNodePos.lengthPos;
						
						for (int j = FirstReorderIndex; j <= LastReorderIndex; j++) {
							gpsNodesToMatch.get(j).lengthPosReordered = PosReorder;
							gpsNodesToMatch.get(j).isReordered = true;
							PosReorder = PosReorder + DisReorderStep;
						}
						
						
						minNodePos = null;
						minNodeIndex = -1;
						maxNodePos = null;
						maxNodeIndex = -1;
						maxNodePos2 = null;
						maxNodeIndex2 = -1;
						
						inReorderPos = false;
						countOutOfReorderPos = 0;
					}
				}
			}
			
			previousN = n;
		}
		
		// spread nodes in links

		for (int i = 0; i < matchedNLinks.size(); i++) {
			MatchedNLink  mnl = matchedNLinks.get(i);
			mnl.matchedGPSNodes.clear();
		}

		for (int i = 0; i < gpsNodesToMatch.size(); i++) {
			MatchedGPSNode n = gpsNodesToMatch.get(i);

			for (int j = 0; j < matchedNLinks.size(); j++) {
				MatchedNLink  mnl = matchedNLinks.get(j);

				if (	(n.isReordered == false && n.lengthPos <= mnl.lengthPosEnd)
						||
						(n.isReordered == true && n.lengthPosReordered <= mnl.lengthPosEnd)
					) {
					
					mnl.matchedGPSNodes.add(n);
					n.matchedNLink = mnl;

					if (n.isReordered) {
						double lenPosOfNinLink = n.lengthPosReordered - mnl.lengthPosStart;
						n.matched_distribution_in_WayPartyReordered = lenPosOfNinLink / mnl.getStreetLink().length;
						n.setMatchedXYreordered();
					}
					j = matchedNLinks.size();
				}
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
