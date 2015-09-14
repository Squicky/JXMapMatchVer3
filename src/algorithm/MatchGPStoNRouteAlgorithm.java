package algorithm;

import interfaces.MatchingGPSObject;
import interfaces.StatusUpdate;

import java.awt.Color;
import java.awt.Component;
import java.util.Vector;

import myClasses.myOSMWayPart;
import cartesian.Coordinates;
import gps.GPSNode;
import gps.GPSTrace;
import route.SelectedNRoute;

public class MatchGPStoNRouteAlgorithm implements MatchingGPSObject {

	// enable/disable additional features
	@SuppressWarnings("unused")
	private boolean useReorder = false;
	@SuppressWarnings("unused")
	private boolean useProject = false;

	// constants for match GPS to N route algorithm state
	public static final String MATCH_GPS_TO_N_ROUTE_RUNNING = "RUNNING";
	public static final String MATCH_GPS_TO_N_ROUTE_PAUSED = "PAUSED";
	public static final String MATCH_GPS_TO_N_ROUTE_RECESSED = "RECESSED";

	// private SelectedNRoute selectedNRoute;
	// private GPSTrace gpsTrace;
	private long refTimeStamp; // timestamp where measurement started

	// Used class ReorderedMatchedGPSNode is incorrect: 
	// GPSNodes are NOT reordered !
	// relic of JXMapMatchVer2
	private Vector<ReorderedMatchedGPSNode> GPSNodes = new Vector<>();
	private Vector<MatchedNLink> matchedNLinks = new Vector<>();

	private Color unmatchedLinkColor;
	private Color unmatchedNodeColor;

	// save current algorithm state here
	private String matchGPStoNRouteAlgorithmState;

	public MatchGPStoNRouteAlgorithm(SelectedNRoute selectedNRoute, GPSTrace gpsTrace, Color unmatchedLinkColor,
			Color matchedLinkColor, Color unmatchedNodeColor, Color matchedNodeColor, StatusUpdate statusUpdate,
			Component drawComponent) {
		
		super();

		// save references
		this.refTimeStamp = gpsTrace.getRefTimeStamp();

		this.unmatchedLinkColor = unmatchedLinkColor;
		this.unmatchedNodeColor = unmatchedNodeColor;

		//this.drawComponent = drawComponent;

		// wrap selected n route & GPS trace for matching/drawing
		this.matchedNLinks = wrapSelectedNRoute(selectedNRoute);
		this.GPSNodes = wrapSelectedGPSTrace(gpsTrace);

		matchGPStoNRouteAlgorithmState = MATCH_GPS_TO_N_ROUTE_RECESSED;
	}

	public void executeMatchGPStoNRouteAlgorithm(boolean reorder, boolean project) {

		setMatchGPStoNRouteAlgorithmState(MATCH_GPS_TO_N_ROUTE_RUNNING);

		this.useReorder = reorder;
		this.useProject = project;

		int MaxCountCheckNext = 20;

		double bestDistanceofAllGPSNode = Double.MAX_VALUE;
		int bestCountCheckNext = 1;
		
		// 0 = Search in loop for bestCountCheckNext, without matchGPSNodeToNLink
		// 1 = Search in loop finished, run loop again with bestCountCheckNext and matchGPSNodeToNLink 
		// 2 = exit loop
		int statusSearchBestCountCheckNext = 0;
		
		for (int iCountCheckNext = 1; iCountCheckNext <= MaxCountCheckNext || statusSearchBestCountCheckNext != 2 ; iCountCheckNext++) {
			
			int currentNodeIndex = 0;
			int currentNLinkIndex = 0;
			int maxIndex = matchedNLinks.size() - 1;
			MatchedNLink currentMatchedNLink = matchedNLinks.get(currentNLinkIndex);
			MatchedNLink nextMatchedNLink = currentMatchedNLink;
			MatchedNLink nearestMatchedNLink = currentMatchedNLink;
			
			double tempDistanceofAllGPSNode = 0;
			
			int CountCheckNext;
			if (statusSearchBestCountCheckNext == 0) {
				CountCheckNext = iCountCheckNext;
			} else {
				CountCheckNext = bestCountCheckNext;
			}			
			
			for (MatchedGPSNode matchedGPSNode : GPSNodes) {

				currentMatchedNLink = matchedNLinks.get(currentNLinkIndex);
				nearestMatchedNLink = currentMatchedNLink;

				myOSMWayPart curWP = currentMatchedNLink.getStreetLink();

				double disToCur = Coordinates.getDistance(matchedGPSNode, curWP);

				double disToNearest = disToCur;
				int IndexOfdisToNearest = currentNLinkIndex;

				for (int i = 0; i < CountCheckNext; i++) {
					if ((currentNLinkIndex + 1 + i) <= maxIndex) {

						int nextNLinkIndex = currentNLinkIndex + 1 + i;

						nextMatchedNLink = matchedNLinks.get(nextNLinkIndex);

						myOSMWayPart nextWP = nextMatchedNLink.getStreetLink();

						//nextWPOsmID = nextWP.parentWay.id;

						double disToNextTemp = Coordinates.getDistance(matchedGPSNode, nextWP);

						if (disToNextTemp < disToNearest) {
							disToNearest = disToNextTemp;
							IndexOfdisToNearest = nextNLinkIndex;
							nearestMatchedNLink = nextMatchedNLink;
						}
					}
				}

				currentNLinkIndex = IndexOfdisToNearest;

				if (statusSearchBestCountCheckNext == 1) {
					matchGPSNodeToNLink(nearestMatchedNLink, matchedGPSNode, currentNodeIndex);					
				}

				tempDistanceofAllGPSNode += disToNearest;

				// increase node index
				currentNodeIndex++;
			}
			
			if (tempDistanceofAllGPSNode < bestDistanceofAllGPSNode) {
				bestDistanceofAllGPSNode = tempDistanceofAllGPSNode;
				bestCountCheckNext = iCountCheckNext;
			}
			
			if (statusSearchBestCountCheckNext == 0 && iCountCheckNext == MaxCountCheckNext) {
				statusSearchBestCountCheckNext = 1;
			} else if (statusSearchBestCountCheckNext == 1) {
				statusSearchBestCountCheckNext = 2;
			}
			
		}
		
		for (int i=0; i < (GPSNodes.size() - 1); i++) {
			
			MatchedGPSNode n1 = GPSNodes.get(i);
			MatchedGPSNode n2 = GPSNodes.get(i+1);

			double x1, y1;
			if (n1.isReordered) {
				x1 = n1.matchedXreordered;
				y1 = n1.matchedYreordered;
			} else {
				x1 = n1.matchedX;
				y1 = n1.matchedY;
			}
			
			double x2, y2;
			if (n1.isReordered) {
				x2 = n2.matchedXreordered;
				y2 = n2.matchedYreordered;
			} else {
				x2 = n2.matchedX;
				y2 = n2.matchedY;
			}
			
			if (x1 == x2 && y1 == y2) {
				n1.isUniqueMatchedXY = false;
				n2.isUniqueMatchedXY = false;
			}
			
		}
		

	}

