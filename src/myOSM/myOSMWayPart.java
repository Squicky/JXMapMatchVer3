package myOSM;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;

import cartesian.Coordinates;
import algorithm.MatchedRange;

//import osm.StreetLink;

public class myOSMWayPart {

	public static final int NO_CONNECTION = 0;
	public static final int START_NODE = 1;
	public static final int END_NODE = 2;
	public static final int BOTH_NODE = 3;

	private static int runID = 0;

	public int ObjID = -1;
	public long xid = -1;
	public long xmyid = -1;

	public myOSMNode startNode;
	public myOSMNode endNode;
	public myOSMWay parentWay;
	public int parentWayStepNr = -1;
	public boolean isBackDirection = false;
	// public StreetLink streetLink = null;

	private Vector<MatchedRange> matchedRanges;

	private int selectedCounter;

	private boolean artificial = false; // is this link artificial (not part of
										// parsed street map)

	public final static long NO_ID = -1;

	public double length = -1;

	public myEdge edge = null;

	public double startEdgeLength = -1;

	public double endEdgeLength = -1;

	public double startWayLengthPos = -1;

	public double endWayLengthPos = -1;

	public myOSMWayPart WayPartBackDirektion = null;

	public int CountMatchedGPSNodes = 0;

	public myOSMWayPart(myOSMNode n1, myOSMNode n2, long myid,
			long startNodeId, long endNodeId) {
		this(n1, n2, NO_ID, false, myid, startNodeId, endNodeId);
	}

	public myOSMWayPart(myOSMNode n1, myOSMNode n2, myOSMWay way, int StepNr,
			boolean BackDirection) {

		startNode = n1;
		endNode = n2;
		parentWay = way;
		parentWayStepNr = StepNr;
		isBackDirection = BackDirection;

		ObjID = runID;
		runID++;

		selectedCounter = 0;

		startNode.WayPartsOutgoing_add(this);

		startNode.setXY();
		endNode.setXY();

		length = Coordinates.getDistance(startNode, endNode);
	}

	public myOSMWayPart(myOSMNode n1, myOSMNode n2, long id,
			boolean artificial, long myid, long startNodeId, long endNodeId) {

		this(n1, n2, null, -1, false);

		// set artificial flag
		this.artificial = artificial;

		// set id
		this.xid = id;
		this.xmyid = myid;

		// set selected counter
		selectedCounter = 0;
	}

	public myOSMWayPart(myOSMNode n1, myOSMNode n2, boolean artificial,
			long myid, long startNodeId, long endNodeId) {
		this(n1, n2, NO_ID, artificial, myid, startNodeId, endNodeId);
	}

	public void setStartEndEdgeLength() {
		if (this.edge != null) {
			this.startEdgeLength = this.edge.length
					* (this.startWayLengthPos / this.parentWay.length);
			this.endEdgeLength = this.edge.length
					* (this.endWayLengthPos / this.parentWay.length);
		}
	}

