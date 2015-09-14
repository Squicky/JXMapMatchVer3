package route;

import java.util.LinkedList;
import java.util.Vector;

import algorithm.MatchedLink;
import myClasses.myOSMNode;
import myClasses.myOSMWayPart;
import cartesian.Coordinates;
import gps.GPSNode;
import gps.GPSTrace;

/**
 * @author Daniel Sathees Elmo
 * 
 *         this class represents an route which will be build by the N Route
 *         algorithm it also implements the comparable interface so it can be
 *         automatically added into an sorted set (uses compareTo method to
 *         sort)
 */

public class NRoute implements Comparable<NRoute>, Cloneable {

	public String history = "";

	// constants for compareTo method
	public static final int EQUAL_SCORE = 0;
	public static final int BETTER_SCORE = -1;
	public static final int WORSE_SCORE = 1;

	public static int objCount = 0;
	public int objID = 0;

	// save reference to GPS trace
	private GPSTrace gpsTrace;

	// GPS node index offset for looking points in future
	private int gpsNodeIndexOffset;

	// values for offset (in order to determine outgoing node of a link
	// we look a GPS node in future and measure shortest distance to a link)
	public static final int DEFAULT_GPS_NODE_INDEX_OFFSET = 5;
	public static final int MAX_GPS_NODE_INDEX_OFFSET = 10;

	// save matched street links added to route as matched link
	private Vector<MatchedLink> nRouteLinks;

	// save score of this route in respect of GPS trace
	public LinkedList<Double> scoreList = new LinkedList<Double>();
	private double score;
	private double previousScore;
	private int countUpdateScoreNegative = 0;
	
	private double length;	

	// save (optional) reference to previous n route
	private NRoute previousNRoute;

	// should current GPS node be matched to previous link?
	private boolean matchToPreviousLink;

	// reference to previous matched link
	private MatchedLink previousMatchedLink;

	/**
	 * initialize with GPS trace
	 * 
	 * @param gpsTrace
	 */
	public NRoute(GPSTrace gpsTrace, String historyOfParent) {

		objID = objCount;
		objCount++;

		//history = objID + " " + historyOfParent;

		// save reference
		this.gpsTrace = gpsTrace;
		// initialize vector
		this.nRouteLinks = new Vector<MatchedLink>();
		// set score zero at beginning
		score = 0;
		// set previous route null for now
		this.previousNRoute = null;
		// set default GPS node index offset
		this.gpsNodeIndexOffset = DEFAULT_GPS_NODE_INDEX_OFFSET;

	}

	public NRoute(GPSTrace gpsTrace) {

		this(gpsTrace, "");

	}

	public double getLength() {
		return this.length;
	}
	
	private void addlength(double _length) {
		this.length += _length;
	}
	

	public boolean istGleich(NRoute other) {

		if (other.getScore() != this.getScore()) {
			return false;
		}

		if (other.nRouteLinks.size() != this.nRouteLinks.size()) {
			return false;
		}

		for (int i = 0; i < other.nRouteLinks.size(); i++) {
			if (other.nRouteLinks.get(i).getStreetLink() != this.nRouteLinks.get(i).getStreetLink()) {
				return false;
			}
		}

		return true;
	}

