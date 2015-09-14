package algorithm;

import interfaces.StatusUpdate;

import java.awt.Component;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import cartesian.Coordinates;
import gps.GPSNode;
import gps.GPSTrace;
import myClasses.myOSMMap;
import myClasses.myOSMWay;
import myClasses.myOSMWayPart;
import route.NRoute;

/**
 * @author Daniel Sathees Elmo
 * 
 *         this class manages a street map, a GPS trace, a selected route and
 *         matches GPS points to street links using the N Route Algorithm (
 */

public class NRouteAlgorithm {

	// save reference to n routes representing our path
	private TreeSet<NRoute> nRouteSet;

	// save reference to street map and GPS trace
	private myOSMMap myMap;
	private GPSTrace gpsTrace; // reference to GPS trace

	// reference to container size
	private int nRouteSize;

	// reference to draw component, force repaint while animation / after
	// changes
	private Component drawComponent;

	// time in ms thread should sleep after one GPS point is matched
	private static final int DEFAULT_THREAD_SLEEP_TIME = 20;

	// reference to status updating object
	private StatusUpdate statusUpdate;

	// save current algorithm state here
	private String nRouteAlgorithmState;

	// constants for N route algorithm state
	public static final String N_ROUTE_RUNNING = "RUNNING";
	public static final String N_ROUTE_PAUSED = "PAUSED";
	public static final String N_ROUTE_RECESSED = "RECESSED";

	// constants to control N route size
	public static final int MIN_N_ROUTE_SIZE = 1;
	public static final int DEFAULT_N_ROUTE_SIZE = 10;
	public static final int MAX_N_ROUTE_SIZE = 1000;

	// constants for minimum, maximum, default intersection reached value
	public static final double DEFAULT_INTERSECTION_REACHED_THRESHOLD = 0.9;
	public static final double MIN_INTERSECTION_REACHED_THRESHOLD = 0.1;
	public static final double MAX_INTERSECTION_REACHED_THRESHOLD = 1.0;

	// keeps current intersection reached limit
	private double intersectionReachedTreshold = DEFAULT_INTERSECTION_REACHED_THRESHOLD;

	// keeps track of current n route index (for debugging)
	// private int nRouteIndex = 0;

	/**
	 * constructor needs a street man, a GPS trace and a draw component which do
	 * the painting
	 * 
	 * @param streetMap
	 * @param gpsTrace
	 * @param drawComponent
	 */
	public NRouteAlgorithm(myOSMMap myMap, GPSTrace gpsTrace, StatusUpdate statusUpdate, Component drawComponent) {
		// save references
		// this.streetMap = streetMap;
		this.myMap = myMap;
		// this.streetLinks = streetMap.getStreetLinksVector();
		this.gpsTrace = gpsTrace;
		this.statusUpdate = statusUpdate;
		this.drawComponent = drawComponent;

		// initialize vector which includes all path representing our matched
		// route
		nRouteSet = new TreeSet<NRoute>();

		// set algorithm state
		nRouteAlgorithmState = N_ROUTE_RECESSED;
	}

