package gps;


/**
 * @author Daniel Sathees Elmo
 * 
 * class represents an GPS Point, saves also info about its matched position and color for
 * drawing 
 */

//TODO: don't save matched/draw values here, instead create separate class with reference to this node 

public class GPSNode {
	
	// time stamp
    private long timestamp = 0;
    private double x = 0;
    private double y = 0;
    private double lat = 0;
    private double lon = 0;
    
    public int status = 0;
    
    /**
     * create new GPSNode at position (x,y) with timestamp t
     * @param x
     * @param y
     * @param timestamp
     */
    public GPSNode(double x , double y, long timestamp, double lon, double lat){
    	// save position and timestamp
        this.x = x;
        this.y = y;
        this.timestamp = timestamp;	
        this.lon = lon;
        this.lat = lat;        
    }
    
    /**
     * set GPSNode's X-Position
     * @param x
     */
    public void setX(double x){
        this.x = x;
    }

    /**
     * set GPSNode's Y-Position
     * @param y
     */
    public void setY(double y){
        this.y = y;
    }

    /**
     * get GPSNode's X-Position
     * @return (int) X-Position
     */
    public double getX(){
        return x;
    }
    
    /**
     * get GPSNode's X-Position
     * @return (int) Y-Position
     */
    public double getY(){
        return y;
    }
    
    /**
     * get GPSNode's Lon-Position
     * @return (double) Lon-Position
     */
    public double getLon(){
        return lon;
    }
    
    /**
     * get GPSNode's Lat-Position
     * @return (double) lat-Position
     */
    public double getLat(){
        return lat;
    }
    
    /**
     * set GPSNode's timestamp
     * @param timestamp
     */
    public void setTimestamp(long timestamp){
        this.timestamp = timestamp;
    }

    /**
     * get GPSNode's timestamp
     * @return (long) timestamp
     */
    public long getTimestamp(){
        return timestamp;
    }
}
