package gps;

import org.jdesktop.swingx.mapviewer.GeoPosition;

import cartesian.Coordinates;

/**
 * @author Tobias
 * @author Daniel Sathees Elmo
 */

//TODO replace array with current, more dynamic structure

public class GPSTrace {
    // store all points in array data[]
    // store latitude , longitude , timestamp in array data[]

    // one could use Vector() instead of array
    private GPSNode [] nodes;
    private int nrOfNodes=0;
    private int maxNrOfNodes=0;

    // reset min/max values for gps nodes
    private double minX=Integer.MAX_VALUE;
    private double minY=Integer.MAX_VALUE;
    private double maxX=-Integer.MAX_VALUE;
    private double maxY=-Integer.MAX_VALUE;
    
    // save reference timestamp which we had to add to 
    // GPS node timestamps to get absolute timestamp since 01.01.1970
    private long refTimeStamp;

    /**
     * create GPSPath with number of GPSNodes
     * @param nrOfGPSPoints
     */
    public GPSTrace(int nrOfGPSPoints, long timestamp ){
        // store up to (NR) GPSNodes
        maxNrOfNodes = nrOfGPSPoints;
        nodes = new GPSNode[maxNrOfNodes];
        this.refTimeStamp = timestamp;
    }
    
    public int getNodeStatus(int index) {
    	return nodes[index].status;
    }

    public void setNodeStatus(int index, int _status) {
    	nodes[index].status = _status;
    }
    
    public void resetNodeSatus() {
        for(int i=0; i<getNrOfNodes(); i++){
        	nodes[i].status = 0;
        }
    }

    
    public long getRefTimeStamp(){
    	return refTimeStamp;
    }

    /**
     * add GPSNode (Position: [x,y] with timestamp t)
     * to GPSPath
     * @param x
     * @param y
     * @param t
     */
    public void addNode(double x, double y, long t, double lon,  double lat){
        if (nrOfNodes < maxNrOfNodes){
        	// if there is space in array, add GPS node
            nodes[nrOfNodes] = new GPSNode(x,y,t, lon, lat);
            nrOfNodes++;
            
            // set minX, minY, maxY and maxX
            if(x<minX) minX=x;
            if(y<minY) minY=y;
            if(x>maxX) maxX=x;
            if(y>maxY) maxY=y;
        }
    }

    /**
     * add GPSNode to GPSPath
     * @param gpsNode
     */
    public void addNode(GPSNode gpsNode){
    	// add GPS point by creating new one and copy given values
    	addNode(gpsNode.getX(), gpsNode.getY(), gpsNode.getTimestamp(), gpsNode.getLon(), gpsNode.getLat());
    }

    /**
     * get number of GPSNodes
     * @return NrOfNodes
     */
    public int getNrOfNodes(){
        return nrOfNodes;
    }

    /**
     * get time stamp of GPSNode in array with index i
     * returns 0 if GPSNode i does not exist
     * @param i
     * @return (long) time stamp
     */
    public long getNodeTimestamp(int i){
        if (i<0) i=0;
        if (i<nrOfNodes) return nodes[i].getTimestamp();
        return 0;
    }

    /**
     * get X-Position of GPSNode i
     * returns 0 if GPSNode i does not exist
     * @param i
     * @return (int) X-Pos
     */
    public double getNodeX(int i){
        if (i<0) i=0;
        if (i<nrOfNodes) return nodes[i].getX();
        return 0;
    }

    /**
     * get Y-Position of GPSNode i
     * returns 0 if GPSNode i does not exist
     * @param i
     * @return (int) Y-Pos
     */
    public double getNodeY(int i){
        if (i<0) i=0;
        if (i<nrOfNodes) return nodes[i].getY();
        return 0;
    }
    
    /**
     * get first GPSNode in GPSPath
     * returns null if no Nodes exist
     * @return first GPSNode
     */
    public GPSNode getFirst(){
        if (nrOfNodes > 0) return nodes[0];
        return null;
    }

    /**
     * get last GPSNode in GPSPath
     * returns null if no Nodes exist
     * @return last GPSNode
     */
    public GPSNode getLast(){
        if (nrOfNodes > 0)return nodes[nrOfNodes-1];
        return null;
    }

    /**
     * get GPSNode i
     * returns null if no Nodes exist
     * @param i
     * @return GPSNode i
     */
    public GPSNode getNode(int i){
        if (i<0) i=0;
        if (i<nrOfNodes) return nodes[i];
        return null;
    }
    /**
     * get Node at Position (x,y)
     * returns null if Node does not exist
     * @param x
     * @param y
     * @return GPSNode
     */
    public GPSNode getNode(double x, double y){
        int i=0;
        while (i<nrOfNodes){
            // check if node already exists
            if ((x==nodes[i].getX())&&(y==nodes[i].getY())) return nodes[i];
            i++;
        }
        // node does not exist
        return null;
    }

    public double getMinX(){
        return minX;
    }

    public double getMinY(){
        return minY;
    }

    public double getMaxX(){
        return maxX;
    }

    public double getMaxY(){
        return maxY;
    }
    
    public double getMinLon() {
    	return Coordinates.getGeoPos(minX, minY).getLongitude();
    }
    
    public double getMinLat() {
    	// take max x/y values for calculation, cause map x-coordinate starts at top
    	return Coordinates.getGeoPos(maxX, maxY).getLatitude();
    }
    
    public double getMaxLon() {
    	return Coordinates.getGeoPos(maxX, maxY).getLongitude();
    }
    
    public double getMaxLat() {
    	// take max x/y values for calculation, cause map x-coordinate starts at top
    	return Coordinates.getGeoPos(minX, minY).getLatitude();
    }
    
    public GeoPosition getStartGeoPos() {
    	return Coordinates.getGeoPos(nodes[0].getX(), nodes[0].getY());
    }
    
    public GeoPosition getMiddleGeoPos() {
    	return Coordinates.getMiddleGeoPos(minX, minY, maxX, maxY);
    }
}
