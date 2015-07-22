package route;

import java.util.NoSuchElementException;
import java.util.Vector;
import myOSM.myOSMMap;
import myOSM.myOSMNode;
import myOSM.myOSMWayPart;
import cartesian.Coordinates;

/**
 * @author Daniel Sathees Elmo
 * 
 * this class represents an route which an user can build by
 * clicking several links
 */

public class SelectedRoute {
	
	public static final int NO_CONNECTION = 0;
	public static final int START_NODE = 1;
	public static final int END_NODE = 2;
	public static final int BOTH_NODE = 3;

	//	private StreetMap streetMap;							// reference to street map
	private myOSMMap myMap;							// reference to street map

	private myOSMWayPart selectableStreetLink;				// current street link which can be selected
	private Vector<myOSMWayPart> selectableStreetLinksPool;	// pool of all selectable street links
	private Vector<myOSMWayPart> selectedRoute;				// current selected Route

	private boolean noSelectableLinkLeft;					// should all links be selectable, cause there isn't any selectable link left
	private boolean allLinksSelectableMode;					// user wants to select all links
	
	private double lastKnownPosX;								// saves last known x-position
	private double lastKnownPosY;								// saves last known y-position

	/**
	 * constructor initializes with street map
	 * @param streetMap
	 */
	public SelectedRoute(myOSMMap myMap) {
		// save reference to street map, initialize new vectors, at the begin
		// set boolean variables false by default, save last known position with 0,0
//		this.streetMap = streetMap;
		this.myMap = myMap;
		
		selectedRoute = new Vector<myOSMWayPart>();
		selectableStreetLinksPool = new Vector<myOSMWayPart>();
		noSelectableLinkLeft = false;
		allLinksSelectableMode = false;
		saveLastKnownPosition(0, 0);
	}
	
	/**
	 * sets current selectable route according to given x, y position
	 * and current build build path
	 * @param x
	 * @param y
	 */
	public void setSelectableLink(double x, double y){
		// save position
		saveLastKnownPosition(x, y);
		
		// update selectable link pool
		updateSelectableStreetLinksPool();
		
		// try to get nearest selectable link from pool
		selectableStreetLink = getNearestLink(x, y);
		
		// if no street link is selectable, all links can be selected
		if (selectableStreetLink == null) {
			noSelectableLinkLeft = true;
			updateSelectableStreetLinksPool();
			selectableStreetLink = getNearestLink(x, y);
		}
	}
	
	public void switchAllLinksSelectableMode() {
		switchAllLinksSelectableMode(lastKnownPosX, lastKnownPosY);
	}
	
	public void switchAllLinksSelectableMode(double x, double y) {
		allLinksSelectableMode = !allLinksSelectableMode;
		setSelectableLink(x, y);
	}
	
	
	public void updateSelectableStreetLinksPool(){
		// no street link was selected, so all street links are selectable
		if (selectedRoute.isEmpty() || noSelectableLinkLeft || allLinksSelectableMode) {
			addAllStreetLinksToPool();
		}
		// forwarding stars of free nodes must be selectable
		else {
			// first clear pool, not all links are choosable
			selectableStreetLinksPool.clear();
			//allLinksSelectable = false;
			
			// get last added street link and last but one
			myOSMWayPart lastAddedSelectedLink = getLastSelectedLink();
			myOSMWayPart lastButOneAddedSelectedLink = getLastButOneSelectedLink();
			
			// check connection between links
			int streetLinkConnection = lastAddedSelectedLink.isConnectedTo(lastButOneAddedSelectedLink);
			
			// add all street links start forwarding from end node, if start node is the connecting link,
			// or there is no connection between these links
			if (streetLinkConnection == START_NODE || streetLinkConnection == BOTH_NODE || streetLinkConnection == NO_CONNECTION)
				 selectableStreetLinksPool.addAll(lastAddedSelectedLink.getEndNode().getLinks());
			
			// analog to above for end node
			if (streetLinkConnection == END_NODE || streetLinkConnection == BOTH_NODE || streetLinkConnection == NO_CONNECTION)
				selectableStreetLinksPool.addAll(lastAddedSelectedLink.getStartNode().getLinks());
		}
	}
	
	private void addAllStreetLinksToPool() {
		// get all street links of map as vector
		selectableStreetLinksPool = myMap.getStreetLinksVector();
		
		/*
		// clear pool, all links are now choosable
		selectableStreetLinksPool.clear();

		// convert street links array to vector and resize it to real size
		Collections.addAll(selectableStreetLinksPool, streetMap.getLinks());
		selectableStreetLinksPool.setSize(streetMap.getNrOfLinks());
		*/
	}
	