	public void setEdge() {

		Map<Integer, myEdge> edges = this.parentWay.map.edges
				.get(this.parentWay.id);

		this.edge = null;

		if (edges == null) {

			System.out.println("Error: setEdge(): no net-edges for "
					+ this.parentWay.id);

		} else {

			for (int i = 0; i < edges.size(); i++) {

				myEdge edge = edges.get(i);

				if (this.startNode.id == edge.startNode
						&& this.endNode.id == edge.endNode) {
					this.edge = edge;
				}

				/*
				 * 
				 * if (this.isBackDirection == false) {
				 * 
				 * if (edge.reverse_direction == false) {
				 * 
				 * int startIndexOfWP =
				 * this.parentWay.IndexOfNodeId.get(this.startNode.id); int
				 * endIndexOfWP =
				 * this.parentWay.IndexOfNodeId.get(this.endNode.id);
				 * 
				 * Integer startIndexOfE =
				 * this.parentWay.IndexOfNodeId.get(edge.startNode); Integer
				 * endIndexOfE = this.parentWay.IndexOfNodeId.get(edge.endNode);
				 * 
				 * if (startIndexOfE == null || endIndexOfE == null) {
				 * startIndexOfE = 0; startIndexOfE = null; }
				 * 
				 * if (startIndexOfE <= startIndexOfWP && endIndexOfWP <=
				 * endIndexOfE) {
				 * 
				 * if (this.edge == null) { this.edge = edges.get(i); } else {
				 * System.out.println("Error: setEdge(): edge doppelt");
				 * System.exit(-1); } } }
				 * 
				 * } else {
				 * 
				 * if (edges.get(i).reverse_direction == true ) {
				 * 
				 * int startIndexOfWP =
				 * this.parentWay.IndexOfNodeId.get(this.startNode.id); int
				 * endIndexOfWP =
				 * this.parentWay.IndexOfNodeId.get(this.endNode.id);
				 * 
				 * int startIndexOfE =
				 * this.parentWay.IndexOfNodeId.get(edges.get(i).startNode); int
				 * endIndexOfE =
				 * this.parentWay.IndexOfNodeId.get(edges.get(i).endNode);
				 * 
				 * if (startIndexOfE >= startIndexOfWP && endIndexOfWP >=
				 * endIndexOfE) {
				 * 
				 * if (this.edge == null) { this.edge = edges.get(i); } else {
				 * System.out.println("Error: setEdge(): edge doppelt");
				 * System.exit(-1); } } } }
				 */

			}

			if (this.edge == null) {
				System.out.println("Error: setEdge(): no net-edge for "
						+ this.parentWay.id + " : " + this.startNode.id
						+ " -> " + this.endNode.id);
				// System.exit(-1);
			}
		}

	}

	public myOSMNode getStartNode() {
		return startNode;
	}

	public myOSMNode getEndNode() {
		return endNode;
	}

	public double getStartX() {
		return this.startNode.x;
	}

	public double getStartY() {
		return this.startNode.y;
	}

	public double getEndX() {
		return this.endNode.x;
	}

	public double getEndY() {
		return this.endNode.y;
	}

	public int getID() {
		return this.ObjID;
	}

	public int isConnectedTo(myOSMWayPart streetLink) {
		// if link was given
		if (streetLink != null) {
			// is start node connected to other link
			boolean startNodeConnection = (startNode == streetLink
					.getStartNode() || startNode == streetLink.getEndNode());
			// is end node connected to other link
			boolean endNodeConnection = (endNode == streetLink.getStartNode() || endNode == streetLink
					.getEndNode());

			// how are links connected
			if (startNodeConnection && endNodeConnection) {
				// both nodes are connected
				return BOTH_NODE;
			} else if (startNodeConnection) {
				// start node is connecting node
				return START_NODE;
			} else if (endNodeConnection) {
				// end node is connecting node
				return END_NODE;
			}
		}

		// no connection between street links
		return NO_CONNECTION;
	}

	public int getStartNode(myOSMWayPart streetLink) {
		int connectingNode = isConnectedTo(streetLink);

		switch (connectingNode) {

		case START_NODE:
			// end node will be the starting node
			// when we drive along this street
			return END_NODE;

		case END_NODE:
			// start node will be...
			return START_NODE;

		default:
			return connectingNode;
		}
	}

	public void increaseSelectCounter() {
		++selectedCounter;
	}

	public void decreaseSelectCounter() {
		if (selectedCounter > 0)
			--selectedCounter;
	}

	public int getSelectCounter() {
		return selectedCounter;
	}

	public void resetSelectCounter() {
		selectedCounter = 0;
	}

	public boolean isArtificial() {
		return artificial;
	}

	// ///////////// Matched Ranges /////////////////////////////////////
	/**
	 * add range, which can be matched to this link
	 * 
	 * @param start
	 * @param end
	 * @param matched
	 */
	public void addMatchedRange(int start, int end, boolean matched) {
		matchedRanges.add(new MatchedRange(start, end, matched));
	}

