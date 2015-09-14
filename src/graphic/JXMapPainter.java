/*
 * This class is used to draw Routes, GPS Data, .... into JXMapKit
 */

package graphic;

import myClasses.myOSMMap;
import myClasses.myOSMNode;
import myClasses.myOSMWay;
import myClasses.myOSMWayPart;

import org.jdesktop.swingx.*;

import algorithm.MatchedGPSNode;
import algorithm.MatchedLink;
import algorithm.MatchedNLink;
import algorithm.ReorderedMatchedGPSNode;
import route.*;
import gps.GPSTrace;

import java.awt.*;
import java.util.Vector;

/**
 * @author Tobias
 * @author Daniel Sathees Elmo
 * @author Adrian Skuballa
 */

public class JXMapPainter {

    /**
     * draw GPSPath path on Graphics g (Color: color)
     * GPS Point n is highlighted
     * use JXMapView map to get zoom
     * @param g
     * @param map
     * @param gpsNodesToMatch
     * @param gpsColor
     * @param n
     */
    public void drawGPSPath(Graphics2D g,JXMapViewer map, Vector<MatchedGPSNode> gpsNodesToMatch, MatchedGPSNode gpsNextNodeToMatch, Color gpsColor, Color gpsNextToMatchColor,  double zoomFactor){
        // create graphics
        g = (Graphics2D) g.create();
        //convert from viewport to world bitmap
        Rectangle rect = map.getViewportBounds();
        //System.out.println(rect.getX() + ", " + rect.getY() + "  " + rect.getWidth() + ", " + rect.getHeight());
        g.translate(-rect.x, -rect.y);

        //do the drawing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // set brush
        g.setStroke(new BasicStroke(1));
        
    	Rectangle Rectangle_getViewportBounds = map.getViewportBounds();
    	double x_min = Rectangle_getViewportBounds.getMinX();
    	double y_min = Rectangle_getViewportBounds.getMinY();
    	double x_max = Rectangle_getViewportBounds.getMaxX();
    	double y_max = Rectangle_getViewportBounds.getMaxY();

    	x_min = (x_min + 1) * zoomFactor;
    	y_min = (y_min + 1) * zoomFactor;
    	x_max = (x_max + 1) * zoomFactor;
    	y_max = (y_max + 1) * zoomFactor;

        //draw every GPS node of trace
        for(MatchedGPSNode matchedGPSNode : gpsNodesToMatch){
        	//set color
        	g.setColor(matchedGPSNode.getColor());
            // draw rect for every GPS Point
            // devide x,y coordinates by 2^(zoom-1) to fit to current zoom
            g.drawRect((int)(matchedGPSNode.getDrawX()/zoomFactor),
                       (int)(matchedGPSNode.getDrawY()/zoomFactor), 3, 3);
        }
        
        if (gpsNextNodeToMatch != null) {
        	// set color for next GPS node to match
        	g.setColor(gpsNextToMatchColor);
        	// next GPS node to match
        	g.drawRect((int) (gpsNextNodeToMatch.getDrawX()/zoomFactor),
        		   (int) (gpsNextNodeToMatch.getDrawY()/zoomFactor), 3, 3);
        }
        
        // release graphics
        g.dispose();
    }
    
