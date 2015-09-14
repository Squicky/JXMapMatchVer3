package route;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Vector;

import logging.Logger;
import myClasses.myOSMMap;
import myClasses.myOSMNode;
import myClasses.myOSMWayPart;

import org.jdesktop.swingx.mapviewer.GeoPosition;

import algorithm.MatchedLink;
import algorithm.NRouteAlgorithm;
import cartesian.Coordinates;

public class SelectedNRoute {
	
	private double lastKnownPosX;						// saves last known x-position
	private double lastKnownPosY;						// saves last known y-position
	
	private ArrayList<myOSMWayPart> streetLinksStart;
	private ArrayList<myOSMWayPart> streetLinksEnd;
	
	private myOSMWayPart selectableLink;
	
	private myOSMWayPart deletableLink;
	
	private boolean isNRouteSplitted = false;
		
	public SelectedNRoute(myOSMMap myMap, NRouteAlgorithm nRouteAlgorithm, Component drawComponent) {
		// call other constructor
		this( myMap, drawComponent);
		
		// initialize start array list, convert best route to array list
		streetLinksStart = convertNRouteToArrayList(nRouteAlgorithm.getNRoute(/*0*/));
	}
	
	public SelectedNRoute(myOSMMap myMap, Component drawComponent) {
		// initialize array lists 
		streetLinksStart = new ArrayList<>();
		streetLinksEnd = new ArrayList<>();
		
		// initialize mouse position
		saveLastKnownPosition(0, 0);
	}
	
	/**
	 * adds given link to start link set
	 * note: should only used by algorithm not by user
	 * 
	 * @param link
	 * @return
	 */
	public boolean addStartLink(myOSMWayPart link) {
		
		// add link to start links array list
		streetLinksStart.add(link);
		
		return true;
	}
	
	/**
 	 * adds given link to end link set
	 * note: should only used by algorithm not by user
	 * 
	 * @param link
	 * @return
	 */
	public boolean addEndLink(myOSMWayPart link) {
		
		// add link to end links array list
		streetLinksEnd.add(link);
		
		// update state
		isNRouteSplitted = true;
		
		return true;
	}
	
	private ArrayList<myOSMWayPart> convertNRouteToArrayList(Vector<NRoute> nRouteVec) {
		ArrayList<myOSMWayPart> streetLinksArrayList = new ArrayList<>();
		
		for (NRoute nRoute : nRouteVec) {
			// get n route matched links
			Vector<MatchedLink> matchedLinksVector = nRoute.getNRouteLinks();
			
			for (MatchedLink matchedLink : matchedLinksVector) {
				// add to array list
				streetLinksArrayList.add(matchedLink.getStreetLink());
			}
		}
		
		return streetLinksArrayList;
	}
	
	public void setEditableLinks(double x, double y) {
		saveLastKnownPosition(x, y);
		seekSelectableLink();
		seekDeletableLink();
	}
	
	public void addLink(double x, double y) {
		saveLastKnownPosition(x, y);
		
		// add to first part?
		if (!streetLinksStart.isEmpty()) {
			// get first link
			//StreetLink streetLink = streetLinksStart.get(0);
			myOSMWayPart streetLink = streetLinksStart.get(0);
			
			if (streetLink.startNode.WayPartsIncoming_contains(selectableLink) 
//					|| streetLink.endNode.WayPartsIncoming_contains(selectableLink)
					) {
				
				streetLinksStart.add(0, selectableLink);
				System.out.println("streetLinksStart.add: " + selectableLink.parentWay.id + " | " +  selectableLink.startNode.id + " -> " + selectableLink.endNode.id);
				if (canNRoutesBeMerged()) {
					mergeNRoutes();
				}
				setEditableLinks(lastKnownPosX, lastKnownPosY);
				return;
			}
			
			int streetLinkStartSize = streetLinksStart.size();
			
			if (streetLinkStartSize > 1) {
				
				streetLink = streetLinksStart.get(streetLinkStartSize - 1);
				
				if (
//						streetLink.startNode.WayPartsOutgoing_contains(selectableLink) ||
							streetLink.endNode.WayPartsOutgoing_contains(selectableLink)) {
					
						streetLinksStart.add(selectableLink);
						if (canNRoutesBeMerged()) {
							mergeNRoutes();
						}
						setEditableLinks(lastKnownPosX, lastKnownPosY);
						return;
				}
			}
		}
		
		// add to second part?
		if (!streetLinksEnd.isEmpty()) {
			// get first link
//			StreetLink streetLink = streetLinksEnd.get(0);
			myOSMWayPart streetLink = streetLinksEnd.get(0);
					
			if (streetLink.startNode.WayPartsIncoming_contains(selectableLink)
//					|| streetLink.endNode.WayPartsIncoming_contains(selectableLink)
					) {
				streetLinksEnd.add(0, selectableLink);
				if (canNRoutesBeMerged()) mergeNRoutes();
				setEditableLinks(lastKnownPosX, lastKnownPosY);
				return;
			}
			
			int streetLinkStartSize = streetLinksEnd.size();
			
			if (streetLinkStartSize > 1) {
				
				streetLink = streetLinksEnd.get(streetLinkStartSize - 1);
				
				if (
//						streetLink.startNode.WayPartsOutgoing_contains(selectableLink) ||
						streetLink.endNode.WayPartsOutgoing_contains(selectableLink)) {
						streetLinksEnd.add(selectableLink);
						if (canNRoutesBeMerged()) mergeNRoutes();
						setEditableLinks(lastKnownPosX, lastKnownPosY);
						return;
				}
			}
		}	
	}
	
