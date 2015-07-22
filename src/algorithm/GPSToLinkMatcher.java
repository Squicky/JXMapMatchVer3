package algorithm;

import interfaces.MatchingGPSObject;

import java.awt.Color;
import java.awt.Component;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import myOSM.myOSMMap;
import myOSM.myOSMWayPart;
import cartesian.Coordinates;
import route.SelectedRoute;
import gps.GPSNode;
import gps.GPSTrace;

/**
 * @author Daniel Sathees Elmo
 * 
 * this class manages a street map, a GPS trace, a selected route and
 * matches GPS points to selected links
 */

public class GPSToLinkMatcher implements MatchingGPSObject {
	
//	private StreetMap streetMap;						// reference to street map
	private myOSMMap myMap;						// reference to street map
	
	private long refTimeStamp;							// timestamp where measurement started
	private Vector<MatchedGPSNode> gpsNodesToMatch;		// reference to wrapped GPS nodes
	private SelectedRoute selectedRoute;				// reference to user build selected route
	
	private Component drawComponent;					// reference to draw component, force repaint while animation / after changes

	private Color colorGradient[];						// store different colors which create an color gradient
	private static final int COLOR_GRADIENT_STEPS = 20;	// color/animation steps
	private static final long SLEEP = 15;				// pause between next animation step
	
	private int lastMatchedIndex;						// index of last matched GPS point
	private int currentIndexToMatch;					// index where next matching algorithm starts
	
	private static final int MAX_INDEX_DIFF = 100;		// maximum index difference between last and next matched GPS point  
	
	private boolean busy;								// state if algorithm is still working
	
	/**
	 * constructor needs a street man, a GPS trace and a draw component which does the painting
	 * @param streetMap
	 * @param gpsTrace
	 * @param drawComponent
	 */
	public GPSToLinkMatcher(myOSMMap myMap, GPSTrace gpsTrace, Component drawComponent) {
		// save references, create new selected route
//		this.streetMap = streetMap;
		this.myMap = myMap;
		this.refTimeStamp = gpsTrace.getRefTimeStamp();
		this.gpsNodesToMatch = wrapGPSTrace(gpsTrace);
		this.drawComponent = drawComponent;
//		this.selectedRoute = new SelectedRoute(this.streetMap);
		this.selectedRoute = new SelectedRoute(this.myMap);
			
		// create color gradient between two color with a number of steps
		colorGradient = getColorGradient(Color.BLUE, Color.CYAN, COLOR_GRADIENT_STEPS);
		
		// start matching at GPS node with index 0;
		lastMatchedIndex = -1;
		currentIndexToMatch = 0;
		
		// notice working state
		busy = false;
	}
	
	private Vector<MatchedGPSNode> wrapGPSTrace(GPSTrace gpsTrace) {
		
		Vector<MatchedGPSNode> matchedGPSNodes = new Vector<>();
		
		for(int i=0; i < gpsTrace.getNrOfNodes(); i++) {
			GPSNode gpsNode = gpsTrace.getNode(i);
			// create wrapped class
			MatchedGPSNode matchedGPSNode = new MatchedGPSNode(gpsNode, Color.BLUE);
			// store
			matchedGPSNodes.add(matchedGPSNode);
		}
		
		return matchedGPSNodes;
	}
	
	public long getRefTimeStamp() {
		return this.refTimeStamp;
	}

	/**
	 * delegate mouse position to selected route instance
	 * @param x
	 * @param y
	 */
	public void setSelectableLink(double x, double y) {
		selectedRoute.setSelectableLink(x, y);
	}
	
	/**
	 * switch all-links-selectable mode on/off
	 */
	public void switchAllLinksSelectableMode(){
		selectedRoute.switchAllLinksSelectableMode();
		drawComponent.repaint();
	}
	
	/**
	 * 
	 */
	public void addLinkWithoutMatching() {
		if (!busy && selectedRoute.addLink()) {
			// now we're busy
			busy = true;
			
			// set last added link as unmatched!
			selectedRoute.getLastSelectedLink().addMatchedRange(-1, -1, false);
			
			// redraw
			drawComponent.repaint();
			
			// we're not busy anymore
			busy = false;
		}
	}
	