	public boolean istNRouteLinksGleichUndScoreBesser(NRoute other) {

		if (other.getScore() <= this.getScore()) {
			return false;
		}

		if (other.nRouteLinks.size() != this.nRouteLinks.size()) {
			return false;
		}

		for (int i = 0; i < other.nRouteLinks.size(); i++) {
			if (other.nRouteLinks.get(i).getStreetLink() != this.nRouteLinks.get(i).getStreetLink()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * add link to n route container, set matched range, and set as matched
	 * 
	 * @param streetLink
	 * @param minGPSNodeIndex
	 * @param maxGPSNodeIndex
	 */
	public void addLink(myOSMWayPart myWayPart, int minGPSNodeIndex, int maxGPSNodeIndex) {
		// create new matched link
		MatchedLink matchedLink = new MatchedLink(myWayPart, minGPSNodeIndex, maxGPSNodeIndex);
		// add link and range
		nRouteLinks.add(matchedLink);
		this.addlength(matchedLink.getStreetLink().length);
		// update score
		updateScore(getScoreForLink(matchedLink));
	}

	/**
	 * add link to n route container, set matched range, and set as matched
	 * 
	 * @param streetLink
	 * @param minGPSNodeIndex
	 * @param maxGPSNodeIndex
	 */
	/*
	 * public void addLink(StreetLink streetLink, int minGPSNodeIndex, int
	 * maxGPSNodeIndex) {
	 * 
	 * 
	 * 
	 * // create new matched link MatchedLink matchedLink = new
	 * MatchedLink(streetLink, minGPSNodeIndex, maxGPSNodeIndex); // add link
	 * and range nRouteLinks.add(matchedLink); // update score updateScore(
	 * getScoreForLink(matchedLink) ); }
	 */

	/**
	 * add link to n route container, set matched range, and set as matched
	 * 
	 * @param myWayPart
	 * @param gpsNodeIndex
	 */
	public void addLink(myOSMWayPart myWayPartNew, int gpsNodeIndex) {
		// we assume that given GPS node should be matched to previous link due
		// to better score
		matchToPreviousLink = true;

		// check if n route link vector isn't empty
		if (!nRouteLinks.isEmpty()) {

			// get previous matched link and new GPS node
			previousMatchedLink = nRouteLinks.lastElement();

			/*
			myOSMWayPart previousWayPart = previousMatchedLink.getStreetLink();

			GPSNode gpsNode = gpsTrace.getNode(gpsNodeIndex);

			
			// calculate distance to previous and new link
			double distanceToLastMatchedLink = Coordinates.getDistance(gpsNode, previousWayPart);
			double distanceToNewStreetLink = Coordinates.getDistance(gpsNode, myWayPartNew);

			// if distance to previous link less...
			if (distanceToLastMatchedLink < distanceToNewStreetLink) {
				// ...match new GPS node to previous link
				addGPSNodeToLastLink(gpsNodeIndex);

				// create "unmatched" link for new link and add it to n route
				// link vector
				MatchedLink matchedLink = new MatchedLink(myWayPartNew, -1, -1);
				nRouteLinks.add(matchedLink);

				// done
				return;
			}
			*/

		}

		// if distance to new link is shorter or n route link vector is empty,
		// create/match to new link
		addLink(myWayPartNew, gpsNodeIndex, gpsNodeIndex);
		matchToPreviousLink = false;
	}

	/**
	 * adds an GPS node to last added link
	 * 
	 * @param GPSNodeIndex
	 * @return boolean
	 */
	public boolean addGPSNodeToLastLink(int GPSNodeIndex) {
		// add gpsNode to range of last added link, if container is not empty
		if (!nRouteLinks.isEmpty()) {

			// get last added link
			MatchedLink lastAddedMatchedLink = nRouteLinks.lastElement();

			// if last GPS node was matched to previous link, check if we can
			// match the new GPS node
			// to new link
			if (matchToPreviousLink) {

				// get last link, previous matched link and new GPS node

				myOSMWayPart lastWaypart = lastAddedMatchedLink.getStreetLink();

				myOSMWayPart previousWayPart = previousMatchedLink.getStreetLink();

				GPSNode gpsNode = gpsTrace.getNode(GPSNodeIndex);

				// calculate distance to previous matched and last added link
				double distanceToPreviousMatchedLink = Coordinates.getDistance(gpsNode, previousWayPart);
				double distanceToLastAddedLink = Coordinates.getDistance(gpsNode, lastWaypart);

				// if distance to previous link less...
				if (distanceToLastAddedLink < distanceToPreviousMatchedLink) {
					// ... match to new link
					lastAddedMatchedLink.setRangeStartIndex(GPSNodeIndex);
					matchToPreviousLink = false;
				}
			}

			// update range
			if (lastAddedMatchedLink.getRangeSize() == 0) {
				lastAddedMatchedLink.setRangeStartIndex(GPSNodeIndex);
				lastAddedMatchedLink.setRangeEndIndex(GPSNodeIndex);
			} else {
				lastAddedMatchedLink.setRangeEndIndex(GPSNodeIndex);
			}

			// update score for just last added GPS node index
			updateScore(getScoreForLinkAndRange(lastAddedMatchedLink, lastAddedMatchedLink.getRangeEndIndex(),
					lastAddedMatchedLink.getRangeEndIndex()));

			// adding successful
			return true;
		}

		// no link there, return false
		return false;
	}

	/**
	 * adds an GPS node to last added link
	 * 
	 * @param GPSNodeIndex
	 * @return boolean
	 */
	/*
	 * public boolean addGPSNodeToLastLink(int GPSNodeIndex) { // add gpsNode to
	 * range of last added link, if container is not empty if
	 * (!nRouteLinks.isEmpty()) { // get last added link MatchedLink
	 * lastAddedMatchedLink = nRouteLinks.lastElement(); // update range
	 * lastAddedMatchedLink.setRangeEndIndex(GPSNodeIndex); // update score for
	 * just last added GPS node index updateScore(
	 * getScoreForLinkAndRange(lastAddedMatchedLink,
	 * lastAddedMatchedLink.getRangeEndIndex(),
	 * lastAddedMatchedLink.getRangeEndIndex()) );
	 * 
	 * // adding successful return true; }
	 * 
	 * // otherwise return false return false; }
	 */

	/**
	 * removes last matched GPS if possible
	 * 
	 * @return boolean
	 */
	public boolean removeLastGPSNodeFromLastLink() {
		// remove last matched GPS node from last link
		// if route container isn't empty and we got at least one matched GPS
		// node
		// if (!nRouteLinks.isEmpty() &&
		// (nRouteLinks.lastElement().getRangeSize() > 0)) {

		if (!nRouteLinks.isEmpty()) {
			// get last added matched link
			MatchedLink lastAddedMatchedLink = nRouteLinks.lastElement();
			// update score by subtracting score
			updateScore(-getScoreForLinkAndRange(lastAddedMatchedLink, lastAddedMatchedLink.getRangeEndIndex(),
					lastAddedMatchedLink.getRangeEndIndex()));

			// NOW remove last matched GPS node after updating score
			if (lastAddedMatchedLink.getRangeSize() <= 1) {
				lastAddedMatchedLink.setRangeStartIndex(-1);
				lastAddedMatchedLink.setRangeEndIndex(-1);
			} else {
				lastAddedMatchedLink.setRangeEndIndex(lastAddedMatchedLink.getRangeEndIndex() - 1);
			}

			// removing successful
			return true;
		}

		// couldn't remove last GPS node
		return false;
	}

	/**
	 * updates score of this path by add/sub difference
	 * 
	 * @param difference
	 */
	private void updateScore(double difference) {

		if (0 <= difference) {
			this.previousScore = this.score;
			this.score = this.score + difference;
			scoreList.add(difference);
			countUpdateScoreNegative = 0;
		} else {

			difference = difference * (-1.0);
			if (scoreList.getLast() != difference) {
				System.out.println("Error? Debug: NRoute: updateScore");
			}
			
			if (countUpdateScoreNegative == 0) {
				scoreList.removeLast();
				this.score = this.previousScore;
				countUpdateScoreNegative++;
			} else {
				score = 0;
				for (int i = 0; i < scoreList.size() - 1; i++) {
					score += scoreList.get(i);
				}
				this.previousScore = this.score;
				score += scoreList.getLast();
			}
		}
	}

	/**
	 * get score of this path
	 * 
	 * @return
	 */
	public double getScore() {
		return score;
	}

	/**
	 * set GPS node index offset we look in order to determine outgoing link of
	 * street link
	 * 
	 * @param offset
	 */
	public void setGPSNodeIndexOffset(int offset) {
		// clear and save value
		offset = Math.abs(offset);
		this.gpsNodeIndexOffset = (offset <= MAX_GPS_NODE_INDEX_OFFSET) ? offset : MAX_GPS_NODE_INDEX_OFFSET;
	}

	/**
	 * get current set GPS node index offset (to determine outgoing node)
	 * 
	 * @param offset
	 * @return int
	 */
	public int getGPSNodeIndexOffset(int offset) {
		return this.gpsNodeIndexOffset;
	}

	/**
	 * compares to another n route, returns zero if both route have same score
	 * if current route's score is smaller return -1, otherwise return 1
	 */
	@Override
	public int compareTo(NRoute nRoute) {
		// compare n routes
		// if (score == nRoute.getScore()) return EQUAL_SCORE;
		if (this.getScore() == nRoute.getScore()) {

			if (this.getLength() < nRoute.getLength()) {
				return BETTER_SCORE;
			} else {
				return WORSE_SCORE;
			}
		}
		if (this.getScore() < nRoute.getScore()) {
			return BETTER_SCORE;
		}

		// otherwise the comparative n route has a better score
		return WORSE_SCORE;
	}

	@Override
	public NRoute clone() {
		// create new instance
		NRoute nRouteClone = new NRoute(this.gpsTrace, this.history);

		// copy properties

		nRouteClone.scoreList.addAll(this.scoreList);
		nRouteClone.score = this.score;
		nRouteClone.previousScore = this.previousScore;
		
		nRouteClone.gpsNodeIndexOffset = this.gpsNodeIndexOffset;
		nRouteClone.previousNRoute = this.previousNRoute;

		/*
		 * copy vector, here be careful! references to street link can be
		 * adopted, but copy (create new) matched ranges!
		 */

		// create new vector for copied matched link
		Vector<MatchedLink> nRouteLinksClone = new Vector<MatchedLink>();

		for (MatchedLink matchedLink : nRouteLinks) {
			// copy matched link
			MatchedLink matchedLinkClone = new MatchedLink(matchedLink.getStreetLink(),
					matchedLink.getRangeStartIndexForClone(), matchedLink.getRangeEndIndexForClone());
			// add to vector
			nRouteLinksClone.add(matchedLinkClone);
		}

		// assign copied vector to cloned object
		nRouteClone.nRouteLinks = nRouteLinksClone;

		// return cloned NRoute
		return nRouteClone;
	}

	/**
	 * calculates score of a link
	 * 
	 * @param matchedLink
	 * @return double
	 */
	private double getScoreForLink(MatchedLink matchedLink) {
		return getScoreForLinkAndRange(matchedLink, matchedLink.getRangeStartIndex(), matchedLink.getRangeEndIndex());
	}

	/**
	 * calculates score of a link to a certain range
	 * 
	 * @param streetLink
	 * @param minGPSNodeIndex
	 * @param maxGPSNodeIndex
	 * @return double
	 */
	private double getScoreForLinkAndRange(MatchedLink matchedLink, int minGPSNodeIndex, int maxGPSNodeIndex) {

		// store score for link here
		double linkScore = 0;

		// calculate score for link to its GPS nodes
		for (int i = minGPSNodeIndex; i <= maxGPSNodeIndex; i++) {
			linkScore += Coordinates.getDistance(gpsTrace.getNode(i), matchedLink.getStreetLink());
		}

		// return score
		return linkScore;
	}

	/**
	 * get street links in this route
	 * 
	 * @return
	 */
	public Vector<MatchedLink> getNRouteLinks() {
		return nRouteLinks;
	}

	/**
	 * get last added matched link
	 * 
	 * @return MatchedLink
	 */
	public MatchedLink getLastMatchedLink() {
		// check if vector is empty
		if (!nRouteLinks.isEmpty()) {
			return nRouteLinks.lastElement();
		}

		// return null if there is no last added link
		return null;
	}

	public MatchedLink getFirstMatchedLink() {
		// check if vector is empty
		if (!nRouteLinks.isEmpty()) {
			return nRouteLinks.firstElement();
		}

		// return null if there is no last added link
		return null;
	}

	public Vector<myOSMWayPart> getOutgoingLinksForLastLink() {

		// check if vector is not empty
		if (!nRouteLinks.isEmpty()) {

			Vector<myOSMWayPart> vsl = new Vector<myOSMWayPart>();

			Vector<myOSMWayPart> vwp = getNextOSMWayPart();

			for (int i = 0; i < vwp.size(); i++) {

				myOSMWayPart wp = vwp.get(i);

				// StreetLink sl = wp.streetLink;

				// vsl.add(sl);

				vsl.add(wp);
			}

			return vsl;

			/*
			 * 
			 * 
			 * // get last added link MatchedLink lastMatchedLink =
			 * nRouteLinks.lastElement(); StreetLink lastAddedLink =
			 * lastMatchedLink.getStreetLink();
			 * 
			 * // check if we got at least two links in our vector, find
			 * outgoing node/links // through connection between last two links
			 * if (nRouteLinks.size() > 1) {
			 * 
			 * // get last but one added street links StreetLink
			 * lastButOneAddedLink =
			 * nRouteLinks.get(nRouteLinks.size()-2).getStreetLink();
			 * 
			 * // check where they are connected int connectionType =
			 * lastAddedLink.isConnectedTo(lastButOneAddedLink);
			 * 
			 * // get street links connected to outgoing node if (connectionType
			 * == START_NODE) return
			 * lastAddedLink.getEndNode().getLinksExcept(lastAddedLink); if
			 * (connectionType == END_NODE) return
			 * lastAddedLink.getStartNode().getLinksExcept(lastAddedLink);
			 * 
			 * // Logger.errln("No connection between last two links!");
			 * 
			 * // otherwise find out outgoing link via shortest distance, if we
			 * got some more // GPS points in forward } else if
			 * (lastMatchedLink.getRangeEndIndex() <
			 * (gpsTrace.getNrOfNodes()-1)) { //
			 * Logger.println("Find outgoing node via distance!");
			 * 
			 * // get distance to last GPS node int offsetToLastGPSNode =
			 * ((gpsTrace.getNrOfNodes()-1) -
			 * lastMatchedLink.getRangeEndIndex());
			 * 
			 * // if distance is greater then current GPS node index, take
			 * current set GPS node index offset, // otherwise take calculated
			 * offset int offsetToNextGPSNode = (offsetToLastGPSNode >
			 * gpsNodeIndexOffset) ? gpsNodeIndexOffset : offsetToLastGPSNode;
			 * 
			 * // get "next" GPS node, we'll use this to determine which street
			 * node is the outgoing one GPSNode nextGPSNode =
			 * gpsTrace.getNode(lastMatchedLink.getRangeEndIndex() +
			 * offsetToNextGPSNode);
			 * 
			 * // get start-/end- node of street link StreetNode startNode =
			 * lastAddedLink.getStartNode(); StreetNode endNode =
			 * lastAddedLink.getEndNode();
			 * 
			 * // calculate distance between start-/end node and "next" GPS node
			 * double distanceToStartNode = Coordinates.getDistance(nextGPSNode,
			 * startNode); double distanceToEndNode =
			 * Coordinates.getDistance(nextGPSNode, endNode);
			 * 
			 * // return outgoing links of street node with the shortest
			 * distance to last GPS node return (distanceToStartNode >
			 * distanceToEndNode) ? endNode.getLinksExcept(lastAddedLink) :
			 * startNode.getLinksExcept(lastAddedLink); }
			 */
		}

		// otherwise no links there
		return null;
	}

	public Vector<myOSMWayPart> getNextOSMWayPart() {
		return NRoute.getNextOSMWayPart(this);
	}

	public Vector<myOSMNode> getLastOSMNode() {
		return NRoute.getLastOSMNode(this);
	}

	public static Vector<myOSMNode> getLastOSMNode(NRoute nRoute) {

		Vector<myOSMNode> vn = new Vector<myOSMNode>();

		// StreetLink lastSL =
		// nRoute.getNRouteLinks().lastElement().getStreetLink();
		myOSMWayPart lastWP = nRoute.getNRouteLinks().lastElement().getStreetLink();

		vn.add(lastWP.endNode);

		/*
		 * if (nRoute.getNRouteLinks().size() == 1) { if (lastWP.myWayPart !=
		 * null) { vn.add(lastSL.myWayPart.startNode);
		 * vn.add(lastSL.myWayPart.endNode);
		 * 
		 * return vn; }
		 * 
		 * if (lastSL.myWayPartBackDirection != null) {
		 * vn.add(lastSL.myWayPartBackDirection.startNode);
		 * vn.add(lastSL.myWayPartBackDirection.endNode);
		 * 
		 * return vn; }
		 * 
		 * }
		 * 
		 * StreetLink penultimateSL =
		 * nRoute.getNRouteLinks().get(nRoute.getNRouteLinks().size() -
		 * 2).getStreetLink();
		 * 
		 * if (lastSL.myWayPart != null) {
		 * 
		 * if (penultimateSL.myWayPart != null) { if (lastSL.myWayPart.startNode
		 * == penultimateSL.myWayPart.endNode || lastSL.myWayPart.startNode ==
		 * penultimateSL.myWayPart.startNode) {
		 * 
		 * vn.add(lastSL.myWayPart.endNode); return vn;
		 * 
		 * } else {
		 * 
		 * vn.add(lastSL.myWayPart.startNode); return vn;
		 * 
		 * } }
		 * 
		 * if (penultimateSL.myWayPartBackDirection != null) { if
		 * (lastSL.myWayPart.startNode ==
		 * penultimateSL.myWayPartBackDirection.endNode ||
		 * lastSL.myWayPart.startNode ==
		 * penultimateSL.myWayPartBackDirection.startNode) {
		 * 
		 * vn.add(lastSL.myWayPart.endNode); return vn;
		 * 
		 * } else {
		 * 
		 * vn.add(lastSL.myWayPart.startNode); return vn;
		 * 
		 * } }
		 * 
		 * }
		 * 
		 * if (lastSL.myWayPartBackDirection != null) {
		 * 
		 * if (penultimateSL.myWayPart != null) { if
		 * (lastSL.myWayPartBackDirection.startNode ==
		 * penultimateSL.myWayPart.endNode ||
		 * lastSL.myWayPartBackDirection.startNode ==
		 * penultimateSL.myWayPart.startNode) {
		 * 
		 * vn.add(lastSL.myWayPartBackDirection.endNode); return vn;
		 * 
		 * } else {
		 * 
		 * vn.add(lastSL.myWayPartBackDirection.startNode); return vn; } }
		 * 
		 * if (penultimateSL.myWayPartBackDirection != null) { if
		 * (lastSL.myWayPartBackDirection.startNode ==
		 * penultimateSL.myWayPartBackDirection.endNode ||
		 * lastSL.myWayPartBackDirection.startNode ==
		 * penultimateSL.myWayPartBackDirection.startNode) {
		 * 
		 * vn.add(lastSL.myWayPartBackDirection.endNode); return vn;
		 * 
		 * } else {
		 * 
		 * vn.add(lastSL.myWayPartBackDirection.startNode); return vn;
		 * 
		 * } }
		 * 
		 * }
		 */

		return vn;
	}

	public static Vector<myOSMWayPart> getNextOSMWayPart(NRoute nRoute) {

		// StreetLink lastSL =
		// nRoute.getNRouteLinks().lastElement().getStreetLink();
		myOSMWayPart lastWP = nRoute.getNRouteLinks().lastElement().getStreetLink();

		Vector<myOSMWayPart> vwp = new Vector<myOSMWayPart>();

		for (int i = 0; i < lastWP.endNode.WayPartsOutgoing_size(); i++) {
			myOSMWayPart wp = lastWP.endNode.WayPartsOutgoing_get(i);
			if (wp.endNode != lastWP.startNode) {
				vwp.add(wp);
			}
		}

		/*
		 * if (nRoute.getNRouteLinks().size() == 1) { if (lastSL.myWayPart !=
		 * null) { for (int i = 0; i <
		 * lastSL.myWayPart.endNode.WayPartsToConnectedNotes.size(); i++ ) { if
		 * (lastSL.myWayPart.endNode.WayPartsToConnectedNotes.get(i).streetLink
		 * != lastSL) { vwp.add(
		 * lastSL.myWayPart.endNode.WayPartsToConnectedNotes.get(i) ); } } }
		 * 
		 * if (lastSL.myWayPartBackDirection != null) { for (int i = 0; i <
		 * lastSL
		 * .myWayPartBackDirection.endNode.WayPartsToConnectedNotes.size(); i++
		 * ) { if
		 * (lastSL.myWayPartBackDirection.endNode.WayPartsToConnectedNotes
		 * .get(i).streetLink != lastSL) { vwp.add(
		 * lastSL.myWayPartBackDirection.endNode.WayPartsToConnectedNotes.get(i)
		 * ); } } }
		 * 
		 * return vwp;
		 * 
		 * }
		 * 
		 * StreetLink penultimateSL =
		 * nRoute.getNRouteLinks().get(nRoute.getNRouteLinks().size() -
		 * 2).getStreetLink();
		 * 
		 * if (lastSL.myWayPart != null) {
		 * 
		 * if (penultimateSL.myWayPart != null) {
		 * 
		 * if (lastSL.myWayPart.startNode == penultimateSL.myWayPart.endNode) {
		 * 
		 * for (int i=0; i <
		 * lastSL.myWayPart.endNode.WayPartsToConnectedNotes.size(); i++) {
		 * 
		 * if (lastSL.myWayPart.startNode !=
		 * lastSL.myWayPart.endNode.WayPartsToConnectedNotes.get(i).endNode) {
		 * vwp.add(lastSL.myWayPart.endNode.WayPartsToConnectedNotes.get(i)); }
		 * 
		 * } return vwp;
		 * 
		 * }
		 * 
		 * }
		 * 
		 * if (penultimateSL.myWayPartBackDirection != null) {
		 * 
		 * if (lastSL.myWayPart.startNode ==
		 * penultimateSL.myWayPartBackDirection.endNode) {
		 * 
		 * for (int i=0; i <
		 * lastSL.myWayPart.endNode.WayPartsToConnectedNotes.size(); i++) {
		 * 
		 * if (lastSL.myWayPart.startNode !=
		 * lastSL.myWayPart.endNode.WayPartsToConnectedNotes.get(i).endNode) {
		 * vwp.add(lastSL.myWayPart.endNode.WayPartsToConnectedNotes.get(i)); }
		 * 
		 * } return vwp;
		 * 
		 * }
		 * 
		 * }
		 * 
		 * }
		 * 
		 * 
		 * if (lastSL.myWayPartBackDirection != null) {
		 * 
		 * if (penultimateSL.myWayPart != null) {
		 * 
		 * if (lastSL.myWayPartBackDirection.startNode ==
		 * penultimateSL.myWayPart.endNode) {
		 * 
		 * for (int i=0; i <
		 * lastSL.myWayPartBackDirection.endNode.WayPartsToConnectedNotes
		 * .size(); i++) {
		 * 
		 * if (lastSL.myWayPartBackDirection.startNode !=
		 * lastSL.myWayPartBackDirection
		 * .endNode.WayPartsToConnectedNotes.get(i).endNode) {
		 * vwp.add(lastSL.myWayPartBackDirection
		 * .endNode.WayPartsToConnectedNotes.get(i)); }
		 * 
		 * } return vwp;
		 * 
		 * }
		 * 
		 * }
		 * 
		 * if (penultimateSL.myWayPartBackDirection != null) {
		 * 
		 * if (lastSL.myWayPartBackDirection.startNode ==
		 * penultimateSL.myWayPartBackDirection.endNode) {
		 * 
		 * for (int i=0; i <
		 * lastSL.myWayPartBackDirection.endNode.WayPartsToConnectedNotes
		 * .size(); i++) {
		 * 
		 * if (lastSL.myWayPartBackDirection.startNode !=
		 * lastSL.myWayPartBackDirection
		 * .endNode.WayPartsToConnectedNotes.get(i).endNode) {
		 * vwp.add(lastSL.myWayPartBackDirection
		 * .endNode.WayPartsToConnectedNotes.get(i)); }
		 * 
		 * } return vwp;
		 * 
		 * }
		 * 
		 * } }
		 * 
		 * 
		 * if (lastSL.myWayPart != null) {
		 * 
		 * if (penultimateSL.myWayPart != null) { if (lastSL.myWayPart.startNode
		 * == penultimateSL.myWayPart.endNode || lastSL.myWayPart.startNode ==
		 * penultimateSL.myWayPart.startNode) {
		 * 
		 * for (int i=0; i <
		 * lastSL.myWayPart.endNode.WayPartsToConnectedNotes.size(); i++) {
		 * 
		 * if (lastSL.myWayPart.startNode !=
		 * lastSL.myWayPart.endNode.WayPartsToConnectedNotes.get(i).endNode) {
		 * vwp.add(lastSL.myWayPart.endNode.WayPartsToConnectedNotes.get(i)); }
		 * 
		 * }
		 * 
		 * return vwp;
		 * 
		 * } else { for (int i=0; i <
		 * lastSL.myWayPart.startNode.WayPartsToConnectedNotes.size(); i++) {
		 * 
		 * if (lastSL.myWayPart.startNode !=
		 * lastSL.myWayPart.startNode.WayPartsToConnectedNotes.get(i).endNode) {
		 * vwp.add(lastSL.myWayPart.startNode.WayPartsToConnectedNotes.get(i));
		 * }
		 * 
		 * }
		 * 
		 * return vwp;
		 * 
		 * } }
		 * 
		 * if (penultimateSL.myWayPartBackDirection != null) { if
		 * (lastSL.myWayPart.startNode ==
		 * penultimateSL.myWayPartBackDirection.endNode ||
		 * lastSL.myWayPart.startNode ==
		 * penultimateSL.myWayPartBackDirection.startNode) {
		 * 
		 * for (int i=0; i <
		 * lastSL.myWayPart.endNode.WayPartsToConnectedNotes.size(); i++) {
		 * 
		 * if (lastSL.myWayPart.startNode !=
		 * lastSL.myWayPart.endNode.WayPartsToConnectedNotes.get(i).endNode) {
		 * vwp.add(lastSL.myWayPart.endNode.WayPartsToConnectedNotes.get(i)); }
		 * 
		 * }
		 * 
		 * return vwp;
		 * 
		 * } else { for (int i=0; i <
		 * lastSL.myWayPart.startNode.WayPartsToConnectedNotes.size(); i++) {
		 * 
		 * if (lastSL.myWayPart.startNode !=
		 * lastSL.myWayPart.startNode.WayPartsToConnectedNotes.get(i).endNode) {
		 * vwp.add(lastSL.myWayPart.startNode.WayPartsToConnectedNotes.get(i));
		 * }
		 * 
		 * }
		 * 
		 * return vwp;
		 * 
		 * } }
		 * 
		 * }
		 * 
		 * if (lastSL.myWayPartBackDirection != null) {
		 * 
		 * if (penultimateSL.myWayPart != null) { if
		 * (lastSL.myWayPartBackDirection.startNode ==
		 * penultimateSL.myWayPart.endNode ||
		 * lastSL.myWayPartBackDirection.startNode ==
		 * penultimateSL.myWayPart.startNode) {
		 * 
		 * for (int i=0; i <
		 * lastSL.myWayPartBackDirection.endNode.WayPartsToConnectedNotes
		 * .size(); i++) {
		 * 
		 * if (lastSL.myWayPartBackDirection.startNode !=
		 * lastSL.myWayPartBackDirection
		 * .endNode.WayPartsToConnectedNotes.get(i).endNode) {
		 * vwp.add(lastSL.myWayPartBackDirection
		 * .endNode.WayPartsToConnectedNotes.get(i)); }
		 * 
		 * }
		 * 
		 * return vwp;
		 * 
		 * } else { for (int i=0; i <
		 * lastSL.myWayPartBackDirection.startNode.WayPartsToConnectedNotes
		 * .size(); i++) {
		 * 
		 * if (lastSL.myWayPartBackDirection.startNode !=
		 * lastSL.myWayPartBackDirection
		 * .startNode.WayPartsToConnectedNotes.get(i).endNode) {
		 * vwp.add(lastSL.myWayPartBackDirection
		 * .startNode.WayPartsToConnectedNotes.get(i)); }
		 * 
		 * }
		 * 
		 * return vwp;
		 * 
		 * } }
		 * 
		 * if (penultimateSL.myWayPartBackDirection != null) { if
		 * (lastSL.myWayPartBackDirection.startNode ==
		 * penultimateSL.myWayPartBackDirection.endNode ||
		 * lastSL.myWayPartBackDirection.startNode ==
		 * penultimateSL.myWayPartBackDirection.startNode) {
		 * 
		 * for (int i=0; i <
		 * lastSL.myWayPartBackDirection.endNode.WayPartsToConnectedNotes
		 * .size(); i++) {
		 * 
		 * if (lastSL.myWayPartBackDirection.startNode !=
		 * lastSL.myWayPartBackDirection
		 * .endNode.WayPartsToConnectedNotes.get(i).endNode) {
		 * vwp.add(lastSL.myWayPartBackDirection
		 * .endNode.WayPartsToConnectedNotes.get(i)); }
		 * 
		 * }
		 * 
		 * return vwp;
		 * 
		 * } else { for (int i=0; i <
		 * lastSL.myWayPartBackDirection.startNode.WayPartsToConnectedNotes
		 * .size(); i++) {
		 * 
		 * if (lastSL.myWayPartBackDirection.startNode !=
		 * lastSL.myWayPartBackDirection
		 * .startNode.WayPartsToConnectedNotes.get(i).endNode) {
		 * vwp.add(lastSL.myWayPartBackDirection
		 * .startNode.WayPartsToConnectedNotes.get(i)); }
		 * 
		 * }
		 * 
		 * return vwp;
		 * 
		 * } } }
		 */

		return vwp;
	}

	public Vector<myOSMWayPart> getLastOSMWayPart() {
		return NRoute.getLastOSMWayPart(this);
	}

	public Vector<myOSMWayPart> getFirstOSMWayPart() {
		return NRoute.getFirstOSMWayPart(this);
	}

	public static Vector<myOSMWayPart> getLastOSMWayPart(NRoute nRoute) {

		Vector<myOSMWayPart> vwp = new Vector<myOSMWayPart>();

		vwp.add(nRoute.getLastMatchedLink().getStreetLink());

		return vwp;
	}

	public static Vector<myOSMWayPart> getFirstOSMWayPart(NRoute nRoute) {

		Vector<myOSMWayPart> vwp = new Vector<myOSMWayPart>();

		vwp.add(nRoute.getFirstMatchedLink().getStreetLink());

		return vwp;
	}

	/**
	 * set/notice previous n route
	 * 
	 * @param nRoute
	 */
	public void setPreviousNRoute(NRoute nRoute) {
		previousNRoute = nRoute;
	}

	/**
	 * get previous n route
	 * 
	 * @return
	 */
	public NRoute getPreviousNRoute() {
		return previousNRoute;
	}

	public int getNRouteLenght() {
		// get size of n route link vector
		int nRouteLenght = nRouteLinks.size();

		// if previous n route existing, add whose size by recursion
		if (previousNRoute != null)
			nRouteLenght += previousNRoute.getNRouteLenght();

		// return length
		return nRouteLenght;
	}

	public void print() {

		for (MatchedLink matchedLink : nRouteLinks) {

			System.out.println(matchedLink.getStreetLink().startNode.id + "-" + matchedLink.getStreetLink().endNode.id);

		}

	}

}