	/**
	 * executes the n route algorithm, needs object that implements status
	 * update interface in order to update current algorithm status
	 * 
	 * @param statusUpdate
	 * @return algorithm could be executed to the end (true) or was aborted
	 *         (false)
	 */
	public boolean executeNRouteAlgorithm(int nRouteSize, double threshold) throws Exception {
		// set N route algorithm state
		// save N route size and intersection reached threshold
		setNRouteAlgorithmState(N_ROUTE_RUNNING);
		setNRouteSize(nRouteSize);
		setIntersectionReachedThreshold(threshold);

		// start algorithm
		statusUpdate.updateStatus("Starting N route algorithm, N = " + nRouteSize + "...");

		// create new sorted set
		TreeSet<NRoute> sortedSetS = new TreeSet<NRoute>();

		for (int currentGPSNodeIndex = 0; currentGPSNodeIndex < gpsTrace.getNrOfNodes(); currentGPSNodeIndex++) {
			gpsTrace.setNodeStatus(currentGPSNodeIndex, 0);
		}

		// 1. get GPS Point
		// initNewGPSPoint:
		for (int gpsNodeIndex = 0; gpsNodeIndex < 1 /* gpsTrace.getNrOfNodes() */; gpsNodeIndex++) {
			// 2. initialize sorted set with N nearest path/links
			sortedSetS = getSetOfNPathOfNNearestLinks(gpsNodeIndex);

			gpsTrace.setNodeStatus(gpsNodeIndex, 1);

			// 3. get next GPS point
			for (int currentGPSNodeIndex = gpsNodeIndex + 1; currentGPSNodeIndex < gpsTrace.getNrOfNodes(); currentGPSNodeIndex++) {

				gpsTrace.setNodeStatus(currentGPSNodeIndex, 1);

				// sleep thread and refresh GUI for animation or pause if
				// algorithm was disrupt or
				// shut down algorithm
				if (!sleepThread(DEFAULT_THREAD_SLEEP_TIME)) {
					// reset N route algorithm state
					setNRouteAlgorithmState(N_ROUTE_RECESSED);
					return false;
				}

				// create temporary sorted set v
				TreeSet<NRoute> sortedSetV = new TreeSet<NRoute>();

				// nRouteIndex = 0;

				// match GPS Point on last link of every path
				for (NRoute nRoute : sortedSetS) {

					//long nRouteObjID = nRoute.objID;
					//long nRouteCountMatched = nRoute.getNRouteLinks().lastElement().getRangeSize();
					//double nRouteScore = nRoute.getScore();
					
					// match current GPS node to link
					// Logger.println("\nAdding GPSNode(" + currentGPSNodeIndex
					// + ") to nRoute Nr." + nRouteIndex);
					nRoute.addGPSNodeToLastLink(currentGPSNodeIndex);

					//long nRouteOsmID = nRoute.getNRouteLinks().lastElement().getStreetLink().parentWay.id;
					//nRouteScore = nRoute.getScore();
					//nRouteCountMatched = nRoute.getNRouteLinks().lastElement().getRangeSize();
					
					sortedSetV.add(nRoute);

					createChildPathAndAddToSet(nRoute, sortedSetV);

					// increase n route index
					// nRouteIndex++;
				}

				
				// extract best n path and set as current sorted set
				sortedSetS = getBestNPathFromSortedSet(sortedSetV);

				// for painting
				nRouteSet = sortedSetS;

				drawComponent.repaint();

				// printScore(sortedSetS);
			}

		}

		NRoute bestNRoute = null;

		for (NRoute nRoute : sortedSetS) {
			if (bestNRoute == null) {
				bestNRoute = nRoute;
			} else {
				if (nRoute.getScore() == bestNRoute.getScore()) {
					if (nRoute.getLength() < bestNRoute.getLength()) {
						bestNRoute = nRoute;
					}
				}
			}
		}

		sortedSetS.clear();

		sortedSetS.add(bestNRoute);

		// set as new route set
		nRouteSet = sortedSetS;

		// algorithm finished
		statusUpdate.finished("N route algorithm executed");

		// force repaint
		drawComponent.repaint();

		// reset N route algorithm state
		setNRouteAlgorithmState(N_ROUTE_RECESSED);

		// algorithm successfully executed until end
		return true;
	}

