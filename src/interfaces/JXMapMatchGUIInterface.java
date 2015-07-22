package interfaces;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.EventListener;

import org.jdesktop.swingx.JXMapKit;

/**
 * 
 * @author Daniel Sathees Elmo
 * 
 * This interface helps to keep track of current status by 
 * using call-back functions specially for JXMapMatchGUI
 */

public interface JXMapMatchGUIInterface extends StatusUpdate{
	public JXMapKit getJXMapKit();									// get reference to JXMapKit
	
	public void setTileFactory(String baseURL, String xParam,		// set custom tile factory
							   String yParam, String zParam,
							   int minZoom,	int maxZoom, 
							   int totalZoom, int tileSize,
							   boolean defaultXOrientation,
							   boolean defaultYOrientation);
	
	public void setTileFactory(String url, String xParam, 			// set custom tile factory with default values
							   String yParam, String zParam);		
	
	public void setEventListener(EventListener eventListener);		// register events with given event listener
	
	public void enableGUI(boolean enable);							// enable/disable GUI
	
	public void setGPSButtonText(String text);						// set caption of GPS trace load button
	public void setStreetMapButtonText(String text);				// set caption of street map load button
	
	public void makeDrawStreetMapAvailable(boolean enable);			// enable/disable the possibility to choose if street map should be drawn 
	public void makeDrawGPSTraceAvailable(boolean enable);			// enable/disable the possibility to choose if GPS trace should be drawn
	public void makeDrawNRouteAvailable(boolean enable);			// enable/disable the possibility to choose if matched route should be drawn
	public void makeDrawSelectedRouteAvailable(boolean enable);		// enable/disable the possibility to choose if selected route should be drawn
	
	public void makeSelectRouteModeAvailable(boolean enable);		// enable/disable the possibility to (de-)activate the selected route mode
	
	public void makeNRouteAlgorithmModeAvailable(boolean enable);	// enable/disable the possibility to use N route algorithm
	public void makeNRouteRouteAvailable(boolean enable);			// enable/disable "Routing" button
	public void makeNRouteMatchAvailable(boolean enable);			// enable/disable "Match" button
	public void makeNRouteSaveAvailable(boolean enable);			// enable/disable "Save" button
	public void makeNRouteExportAvailable(boolean enable);			// enable/disable "Export" (and Normalize) button
	public void makeNRouteResetAvailable(boolean enable);			// enable/disable "Reset" button
	public void makeNRouteButtonsAvaiable(boolean route,			// enable/disable button collection method 
							boolean match, boolean save,
							boolean export, boolean reset);
	
	public void setDrawStreetMap(boolean set);						// set if street map should be drawn
	public void setDrawGPSTrace(boolean set);						// set if GPS trace should be drawn
	public void setDrawNRoute(boolean set);							// set if matched route should be drawn
	public void setDrawSelectedRoute(boolean set);					// set if selected route should be drawn
	public boolean getDrawStreetMap();								// get user choice if street map should be drawn
	public boolean getDrawGPSTrace();								// get user choice if street map should be drawn
	public boolean getDrawNRoute();									// get user choice if street map should be drawn
	public boolean getDrawSelectedRoute();							// get user choice if street map should be drawn
	
	public void setNRouteAlgorithmMode(boolean set);				// set N route algorithm mode
	void setNRouteAlgorithmMode(boolean set,						// set N route algorithm mode, decide if GPS trace can be load
				boolean allowLoadGPSTrace);
	public boolean getNRouteAlgorithmMode();						// get if n route algorithm mode is currently activated
	
	public void setProjectNMatch(boolean enable);					// set if N matched GPS nodes should be projected/stretched on matched link
	public boolean getProjectNMatch();								// get if user enabled this feature
	
	public void setReorderNMatch(boolean enable);					// set if N matched GPS nodes should be reordered according the driving direction
	public boolean getReoderNMatch();								// get if user enabled this feature
	
	public void setNormalizeNMatchedGPSTimeStamp(boolean enable);	// set if time stamp of N matched GPS points should be normalized
	public boolean getNormalizeNMatchedGPSTimeStamp();				// get user choice if time stamps of N matched GPS points should 

	public void setSelectRouteMode(boolean set);					// set route selecting mode
	public boolean getSelectRouteMode();							// get if route selecting mode is currently activated
	
	public void setNormalizeGPSTimeStamp(boolean enable);			// set if time stamp of matched GPS points should be normalized
	public boolean getNormalizeGPSTimeStamp();						// get user choice if time stamps of matched GPS points should be normalized 
	
	public void setNRouteSize(int nRouteSize);						// set size of n route container
	public int getNRouteSize();										// get size of n route container
	
	public void setNRouteThreshold(double threshold);				// set threshold for reaching intersection (N Route Algorithm)
	public double getNRouteThreshold();								// get threshold value for reaching/passing intersection
	
	public boolean setNRouteAlgorithmState(String state);			// set GUI state that N route algorithm is running/paused or recessed
	
	public boolean setMatchGPStoNRouteAlgorithmState(String state);	// set GUI state that match GPS to N route algorithm is running/paused or recessed
	
	public void setSelectedNRoute(int chosenNRoute);				// set index of current selected n route inside container
	public int getSelectedNRoute();									// get index of current selected n route inside container
	
	public void updateNRouteSettings();								// forces to update n route algorithm settings
	
	public Point getTranslatedMousePos(MouseEvent mouseEvent);		// get matched mouse position onto JXMap
	public int getTranslatedMousePosX(MouseEvent mouseEvent);		// get matched x mouse position onto JXMap
	public int getTranslatedMousePosY(MouseEvent mouseEvent);		// get matched y mouse position onto JXMap

	
}
