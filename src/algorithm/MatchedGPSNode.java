package algorithm;

import gps.GPSNode;

import java.awt.Color;

import myOSM.myOSMWayPart;

public class MatchedGPSNode extends GPSNode {
	
	 // matched position
    protected double matchedX = 0;
    protected double matchedY = 0;
    
    // current matched distance (MAX by default)
    private double matchedDistance = Double.MAX_VALUE;
    
    // is this node matched?
    protected boolean matched = false;
    
    // graphic position for drawing / animation
    protected double drawX = 0;
    protected double drawY = 0;
    
    // color for drawing
    protected Color initColor;
    protected Color color;

	public double matched_percent_in_WayParty = -1;
	public myOSMWayPart matchtedWayPart = null;
	
	public MatchedGPSNode(GPSNode gpsNode, Color color) {
		super(gpsNode.getX(), gpsNode.getY(), gpsNode.getTimestamp(), gpsNode.getLon(), gpsNode.getLat());
		this.initColor = color;
		
		// set matched state as not matched
        // and sets matched distance to maximum value
		// reset also matched/drawing positions
		resetMatched();
	}

	public void setMatchedX(double matchedX) {
		this.matchedX = matchedX;
		this.matched = true;
	}
	
	public double getMatchedX() {
		return matchedX;
	}

	public void setMatchedY(double matchedY) {
		this.matchedY = matchedY;
		this.matched = true;
	}

	public double getMatchedY() {
		return matchedY;
	}

	public void setMatched(boolean matched) {
		this.matched = matched;
	}
	
	public boolean isMatched() {
		return matched;
	}
	
	/**
    * set distance to matched position
    * @param matchedDistance
    */
    public void setMatchedDistance(double matchedDistance){
    	this.matchedDistance = matchedDistance;
    }
   
	/**
	* get distance to matched position
	* @return
	*/
    public double getMatchedDistance(){
    	return matchedDistance;
    }
	
	public void setColor(Color color) {
		this.color = color;
	}

	public Color getColor() {
		return color;
	}
	
	public void setDrawX(double drawX) {
		this.drawX = drawX;
	}
	
	public double getDrawX() {
		return drawX;
	}

	public void setDrawY(double drawY) {
		this.drawY = drawY;
	}

	public double getDrawY() {
		return drawY;
	}
	
	/**
    * forgets that its matched ;)
    */
	public void resetMatched(){
		this.matchedDistance = Double.MAX_VALUE;
		this.matched = false;
		this.matchedX = this.getX();
		this.matchedY = this.getY();
		this.drawX = this.getX();
		this.drawY = this.getY();
		this.color = initColor;
	}
}