	/**
	 * creates an set of N nearest nRoutes to given GPS point, stores them into
	 * a tree set which is sorted to ascending distance to GPS point
	 * 
	 * @param GPSNodeIndex
	 * @return TreeSet<NRoute>
	 */
	private TreeSet<NRoute> getSetOfNPathOfNNearestLinks(int GPSNodeIndex) {

		// store n nearest links here by creating path for each link
		TreeSet<NRoute> nRouteSet = new TreeSet<NRoute>();

		// current minimum found distance
		// double currentMinDistance = 0;

		// nRouteSize = nRouteSize *2;

		double arMinDis[] = new double[nRouteSize];
		myOSMWayPart arMinWp[] = new myOSMWayPart[nRouteSize];

		for (int i = 0; i < nRouteSize; i++) {
			arMinDis[i] = Double.MAX_VALUE;
		}

		int count = 0;
		// search nearest link
		myOSMWay w;
		myOSMWayPart wp;
		for (int wi = 0; wi < myMap.ways.size(); wi++) {

			w = myMap.ways.get(wi);

			for (int wpi = 0; wpi < w.WayParts.length; wpi++) {

				wp = w.WayParts[wpi];

				double distance = Coordinates.getDistance(gpsTrace.getNode(GPSNodeIndex), wp);

				if (distance < arMinDis[nRouteSize - 1]) {
					count++;

					arMinDis[nRouteSize - 1] = distance;
					arMinWp[nRouteSize - 1] = wp;

					for (int i = nRouteSize - 1; i >= 1; i--) {

						if (arMinDis[i] < arMinDis[i - 1]) {
							double d = arMinDis[i];
							arMinDis[i] = arMinDis[i - 1];
							arMinDis[i - 1] = d;

							myOSMWayPart wpx = arMinWp[i];
							arMinWp[i] = arMinWp[i - 1];
							arMinWp[i - 1] = wpx;
						}

					}
				}
			}
		}

		for (int i = 0; i < count && i < nRouteSize; i++) {
			NRoute nRoute = new NRoute(gpsTrace);
			nRoute.addLink(arMinWp[i], GPSNodeIndex);

			nRouteSet.add(nRoute);
		}

		/*
		 * // get n nearest links for (int i = 1; i <= nRouteSize; i++) {
		 * 
		 * Logger.println("Starting run nr." + i + " of " + nRouteSize);
		 * 
		 * // save minimum distance and street link during search progress
		 * double minDistance = Double.MAX_VALUE; // StreetLink
		 * nearestStreetLink = null; myOSMWayPart nearestWayPart = null;
		 * 
		 * 
		 * // search nearest link myOSMWay w; myOSMWayPart wp; for (int wi=0; wi
		 * < myMap.ways.size(); wi++) {
		 * 
		 * w = myMap.ways.get(wi);
		 * 
		 * for (int wpi=0; wpi < w.WayParts.length; wpi++) {
		 * 
		 * wp = w.WayParts[wpi];
		 * 
		 * // get distance to street link double distance =
		 * Coordinates.getDistance(gpsTrace.getNode(GPSNodeIndex), wp);
		 * 
		 * // check if link is in the run if ((distance < minDistance) &&
		 * (distance > currentMinDistance)) { // save distance and street link
		 * minDistance = distance; nearestWayPart = wp; } } }
		 */

		/*
		 * for (StreetLink streetLink : streetLinks) {
		 * 
		 * // get distance to street link double distance =
		 * Coordinates.getDistance(gpsTrace.getNode(GPSNodeIndex), streetLink);
		 * 
		 * // check if link is in the run if ((distance < minDistance) &&
		 * (distance > currentMinDistance)) { // save distance and street link
		 * minDistance = distance; nearestStreetLink = streetLink; } }
		 */

		/*
		 * // create new path if nearest link was found if (nearestWayPart !=
		 * null) {
		 * 
		 * Logger.println("Found nearest link Nr." + i);
		 * 
		 * // save new current minimum distance currentMinDistance =
		 * minDistance;
		 * 
		 * // create new path through adding nearest street link to new n route
		 * container NRoute nRoute = new NRoute(gpsTrace);
		 * nRoute.addLink(nearestWayPart, GPSNodeIndex);
		 * 
		 * // add to tree set, gets automatically ordered by score
		 * nRouteSet.add(nRoute); } else { Logger.println("No nearest link nr. "
		 * + i + " found!"); } }
		 */

		// return tree set
		return nRouteSet;
	}

	/**
	 * creates child path using outgoing links of last link inside given n route
	 * dematches last GPS node from last n route link and matches it to new
	 * outgoing links of child path
	 * 
	 * @param nRoute
	 * @param sortedSet
	 * @return
	 */
	private boolean createChildPathAndAddToSet(NRoute nRoute, TreeSet<NRoute> sortedSet) {
		// try to get outgoing links for last link in n route
		Vector<myOSMWayPart> outgoingLinks = nRoute.getOutgoingLinksForLastLink();

		// build child path if we could get outgoing link(s)
		if (outgoingLinks != null) {

			// Logger.println("Found outgoing links for NRoute Nr." +
			// nRouteIndex);

			// create child path for every outgoing link
			for (myOSMWayPart outgoingLink : outgoingLinks) {

				// copy given n route
				NRoute nRouteChild = nRoute.clone();

				// notice last matched GPS node and dematch
				int lastMatchedGPSNodeIndex = nRouteChild.getLastMatchedLink().getRangeEndIndex();
				nRouteChild.removeLastGPSNodeFromLastLink();

				//long nRouteObjID = nRouteChild.objID;
				//long nRouteCountMatched = nRoute.getNRouteLinks().lastElement().getRangeSize();
				//double nRouteScore = nRouteChild.getScore();
				
				// add current outgoing link to child path
				// match last GPS node on it
				nRouteChild.addLink(outgoingLink, lastMatchedGPSNodeIndex);

				//long nRouteosmID = nRouteChild.getNRouteLinks().lastElement().getStreetLink().parentWay.id;
				//nRouteScore = nRouteChild.getScore();
				//nRouteCountMatched = nRouteChild.getNRouteLinks().lastElement().getRangeSize();
				
				// add child path to hand over sorted set
				sortedSet.add(nRouteChild);

				// nRoute.getLastOSMWayPart();

				GPSNode gpsNode = gpsTrace.getNode(lastMatchedGPSNodeIndex);

				double distance_nRoute = Coordinates.getDistance(gpsNode, nRoute.getLastMatchedLink().getStreetLink());
				double distance_nRouteChild = Coordinates.getDistance(gpsNode, nRouteChild.getLastMatchedLink()
						.getStreetLink());

				if (distance_nRouteChild < distance_nRoute) {
					createChildPathAndAddToSet(nRouteChild, sortedSet);
				}
			}

			// creating and matching child path successful
			return true;
		}

		// no outgoing links for last link, respectively no
		// outgoing node/links could be determined
		return false;
	}