	/**
	 * adds current selected link if possible
	 * @param x current mouse x-position
	 * @param y current mouse y-position
	 * @return link could be added
	 */
	public boolean addLink(double x, double y ) {		
		// save position
		saveLastKnownPosition(x, y);
		
		if (selectableStreetLink != null) {
			// adjust current selectable link
			adjustSelectableLink();
				
			// mark as selected, add link to vector
			selectableStreetLink.increaseSelectCounter();
			selectedRoute.add(selectableStreetLink);
			
			// get next selectable link, deactivate all links selectable mode
			allLinksSelectableMode = false;
			noSelectableLinkLeft = false;
			setSelectableLink(x, y);
			
			// successfully link added
			return true;
		}
		// no link could be added
		return false;
	}
	
	/**
	 * adds current selected link if possible
	 * uses last known x,y position for determination
	 * of next selectable link
	 * @return
	 */
	public boolean addLink() {
		return addLink(lastKnownPosX, lastKnownPosY);
	}
	
	/**
	 * remove last added link
	 * @param x current mouse x-position
	 * @param y current mouse y-position
	 * @return link could be removed
	 */
	public boolean removeLink(double x, double y){
		// save position
		saveLastKnownPosition(x, y);
		
		// remove last added street link
		if (!selectedRoute.isEmpty()){
			// decrease select counter of last added link, remove just last element,  
			// therefore we use index instead of object reference
			myOSMWayPart lastAddedStreetLink = selectedRoute.lastElement();
			lastAddedStreetLink.decreaseSelectCounter();
			removeLastSelectedLink();
			
			// update link pool and calculate next selectable link
			noSelectableLinkLeft = false;
			setSelectableLink(x, y);
			
			// success
			return true;
		}
		// no link removed
		return false;
	}

	/**
	 * get number of selected links
	 * @return
	 */
	public int getNrOfSelectedLinks(){
		return selectedRoute.size();
	}
	
	/**
	 * get link at specific index in vector
	 * @param index
	 * @return
	 */
	public myOSMWayPart getSelectedLink(int index){
		if ( (index >= 0) && (index < selectedRoute.size()) )
				return selectedRoute.get(index);
		// otherwise return null;
		return null;
	}
	
	/**
	 * get current selectable link
	 * @return
	 */
	public myOSMWayPart getSelectableLink(){
		return selectableStreetLink;
	}
	
	/**
	 * is currently a selectable link present?
	 * @return
	 */
	public boolean selectableStreetLink(){
		return (selectableStreetLink != null);
	}
	
	/**
	 * get vector of selected links
	 * @return
	 */
	public Vector<myOSMWayPart> getSelectedLinks(){
		return selectedRoute;
	}
	
	/**
	 * get last selected link
	 * @return
	 */
	public myOSMWayPart getLastSelectedLink(){
		try{ return selectedRoute.lastElement(); } 
		catch (NoSuchElementException e){ return null; }
	}
	
	/**
	 * get last but one added link
	 * @return
	 */
	public myOSMWayPart getLastButOneSelectedLink(){
		try{ return selectedRoute.get(selectedRoute.size()-2);}
		catch (IndexOutOfBoundsException e){ return null;}
	}
	
	/**
	 * is selected route container empty?
	 * @return
	 */
	public boolean isEmpty(){
		return selectedRoute.isEmpty();
	}
	