	public void deleteLink(double x, double y) {
		saveLastKnownPosition(x, y);
		
		if (isNRouteSplitted) {
			// start part
			if (streetLinksStart.contains(deletableLink)) {
				streetLinksStart.remove(deletableLink);
			}
			
			// end part
			if (streetLinksEnd.contains(deletableLink)) {
				streetLinksEnd.remove(deletableLink);
			}
			
			if (streetLinksStart.isEmpty() && (streetLinksEnd.isEmpty() == false)) {
				streetLinksStart.addAll(streetLinksEnd) ;
				streetLinksEnd.clear();

				isNRouteSplitted = false;
			}
			
		} else {
			
			// start or end?
			if (!streetLinksStart.isEmpty()) {
				myOSMWayPart streetLinkStart = streetLinksStart.get(0);
				myOSMWayPart streetLinkEnd = streetLinksStart.get(streetLinksStart.size()-1);
				
				if (deletableLink == streetLinkStart || deletableLink == streetLinkEnd) {
					streetLinksStart.remove(deletableLink);
					setEditableLinks(lastKnownPosX, lastKnownPosY);
					return;
				}
			}
			
			// split
			ArrayList<myOSMWayPart> tmpStreetLinksStart = new ArrayList<>();
			ArrayList<myOSMWayPart> tmpStreetLinksEnd = new ArrayList<>();
			
			boolean linkToDeleteFound = false;
			
			// split n route
			for (myOSMWayPart streetLink : streetLinksStart) {
				
				if (streetLink == deletableLink) {
					linkToDeleteFound = true;
					continue;
				}
				
				if (linkToDeleteFound) {
					tmpStreetLinksEnd.add(streetLink);
				} else {
					tmpStreetLinksStart.add(streetLink);
				}
			}
			
			isNRouteSplitted = true;
			streetLinksStart = tmpStreetLinksStart;
			streetLinksEnd = tmpStreetLinksEnd;			
		}
		
		setEditableLinks(x, y);
	}
	