	/**
	 * add new link by delegating mouse position to selected route, and do matching
	 * @param x
	 * @param y
	 */
	public void addLink(double x, double y) {
		 // delegate to select link
		 if (!busy && selectedRoute.addLink(x, y)) {
			 // now we're busy
			 busy = true;
			 
			// try to match next GPS points to last added link and do some animation in a separate thread
			SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
				@Override
				protected Boolean doInBackground() throws Exception {
					// match GPS Points to last added link
					matchGPSToLastLinkWorker();
					
					// adding process successful
					return true;
				}
				
				@Override
				protected void done() {
					try { if (get()) busy = false; }	// reset busy state
					catch (InterruptedException | ExecutionException e) {;}
				}
			};		
			
			//do process in background
			worker.execute();			
		}
	}
	
	public void adjustLink(){
		 // delegate to select link
		 if (!busy) {
			 // now we're busy
			 busy = true;
			 
			// try to adjust last added link
			SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
				@Override
				protected Boolean doInBackground() throws Exception {
					//adjust last selected link
					adjustLastAddedLink();
					
					//loading process successful
					return true;
				}
				
				@Override
				protected void done() {
					try { if (get()) busy = false; }	// reset busy state
					catch (InterruptedException | ExecutionException e) {;}
				}
			};		
			
			//do process in background
			worker.execute();			
		}
	}
	
	/**
	 * delegate mouse position to selected route, and dematch last added link
	 * @param x
	 * @param y
	 */
	public void removeLink(final double x, final double y) {
		 if (!busy){
			 // now we're busy
			 busy = true;

			 // dematching algorithm/animation run in a separate thread
			 SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>(){
				 @Override
				 protected Boolean doInBackground() throws Exception {
					 // dematch last GPS Points
					 dematchGPSToLink();
					
					 // delegate to remove link
					 selectedRoute.removeLink(x, y);
					
					 // process successful
					 return true;
				 }
				
				 @Override
				 protected void done(){
					 try { if (get()) busy = false; }	// reset busy state
					 catch (InterruptedException | ExecutionException e) {;}
				 }
			 };		
			
			 //do process in background
			 worker.execute();
		 }
	}
	
	/**
	 * return reference to selected route class
	 * @return SelectedRoute
	 */
	public SelectedRoute getSelectedRoute() {
		return selectedRoute;
	}
	
	/**
	 * return all selected links
	 * @return Vector<StreetLink>
	 */
	public Vector<myOSMWayPart> getSelectedRouteLinks(){
		return selectedRoute.getSelectedLinks();
	}
	
	/**
	 * this method tries to match next GPS points to last added links 
	 * @return if some GPS points could be matched
	 */
	private boolean matchGPSToLastLinkWorker() {
		// try to get last and last but one added link 
		myOSMWayPart lastAddedLink = selectedRoute.getLastSelectedLink();
		myOSMWayPart lastButOneAddedLink = selectedRoute.getLastButOneSelectedLink();
		
		int euclidianStartIndex = 0;							// first index where GPS point can matched vertical to a link
		
		int currentMatchedStartIndex = 0;						// start match index of current link (last added link)
		int currentMatchedEndIndex = gpsNodesToMatch.size() -1;	// end match index of current link (last added link)
		
		int lastMatchedStartIndex = 0;							// start match index of last matched link (last but one added link)
		int lastMatchedEndIndex = 0;							// end match index of last matched link (last but one added link)
		
		MatchedPoint matchedPoint;								// contains info about an euclidian matched GPS point
		
		
		// save previous GPS point coordinates, count points which are congruent to previous points
		double previousX = -1;
		double previousY = -1;
		int identicalPointsCounter = 0;
		
		// 1.)
		// get start index to match last added link
		for (int i=currentIndexToMatch; i<gpsNodesToMatch.size(); i++) {
			// ermittelte den gematchten Punkt auf dem Link zum aktuellen GPS Punkt
			matchedPoint = Coordinates.getNearestEuclidianPoint(gpsNodesToMatch.get(i), lastAddedLink);
			
			// count congruent points
			if ((gpsNodesToMatch.get(i).getX() == previousX) && (gpsNodesToMatch.get(i).getY() == previousY)) {
				identicalPointsCounter++;
			}
			// set new previous x/y coordinates
			previousX = gpsNodesToMatch.get(i).getX();
			previousY = gpsNodesToMatch.get(i).getY();
			
			// falls Abstand zu groß zum nächsten matchbaren GPS Punkt oder
			if ( (matchedPoint.isEuclidian() && ((i - lastMatchedIndex - identicalPointsCounter) > MAX_INDEX_DIFF)) ||
				 ((i == gpsNodesToMatch.size()-1) && !matchedPoint.isEuclidian()) ) {
				System.err.println("\nZu weit entfernt! EuclidianStartIndex/CurrentMatchedStartIndex: [" + i + ",");	// link couldn't be matched
				lastAddedLink.addMatchedRange(-1, -1, false);
				return false;	// couldn't find point to start match algorithm
			}
			
			// merke wenn passender euclidischer Punkt gefunden wurde, der keine Regeln verletzt
			if (matchedPoint.isEuclidian()) {
					euclidianStartIndex = i; // set euclidian index
					break;
			}
		}		
		
		// match start index for last added link
		currentMatchedStartIndex = euclidianStartIndex;
		
		System.out.println("\nEuclidianStartIndex/CurrentMatchedStartIndex/: [" + currentMatchedStartIndex + ",");
			
		// 2.)
		// falls es einen vorletzten link gibt prüfe seine gematchten punkte ob sie besser zum neuen link passen
		if ((lastButOneAddedLink != null) && lastButOneAddedLink.isLastMatched()) {
			// get matched start and end index of last but one added link
			lastMatchedStartIndex = lastButOneAddedLink.getLastMatchedRangeStart();
			lastMatchedEndIndex = lastButOneAddedLink.getLastMatchedRangeEnd();
			
			System.out.println("Vorletzer Link vorhanden! gematched: " + lastButOneAddedLink.isLastMatched() + "\n\tAlter Range: [" + lastMatchedStartIndex + ", " + lastMatchedEndIndex + "]");
			
			int newLastMatchedEndIndex = 0;
			
			// ermittele bis wohin die zwischen gps nodes zum vorletzen link besser passen
			for (int i=euclidianStartIndex; i>=lastMatchedStartIndex; i--) {
				if (Coordinates.getDistance(gpsNodesToMatch.get(i), lastAddedLink) > Coordinates.getDistance(gpsNodesToMatch.get(i), lastButOneAddedLink)){
					newLastMatchedEndIndex = i;
					currentMatchedStartIndex = i+1;
					break;
				}
			}
			
			System.out.println("\tNeuer Range: [" + lastMatchedStartIndex + ", " + newLastMatchedEndIndex + "]");
			
			// berechne überschneidungen
			int rangeDiff = lastMatchedEndIndex - currentMatchedStartIndex;
			
			// Bei rangeDiff >= 0 release alte gps nodes
			if (rangeDiff >= 0){
				System.out.println("\tRelease GPS Punkte vom altem Link: [" + currentMatchedStartIndex + ", " + lastMatchedEndIndex + "]");
				//release gps nodes die noch zum alten link gehören ermitteln
				releaseGPSNodes(currentMatchedStartIndex, lastMatchedEndIndex, lastButOneAddedLink);
			}
			// bei rangeDiff < -1 gibts gps punkte zwischen den link die zum alten link gematched werden
			else if (rangeDiff < -1){
				System.out.println("\tFüge GPS Punkte zum alten Link: [" + (lastMatchedEndIndex+1) + ", " + (currentMatchedStartIndex-1) + "]");
				addGPSToLink((lastMatchedEndIndex+1), (currentMatchedStartIndex-1), lastButOneAddedLink);
			}
			else
				System.out.println("\tKeine Änderungen am alten Link!");
		}
		
		//3.)
		//match punkte für neuen link
		for (int i=(euclidianStartIndex+1); i<gpsNodesToMatch.size(); i++){
			matchedPoint = Coordinates.getNearestEuclidianPoint(gpsNodesToMatch.get(i), lastAddedLink);
			if (!matchedPoint.isEuclidian()){
				currentMatchedEndIndex = i-1;
				break;
			}
		}
		
		System.out.println("Current Range: [" + currentMatchedStartIndex + ", " + currentMatchedEndIndex + "]");
		
		matchGPSToLink(currentMatchedStartIndex, currentMatchedEndIndex, lastAddedLink);
		
		// set index for next matching for next function call
		lastMatchedIndex = currentMatchedEndIndex;
		currentIndexToMatch = lastMatchedIndex + 1;
		
		// matching successful
		return true;
	}
	
	private boolean adjustLastAddedLink() {
		myOSMWayPart lastAddedLink = selectedRoute.getLastSelectedLink();
		myOSMWayPart lastButOneAddedLink = selectedRoute.getLastButOneSelectedLink();
		
		if (lastAddedLink != null) {
			if (lastAddedLink.isLastMatched()) {
				System.out.println ("\nLetzer Link gematcht! Wird angepasst:\n\tAlter Range: [" + lastAddedLink.getLastMatchedRangeStart() + ", " + lastAddedLink.getLastMatchedRangeEnd() + "]" +
									"\n\tNeuer Range: [" + lastAddedLink.getLastMatchedRangeStart() + ", " + currentIndexToMatch + "]");
				addGPSToLink(lastAddedLink.getLastMatchedRangeEnd()+1, currentIndexToMatch, lastAddedLink);
			}
			else {
				System.out.println ("\nLetzer Link nicht gematcht!");
				
				int currentMatchedStartIndex = lastMatchedIndex + 1;
				
				if (lastButOneAddedLink != null && lastButOneAddedLink.isLastMatched()) {
					// get matched start and end index of last but one added link
					int lastMatchedStartIndex = lastButOneAddedLink.getLastMatchedRangeStart();
					int lastMatchedEndIndex = lastButOneAddedLink.getLastMatchedRangeEnd();
					
					System.out.println("Vorletzer Link vorhanden! gematched: " + lastButOneAddedLink.isLastMatched() + "\n\tAlter Range: [" + lastMatchedStartIndex + ", " + lastMatchedEndIndex + "]");
					
					int newLastMatchedEndIndex = 0;
					
					// ermittele bis wohin die zwischen gps nodes zum vorletzen link besser passen
					for(int i=currentIndexToMatch; i>=lastMatchedStartIndex; i--) {
						if (Coordinates.getDistance(gpsNodesToMatch.get(i), lastAddedLink) > Coordinates.getDistance(gpsNodesToMatch.get(i), lastButOneAddedLink)){
							newLastMatchedEndIndex = i;
							currentMatchedStartIndex = i+1;
							break;
						}
					}
					
					System.out.println("\tNeuer Range: [" + lastMatchedStartIndex + ", " + newLastMatchedEndIndex + "]");
					
					// berechne überschneidungen
					int rangeDiff = lastMatchedEndIndex - currentMatchedStartIndex;
					
					// Bei rangeDiff >= 0 release alte gps nodes
					if (rangeDiff >= 0){
						System.out.println("\tRelease GPS Punkte vom altem Link: [" + currentMatchedStartIndex + ", " + lastMatchedEndIndex + "]");
						//release gps nodes die noch zum alten link gehören ermitteln
						releaseGPSNodes(currentMatchedStartIndex, lastMatchedEndIndex, lastButOneAddedLink);
					}
					// bei rangeDiff < -1 gibts gps punkte zwischen den link die zum alten link gematched werden
					else if (rangeDiff < -1){
						System.out.println("\tFüge GPS Punkte zum alten Link: [" + (lastMatchedEndIndex+1) + ", " + (currentMatchedStartIndex-1) + "]");
						addGPSToLink((lastMatchedEndIndex+1), (currentMatchedStartIndex-1), lastButOneAddedLink);
					}
					else
						System.out.println("\tKeine Änderungen am alten Link!");
					
				}
				
				lastAddedLink.removeLastMatchedRange();
				matchGPSToLink(currentMatchedStartIndex, currentIndexToMatch, lastAddedLink);
				System.out.println("Current Range: [" + currentMatchedStartIndex + ", " + currentIndexToMatch + "]" );
			}
			
			lastMatchedIndex = currentIndexToMatch;
			increaseCurrentIndexToMatch();
			
			// successfully adjusted last link
			return true;
		}
		
		// no changes made
		return false;
	}
	
	private void addGPSToLink(int startIndex, int endIndex, myOSMWayPart streetLink) {
		// adjust matching range
		streetLink.setLastMatchedRangeEnd(endIndex);
		streetLink.setLastMatched(true);
		
		// match points
		for (int i=startIndex; i<=endIndex; i++){
			MatchedPoint euclidianPoint = Coordinates.getNearestEuclidianPoint(gpsNodesToMatch.get(i), streetLink);
			gpsNodesToMatch.get(i).setMatchedX(euclidianPoint.getX());
			gpsNodesToMatch.get(i).setMatchedY(euclidianPoint.getY());
			gpsNodesToMatch.get(i).setMatchedDistance(euclidianPoint.getDistance());
		}
		
		// animate
		for (int i=0; i<COLOR_GRADIENT_STEPS; i++){
			
			// sleep this thread due to create an animation
			try { Thread.sleep(SLEEP);}
			catch (InterruptedException e) {;}
			
			// moving vector factor
			double f=i/(double) COLOR_GRADIENT_STEPS;
			
			// gradual move matched GPS node from original GPS position to matched position
			for (int j=startIndex; j<=endIndex; j++){
				
				//get next position of GPS nodes
				double nextX = (gpsNodesToMatch.get(j).getX() + (f*(gpsNodesToMatch.get(j).getMatchedX() - gpsNodesToMatch.get(j).getX())));
				double nextY = (gpsNodesToMatch.get(j).getY() + (f*(gpsNodesToMatch.get(j).getMatchedY() - gpsNodesToMatch.get(j).getY())));
				
				//set calculated moved position as next position to draw
				gpsNodesToMatch.get(j).setDrawX(nextX);
				gpsNodesToMatch.get(j).setDrawY(nextY);
				gpsNodesToMatch.get(j).setColor(colorGradient[i]);
			}
			
			// redraw moved GPS nodes
			drawComponent.repaint();
		}
		
	}
	
	private void releaseGPSNodes(int startIndex, int endIndex, myOSMWayPart streetLink) {
			// reset gps points as matched
			for (int i=startIndex; i<=endIndex; i++) {
				gpsNodesToMatch.get(i).resetMatched();
			}
			
			// set new range
			streetLink.setLastMatchedRangeEnd(startIndex-1);
			
			// animate release of GPS nodes
			for (int i=0; i<COLOR_GRADIENT_STEPS; i++){		
				// sleep this thread due to create an animation
				try { Thread.sleep(SLEEP);}
				catch (InterruptedException e) {;}
				
				// moving factor for vector
				double f=i/(double) COLOR_GRADIENT_STEPS;
				
				// gradual move matched GPS node from original GPS position to matched position
				for (int j=startIndex; j<=endIndex; j++){
					
					//get next position of GPS nodes
					double nextX = (gpsNodesToMatch.get(j).getMatchedX() + (f*(gpsNodesToMatch.get(j).getX() - gpsNodesToMatch.get(j).getMatchedX())));
					double nextY = (gpsNodesToMatch.get(j).getMatchedY() + (f*(gpsNodesToMatch.get(j).getY() - gpsNodesToMatch.get(j).getMatchedY())));
					
					//set calculated moved position as next position to draw
					gpsNodesToMatch.get(j).setDrawX(nextX);
					gpsNodesToMatch.get(j).setDrawY(nextY);
					gpsNodesToMatch.get(j).setColor(colorGradient[COLOR_GRADIENT_STEPS-(i+1)]);
				}
				
				// redraw moved GPS nodes
				drawComponent.repaint();
		}
	}
	
	private void matchGPSToLink(int startIndex, int endIndex, myOSMWayPart streetLink){
		// set range
		streetLink.addMatchedRange(startIndex, endIndex, true);
		
		// match points
		for (int i=startIndex; i<=endIndex; i++){
			MatchedPoint euclidianPoint = Coordinates.getNearestEuclidianPoint(gpsNodesToMatch.get(i), streetLink);
			gpsNodesToMatch.get(i).setMatchedX(euclidianPoint.getX());
			gpsNodesToMatch.get(i).setMatchedY(euclidianPoint.getY());
			gpsNodesToMatch.get(i).setMatchedDistance(euclidianPoint.getDistance());
		}
		
		//animate
		for (int i=0; i<COLOR_GRADIENT_STEPS; i++){
			
			//sleep this thread due to create an animation
			try { Thread.sleep(SLEEP);}
			catch (InterruptedException e) {;}
			
			//moving vector factor
			double f=i/(double) COLOR_GRADIENT_STEPS;
			
			//gradual move matched GPS node from original GPS position to matched position
			for (int j=startIndex; j<=endIndex; j++){
				
				//get next position of GPS nodes
				double nextX = (gpsNodesToMatch.get(j).getX() + (f*(gpsNodesToMatch.get(j).getMatchedX() - gpsNodesToMatch.get(j).getX())));
				double nextY = (gpsNodesToMatch.get(j).getY()  + (f*(gpsNodesToMatch.get(j).getMatchedY() - gpsNodesToMatch.get(j).getY())));
				
				//set calculated moved position as next position to draw
				gpsNodesToMatch.get(j).setDrawX(nextX);
				gpsNodesToMatch.get(j).setDrawY(nextY);
				gpsNodesToMatch.get(j).setColor(colorGradient[i]);
			}
			
			//redraw moved GPS nodes
			drawComponent.repaint();
		}
	}
	
	private void dematchGPSToLink() {
		// get last added link for dematching
		myOSMWayPart lastAddedLink = selectedRoute.getLastSelectedLink();
		
		if (lastAddedLink != null) {
			if (lastAddedLink.isLastMatched()) {
				currentIndexToMatch = lastAddedLink.getLastMatchedRangeStart();
				lastMatchedIndex = currentIndexToMatch - 1;
				releaseGPSNodes(lastAddedLink.getLastMatchedRangeStart(), lastAddedLink.getLastMatchedRangeEnd(), lastAddedLink);
				System.out.println("Letzter Link gematcht! Neuer currentIndexToMatch: " + currentIndexToMatch + ", lastMatchedIndex: " + lastMatchedIndex);
			}
			else {
				System.err.print("Letzer Link nicht gematcht! ");
				System.out.println("currentIndexToMatch: " + currentIndexToMatch + "lastMatchedIndex: " + lastMatchedIndex);
			}
			lastAddedLink.removeLastMatchedRange();
		}
	}
	
	public MatchedGPSNode getCurrentGPSPointToMatch() {
		if (currentIndexToMatch < gpsNodesToMatch.size())
			return gpsNodesToMatch.get(currentIndexToMatch);
		// otherwise return null
		return null;
	}
	
	private void setCurrentIndexToMatch(int newIndex) {
		currentIndexToMatch = newIndex;
		drawComponent.repaint();
	}
	
	public boolean increaseCurrentIndexToMatch() {
		if (currentIndexToMatch + 1 < (gpsNodesToMatch.size()-1)) {
			setCurrentIndexToMatch(++currentIndexToMatch);
			return true;
		}
		// no forward stepping of match index possible
		return false;
	}
	
	public boolean decreaseCurrentIndexToMatch() {
		if ((currentIndexToMatch - 1) > lastMatchedIndex) {
			setCurrentIndexToMatch(--currentIndexToMatch);
			return true;
		}
		// no back stepping of match index possible
		return false;
	}
	
	/**
	 * create a color gradient between startColor and targetColor with given steps
	 * @param startColor
	 * @param targetColor
	 * @param steps
	 * @return Color[]
	 */
	private Color[] getColorGradient(Color startColor, Color targetColor, int steps){
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
		for (int i=0; i<steps; i++) {
			colors[i] = new Color(startColorRed + (int) (i*stepRed),
								 startColorGreen + (int) (i*stepGreen),
								 startColorBlue + (int) (i*stepBlue));
		}
		
		// return this array
		return colors;
	}

	@Override
	public Vector<MatchedGPSNode> getMatchedGPSNodes() {
		return gpsNodesToMatch;
	}
	
	/*
	private void matchGPSToLink(){
		StreetLink lastSelectedLink = selectedRoute.getLastSelectedLink();
		
		
		if (lastSelectedLink!=null){
			
			GPSNode gpsNode;
			int nearestX;
			int nearestY;
			double distance;
			
			boolean setfirstMatchedGPS = false;
			int firstMatchedGPSNode=0;
			int lastMatchedGPSNode=0;
			
			int index=0;
			
			for (; index < gpsTrace.getNrOfNodes(); index++){
				gpsNode = gpsTrace.getNode(index);
				
				EuclidianPoint euclidianPoint = Coordinates.getNearestEuclidianPoint(gpsNode, lastSelectedLink);
				
				
				//nearestX = Coordinates.getNearestPointX(gpsNode, lastSelectedLink);
				//nearestY = Coordinates.getNearestPointY(gpsNode, lastSelectedLink);
				//distance = Coordinates.getDistance(gpsNode, lastSelectedLink);
				
				
				if (euclidianPoint.isEuclidian()){//&& euclidianPoint.getDistance() < 50){
					if (gpsNode.getMatchedDistance() > euclidianPoint.getDistance()){
						//System.out.println("Node Nr." + index + " Distanz = " + distance + "px");
						if (!setfirstMatchedGPS){
							firstMatchedGPSNode = index;
							setfirstMatchedGPS = true;
						}
						
						gpsNode.setMatchedX(euclidianPoint.getX());
						gpsNode.setMatchedY(euclidianPoint.getY());
						gpsNode.setMatchedDistance(euclidianPoint.getDistance());
						//System.out.println("gpsX: " + gpsNode.getX() + " gpsY: " + gpsNode.getY() + " matchedX: " + nearestX + " matchedY: " + nearestY);
						if (index>=lastMatchedGPSNode) lastMatchedGPSNode = index;
					}
				}
				else if (setfirstMatchedGPS) break;
					
				//else System.out.println("Weit entfernter Punkt! Node Nr." + i + " Distanz = " + distance + "px");
			}
			
			//copy ascertained index range values to final variables since inner anonymous
			//classes can only handle these kind of variables
			final int startIndex = firstMatchedGPSNode;
			final int endIndex = lastMatchedGPSNode+1;
			
			//matching animation
			SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>(){

				@Override
				protected Boolean doInBackground() throws Exception {
					
					for (int i=0; i<STEPS; i++){
						
						//sleep this thread due to create an animation
						try { Thread.sleep(SLEEP);}
						catch (InterruptedException e) {;}
						
						double f=i/(double) STEPS;
						
						//gradual move matched GPS node from original GPS position to matched position
						for (int j=startIndex; j<=endIndex; j++){
							
							//get next position of GPS nodes
							int nextX = (int) (gpsTrace.getNodeX(j) + (f*(gpsTrace.getMatchedNodeX(j) - gpsTrace.getNodeX(j))));
							int nextY = (int) (gpsTrace.getNodeY(j) + (f*(gpsTrace.getMatchedNodeY(j) - gpsTrace.getNodeY(j))));
							
							//set calculated moved position as next position to draw
							gpsTrace.getNode(j).setDrawX(nextX);
							gpsTrace.getNode(j).setDrawY(nextY);
							gpsTrace.getNode(j).setColor(colorGradient[i]);
						}
						
						//redraw moved GPS nodes
						drawComponent.repaint();
					}
					//loading process successful
					return true;
				}
			};
			//do loading process in background
			worker.execute();
		}
	}
	
	
	private Color[] getColorGradient(Color startColor, Color targetColor, int steps){
		//save color gradient into array
		Color[] colors = new Color[steps];
		
		int halfSteps = steps/2;

		//save r, g, b values of start color
		int startColorRed = startColor.getRed();
		int startColorGreen = startColor.getGreen();
		int startColorBlue = startColor.getBlue();

		//save r, g, b values of end color
		int targetColorRed = targetColor.getRed();
		int targetColorGreen = targetColor.getGreen();
		int targetColorBlue = targetColor.getBlue();
		
		//calculate step interval for reaching white color by every step
		int stepRed1 = (int) ((255 - startColorRed) / (double) halfSteps);
		int stepGreen1 = (int) ((255 - startColorGreen) / (double) halfSteps);
		int stepBlue1 = (int) ((255 - startColorBlue) / (double) halfSteps);
		
		//calculate step interval for reaching target color starting by white color
		int stepRed2 = (int) ((targetColorRed - 255) / (double) halfSteps);
		int stepGreen2 = (int) ((targetColorGreen - 255) / (double) halfSteps);
		int stepBlue2 = (int) ((targetColorBlue - 255) / (double) halfSteps);
		
		for (int i=0; i<halfSteps; i++){
			colors[i] = new Color(startColorRed + (int) (i*stepRed1),
								 startColorGreen + (int) (i*stepGreen1),
								 startColorBlue + (int) (i*stepBlue1));
			
			colors[halfSteps + i] = new Color(255 + (int) (i*stepRed2),
					 						  255 + (int) (i*stepGreen2),
					 						  255 + (int) (i*stepBlue2));
		}
		
		return colors;
	}
	*/
}
