package algorithm;

import interfaces.MatchingGPSObject;
import interfaces.StatusUpdate;
import java.awt.Color;
import java.awt.Component;
import java.util.Vector;
import myOSM.myOSMWayPart;
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

	// time in ms thread should sleep after one GPS point is matched
	// private static final int DEFAULT_THREAD_SLEEP_TIME = 10;

	// animation
	//private static final int COLOR_GRADIENT_STEPS = 10;
	//private static final long SLEEP_ANIMATION = 0;

	//private Color gpsNodeColorGradient[]; // store different colors which create
											// an color gradient
	//private Color nLinkColorGradient[]; // store different colors which create
										// an color gradient

	// private SelectedNRoute selectedNRoute;
	// private GPSTrace gpsTrace;
	private long refTimeStamp; // timestamp where measurement started

	Vector<ReorderedMatchedGPSNode> reorderedMatchedGPSNodes = new Vector<>();
	Vector<MatchedNLink> matchedNLinks = new Vector<>();

	private Color unmatchedLinkColor;
	private Color unmatchedNodeColor;

	//private Component drawComponent;

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
		this.reorderedMatchedGPSNodes = wrapSelectedGPSTrace(gpsTrace);

		// create color gradients between two color with a number of steps
		//nLinkColorGradient = getColorGradient(unmatchedLinkColor, matchedLinkColor, COLOR_GRADIENT_STEPS);
		//gpsNodeColorGradient = getColorGradient(unmatchedNodeColor, matchedNodeColor, COLOR_GRADIENT_STEPS);

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

			for (MatchedGPSNode matchedGPSNode : reorderedMatchedGPSNodes) {

				currentMatchedNLink = matchedNLinks.get(currentNLinkIndex);
				nearestMatchedNLink = currentMatchedNLink;

				myOSMWayPart curWP = currentMatchedNLink.getStreetLink();
				//long curWPOsmID = curWP.parentWay.id;
				//long nextWPOsmID = 0;

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

				/*
				while (disToNearest < disToCur && currentNLinkIndex != IndexOfdisToNearest) {

					currentNLinkIndex = IndexOfdisToNearest;
					
					if (currentNLinkIndex == 196 || currentNLinkIndex == 202 ) {
						currentNLinkIndex++;
						currentNLinkIndex--;
					}
					
					disToCur = disToNearest;

					for (int i = 0; i < CountCheckNext; i++) {
						if ((currentNLinkIndex + 1 + i) <= maxIndex) {

							double disToNextTemp = Coordinates.getDistance(matchedGPSNode,
									matchedNLinks.get(currentNLinkIndex + 1 + i).getStreetLink());

							if (disToNextTemp < disToNearest) {
								disToNearest = disToNextTemp;
								IndexOfdisToNearest = currentNLinkIndex + 1 + i;
							}
						}
					}
				}
				*/

				if (statusSearchBestCountCheckNext == 1) {
					matchGPSNodeToNLink(nearestMatchedNLink, matchedGPSNode, currentNodeIndex);					
				}

				tempDistanceofAllGPSNode += disToNearest;

				// animateCurrentLink(matchedNLinks.get(IndexOfdisToNearest));

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
		
		for (int i=0; i < (reorderedMatchedGPSNodes.size() - 1); i++) {
			
			MatchedGPSNode n1 = reorderedMatchedGPSNodes.get(i);
			MatchedGPSNode n2 = reorderedMatchedGPSNodes.get(i+1);

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

	/*
	 * private void applyAdditionalFeature(MatchedNLink currentMatchedNLink, int
	 * startingNode) {
	 * 
	 * Logger.println("\napply addition feature:\n=============================")
	 * ;
	 * 
	 * Vector<ShareIndex> shareIndexList = new Vector<>();
	 * 
	 * // create artificial link which is build of first and last GPS node from
	 * matched N link ReorderedMatchedGPSNode gpsNodeStart =
	 * reorderedMatchedGPSNodes.get(currentMatchedNLink.getRangeStartIndex());
	 * ReorderedMatchedGPSNode gpsNodeEnd =
	 * reorderedMatchedGPSNodes.get(currentMatchedNLink.getRangeEndIndex());
	 * myOSMNode startNode = new myOSMNode(gpsNodeStart.getX(),
	 * gpsNodeStart.getY(), -2); myOSMNode endNode = new
	 * myOSMNode(gpsNodeEnd.getX(), gpsNodeEnd.getY(), -2); myOSMWayPart artLink
	 * = new myOSMWayPart(startNode, endNode, -3, startNode.id, endNode.id);
	 * double artLinkLength = artLink.getLength();
	 * Logger.println("artificial Link length: " + artLinkLength);
	 * 
	 * // match points to artificial link for (int
	 * i=currentMatchedNLink.getRangeStartIndex(); i <=
	 * currentMatchedNLink.getRangeEndIndex(); i++) {
	 * 
	 * // get corresponding GPS node ReorderedMatchedGPSNode matchedGPSNode =
	 * reorderedMatchedGPSNodes.get(i);
	 * 
	 * // get matched position on artificial link double matchedX =
	 * Coordinates.getNearestPointX(matchedGPSNode, artLink); double matchedY =
	 * Coordinates.getNearestPointY(matchedGPSNode, artLink);
	 * 
	 * // build share link from artificial link start node until matched
	 * position myOSMNode shareStartNode = new myOSMNode (artLink.getStartX(),
	 * artLink.getStartY(), artLink.xmyid); myOSMNode shareEndNode = new
	 * myOSMNode (matchedX, matchedY, -4); myOSMWayPart shareLink = new
	 * myOSMWayPart(shareStartNode, shareEndNode, -4, shareStartNode.id,
	 * shareEndNode.id);
	 * 
	 * // get share, avoid null division double share = (artLinkLength != 0.0f)
	 * ? shareLink.getLength() / artLinkLength : 0.0f ;
	 * 
	 * // save share, index and current matched position shareIndexList.add(new
	 * ShareIndex(share, i, matchedGPSNode.getMatchedX(),
	 * matchedGPSNode.getMatchedY()));
	 * 
	 * Logger.println("Share: " + share + "\t\tIndex: " + i); }
	 * 
	 * // reorder/sort if (useReorder) { Collections.sort(shareIndexList);
	 * 
	 * Logger.println("Sorted:\n===========)"); int compareIndex =
	 * currentMatchedNLink.getRangeStartIndex(); for (ShareIndex sI :
	 * shareIndexList) {
	 * 
	 * // set previous and current index ReorderedMatchedGPSNode matchedGPSNode
	 * = reorderedMatchedGPSNodes.get(compareIndex);
	 * matchedGPSNode.setPrevIndex(compareIndex);
	 * matchedGPSNode.setCurIndex(sI.getIndex());
	 * matchedGPSNode.setMatchedX(sI.getMatchedX());
	 * matchedGPSNode.setMatchedY(sI.getMatchedY());
	 * 
	 * Logger.print("Share: " + sI.getShare() + "\t\tIndex: " + sI.getIndex());
	 * if ((compareIndex) != sI.getIndex()) { Logger.print(" Reordered! Prev: "
	 * + compareIndex + " Cur: " + sI.getIndex()); } Logger.print("\n");
	 * compareIndex++; } }
	 * 
	 * // projection/matching if (useProject) {
	 * 
	 * // get matched link as link //myOSMWayPart curLink =
	 * currentMatchedNLink.getStreetLink(); double curLinkStartX = 0; //
	 * (startingNode == StreetLink.START_NODE) ? curLink.getStartX() :
	 * curLink.getEndX(); double curLinkStartY = 0; // (startingNode ==
	 * StreetLink.START_NODE) ? curLink.getStartY() : curLink.getEndY(); double
	 * curLinkEndX = 0; // (startingNode == StreetLink.START_NODE) ?
	 * curLink.getEndX() : curLink.getStartX(); double curLinkEndY = 0; //
	 * (startingNode == StreetLink.START_NODE) ? curLink.getEndY() :
	 * curLink.getStartY();
	 * 
	 * double curLinkVecX = curLinkEndX - curLinkStartX; double curLinkVecY =
	 * curLinkEndY - curLinkStartY;
	 * 
	 * // match nodes according to share int nodeIndex =
	 * currentMatchedNLink.getRangeStartIndex(); for (ShareIndex sI :
	 * shareIndexList) {
	 * 
	 * //double shareIndex = (shareIndexList.size() == 1) ? 0.5f :
	 * sI.getShare();
	 * 
	 * // get matched position double matchedX = (curLinkStartX + sI.getShare()
	 * * curLinkVecX); double matchedY = (curLinkStartY + sI.getShare() *
	 * curLinkVecY);
	 * 
	 * // write value to matched GPS node ReorderedMatchedGPSNode matchedGPSNode
	 * = reorderedMatchedGPSNodes.get(nodeIndex);
	 * matchedGPSNode.setMatchedX(matchedX);
	 * matchedGPSNode.setMatchedY(matchedY); nodeIndex++; }
	 * 
	 * }
	 * 
	 * }
	 */

	// private void matchGPSNodeToNLink(MatchedNLink matchedNLink,
	// MatchedGPSNode matchedGPSNode, int nodeIndex) {
	//
	// Logger.print("matching GPS node: " + nodeIndex + "\n" +
	// "----------------------------------------" + "\n");
	//
	// // get matched position on link
	// int matchedX = Coordinates.getNearestPointX(matchedGPSNode.getNode(),
	// matchedNLink.getStreetLink());
	// int matchedY = Coordinates.getNearestPointY(matchedGPSNode.getNode(),
	// matchedNLink.getStreetLink());
	//
	// // set matched position to GPS node
	// matchedGPSNode.setMatchedX(matchedX);
	// matchedGPSNode.setMatchedY(matchedY);
	//
	// // adjust matching range
	// if (matchedNLink.isMatched()) {
	// matchedNLink.setRangeEndIndex(nodeIndex);
	// }
	// // first node to match, so set start index
	// else {
	// matchedNLink.setRangeStartIndex(nodeIndex);
	// matchedNLink.setRangeEndIndex(nodeIndex);
	// matchedNLink.setMatched(true);
	// }
	// }

	private void matchGPSNodeToNLink(MatchedNLink matchedNLink, MatchedGPSNode matchedGPSNode, int nodeIndex) {

		// Logger.print("matching GPS node: " + nodeIndex + "\n" +
		// "----------------------------------------" + "\n");

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

	/*
	 * private void animateCurrentLink(MatchedNLink matchedNLink) {
	 * 
	 * //Logger.println("\nanimate current link: " +
	 * matchedNLink.getRangeStartIndex() + " - " +
	 * matchedNLink.getRangeEndIndex() + "\n");
	 * 
	 * //animate for (int i=0; i < COLOR_GRADIENT_STEPS; i++){
	 * 
	 * i = COLOR_GRADIENT_STEPS - 1;
	 * 
	 * //sleep this thread due to create an animation try {
	 * Thread.sleep(SLEEP_ANIMATION); } catch (InterruptedException e) { ; }
	 * 
	 * //moving vector factor double f=i/(double) COLOR_GRADIENT_STEPS;
	 * 
	 * //gradual move matched GPS node from original GPS position to matched
	 * position for (int j=matchedNLink.getRangeStartIndex();
	 * j<=matchedNLink.getRangeEndIndex(); j++){
	 * 
	 * j = matchedNLink.getRangeEndIndex();
	 * 
	 * // get matched GPS node MatchedGPSNode matchedGPSNode =
	 * reorderedMatchedGPSNodes.get(j);
	 * 
	 * //get next position of GPS nodes double nextX = (matchedGPSNode.getX() +
	 * (f*(matchedGPSNode.getMatchedX() - matchedGPSNode.getX()))); double nextY
	 * = (matchedGPSNode.getY() + (f*(matchedGPSNode.getMatchedY() -
	 * matchedGPSNode.getY())));
	 * 
	 * //set calculated color and moved position as next position to draw
	 * matchedGPSNode.setDrawX(nextX); matchedGPSNode.setDrawY(nextY);
	 * matchedGPSNode.setColor(gpsNodeColorGradient[i]);
	 * 
	 * // set calculated color for n Link
	 * matchedNLink.setColor(nLinkColorGradient[i]); }
	 * 
	 * //redraw moved GPS nodes drawComponent.repaint(); } }
	 */

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
		return reorderedMatchedGPSNodes;
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

	/*
	 * private boolean sleepThread(long milliseconds) { // Thread sleep for
	 * animation and refresh painting do { try { Thread.sleep(milliseconds); }
	 * catch (InterruptedException e) { e.printStackTrace();}
	 * drawComponent.repaint(); } while (getMatchGPStoNRouteAlgorithmState() ==
	 * MATCH_GPS_TO_N_ROUTE_PAUSED);
	 * 
	 * // check if algorithm should be continued or shut down return
	 * (getMatchGPStoNRouteAlgorithmState() == MATCH_GPS_TO_N_ROUTE_RUNNING) ?
	 * true : false; }
	 */

	/**
	 * create a color gradient between startColor and targetColor with given
	 * steps
	 * 
	 * @param startColor
	 * @param targetColor
	 * @param steps
	 * @return Color[]
	 */
	/*
	private Color[] getColorGradient(Color startColor, Color targetColor, int steps) {
		// save color gradient into array
		Color[] colors = new Color[steps];

		// save r, g, b values of start color
		int startColorRed = startColor.getRed();
		int startColorGreen = startColor.getGreen();
		int startColorBlue = startColor.getBlue();

		// save r, g, b values of end color
		int targetColorRed = targetColor.getRed();
		int targetColorGreen = targetColor.getGreen();
		int targetColorBlue = targetColor.getBlue();

		// calculate step interval for reaching target color by every step
		int stepRed = (int) ((targetColorRed - startColorRed) / (double) steps);
		int stepGreen = (int) ((targetColorGreen - startColorGreen) / (double) steps);
		int stepBlue = (int) ((targetColorBlue - startColorBlue) / (double) steps);

		// create colors for gradient and save to an color array
		for (int i = 0; i < steps; i++) {
			colors[i] = new Color(startColorRed + (int) (i * stepRed), startColorGreen + (int) (i * stepGreen),
					startColorBlue + (int) (i * stepBlue));
		}

		// return this array
		return colors;
	}
*/
	
	/**
	 * subclass which stores a GPS node position-share of a whole link and it's
	 * index
	 */
	/*
	 * private class ShareIndex implements Comparable<ShareIndex> {
	 * 
	 * private double share; private int index; private double matchedX; private
	 * double matchedY;
	 * 
	 * 
	 * ShareIndex(double share, int index, double matchedX, double matchedY) {
	 * this.share = share; this.index = index; }
	 * 
	 * 
	 * 
	 * public double getMatchedX() { return matchedX; }
	 * 
	 * 
	 * 
	 * public void setMatchedX(int matchedX) { this.matchedX = matchedX; }
	 * 
	 * 
	 * 
	 * 
	 * public double getMatchedY() { return matchedY; }
	 * 
	 * 
	 * 
	 * public void setMatchedY(int matchedY) { this.matchedY = matchedY; }
	 * 
	 * 
	 * 
	 * 
	 * public double getShare() { return share; }
	 * 
	 * 
	 * public void setShare(double share) { this.share = share; }
	 * 
	 * 
	 * public int getIndex() { return index; }
	 * 
	 * 
	 * public void setIndex(int index) { this.index = index; }
	 * 
	 * 
	 * @Override public int compareTo(ShareIndex o) { double diff = getShare() -
	 * o.getShare(); // if share value of this object - share value of o... //
	 * ... = 0 => equal position // ... < 0 => put o behind this object // ... >
	 * 0 => put this object behind o return (diff == 0.0f) ? 0 : (diff > 0.0f) ?
	 * 1 : -1; } }
	 */

	@Override
	public long getRefTimeStamp() {
		return this.refTimeStamp;
	}

	@Override
	public Vector<MatchedGPSNode> getMatchedGPSNodes() {
		// convert vector
		Vector<MatchedGPSNode> matchedGPSNodes = new Vector<>();
		for (ReorderedMatchedGPSNode reorderedMatchedGPSNode : reorderedMatchedGPSNodes) {
			matchedGPSNodes.add(reorderedMatchedGPSNode);
		}

		return matchedGPSNodes;
	}
}