    public void drawGPSPath(Graphics2D g,JXMapViewer map, GPSTrace gpsTrace, Color gpsColor, double zoomFactor){
        // create graphics
        g = (Graphics2D) g.create();
        //convert from viewport to world bitmap
        Rectangle rect = map.getViewportBounds();
        //System.out.println(rect.getX() + ", " + rect.getY() + "  " + rect.getWidth() + ", " + rect.getHeight());
        g.translate(-rect.x, -rect.y);

        //do the drawing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // set brush
        g.setStroke(new BasicStroke(1));
        
    	Rectangle Rectangle_getViewportBounds = map.getViewportBounds();
    	double x_min = Rectangle_getViewportBounds.getMinX();
    	double y_min = Rectangle_getViewportBounds.getMinY();
    	double x_max = Rectangle_getViewportBounds.getMaxX();
    	double y_max = Rectangle_getViewportBounds.getMaxY();
    	
    	x_min = (x_min + 1) * zoomFactor;
    	y_min = (y_min + 1) * zoomFactor;
    	x_max = (x_max + 1) * zoomFactor;
    	y_max = (y_max + 1) * zoomFactor;
        
    	double x;
    	double y;
    	
        //draw every GPS node of trace
        for(int i=gpsTrace.getNrOfNodes()-1; i>=0; i--){
        	
			x = gpsTrace.getNodeX(i);
			y = gpsTrace.getNodeY(i);
        	if ( (x_min <= x && x <= x_max && y_min <= y && y <= y_max) ) {
    					
				//set color
            	if (gpsTrace.getNodeStatus(i) == 1) {
            		g.setColor(Color.RED);
            	} else {
            		g.setColor(gpsColor);
            	}
            	
                // draw rect for every GPS Point
                // devide x,y coordinates by 2^(zoom-1) to fit to current zoom
                g.drawRect((int) (x/zoomFactor), (int)(y/zoomFactor), 3, 3);
        	}

        }
      
        // release graphics
        g.dispose();
    }
    
    static int cc = 0;
    
    public void drawNRoute(Graphics2D g, JXMapViewer map, Vector<NRoute> nRoutes, Color nRouteColor, double zoomFactor) {
    	
    	if (nRoutes == null) {
    		return;
    	}
    	
    	// create graphics
        g = (Graphics2D) g.create();
        //convert from view port to world bitmap
        Rectangle rect = map.getViewportBounds();
        g.translate(-rect.x, -rect.y);
        
        // do the drawing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // set color
        g.setColor(nRouteColor);
        
        // set brush
        g.setStroke(new BasicStroke(3));
        
        g.setColor(Color.YELLOW);
        
    	Rectangle Rectangle_getViewportBounds = map.getViewportBounds();
    	double x_min = Rectangle_getViewportBounds.getMinX();
    	double y_min = Rectangle_getViewportBounds.getMinY();
    	double x_max = Rectangle_getViewportBounds.getMaxX();
    	double y_max = Rectangle_getViewportBounds.getMaxY();
    	
    	x_min = (x_min + 1) * zoomFactor;
    	y_min = (y_min + 1) * zoomFactor;
    	x_max = (x_max + 1) * zoomFactor;
    	y_max = (y_max + 1) * zoomFactor;
        
		// draw route for each route
    	
    	myOSMWayPart[] lastWayParts = new myOSMWayPart[nRoutes.size()];
    	
        for (int i=0; i < nRoutes.size(); i++ ){
        	
        	//if (i == cc) 
        	{
        		NRoute nRoute = nRoutes.get(i);
        		
        		//System.out.println(i + "/" + nRoutes.size() + " | " + nRoute.getLastOSMWayPart().get(0).parentWay.id + " | " + nRoute.getScore());
        
        		int indexWP = 0;
        		int lastIndexWP = nRoute.getNRouteLinks().size() - 1;
        		        		
    			for (MatchedLink nRouteLink : nRoute.getNRouteLinks()) {
    				    				
    				myOSMWayPart wp = nRouteLink.getStreetLink();
    						
    				if (indexWP != lastIndexWP) {
        				// draw line for every link
        				// devide x,y coordinates by 2^(zoom-1) to fit to current zoom
        				g.drawLine((int) (wp.startNode.x / zoomFactor),
        						(int) (wp.startNode.y / zoomFactor),
        						(int) (wp.endNode.x / zoomFactor),
        						(int) (wp.endNode.y / zoomFactor));    					
    				} else {
    					lastWayParts[i] = wp;
    				}
    				
    				indexWP++;
    			}
        	}
        }
        
        for (int i=0; i < nRoutes.size(); i++ ){
        	lastWayParts[i] = nRoutes.get(i).getNRouteLinks().lastElement().getStreetLink();
        }
        
        g.setColor(Color.BLACK);
       
        for (int i=lastWayParts.length-1; 0 <= i; i-- ){
        	
        	//if (i == cc) 
        	{
        		myOSMWayPart wp = lastWayParts[i];
        		
        		if (i == 0) 
        		{
        			g.setColor(Color.RED);
        		}
        		
        		g.drawLine((int) (wp.startNode.x / zoomFactor),
						(int) (wp.startNode.y / zoomFactor),
						(int) (wp.endNode.x / zoomFactor),
						(int) (wp.endNode.y / zoomFactor)); 
        	}
       
        }

        if (cc < nRoutes.size()) {
        	//System.out.println(cc + "/" + nRoutes.size() + " nRoute objID: " + nRoutes.get(cc).objID);        	
        }
        
        if (nRoutes.size() > 0) {
    		cc++;
    		cc = cc % nRoutes.size();        	
        }

    }
    