	private void seekSelectableLink() {
		//save nearest point coordinates on street link
		double nearestX;
		double nearestY;
		
        //start and end position of streetLink
		double ax,ay,bx,by;
		
		// save distance/current minimal distance to a street link
		double distance = Double.MAX_VALUE;
        double minDistance = Double.MAX_VALUE;
        
        // store nearest street link here
        myOSMWayPart nearestStreetLink = null;
		
		// get street links which could be expanded
		ArrayList<myOSMWayPart> streetLinksToExpandPool = new ArrayList<>();
		
		if (!streetLinksStart.isEmpty()) {
			streetLinksToExpandPool.add(streetLinksStart.get(0));
			if (streetLinksStart.size() > 1) {
				streetLinksToExpandPool.add(streetLinksStart.get(streetLinksStart.size()-1));
			}
		}
		
		if (!streetLinksEnd.isEmpty()) {
			streetLinksToExpandPool.add(streetLinksEnd.get(0));
			if (streetLinksEnd.size() > 1) {
				streetLinksToExpandPool.add(streetLinksEnd.get(streetLinksEnd.size()-1));
			}
		}
		
		for (myOSMWayPart streetLink : streetLinksToExpandPool) {
			//get StartNode and EndNode of Link i
    		ax = streetLink.startNode.x;
    		ay = streetLink.startNode.y;
    		bx = streetLink.endNode.x;
    		by = streetLink.endNode.y;
    		
    		//get distance
    		nearestX=Coordinates.getNearestPointX(lastKnownPosX,lastKnownPosY,ax,ay,bx,by);
    		nearestY=Coordinates.getNearestPointY(lastKnownPosX,lastKnownPosY,ax,ay,bx,by);
    		distance=Coordinates.getDistanceSquared(lastKnownPosX,lastKnownPosY,nearestX,nearestY);
    		
    		//check if distance is below current minimal distance
    		if (distance < minDistance){
    			minDistance = distance;
    			nearestStreetLink=streetLink;
    		}
		}
				
		boolean isFirstStreetLink = false;
		if (streetLinksStart.size() > 0) {
			if ( nearestStreetLink == streetLinksStart.get(0) ) {
				isFirstStreetLink = true;
			}
		}
		if (streetLinksEnd.size() > 0) {
			if ( nearestStreetLink == streetLinksEnd.get(0) ) {
				isFirstStreetLink = true;
			}
		}
		
			
			
		// get right node
		myOSMNode n = getOutgoingStreetNode(nearestStreetLink, isFirstStreetLink);
		
		Vector<myOSMWayPart> adjustableLinks;
		if (isFirstStreetLink) {
			adjustableLinks = n.getIncomingWayPartExceptNotFrom(nearestStreetLink.endNode);
		} else {
			adjustableLinks = n.getOutgoingWayPartExceptNotTo(nearestStreetLink.startNode);			
		}

		
		// reset values
		distance = Double.MAX_VALUE;
        minDistance = Double.MAX_VALUE;
		
		for(myOSMWayPart streetLink : adjustableLinks) {
			
			//get StartNode and EndNode of Link i
    		ax = streetLink.getStartX();
    		ay = streetLink.getStartY();
    		bx = streetLink.getEndX();
    		by = streetLink.getEndY();
    		
    		//get distance
    		nearestX=Coordinates.getNearestPointX(lastKnownPosX,lastKnownPosY,ax,ay,bx,by);
    		nearestY=Coordinates.getNearestPointY(lastKnownPosX,lastKnownPosY,ax,ay,bx,by);
    		distance=Coordinates.getDistanceSquared(lastKnownPosX,lastKnownPosY,nearestX,nearestY);
    		
    		//check if distance is below current minimal distance
    		if (distance < minDistance){
    			minDistance = distance;
    			nearestStreetLink=streetLink;
    		}
			
		}
		
		// get outgoing link for nearest expandable street link
		selectableLink = nearestStreetLink;
	}
	
	private myOSMNode getOutgoingStreetNode(myOSMWayPart streetLink, boolean isFirstStreetLink) {
		
		if (streetLink != null) {
			if (isFirstStreetLink) {
				return streetLink.startNode;			
			} else {
				return streetLink.endNode;							
			}
		}

		/*
		myOSMNode startNode = streetLink.startNode;
		myOSMNode endNode = streetLink.endNode;
		
		Vector<myOSMWayPart> startLinks =  startNode.getLinksExcept(streetLink);
		Vector<myOSMWayPart> endLinks = endNode.getLinksExcept(streetLink);
		
		for (myOSMWayPart link : startLinks) {
			if (streetLinksStart.contains(link) || streetLinksEnd.contains(link)) {
				return endNode;
			}
		}
		
		for (myOSMWayPart link : endLinks) {
			if (streetLinksStart.contains(link) || streetLinksEnd.contains(link)) {
				return startNode;
			}
		}
		*/
		
		return null;
	}
	
