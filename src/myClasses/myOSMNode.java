package myClasses;

import java.awt.geom.Point2D;
import java.util.Vector;
import cartesian.Coordinates;

public class myOSMNode {

	public long id = -1;
		
	public double lon = -1;
	public double lat = -1;

	public double x = -1;
	public double y = -1;
	
	private Vector<myOSMWayPart> WayPartsOutgoing = new Vector<myOSMWayPart>(2, 2);

	private Vector<myOSMWayPart> WayPartsIncoming = new Vector<myOSMWayPart>(2, 2);
	
	public myOSMNode() {}
	
    public myOSMNode(double x, double y, long id) {
    	this.x = x;
    	this.y = y;
        
        this.id = id;
    }
	
	public void setXY() {
		if (lon != -1 && lat != -1) {
			Point2D p = Coordinates.getCartesianXY(lon, lat);
			x = p.getX();
			y = p.getY();			
		}
	}
	
	public void WayPartsOutgoing_add(myOSMWayPart wp) {
		WayPartsOutgoing.add(wp);
		
		wp.endNode.WayPartsIncoming.add(wp);
	}

	public int WayPartsOutgoing_size() {
		 return WayPartsOutgoing.size();
	}

	public myOSMWayPart WayPartsOutgoing_get(int index) {
		 return WayPartsOutgoing.get(index);
	}
	
	public boolean WayPartsOutgoing_contains(myOSMWayPart wp) {
		 return WayPartsOutgoing.contains(wp);
	}

	public boolean WayPartsIncoming_contains(myOSMWayPart wp) {
		 return this.WayPartsIncoming.contains(wp);
	}

	/*
	 * return all outgoing wayParts, but ignore excludedNode as target
	 */
    public Vector<myOSMWayPart> getOutgoingWayPartExceptNotTo(myOSMNode excludedNode) {
    	// container for street links
    	Vector<myOSMWayPart> linkContainer = new Vector<>(this.WayPartsOutgoing.size());
    	
    	// add all outgoing links except given one
    	for (myOSMWayPart link : this.WayPartsOutgoing) {
    		if (link.endNode != excludedNode) {
    			linkContainer.add(link);
    		}
    	}
    	
    	// return extracted outgoing links
    	return linkContainer;
    }

	/*
	 * return all incoming wayParts, but ignore excludedNode as origin
	 */
    public Vector<myOSMWayPart> getIncomingWayPartExceptNotFrom(myOSMNode excludedNode) {
    	// container for street links
    	Vector<myOSMWayPart> linkContainer = new Vector<>(this.WayPartsIncoming.size());
    	
    	// add all outgoing links except given one
    	for (myOSMWayPart link : this.WayPartsIncoming) {
    		if (link.startNode != excludedNode) {
    			linkContainer.add(link);
    		}
    	}
    	
    	// return extracted outgoing links
    	return linkContainer;
    }

	/*
	 * return all incoming wayParts, but ignore excludedLink
	 */
    public Vector<myOSMWayPart> getOutgoingWayPartExcept(myOSMWayPart excludedLink) {
    	// container for street links
    	Vector<myOSMWayPart> linkContainer = new Vector<>(this.WayPartsOutgoing.size());
    	
    	// add all outgoing links except given one
    	for (myOSMWayPart link : this.WayPartsOutgoing) {
    		if (link != excludedLink) {
    			linkContainer.add(link);
    		}
    	}
    	
    	// return extracted outgoing links
    	return linkContainer;
    }
    
    /*
     * return all outgoing wayParts
     */
    public Vector<myOSMWayPart> getLinks(){
    	//return all links belongs to this node
    	return this.WayPartsOutgoing;
    }
    
    /*
     * add outgoing wayPart
     */
    public void addLink(myOSMWayPart link){
    	this.WayPartsOutgoing.add(link);
    }
    
    /*
     * remove outgoing wayPart
     */
    public void removeLink(myOSMWayPart streetLink) {
    	WayPartsOutgoing.remove(streetLink);
    	System.out.println("Error: myOSMNode: removeLink(myOSMWayPart link)");
    }
}