	/**
	 * edit last added range
	 * 
	 * @param start
	 * @param end
	 * @return there was a last range
	 */
	public boolean setLastMatchedRange(int start, int end) {
		if (!matchedRanges.isEmpty()) {
			matchedRanges.lastElement().setRangeStartIndex(start);
			matchedRanges.lastElement().setRangeEndIndex(end);
			return true;
		}

		return false;
	}

	/**
	 * edit last added range
	 * 
	 * @param start
	 * @param end
	 * @param matched
	 * @return there was a last range
	 */
	public boolean setLastMatchedRange(int start, int end, boolean matched) {
		if (!matchedRanges.isEmpty()) {
			matchedRanges.lastElement().setRangeStartIndex(start);
			matchedRanges.lastElement().setRangeEndIndex(end);
			matchedRanges.lastElement().setMatched(matched);
			return true;
		}

		return false;
	}

	/**
	 * set start index of last added range
	 * 
	 * @param start
	 * @return there was a last range
	 */
	public boolean setLastMatchedRangeStart(int start) {
		if (!matchedRanges.isEmpty()) {
			matchedRanges.lastElement().setRangeStartIndex(start);
			return true;
		}

		return false;
	}

	/**
	 * set end index of last added range
	 * 
	 * @param end
	 * @return there was a last range
	 */
	public boolean setLastMatchedRangeEnd(int end) {
		if (!matchedRanges.isEmpty()) {
			matchedRanges.lastElement().setRangeEndIndex(end);
			return true;
		}

		return false;
	}

	/**
	 * set matched state of last added range
	 * 
	 * @param matched
	 * @return there was a last range
	 */
	public boolean setLastMatched(boolean matched) {
		if (!matchedRanges.isEmpty()) {
			matchedRanges.lastElement().setMatched(matched);
			return true;
		}

		return false;
	}

	/**
	 * get matched state of last added range
	 * 
	 * @return
	 */
	public boolean isLastMatched() {
		if (!matchedRanges.isEmpty()) {
			return matchedRanges.lastElement().getMatched();
		}
		return false;
	}

	/**
	 * get last added range
	 * 
	 * @return MatchedRange or null if there is no MatchedRange left
	 */
	public MatchedRange getLastMatchedRange() {
		try {
			return matchedRanges.lastElement();
		} catch (NoSuchElementException e) {
			System.out.println("No last matched Range!");
		}

		return null;
	}

	/**
	 * return start index of last added range
	 * 
	 * @return
	 */
	public int getLastMatchedRangeStart() {
		if (!matchedRanges.isEmpty())
			return matchedRanges.lastElement().getRangeStartIndex();
		else
			return -1;
	}

	/**
	 * return end index of last added range
	 * 
	 * @return
	 */
	public int getLastMatchedRangeEnd() {
		if (!matchedRanges.isEmpty())
			return matchedRanges.lastElement().getRangeEndIndex();
		else
			return -1;
	}

	/**
	 * return size of last added matched range
	 * 
	 * @return
	 */
	/*
	public int getLastMatchedRangeSize() {
		if (!matchedRanges.isEmpty())
			return matchedRanges.lastElement().getRangeSize();
		else
			return -1;
	}
	*/

	/**
	 * remove last added range if possible
	 * 
	 * @return there was a range which could be removed
	 */
	public boolean removeLastMatchedRange() {
		if (!matchedRanges.isEmpty()) {
			matchedRanges.remove(matchedRanges.size() - 1);
			return true;
		}

		return false;
	}

	/**
	 * remove all ranges from link
	 */
	public void resetMatchedRanges() {
		matchedRanges.clear();
	}

	public double getLength() {
		// link as vector with x & y components
		double vecX = getEndX() - getStartX();
		double vecY = getStartY() - getEndY();

		// calculate length
		return Math.sqrt((vecX * vecX) + (vecY * vecY));
	}

}