	/**
	 * get sorted set which only includes the n best scored path from given
	 * sorted set
	 * 
	 * @param sortedSet
	 * @return TreeSet<NRoute>
	 */
	public TreeSet<NRoute> getBestNPathFromSortedSet(TreeSet<NRoute> sortedSet) {

		// NRoutes Start und Ende gleich, dann nur mit bestem Score

		{
			Vector<NRoute> filteredNRoutes = new Vector<NRoute>(sortedSet.size());

			Iterator<NRoute> it = sortedSet.iterator();
			while (it.hasNext()) {

				NRoute nRoute = it.next();

				boolean toAdd = true;

				for (int i = filteredNRoutes.size() - 1; i >= 0; i--) {

					NRoute nRouteNew = filteredNRoutes.get(i);

					if (nRoute != nRouteNew) {


						myOSMWayPart wp1 = nRoute.getLastOSMWayPart().get(0);
						myOSMWayPart wp2 = nRouteNew.getLastOSMWayPart().get(0);

						if (wp1 == wp2) {

							if (nRoute.getScore() < nRouteNew.getScore()) {

								filteredNRoutes.remove(i);
								filteredNRoutes.add(nRoute);
								i = filteredNRoutes.size() - 1;
								toAdd = false;

							} else if (nRoute.getScore() == nRouteNew.getScore()) {

								if (nRoute.getLength() < nRouteNew.getLength()) {

									filteredNRoutes.remove(i);
									filteredNRoutes.add(nRoute);
									i = filteredNRoutes.size() - 1;
									toAdd = false;

								} else {

									toAdd = false;

								}

							} else {

								toAdd = false;

							}

						}

						// }

					}

				}

				if (toAdd) {

					filteredNRoutes.add(nRoute);

				}

			}

			sortedSet.clear();
			for (int i = 0; i < filteredNRoutes.size(); i++) {
				sortedSet.add(filteredNRoutes.get(i));
			}

		}

		// store extracted routes here
		TreeSet<NRoute> nBestSortedSet = new TreeSet<NRoute>();

		/*
		Iterator<NRoute> itt = sortedSet.iterator();
		int i = 0;
		while (itt.hasNext()) {
			NRoute nRoute = itt.next();
			String s = "" + i + "/" + sortedSet.size() + "| " + nRoute.getNRouteLinks().lastElement().getStreetLink().parentWay.id;
			s = s + " |" + nRoute.objID;
			s = s + "| " + nRoute.getScore() ; 
			s = s + " | " + nRoute.history;
			i++;
			System.out.println(s);
		}
		*/
		
		// initialize iterator
		Iterator<NRoute> it = sortedSet.iterator();

		while (it.hasNext()) {
			
			NRoute nRoute = it.next();
			
			if (nBestSortedSet.size() <= nRouteSize) {
				nBestSortedSet.add(nRoute);
			} else {
				if (nBestSortedSet.last().getScore() == nRoute.getScore()) {
					nBestSortedSet.add(nRoute);
				} else {
					return nBestSortedSet;					
				}
			}
		}
		
		
		//int CountAdded = 0;
		/*
		NRoute nRouteAlt = null;
		NRoute nRouteNeu = null;
		boolean istGleichGut = false;
		boolean alteNRouteLinksgleichundScoreBesser = false;
		while (it.hasNext()) {
			istGleichGut = false;
			alteNRouteLinksgleichundScoreBesser = false;

			nRouteNeu = it.next();

			Iterator<NRoute> it2 = nBestSortedSet.iterator();

			while (it2.hasNext()) {
				nRouteAlt = it2.next();

				if (nRouteNeu.istGleich(nRouteAlt)) {
					istGleichGut = true;
				}

				if (nRouteNeu.istNRouteLinksGleichUndScoreBesser(nRouteAlt)) {
					nBestSortedSet.remove(nRouteAlt);
					it2 = nBestSortedSet.iterator();
					istGleichGut = false;
					alteNRouteLinksgleichundScoreBesser = false;
				} else {
					if (nRouteAlt.istNRouteLinksGleichUndScoreBesser(nRouteNeu)) {
						alteNRouteLinksgleichundScoreBesser = true;
					}
				}
			}

			if (nBestSortedSet.size() == 0) {
				nBestSortedSet.add(nRouteNeu);
			} else if (istGleichGut == false && alteNRouteLinksgleichundScoreBesser == false) {
				if (nBestSortedSet.size() <= nRouteSize) {
					nBestSortedSet.add(nRouteNeu);
					if (nRouteNeu.getNRouteLinks().lastElement().getRangeSize() > 0) {
//						CountAdded++;
					}
				} else {
					if (nBestSortedSet.last().getScore() == nRouteNeu.getScore()) {
						nBestSortedSet.add(nRouteNeu);
						if (nRouteNeu.getNRouteLinks().lastElement().getRangeSize() > 0) {
//							CountAdded++;
						}
					}
					return nBestSortedSet;
				}
			}

		}
		*/
		

		/*
		itt = nBestSortedSet.iterator();
		i = 0;
		while (itt.hasNext()) {
			NRoute nRoute = itt.next();
			String s = "" + i + "/" + sortedSet.size() + "| " + nRoute.getNRouteLinks().lastElement().getStreetLink().parentWay.id;
			s = s + " |" + nRoute.objID;
			s = s + "| " + nRoute.getScore() ; 
			s = s + " | " + nRoute.history;
			i++;
			//System.out.println(s);
		}
		*/
		
		// return set including best n path
		return nBestSortedSet;
	}