    public void drawSelectedNRoute(Graphics2D g, JXMapViewer map, SelectedNRoute selectedNRoute, Color nRouteColor, Color selectableColor, Color deletableColor, double zoomFactor) {
     	// create graphics
        g = (Graphics2D) g.create();
        // convert from view port to world bitmap
        Rectangle rect = map.getViewportBounds();
        g.translate(-rect.x, -rect.y);

        // do the drawing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // set brush
        g.setStroke(new BasicStroke(4));
        
        // set color for normal n route links
        g.setColor(nRouteColor);
        
    	Rectangle Rectangle_getViewportBounds = map.getViewportBounds();
    	double x_min = Rectangle_getViewportBounds.getMinX();
    	double y_min = Rectangle_getViewportBounds.getMinY();
    	double x_max = Rectangle_getViewportBounds.getMaxX();
    	double y_max = Rectangle_getViewportBounds.getMaxY();
    	
    	x_min = (x_min + 1) * zoomFactor;
    	y_min = (y_min + 1) * zoomFactor;
    	x_max = (x_max + 1) * zoomFactor;
    	y_max = (y_max + 1) * zoomFactor;
        
        //draw selected N route (Start)
		for (myOSMWayPart nRouteLink : selectedNRoute.getNRouteLinksStart()) {
			// draw line for every link
			// devide x,y coordinates by 2^(zoom-1) to fit to current zoom

			g.drawLine((int) (nRouteLink.getStartX() / zoomFactor),
					   (int) (nRouteLink.getStartY() / zoomFactor),
					   (int) (nRouteLink.getEndX() / zoomFactor),
					   (int) (nRouteLink.getEndY() / zoomFactor));
		}
		
		//draw selected N route (End)
		for (myOSMWayPart nRouteLink : selectedNRoute.getNRouteLinksEnd()) {
			// draw line for every link
			// devide x,y coordinates by 2^(zoom-1) to fit to current zoom
			g.drawLine((int) (nRouteLink.getStartX() / zoomFactor),
					   (int) (nRouteLink.getStartY() / zoomFactor),
					   (int) (nRouteLink.getEndX() / zoomFactor),
					   (int) (nRouteLink.getEndY() / zoomFactor));
		}
		
		// draw selectable street link 
		g.setColor(selectableColor);
		
		myOSMWayPart selectableLink = selectedNRoute.getSelectableLink();
		
		if (selectableLink != null) {
			g.drawLine((int) (selectableLink.getStartX() / zoomFactor),
					   (int) (selectableLink.getStartY() / zoomFactor),
					   (int) (selectableLink.getEndX() / zoomFactor),
					   (int) (selectableLink.getEndY() / zoomFactor));
		}
		
		// draw selectable street link 
		g.setColor(Color.RED);
				
		myOSMWayPart deletableStreetLink = selectedNRoute.getDeletableLink();
				
		if (deletableStreetLink != null) {
			g.drawLine((int) (deletableStreetLink.getStartX() / zoomFactor),
					   (int) (deletableStreetLink.getStartY() / zoomFactor),
					   (int) (deletableStreetLink.getEndX() / zoomFactor),
					   (int) (deletableStreetLink.getEndY() / zoomFactor));
		}
				
				

    }
    
