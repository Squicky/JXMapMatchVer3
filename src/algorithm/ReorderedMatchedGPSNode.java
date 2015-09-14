package algorithm;

import gps.GPSNode;

import java.awt.Color;
import java.util.Vector;

public class ReorderedMatchedGPSNode extends MatchedGPSNode {
	
    // save matched / reordered GPS node index 
    private final static int NO_INDEX = -1;
    private int prevIndex = NO_INDEX;
    private int curIndex = NO_INDEX;
    
	public ReorderedMatchedGPSNode(GPSNode gpsNode, Color color) {
		super(gpsNode, color);
	}
	
	public boolean hasIndexChanged() {
		if (prevIndex == NO_INDEX || curIndex == NO_INDEX)
			return false;
		
		return (prevIndex != curIndex);
	}
	
	public int getPrevIndex() {
		return prevIndex;
	}

	public void setPrevIndex(int prevIndex) {
		this.prevIndex = prevIndex;
	}

	public int getCurIndex() {
		return curIndex;
	}

	public void setCurIndex(int curIndex) {
		this.curIndex = curIndex;
	}
	
	public static void reorderMatchedGPSNodes(Vector<MatchedNLink> matchedNLinks, Vector<MatchedGPSNode> gpsNodesToMatch) {
		
		MatchedNLink.setLengthPos(matchedNLinks);
		
		MatchedGPSNode minNodePos = null;
		//int minNodeIndex = -1;
		MatchedGPSNode maxNodePos = null;
		//int maxNodeIndex = -1;
		MatchedGPSNode maxNodePos2 = null;
		//int maxNodeIndex2 = -1;
		
		boolean inReorderPos = false;
		int countOutOfReorderPos = 0;
		
		//double minD = -1;
		//double nD = -1;
		//double maxD = -1;
		//double maxD2 = -1;
		
		MatchedGPSNode previousN = null;
		
		for (int i = 0; i < gpsNodesToMatch.size(); i++) {
			MatchedGPSNode n = gpsNodesToMatch.get(i);

			//nD = n.lengthPos;
			
			if ( previousN != null && n.lengthPos < previousN.lengthPos) {
				inReorderPos = true;
				
				if (maxNodePos2 == null) {
					maxNodePos2 = previousN;
					//maxD2 = previousN.lengthPos;
					//maxNodeIndex2 = i - 1;
				}
			}

			if (inReorderPos) {
				
				if (maxNodePos2 == null || n.lengthPos > maxNodePos2.lengthPos) {
					maxNodePos2 = n;
					//maxD2 = maxNodePos2.lengthPos;
					//maxNodeIndex2 = i;
				}
				
				if (minNodePos == null || n.lengthPos < minNodePos.lengthPos) {
					minNodePos = n;
					//minD = minNodePos.lengthPos;
					//minNodeIndex = i;
					
					if (maxNodePos2 != null) {
						maxNodePos = maxNodePos2;
						//maxD = maxNodePos.lengthPos;
						//maxNodeIndex = maxNodeIndex2;
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
						//minNodeIndex = -1;
						maxNodePos = null;
						//maxNodeIndex = -1;
						maxNodePos2 = null;
						//maxNodeIndex2 = -1;
						
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
}