	private boolean canNRoutesBeMerged() {
		if (isNRouteSplitted && !streetLinksStart.isEmpty() && !streetLinksEnd.isEmpty()) {
			myOSMWayPart streetLinkStartLast = streetLinksStart.get(streetLinksStart.size()-1);
			
			Vector<myOSMWayPart> startLinks = streetLinkStartLast.getStartNode().getOutgoingWayPartExcept(streetLinkStartLast);
			Vector<myOSMWayPart> endLinks = streetLinkStartLast.getEndNode().getOutgoingWayPartExcept(streetLinkStartLast);
			
			myOSMWayPart streetLinkEndFirst = streetLinksEnd.get(0);
			
			if (startLinks.contains(streetLinkEndFirst) || endLinks.contains(streetLinkEndFirst)) {
				Logger.println("N Routes merged");
				return true;
			} 
		}
		
		return false;
	}
	
	private void mergeNRoutes() {
		ArrayList<myOSMWayPart> tmpStreetLinksStart = new ArrayList<>();
		
		tmpStreetLinksStart.addAll(streetLinksStart);
		tmpStreetLinksStart.addAll(streetLinksEnd);
		
		streetLinksStart = tmpStreetLinksStart;
		streetLinksEnd.clear();
		
		isNRouteSplitted = false;
	}
	
	private void seekDeletableLink() {
		//save nearest point coordinates on street link
		double nearestX;
		double nearestY;
		
        //start and end position of streetLink
		double ax,ay,bx,by;
		
		// save distance/current minimal distance to a street link
		double distance = Double.MAX_VALUE;
        double minDistance = Double.MAX_VALUE;
        
        // store nearest street link here
        myOSMWayPart nearestStreetLink = null;
		
		// get street links which could be expanded
		ArrayList<myOSMWayPart> streetLinksToExpandPool = new ArrayList<>();
		
		if (isNRouteSplitted) {
			if (!streetLinksStart.isEmpty()) {
				streetLinksToExpandPool.add(streetLinksStart.get(0));
				if (streetLinksStart.size() > 1) {
					streetLinksToExpandPool.add(streetLinksStart.get(streetLinksStart.size()-1));
				}
			}
			
			if (!streetLinksEnd.isEmpty()) {
				streetLinksToExpandPool.add(streetLinksEnd.get(0));
				if (streetLinksEnd.size() > 1) {
					streetLinksToExpandPool.add(streetLinksEnd.get(streetLinksEnd.size()-1));
				}
			}
		} else {
			streetLinksToExpandPool = streetLinksStart;
		}

		for (myOSMWayPart streetLink : streetLinksToExpandPool) {
			//get StartNode and EndNode of Link i
    		ax = streetLink.getStartX();
    		ay = streetLink.getStartY();
    		bx = streetLink.getEndX();
    		by = streetLink.getEndY();
    		
    		//get distance
    		nearestX=Coordinates.getNearestPointX(lastKnownPosX,lastKnownPosY,ax,ay,bx,by);
    		nearestY=Coordinates.getNearestPointY(lastKnownPosX,lastKnownPosY,ax,ay,bx,by);
    		distance=Coordinates.getDistanceSquared(lastKnownPosX,lastKnownPosY,nearestX,nearestY);
    		
    		//check if distance is below current minimal distance
    		if (distance < minDistance){
    			minDistance = distance;
    			nearestStreetLink=streetLink;
    		}
		}
		
		// get outgoing link for nearest expandable street link
		deletableLink = nearestStreetLink;
	}
	
	public ArrayList<myOSMWayPart> getNRouteLinksStart() {
		return streetLinksStart;
	}
	
	public ArrayList<myOSMWayPart> getNRouteLinksEnd() {
		return streetLinksEnd;
	}
	
	public myOSMWayPart getSelectableLink() {
		return selectableLink;
	}
	
	public myOSMWayPart getDeletableLink() {
		return deletableLink;
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
	
	public boolean isNRouteSplit() {
		return isNRouteSplitted;
	}
	
	public void printStartLinks() {
		System.out.print("\nStreetStartLinks: ");
		for (myOSMWayPart link : streetLinksStart) {
			System.out.print(link.getID() + ",");
		}
	}
	
	public void printEndLinks() {
		System.out.print("\nStreetEndLinks: ");
		for (myOSMWayPart link : streetLinksEnd) {
			System.out.print(link.getID() + ",");
		}
	}
	
	public GeoPosition getStartGeoPos() {
		// get first link of start link
		myOSMWayPart startLink = streetLinksStart.get(0);
		
    	return Coordinates.getGeoPos(startLink.getStartX(), startLink.getStartY());
    }
}