	/**
	 * set N route size for N route algorithm
	 * 
	 * @param nRouteSize
	 */
	private void setNRouteSize(int nRouteSize) {
		// save n route size, must be greater equal MIN_N_ROUTE_SIZE
		this.nRouteSize = (nRouteSize >= MIN_N_ROUTE_SIZE) ? nRouteSize : MIN_N_ROUTE_SIZE;
	}

	/**
	 * get current set N route size
	 * 
	 * @return int
	 */
	public int getNRouteSize() {
		return nRouteSize;
	}

	/**
	 * manually set another threshold when a intersection is passed
	 * 
	 * @param threshold
	 */
	public void setIntersectionReachedThreshold(double threshold) {
		// check for not allowed values
		if (threshold < MIN_INTERSECTION_REACHED_THRESHOLD)
			threshold = MIN_INTERSECTION_REACHED_THRESHOLD;
		if (threshold > MAX_INTERSECTION_REACHED_THRESHOLD)
			threshold = MAX_INTERSECTION_REACHED_THRESHOLD;

		// set new threshold
		intersectionReachedTreshold = threshold;
	}

	/**
	 * get current set intersection reached threshold
	 * 
	 * @return double
	 */
	public double getIntersectionReachedThreshold() {
		return intersectionReachedTreshold;
	}