	/**
	 * if there is an gap between last selected link and selectable link,
	 * this method creates an artificial link which builds a connection
	 * and sets it as new selectable link
	 * @return artificial link was created
	 */
	private boolean adjustSelectableLink() {
		// if we had already at least one added link
		if (!selectedRoute.isEmpty() && selectableStreetLink != null) {
			
			myOSMWayPart lastSelectedLink = getLastSelectedLink();
						
			// check if's connected to new link
			if (lastSelectedLink.isConnectedTo(selectableStreetLink) == NO_CONNECTION) {
				
				// prepare node vectors for distance comparison
				Vector<myOSMNode> nodesOfLastLink = new Vector<>();
				Vector<myOSMNode> nodesOfNewLink = new Vector<>();
				
				myOSMWayPart lastButOneSelectedLink = getLastButOneSelectedLink();
				
				// 1.) prepare first vector, find out connection type 
				// between last two selected links, and add free nodes to vector
				int linkConnection = lastSelectedLink.isConnectedTo(lastButOneSelectedLink);
				
				if (linkConnection == START_NODE || linkConnection == NO_CONNECTION)
					nodesOfLastLink.add(lastSelectedLink.getEndNode());
					
				if (linkConnection == END_NODE || linkConnection == NO_CONNECTION)
					nodesOfLastLink.add(lastSelectedLink.getStartNode());
				
				// 2.) prepare second vector, the nodes of new link are always free,
				// cause they aren't already connected
				nodesOfNewLink.add(selectableStreetLink.getStartNode());
				nodesOfNewLink.add(selectableStreetLink.getEndNode());
				
				double distance = 0;
				double minDistance = Double.MAX_VALUE;
				myOSMNode startNode = null;
				myOSMNode endNode = null;
				
				// seek nodes of last and new link which have the shortest distance
				for (myOSMNode nodeOfLastLink : nodesOfLastLink) {
					for (myOSMNode nodeOfNewLink : nodesOfNewLink) {
						distance = Coordinates.getDistance(nodeOfLastLink, nodeOfNewLink);
						if (distance < minDistance) {
							// set start and end node of artificial connection link
							// and new minimum distance
							startNode =  nodeOfLastLink;
							endNode = nodeOfNewLink;
							minDistance = distance;
						}
					}
				}
				
				// create artificial link and set it as new selectable link
				myOSMWayPart artificialLink = new myOSMWayPart(startNode, endNode, true, -2, startNode.id, endNode.id);
				
				// add link to start and end nodes
				startNode.addLink(artificialLink);
				endNode.addLink(artificialLink);
				
				//set new selectable link
				selectableStreetLink = artificialLink;
								
				// selectable link was changed to artificial link
				return true;
			}
			// no change made to selectable link
			return false;
		}
		// no change made to selectable link
		return false;
	}
	
	/**
	 * remove last added link, remove link completely (unselectable any more) if it's artificial
	 * @return last selected link was removed
	 */
	private boolean removeLastSelectedLink() {
		if (!selectedRoute.isEmpty()){
			myOSMWayPart lastSelectedLink = selectedRoute.lastElement();
			
			// remove last link completely if its artificial and its select counter equals zero
			// therefore delete v
			if (lastSelectedLink.isArtificial() && lastSelectedLink.getSelectCounter() == 0) {
				lastSelectedLink.getStartNode().removeLink(lastSelectedLink);
				lastSelectedLink.getEndNode().removeLink(lastSelectedLink);
			}
			
			// remove by index not by reference, otherwise all elements in vector with same reference are removed
			selectedRoute.remove(selectedRoute.size()-1);
			
			// last link removed
			return true;
		}
		// no link removed
		return false;
	}
	
	private myOSMWayPart getNearestLink(double x, double y) {
		//nearest street link
		myOSMWayPart nearestStreetLink = null;
		//last selected street link
		myOSMWayPart lastSelectedStreetLink = getLastSelectedLink();
		
		//save nearest point coordinates on street link
		double nearestX;
		double nearestY;
		
		//save distance/current minimal distance to a street link
		double distance = Double.MAX_VALUE;
        double minDistance = Double.MAX_VALUE;
        
        //start and end position of streetLink
        double ax,ay,bx,by;

        for (myOSMWayPart streetLink : selectableStreetLinksPool){
        	//street link mustn't be already selected
        	if ((streetLink != lastSelectedStreetLink) || noSelectableLinkLeft || allLinksSelectableMode){
        		//get StartNode and EndNode of Link i
        		ax = streetLink.getStartX();
        		ay = streetLink.getStartY();
        		bx = streetLink.getEndX();
        		by = streetLink.getEndY();
        		
        		//get distance
        		nearestX=Coordinates.getNearestPointX(x,y,ax,ay,bx,by);
        		nearestY=Coordinates.getNearestPointY(x,y,ax,ay,bx,by);
        		distance=Coordinates.getDistanceSquared(x,y,nearestX,nearestY);
        		
        		//check if distance is below current minimal distance
        		if (distance < minDistance){
        			minDistance = distance;
        			nearestStreetLink=streetLink;
        		}
        	}
        }
        //return nearest street link
        return nearestStreetLink;
	}
	
	/**
	 * saves last known mouse
	 * @param x
	 * @param y
	 */
	private void saveLastKnownPosition(double x, double y) {
		lastKnownPosX = x;
		lastKnownPosY = y;
	}
	
	protected void finalize(){
		System.out.println("Finalize" + this.getClass());
		//set street links as not 
		for (myOSMWayPart streetLink : selectedRoute){
			streetLink.resetSelectCounter();
		}
	}
}