    public void drawSelectedRoute(Graphics2D g, JXMapViewer map, SelectedRoute selectedRoute, Color selectableColor, Color multiSelectableColor, Color selectedColor, Color nonMatchedColor, double zoomFactor){
    	// create graphics
        g = (Graphics2D) g.create();
        // convert from view port to world bitmap
        Rectangle rect = map.getViewportBounds();
        g.translate(-rect.x, -rect.y);

        // do the drawing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // set brush
        g.setStroke(new BasicStroke(4));

    	Rectangle Rectangle_getViewportBounds = map.getViewportBounds();
    	double x_min = Rectangle_getViewportBounds.getMinX();
    	double y_min = Rectangle_getViewportBounds.getMinY();
    	double x_max = Rectangle_getViewportBounds.getMaxX();
    	double y_max = Rectangle_getViewportBounds.getMaxY();
    	
    	x_min = (x_min - 1) * zoomFactor;
    	y_min = (y_min - 1) * zoomFactor;
    	x_max = (x_max + 1) * zoomFactor;
    	y_max = (y_max + 1) * zoomFactor;
        
        //draw selected route
        if (!selectedRoute.isEmpty()){
        	//get selected street links, draw them
        	for(myOSMWayPart selectedStreetLink : selectedRoute.getSelectedLinks()){
                // set color
                g.setColor((selectedStreetLink.isLastMatched() ? selectedColor : nonMatchedColor));                
        		// draw line for every link
        		// devide x,y coordinates by 2^(zoom-1) to fit to current zoom
        		g.drawLine( (int)(selectedStreetLink.getStartX()/zoomFactor),
        				    (int)(selectedStreetLink.getStartY()/zoomFactor),
        					(int)(selectedStreetLink.getEndX()/zoomFactor),
        					(int)(selectedStreetLink.getEndY()/zoomFactor));
        	}
        }

        //draw selectable link
        if (selectedRoute.selectableStreetLink()){
        	//System.out.println(selectedRoute.getSelectableStreetLink().getSelectCounter());
        	//set color
        	g.setColor( (selectedRoute.getSelectableLink().getSelectCounter() < 1) ? selectableColor : multiSelectableColor); 
        	g.drawLine( (int) (selectedRoute.getSelectableLink().getStartX()/zoomFactor),
    				    (int) (selectedRoute.getSelectableLink().getStartY()/zoomFactor),
    				    (int) (selectedRoute.getSelectableLink().getEndX()/zoomFactor),
    				    (int) (selectedRoute.getSelectableLink().getEndY()/zoomFactor));
        }
        
        //release graphics
        g.dispose();
    }
    
    static int c = 0;
    
    /**
     * draw StreetLinks of StreetMap street on Graphics g (Color: color)
     * use JXMapView map to get zoom
     * @param g
     * @param map
     * @param streetMap
     * @param color
     */
    public void drawStreetMap(Graphics2D g, JXMapViewer map, Color color, double zoomFactor, myOSMMap myMap){
    	
    	if (myMap != null){
        	if (myMap.ways != null){
        		c++;
        		if (myMap.ways.size() != 0) {
            		c = c % myMap.ways.size();
        		}
        	}
    	}
		
        // create graphics
        g = (Graphics2D) g.create();
        //convert from viewport to world bitmap
        Rectangle rect = map.getViewportBounds();
        g.translate(-rect.x, -rect.y);

        //do the drawing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    	
        // set brush
        g.setStroke(new BasicStroke(3));
        // set color
        g.setColor(color);

        //Random random = new Random();
    	
    	Rectangle Rectangle_getViewportBounds = map.getViewportBounds();
    	double x_min = (int) Rectangle_getViewportBounds.getMinX();
    	double y_min = (int) Rectangle_getViewportBounds.getMinY();
    	double x_max = (int) Rectangle_getViewportBounds.getMaxX();
    	double y_max = (int) Rectangle_getViewportBounds.getMaxY();
    	
    	x_min = (x_min + 1) * zoomFactor;
    	y_min = (y_min + 1) * zoomFactor;
    	x_max = (x_max + 1) * zoomFactor;
    	y_max = (y_max + 1) * zoomFactor;
    	
    	if (myMap != null ) {
        	
    		myOSMNode n1;
    		myOSMNode n2;
    		myOSMWayPart wp;

    		for (int i=0; i < myMap.ways.size(); i++) {

				myOSMWay w = myMap.ways.get(i);

	    			for (int j=0; j < w.WayParts.length; j++) {

	    				wp = w.WayParts[j];
	    				n1 = wp.startNode;
	    				n2 = wp.endNode;

	    				if ( (x_min <= n1.x && n1.x <= x_max && y_min <= n1.y && n1.y <= y_max) 
	    					|| (x_min <= n2.x && n2.x <= x_max && y_min <= n2.y && n2.y <= y_max)
	    					) {
	    					
//	        				if (c == i) 
	        				{
		            			g.drawLine((int)(n1.x / zoomFactor), (int)(n1.y / zoomFactor), (int)(n2.x / zoomFactor), (int)(n2.y / zoomFactor));	        					
//		        				System.out.println(w.id);
	        				}
	    				}
	    			}
    		}

    		g.dispose();
        }
    }
    	