	/**
	 * this method checks if intersection was reached according to threshold
	 * defined by intersection reached limit property
	 * 
	 * @param streetLink
	 * @return
	 */
	/*
	 * private boolean hasReachedIntersection(NRoute nRoute, MatchedLink
	 * matchedLink) {
	 * 
	 * int px = gpsTrace.getNode(matchedLink.getRangeEndIndex()).getX(); int py
	 * = gpsTrace.getNode(matchedLink.getRangeEndIndex()).getY();
	 * 
	 * myOSMWayPart wp;
	 * 
	 * Vector<myOSMWayPart> vwp = nRoute.getLastOSMWayPart();
	 * 
	 * int j = 0; for (j=0; j < vwp.size(); j++) { wp = vwp.get(j);
	 * 
	 * int ax = wp.startNode.x; int ay = wp.startNode.y; int bx = wp.endNode.x;
	 * int by = wp.endNode.y;
	 * 
	 * double prec = Coordinates.getPercentOfPointInWayPart(px, py, ax, ay, bx,
	 * by);
	 * 
	 * double right = intersectionReachedTreshold *100.0;
	 * 
	 * if ( right < prec) { return true; } }
	 * 
	 * if (0 <= j) { return false; }
	 * 
	 * // get street length double streetLength =
	 * Coordinates.getStreetLength(matchedLink.getStreetLink());
	 * 
	 * // store length between matched points here double gpsPointsDistances =
	 * 0;
	 * 
	 * // calculate sum of distance between matched GPS points, if there are
	 * more than one point if (matchedLink.getRangeSize() > 1) { for (int
	 * i=matchedLink.getRangeStartIndex(); i < matchedLink.getRangeEndIndex();
	 * i++) { gpsPointsDistances += Coordinates.getDistance(gpsTrace.getNode(i),
	 * gpsTrace.getNode(i+1)); } }
	 * 
	 * // check if matched GPS points has reached intersection if
	 * (gpsPointsDistances > (intersectionReachedTreshold * streetLength)) {
	 * return true; } else { return false; } }
	 */

	/**
	 * get vector which includes n routes which belong to desired path index (if
	 * exists)
	 * 
	 * @param selectedNRouteIndex
	 * @return Vector<NRoute>
	 */
	public Vector<NRoute> getNRoute(/* int selectedNRouteIndex */) {
		// store extracted routes here
		Vector<NRoute> selectedNRoute = new Vector<NRoute>(nRouteSet.size());

		// initialize iterator
		Iterator<NRoute> it = nRouteSet.iterator();

		// temporary variables
		// int i = 0;
		NRoute nRoute = null;

		while (it.hasNext()) {

			nRoute = it.next();
	
			// if (i == selectedNRouteIndex || true) {
			selectedNRoute.add(nRoute);
			// return selectedNRoute;
			// }

			// i++;
		}

		// if (selectedNRouteIndex != -1) {
		return selectedNRoute;
		// }

		/*
		 * // try to get desired n route while(it.hasNext() && i <=
		 * selectedNRouteIndex) { nRoute = it.next(); i++; }
		 * 
		 * // if we got a n route, check if we got previous routes if (nRoute !=
		 * null) {
		 * 
		 * // store here previous n routes, first previous n route is // current
		 * n route NRoute previousNRoute = nRoute;
		 * 
		 * do { // add previous n route to vector
		 * selectedNRoute.add(previousNRoute); // try to get next previous n
		 * route previousNRoute = previousNRoute.getPreviousNRoute();
		 * 
		 * // break loop if there aren't any previous n route } while
		 * (previousNRoute != null);
		 * 
		 * // reverse order of n routes, course we starting adding // with last
		 * n route of a path Collections.reverse(selectedNRoute); }
		 * 
		 * // return n routes for desired index return selectedNRoute;
		 */
	}

	/**
	 * set new status for N route algorithm
	 * 
	 * @param status
	 */
	public void setNRouteAlgorithmState(String status) {
		nRouteAlgorithmState = status;
	}

	/**
	 * get current status of N route algorithm
	 * 
	 * @return
	 */
	public String getNRouteAlgorithmState() {
		return nRouteAlgorithmState;
	}

	/*
	 * private void printScore(TreeSet<NRoute> sortedSet) { Logger.print("\n");
	 * 
	 * int i=0;
	 * 
	 * for(NRoute nRoute : sortedSet) { Logger.println("Score for route Nr." + i
	 * + ": " + nRoute.getScore() + " route length: " +
	 * nRoute.getNRouteLenght()); i++; }
	 * 
	 * }
	 */

	private boolean sleepThread(long milliseconds) {
		// Thread sleep for animation and refresh painting
		do {
			try {
				Thread.sleep(milliseconds);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			drawComponent.repaint();
		} while (getNRouteAlgorithmState() == N_ROUTE_PAUSED);

		// check if algorithm should be continued or shut down
		return (getNRouteAlgorithmState() == N_ROUTE_RUNNING) ? true : false;
	}

}