	private void matchGPSNodeToNLink(MatchedNLink matchedNLink, MatchedGPSNode matchedGPSNode, int nodeIndex) {

		// get matched position on link
		myOSMWayPart wp = matchedNLink.getStreetLink();
		double matchedX = Coordinates.getNearestPointX(matchedGPSNode, wp);
		double matchedY = Coordinates.getNearestPointY(matchedGPSNode, wp);

		matchedGPSNode.matched_distribution_in_WayParty = Coordinates.getDistributionOfPointInWayPart(matchedX, matchedY,
				wp.startNode.x, wp.startNode.y, wp.endNode.x, wp.endNode.y);

		matchedGPSNode.matchtedWayPart = wp;

		matchedNLink.matchedGPSNodes.addElement(matchedGPSNode);
		wp.CountMatchedGPSNodes++;

		// set matched position to GPS node
		matchedGPSNode.setMatchedX(matchedX);
		matchedGPSNode.setMatchedY(matchedY);
		matchedGPSNode.setMatched(true);

		// adjust matching range
		if (matchedNLink.isMatched()) {
			matchedNLink.setRangeEndIndex(nodeIndex);
		}
		// first node to match, so set start index
		else {
			matchedNLink.setRangeStartIndex(nodeIndex);
			matchedNLink.setRangeEndIndex(nodeIndex);
			matchedNLink.setMatched(true);
		}
	}

	private Vector<ReorderedMatchedGPSNode> wrapSelectedGPSTrace(GPSTrace gpsTrace) {

		Vector<ReorderedMatchedGPSNode> matchedGPSNodes = new Vector<>();

		for (int i = 0; i < gpsTrace.getNrOfNodes(); i++) {
			GPSNode gpsNode = gpsTrace.getNode(i);

			// create wrapped class
			ReorderedMatchedGPSNode matchedGPSNode = new ReorderedMatchedGPSNode(gpsNode, unmatchedNodeColor);

			// store
			matchedGPSNodes.add(matchedGPSNode);
		}

		return matchedGPSNodes;
	}

	private Vector<MatchedNLink> wrapSelectedNRoute(SelectedNRoute selectedNRoute) {

		Vector<MatchedNLink> matchedNLinks = new Vector<>();

		for (myOSMWayPart streetLink : selectedNRoute.getNRouteLinksStart()) {

			// create wrapped class
			MatchedNLink matchedNLink = new MatchedNLink(streetLink, unmatchedLinkColor);

			// store
			matchedNLinks.add(matchedNLink);
		}

		return matchedNLinks;
	}

	public Vector<ReorderedMatchedGPSNode> getReorderedMatchedGPSNodes() {
		return GPSNodes;
	}

	public Vector<MatchedNLink> getMatchedNLinks() {
		return matchedNLinks;
	}

	/**
	 * set new status for match GPS to N route algorithm
	 * 
	 * @param status
	 */
	public void setMatchGPStoNRouteAlgorithmState(String status) {
		matchGPStoNRouteAlgorithmState = status;
	}

	/**
	 * get current status of match GPS to N route algorithm
	 * 
	 * @return
	 */
	public String getMatchGPStoNRouteAlgorithmState() {
		return matchGPStoNRouteAlgorithmState;
	}


	@Override
	public long getRefTimeStamp() {
		return this.refTimeStamp;
	}

	@Override
	public Vector<MatchedGPSNode> getMatchedGPSNodes() {
		// convert vector
		Vector<MatchedGPSNode> matchedGPSNodes = new Vector<>();
		for (ReorderedMatchedGPSNode reorderedMatchedGPSNode : GPSNodes) {
			matchedGPSNodes.add(reorderedMatchedGPSNode);
		}

		return matchedGPSNodes;
	}
}