    /**
     * draw StreetNodes of StreetMap street on Graphics g (Color: color)
     * use JXMapView map to get zoom
     * @param g
     * @param map
     * @param street
     * @param color
     */
    public void drawStreetNodes(Graphics2D g,JXMapViewer map, Color color, double zoomFactor){
        // create graphics
        g = (Graphics2D) g.create();
        //convert from viewport to world bitmap
        Rectangle rect = map.getViewportBounds();
        
        g.translate(-rect.x, -rect.y);

        //do the drawing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // set brush
        g.setStroke(new BasicStroke(1));
        // set color
        g.setColor(color);

        /*
        for(int i=0; i<street.getNrOfNodes();i++){
            // draw point for every node
            // devide x,y coordinates by 2^(zoom-1) to fit to current zoom
            g.drawRect((int)(street.getNodeX(i)/zoomFactor),
                    (int)(street.getNodeY(i)/zoomFactor),2,2);
        }
        */
        g.dispose();
    }

	public void drawMatchedGPStoNRoute(Graphics2D g, JXMapViewer map,
			Vector<MatchedNLink> matchedNLinks,
			Vector<ReorderedMatchedGPSNode> matchedGPSNodes, double zoomFactor) {
		
		
		// create graphics
        g = (Graphics2D) g.create();
        // convert from view port to world bitmap
        Rectangle rect = map.getViewportBounds();
        g.translate(-rect.x, -rect.y);

        // do the drawing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // set brush
        g.setStroke(new BasicStroke(4));
        
    	Rectangle Rectangle_getViewportBounds = map.getViewportBounds();
    	double x_min = Rectangle_getViewportBounds.getMinX();
    	double y_min = Rectangle_getViewportBounds.getMinY();
    	double x_max = Rectangle_getViewportBounds.getMaxX();
    	double y_max = Rectangle_getViewportBounds.getMaxY();
    	
    	x_min = (x_min + 1) * zoomFactor;
    	y_min = (y_min + 1) * zoomFactor;
    	x_max = (x_max + 1) * zoomFactor;
    	y_max = (y_max + 1) * zoomFactor;
        
        //draw selected N route (Start)
		for (MatchedNLink matchedNLink : matchedNLinks) {

        	if (matchedNLink.isMatched()) {
            	g.setColor(Color.black);        		
        	} else {
            	g.setColor(Color.white);
        	}

			// draw line for every link
			// devide x,y coordinates by 2^(zoom-1) to fit to current zoom
			g.drawLine((int) (matchedNLink.getStreetLink().getStartX() / zoomFactor),
					   (int) (matchedNLink.getStreetLink().getStartY() / zoomFactor),
					   (int) (matchedNLink.getStreetLink().getEndX() / zoomFactor),
					   (int) (matchedNLink.getStreetLink().getEndY() / zoomFactor));
		}
		
		 //draw every GPS node of trace
        for(ReorderedMatchedGPSNode matchedGPSNode : matchedGPSNodes){

        	// set brush
            g.setStroke(new BasicStroke(1));

        	//set color
        	//g.setColor(hasIndexChanged ? Color.MAGENTA : matchedGPSNode.getColor());

        	if (matchedGPSNode.isMatched()) {
            	g.setColor(Color.GREEN);        		
        	} else {
            	g.setColor(Color.RED);
        	}

            // draw rect for every GPS Point
            // devide x,y coordinates by 2^(zoom-1) to fit to current zoom
            g.drawRect((int)(matchedGPSNode.getDrawX()/zoomFactor),
                       (int)(matchedGPSNode.getDrawY()/zoomFactor), 3, 3);
            
        }
        
        g.dispose();
	}
}
