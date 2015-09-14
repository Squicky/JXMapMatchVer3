package jxmapmatch;

import gps.GPSNode;
import gps.GPSTrace;
import gps.GPSTraceStreamer;
import graphic.JXMapPainter;
import interfaces.JXMapMatchGUIInterface;
import interfaces.MatchingGPSObject;
import interfaces.StatusUpdate;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import logging.Logger;
import myClasses.myDataset;
import myClasses.myOSMMap;
import myClasses.mySaveToFile;

import org.jdesktop.swingx.JXMapKit;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.painter.Painter;

import algorithm.GPSToLinkMatcher;
import algorithm.MatchGPStoNRouteAlgorithm;
import algorithm.NRouteAlgorithm;
import algorithm.ReorderedMatchedGPSNode;
import route.SelectedNRoute;
import dialogelements.JFileDialog;
import static algorithm.NRouteAlgorithm.*;
import static algorithm.MatchGPStoNRouteAlgorithm.*;

public class JXMapMatchController implements ActionListener,
											 ChangeListener,
											 ItemListener,
											 MouseListener,
											 MouseMotionListener,
											 KeyListener,
											 StatusUpdate{
	
	// reference to GUI
	private JXMapMatchGUIInterface jxMapMatchGUI;
	
	// references to JXMapKit and its components
	private JXMapKit jxMapKit;
	private JXMapViewer jxMapViewer;
	private JXMapPainter jxMapPainter;
	
	// open/save dialogs
	private JFileDialog jFileOpenDialogGraph; 
	private JFileDialog jFileOpenDialogGPS;
	//private JFileDialog jFileOpenNRoute;
	private JFileDialog jFileSaveNRoute;
	private JFileDialog jFileSaveDialogMatchedGPS;
	
	// define file extensions & descriptions for filter
	private String[] gpsFileExtensions = {"log", "txt", "gpx"};
	private String[] gpsFileDescriptions = { "gps trace log (*.log)", "Text based trace file (*.txt)", "GPX formated xml trace file (*.gpx)"};
	
	//street map and GPS trace
	private GPSTrace gpsTrace;
	//private StreetMap streetMap;
	public myOSMMap myMap;
	
	// matching classes
	private GPSToLinkMatcher gpsToLinkMatcher;
	private NRouteAlgorithm nRouteAlgorithm;
	private MatchGPStoNRouteAlgorithm matchGPStoNRouteAlgorithm; 
	
	// store map file path for a certain N route
	//private String nRouteMapFilePath;
	
	// for adjusting N route
	private SelectedNRoute selectedNRoute;
	
	// control if data should be drawn
	private boolean drawStreetMap = false;
	private boolean drawGPSTrace = false;
	private boolean drawNRoute = false;
	private boolean drawSelectedRoute = false;
	private boolean drawMatchedGPStoNRoute = false;
	
	// set mode where the N route algorithm runs
	private boolean isNRouteAlgorithmMode = false;
	
	// set mode where a route can be clicked
	private boolean selectedRouteMode = false;
	
	// set mode where a n route can be adjusted
	private boolean selectedNRouteMode = false;
	
	// mouse dragging state
	private boolean mouseDragged = false;
	
	// enable open GPS Trace button for N Route Algorithm
	// if selected N Route exists
	private boolean allowNRouteLoadGPSTrace = false;
	// flag to notice that we just want to load new GPS trace
	// without reseting Selected N Route
	private boolean isGPSTraceForSelectedNRoute = false;
	
	// Color constants for drawing
	//public static Color STREET_MAP_COLOR = Color.RED;
	public static Color STREET_MAP_COLOR = new Color(150,150,255);
	//public static Color STREET_MAP_COLOR = Color.black;
	
	public static Color GPS_TRACE_COLOR = Color.BLUE;
	public static Color GPS_TO_MATCH_COLOR = Color.MAGENTA;
	public static Color SELECTABLE_LINK_COLOR = Color.YELLOW;
	public static Color MULTI_SELECTABLE_LINK_COLOR = Color.GREEN;
	public static Color SELECTED_LINK_COLOR = Color.ORANGE;
	public static Color NON_MATCHED_LINK_COLOR = Color.LIGHT_GRAY;
	public static Color N_ROUTE_LINK_COLOR = new Color(0, 0, 255);
	
	public static Color SELECTABLE_N_ROUTE_COLOR = Color.GREEN;
	public static Color DELETABLE_N_ROUTE_COLOR = Color.ORANGE;
	public static Color GPS_TO_N_ROUTE_UNMATCHED_LINK_COLOR = Color.WHITE;
	public static Color GPS_TO_N_ROUTE_MATCHED_LINK_COLOR = Color.BLACK;
	public static Color GPS_TO_N_ROUTE_UNMATCHED_NODE_COLOR = Color.BLUE;
	public static Color GPS_TO_N_ROUTE_MATCHED_NODE_COLOR = Color.CYAN;
	
	// constants for controlling background loading process
	private static final String CLIENT_FILE_DIALOG = "Filedialog";
	private static final String CLIENT_ARGUMENTS = "Arguments";
	private static final String MAP_TO_LOAD = "Map to load";
	private static final String MAP_LOADED = "Map loaded";
	private static final String NROUTE_ARGUMENTS = "N route arguments";
	private static final String NROUTE_MAP_TO_LOAD = "N route map to load";
	private static final String NROUTE_MAP_LOADED = "N route map loaded";
	//private static final String NROUTE_LOADED = "N route loaded";
	//private static final String GPS_TRACE_LOADED = "GPS trace loaded";
	private static final int MAP_FILE_INDEX = 0;
	//private static final int NROUTE_FILE_INDEX = 1;
	private static final int GPS_TRACE_FILE_INDEX = 1;
		
	// store arguments here
	private String[] arguments;
	
	/**
	 * constructor needs JXMapMatchInterface in order to control the GUI
	 * @param jxMapMatchGUI
	 * @param args arguments passed by user (can load automatically map and trace)
	 */
	public JXMapMatchController(JXMapMatchGUIInterface jxMapMatchGUI, String[] args) {
		// save reference to passed arguments
		this.arguments = args;
		
		// save (interface-)reference to GUI
		this.jxMapMatchGUI = jxMapMatchGUI;
		
		/* initialize GUI */
		
		// first set state of drawing components
		jxMapMatchGUI.makeDrawStreetMapAvailable(drawStreetMap);
		jxMapMatchGUI.makeDrawGPSTraceAvailable(drawGPSTrace);
		jxMapMatchGUI.makeDrawNRouteAvailable(drawNRoute);
		jxMapMatchGUI.makeDrawSelectedRouteAvailable(drawSelectedRoute);
		
		// then set state of matching components, disabled by default cause
		// no maps and traces are loaded at program start
		jxMapMatchGUI.makeSelectRouteModeAvailable(false);
		jxMapMatchGUI.makeNRouteAlgorithmModeAvailable(true);
		
		// set event listener of GUI
		jxMapMatchGUI.setEventListener(this);
		
		// get JXMapKit, JXMapViewer, create new map painter,
		// which draws all graphics over JXMapKit and initialize it
		jxMapKit = jxMapMatchGUI.getJXMapKit();
		jxMapViewer = jxMapKit.getMainMap();
		jxMapPainter = new JXMapPainter();
		//TODO Norbert: jxMapMatchGUI.setTileFactory(...);
		initMapPainter(jxMapKit, jxMapPainter);
		
		// initialize file dialogs with file extension filters
		String [] s1 = {"xml", "osm"};
		String [] s2 = {"OpenStreetMap (*.xml)", "OpenStreetMap (*.osm)"};
		//jFileOpenDialogGraph = new JFileDialog((Component) jxMapMatchGUI, "large", "Large routing graph (*.large)");
		jFileOpenDialogGraph = new JFileDialog((Component) jxMapMatchGUI, s1, s2, "C:\\priv\\uni\\MA\\osm_Maps");
		jFileOpenDialogGPS = new JFileDialog((Component) jxMapMatchGUI, gpsFileExtensions , gpsFileDescriptions, "C:\\priv\\uni\\MA\\TRACES\\TRACES.r59619");
		//jFileOpenNRoute = new JFileDialog((Component) jxMapMatchGUI, "nroute", "N route (*.nroute)");
		jFileSaveNRoute = new JFileDialog((Component) jxMapMatchGUI, "nroute", "N route (*.nroute)");
		String [] s3 = {"csv"};
		String [] s4 = {"character-separated valuess (*.csv)"};
		jFileSaveDialogMatchedGPS = new JFileDialog((Component) jxMapMatchGUI, s3, s4, "C:\\priv\\Uni\\MA");
		
		// check program call arguments, and load map/trace files
		checkArgsAndLoadFiles(MAP_TO_LOAD);
	}
	
	/**
	 * checks program arguments and load given map and trace files
	 * @param args
	 */
	private void checkArgsAndLoadFiles(String loadStatus) {
		
		// first of all check arguments
		if (checkArgs(arguments)) {
			
			// if nothing was loaded yet and map file path was passed
			if ((loadStatus == MAP_TO_LOAD) && (arguments.length > 0)) {
				// load map file, first parameter must contain map file path
				
				String s = arguments[MAP_FILE_INDEX];
				if (s.endsWith(".osm")) {
					s = s.replace(".osm", ".net.xml");
				} else if (s.endsWith(".osm.xml")) {
					s = s.replace(".osm.xml", ".net.xml");
				} else {
					System.out.println("OSM Filename has to end with \".osm\" or \".osm.xml\" !");
					System.out.println(".NET Filenname has to be the smae like the OSM Filename and has to end with \".net.xml\" !");
					System.exit(-1);
				}
				
				openRoutingGraph(arguments[MAP_FILE_INDEX], s, CLIENT_ARGUMENTS);
			}
			// if map was loaded and a second argument was passed 
			else if ((loadStatus == MAP_LOADED) && (arguments.length == 2))  {
				// load GPS trace file (second argument)
				openGPSTrace(arguments[GPS_TRACE_FILE_INDEX], CLIENT_ARGUMENTS);
			}
			else if ((loadStatus == NROUTE_MAP_TO_LOAD) && (arguments.length > 0)) {
				// load map file, first parameter must contain map file path, tell
				// function to callback this method in order to possibly load N route
				
				String s = arguments[MAP_FILE_INDEX];
				if (s.endsWith(".osm")) {
					s = s.replace(".osm", ".net.xml");
				} else if (s.endsWith(".osm.xml")) {
					s = s.replace(".osm.xml", ".net.xml");
				} else {
					System.out.println("OSM Filename has to end with \".osm\" or \".osm.xml\" !");
					System.out.println(".NET Filenname has to be the smae like the OSM Filename and has to end with \".net.xml\" !");
					System.exit(-1);
				}
				
				openRoutingGraph(arguments[MAP_FILE_INDEX], s, NROUTE_ARGUMENTS);
			}
			else if ((loadStatus == NROUTE_MAP_LOADED) && (arguments.length == 2)) {
				// load N route from file
				//loadNRoute(arguments[NROUTE_FILE_INDEX], NROUTE_ARGUMENTS);
			}
			// No valid status message was passed
			else {
				if (arguments.length != 0) {
					Logger.err("JXMapMatchController.checkArgsAndLoadFiles(): loadStatus = ");
					Logger.println(loadStatus);
				}
			}
			
		}
		// otherwise print error message
		else {
			Logger.errln("JXMapMatchController.checkArgsAndLoadFiles(): invalid arguments call!");
			Logger.println("Allowed arguments: mapfile [tracefile]");
		}
	}

	////////////////////// EVENT-HANDLING ////////////////////////////////////////////////////////////////////////////////
	/**
	 * method which handles all action events from the GUI
	 * @param ActionEvent
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// which button was pressed?
		switch (e.getActionCommand()) {
			
			// show open dialog for routing graph and load chosen file
			case "Open routing graph file":
				openRoutingGraph();
				break;
			
			// show open dialog for GPS trace files (TXT/GPX) and load chosen file
			case "Open GPS trace file":
				openGPSTrace();
				break;
				
			// user switch/turn off N route algorithm mode
			case "N Route Algorithm":
				setNRouteAlogrithmMode();
				break;
				
			// user executes N route algorithm
			case "change N route algorithm state":
				changeNRouteAlgorithmState(((JButton) e.getSource()).getText());
				break;
				
			// show open dialog for N route file
			case "open N route":
				//openNRoute();
				break;
				
			// show save dialog for (adjusted) N route;
			case "save N route":
				saveNRoute();
				break;
				
			case "export N match":
				saveMatchedGPSNodes(matchGPStoNRouteAlgorithm, jxMapMatchGUI.getKMLNorm(), jxMapMatchGUI.getUniqueGPS());
				break;
				
			// user switch/turn off route selecting mode
			case "Select Route":
				setSelectRouteMode();
				break;
				
			// show save dialog for matched GPS points and export file
			case "Export matched GPS Nodes to file":
				saveMatchedGPSNodes(gpsToLinkMatcher, jxMapMatchGUI.getKMLNorm(), jxMapMatchGUI.getUniqueGPS());
				break;
				
			// user executes match to N route algorithm
			case "change match GPS to N route algorithm state":
				changeMatchGPStoNRouteAlgorithmState(((JButton) e.getSource()).getText());
				break;
			
			// not handled action event
			default:
				System.err.println("Not handled action event: ActionCommand: " + e.getActionCommand() + " ID: " +
								   e.getID());
				break;
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		// get source and its state which caused event
		JCheckBox jCheckBox = (JCheckBox) e.getItemSelectable();
		
		// which check box caused event? is data (GSP, StreetMap etc. loaded)?
		switch(jCheckBox.getText())
		{
			// routing graph should be drawn?
			case "Routing Graph":
				drawStreetMap = jxMapMatchGUI.getDrawStreetMap();
				break;
			
			// GPS trace should be drawn?
			case "GPS Trace":
				drawGPSTrace = jxMapMatchGUI.getDrawGPSTrace();
				break;
			
			// matched route should be drawn?
			case "Matching Route":
				drawNRoute = jxMapMatchGUI.getDrawNRoute();
				break;
			
			// selected route should be drawn?
			case "Selected Route":
				drawSelectedRoute = drawSelectedRoute();
				break;
		}
		
		// repaint JXMapViewer
		jxMapViewer.repaint();
	}
	
	@Override
	public void stateChanged(ChangeEvent changeEvent) {
		// update settings of N Route Panel
		jxMapMatchGUI.updateNRouteSettings();
		
		// force redraw
		jxMapViewer.repaint();
	}
	
	/**
	 * get mouse coordinates to determine nearest selectable link
	 */
	@Override
	public void mouseMoved(MouseEvent mouseEvent) {
		
		// get mouse -> coordinates translated coordinates
		Point mousePoint = jxMapMatchGUI.getTranslatedMousePos(mouseEvent);
		
		// if select route mode is active
		if (selectedRouteMode) {
			// set selectable link
			gpsToLinkMatcher.setSelectableLink(mousePoint.x, mousePoint.y);
			
			// redraw
			jxMapViewer.repaint();
			
			// print matched mouse position
			jxMapMatchGUI.updateStatus(mousePoint.x + ", " + mousePoint.y);
		}
		// if selected n route mode is active
		else if (selectedNRouteMode) {
			
			// set selectable and deletable links
			selectedNRoute.setEditableLinks(mousePoint.x, mousePoint.y);
			
			// redraw
			jxMapViewer.repaint();
			
			// print matched mouse position
			jxMapMatchGUI.updateStatus(mousePoint.x + ", " + mousePoint.y);
		}
	}
	
	/**
	 * handle different mouse button clicks
	 * @param mouseEvent
	 */
	@Override 
	public void mouseReleased(MouseEvent mouseEvent) {
		// route selecting or n route selecting mode must be activated, 
		// mouse mustn't be dragged (dragging should just be used for moving map)
		if ((!mouseDragged)) {
			
			// get mouse -> coordinates translated coordinates
			Point mousePoint = jxMapMatchGUI.getTranslatedMousePos(mouseEvent);
			
			// route selecting mode
			if (selectedRouteMode) {
				// add link (left click) 
				if (mouseEvent.getButton() == MouseEvent.BUTTON1)
					gpsToLinkMatcher.addLink(mousePoint.x, mousePoint.y);
				// adjust link (middle button/wheel button click)
				else if (mouseEvent.getButton() == MouseEvent.BUTTON2)
					gpsToLinkMatcher.adjustLink();
				// remove last link (right click)
				else if (mouseEvent.getButton() == MouseEvent.BUTTON3){
					gpsToLinkMatcher.removeLink(mousePoint.x, mousePoint.y);
				}
			}
			// n route selecting mode
			else if (selectedNRouteMode) {
				if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
					selectedNRoute.addLink(mousePoint.x, mousePoint.y);
				}
				else if (mouseEvent.getButton() == MouseEvent.BUTTON3){
					selectedNRoute.deleteLink(mousePoint.x, mousePoint.y);
				}
				
				// enable "Match" button is Selected N Route is not split
				checkAndEnableMatchButton();
			}
		}
			
		// reset mouse dragging variable
		mouseDragged = false;
		
		//redraw
		jxMapViewer.repaint();
	}
	
	/**
	 * sets flag is mouse was dragged
	 */
	@Override 
	public void mouseDragged(MouseEvent mouseEvent){
		mouseDragged = true;
	}
	
	// not needed events yet
	@Override public void mouseClicked(MouseEvent arg0) {}
	@Override public void mouseEntered(MouseEvent arg0) {}
	@Override public void mouseExited(MouseEvent arg0) {}
	@Override public void mousePressed(MouseEvent arg0) {}
	
	/**
	 * handle different key events
	 * @param keyEvent
	 */
	@Override
	public void keyPressed(KeyEvent keyEvent) {
		// if select route mode is activated and a key was pressed
		if (selectedRouteMode) {
			switch (keyEvent.getKeyCode()) {
				// move current index to match forward
				case KeyEvent.VK_F:
					gpsToLinkMatcher.increaseCurrentIndexToMatch();
					break;
				
				// move current index to match backwards
				case KeyEvent.VK_B:
					gpsToLinkMatcher.decreaseCurrentIndexToMatch();
					break;
					
				// switch all links selectable mode
				case KeyEvent.VK_S:
					gpsToLinkMatcher.switchAllLinksSelectableMode();
					break;
					
				// add link without matching
				case KeyEvent.VK_A:
					gpsToLinkMatcher.addLinkWithoutMatching();
			}
		}
	}
	
	// not needed events yet
	@Override public void keyReleased(KeyEvent arg0) {}
	@Override public void keyTyped(KeyEvent arg0) {}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * show open dialog for selecting routing graph and loads it 
	 */
	private void openRoutingGraph() {
		// if user choose an valid file load routing graph
		if (jFileOpenDialogGraph.showOpenDialog()){
			File f = jFileOpenDialogGraph.getSelectedFile();
			String s = f.getAbsolutePath();
			if (s.endsWith(".osm")) {
				s = s.replace(".osm", ".net.xml");
			} else if (s.endsWith(".osm.xml")) {
				s = s.replace(".osm.xml", ".net.xml");
			} else {
				System.out.println("OSM Filename has to end with \".osm\" or \".osm.xml\" !");
				System.out.println(".NET Filenname has to be the smae like the OSM Filename and has to end with \".net.xml\" !");
				System.exit(-1);
			}
			String netFilePath = s;
			openRoutingGraph(jFileOpenDialogGraph.getSelectedFile(), netFilePath, CLIENT_FILE_DIALOG);
		}
	}
	
	/**
	 * loads routing graph from given file path
	 * @param filepath
	 */
	private void openRoutingGraph(String osmFilePath, String netFilePath, String client) {
		// call overloaded method by creating file instance from given file path
				
		openRoutingGraph(new File(osmFilePath), netFilePath, client);
		
	}
	
	/**
	 * loads routing graph from given file in another thread
	 * @param osmFile
	 * @param client who calls this method
	 */
	private void openRoutingGraph(File osmFile, String _netFilePath, final String client) {
		// get chosen routing graph file
		final File streetMapFile = osmFile;

		final String netFilePath = _netFilePath;

		// set routing graph button caption
		jxMapMatchGUI.setStreetMapButtonText(streetMapFile.getName());
		
		// disable GUI while doing loading
		jxMapMatchGUI.enableGUI(false);
		
		// load GPS trace file in background
		SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

			@Override
			protected Boolean doInBackground() throws Exception {
				try {
					
					if (streetMapFile.getName().endsWith(".osm") || streetMapFile.getName().endsWith(".osm.xml")) {
						
						if (myMap == null) {
							myMap = new myOSMMap(streetMapFile, netFilePath);							
						} else {
							myMap.loadMapFiles(streetMapFile, netFilePath);
						}
						
						myMap.removeUnusedNotesAndWaysAndSetWayParts();
//		                streetMap = myMap.getSteetMap();
					}
					else {						
//						streetMap = OSMStAXGraphReader.convertToStreetMap(streetMapFile.getAbsolutePath(), jxMapMatchGUI);
//						streetMap.setColorOfLinks();
					}
					
//					if (myMap != null && streetMap != null) {
//						myMap.linkToStreetMap(streetMap);						
//					}
					
				} catch (Exception e) {
					System.out.println("Error: " + e.toString());
					
					return false;
				}
				
				// set flag we're not in Selected N Route mode anymore by loading new street map
				isGPSTraceForSelectedNRoute = false;
				
				// checks if map and trace are loaded, in this case initialize and enable components for matching operations
				checkAndInitializeMatchingControllers();
				
				// loading process successful
				return true;
			}
			
			@Override
			protected void done() {
				try {
					// check if loading was successful
					if (get()){ 
						// make street map drawing available
						jxMapMatchGUI.makeDrawStreetMapAvailable(true);
						jxMapMatchGUI.setDrawStreetMap(true);
					
						// release GUI, deactivate N Route Algorithm-, Select Route-Toggle buttons
						jxMapMatchGUI.enableGUI(true);
						
						// check if client was background loading process, notify via status update
						if (client == CLIENT_ARGUMENTS) finished(MAP_LOADED);
						if (client == NROUTE_ARGUMENTS) finished(NROUTE_MAP_LOADED);
					}
				} catch (InterruptedException | ExecutionException e) { e.printStackTrace(); }
			}
		};
		
		//register property change listener, do loading process in background
		worker.execute();
	}
		
	/**
	 * show open dialog for selecting GPS trace and loads it
	 */
	private void openGPSTrace() {
		// if user choose an valid file load GPS trace
		if (jFileOpenDialogGPS.showOpenDialog()){
			openGPSTrace(jFileOpenDialogGPS.getSelectedFile(), CLIENT_FILE_DIALOG);
		}
	}
	
	/**
	 * loads GPS trace from given file path
	 * @param filepath
	 */
	private void openGPSTrace(String filepath, String client) {
		// call overloaded method by creating file instance from given file path
		openGPSTrace(new File(filepath), client);
	}
	
	/**
	 * loads GPS trace from given file instance in another thread
	 * @param file
	 * @param client who calls this method
	 */
	private void openGPSTrace(File file, String client) {
		
		// get chosen GPS trace file
		final File gpsTraceFile = file;

		final String DatasetCellInfoFolderPath = gpsTraceFile.getPath().replace(gpsTraceFile.getName(), "");
		
		if (this.myMap == null) {
			this.myMap = new myOSMMap();
		}
		
		final myOSMMap myMap = this.myMap;

		// set GPS button caption
		jxMapMatchGUI.setGPSButtonText(gpsTraceFile.getName());
				
		// disable GUI
		jxMapMatchGUI.enableGUI(false);
		
		// load GPS trace file in background
		SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>(){
			@Override
			protected Boolean doInBackground() throws Exception {
				// try to load GPS trace from file
				try {
					myMap.loadDatasets(DatasetCellInfoFolderPath);
					
					myMap.loadCellInfos(DatasetCellInfoFolderPath);
					
					gpsTrace = GPSTraceStreamer.convertToGPSPath(gpsTraceFile.getAbsolutePath(), jxMapMatchGUI);

					selectedNRouteMode = false;
					drawMatchedGPStoNRoute = false;
					
				} catch (Exception e) {
					System.out.println(e.toString());
					return false;
				}
				
				// set flag, if we just want to load new GPS Trace for existing Selected N Route
				if (selectedNRouteMode) isGPSTraceForSelectedNRoute = true; 

				// checks if map and trace are loaded, in this case initialize and enable components for matching operations
				checkAndInitializeMatchingControllers();
				
				// loading process successful
				return true;
			}
			
			@Override
			protected void done(){
				try {
					// check if loading was successful
					if (get()) {
						jxMapMatchGUI.makeDrawGPSTraceAvailable(true);
						jxMapMatchGUI.setDrawGPSTrace(true);

						// release GUI
						jxMapMatchGUI.enableGUI(true);
						if (isGPSTraceForSelectedNRoute) setNRouteAlogrithmMode();
						
						// set view and zoom
						jxMapKit.setCenterPosition(gpsTrace.getStartGeoPos());
						jxMapKit.setZoom(1);
					}			
				} catch (InterruptedException | ExecutionException e) { e.printStackTrace(); }
			}
		};
		
		//do loading process in background
		worker.execute();
	}
	
	private void saveMatchedGPSNodes(final MatchingGPSObject matchingGPSObj, boolean kmlNorm, boolean onlyUniqueMatchedGPS) {
		// if user choose an valid file
		if (jFileSaveDialogMatchedGPS.showSaveDialog()){
			
			// get chosen GPS trace file
			final File gpsTraceFile = jFileSaveDialogMatchedGPS.getSelectedFile();
			
			// disable GUI
			jxMapMatchGUI.enableGUI(false);
			
			// save matched GPS trace file in background
			SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>(){
				@Override
				protected Boolean doInBackground() throws Exception {
					try {
						mySaveToFile.saveMatchedGPSTraceToFile(myMap, matchingGPSObj.getMatchedGPSNodes(), matchingGPSObj.getRefTimeStamp(), jxMapMatchGUI.getNormalizeGPSTimeStamp(), gpsTraceFile.getAbsolutePath(), jxMapMatchGUI, matchGPStoNRouteAlgorithm.getMatchedNLinks(), kmlNorm, onlyUniqueMatchedGPS);
					} catch (Exception e) { 
						e.printStackTrace();
						return false;
					}
					
					//saving process was successful
					return true;
				}
				
				@Override
				protected void done(){
					try {
						// check if saving matched GPS nodes was successful
						if (get()) ;
			
						// release GUI
						jxMapMatchGUI.enableGUI(true);
					} catch (InterruptedException | ExecutionException e) { e.printStackTrace(); }
				}
			};
			
			// do saving process in background
			worker.execute();
		}
		
	}

	private void saveNRoute() {
		// if user choose an valid file
		if (jFileSaveNRoute.showSaveDialog()){
			
			// get chosen GPS trace file
			//final File nRouteFile = jFileSaveNRoute.getSelectedFile();
			
			// disable GUI
			jxMapMatchGUI.enableGUI(false);
			
			// save matched GPS trace file in background
			SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>(){
				@Override
				protected Boolean doInBackground() throws Exception {
					try {
						//NRouteStreamer.saveSelectedNRouteToFile(selectedNRoute, nRouteFile.getAbsolutePath(), jxMapMatchGUI);
					} catch (Exception e) { 
						return false;
					}
					
					//saving process was successful
					return true;
				}
				
				@Override
				protected void done(){
					try {
						// check if saving N route was successful
						if (get()) ;
			
						// release GUI
						jxMapMatchGUI.enableGUI(true);
					} catch (InterruptedException | ExecutionException e) { e.printStackTrace(); }
				}
			};
			
			// do saving process in background
			worker.execute();
		}
		
	}
	
	/**
	 * checks if street map and GPS trace are load, initializes matching algorithms
	 * and activates GUI for user
	 * @return
	 */
	private boolean checkAndInitializeMatchingControllers() {
		// if GPS trace and street map are loaded initialize new selected route with current street map and
		// GPS trace, pass JXMapKit as drawing component for animation. In addition initialize
		if (isGPSTraceAndStreetMapLoaded()){
			// initialize matching classes
			initGPSToLinkMatcher();
			initNRouteAlgorithm();
			
			// make drawing options available
			jxMapMatchGUI.makeDrawSelectedRouteAvailable(drawSelectedRoute);
			jxMapMatchGUI.makeDrawNRouteAvailable(drawNRoute);
			
			// make "Select Route" panel available
			jxMapMatchGUI.makeSelectRouteModeAvailable(true);	
			// make "N Route Algorithm" panel...
			jxMapMatchGUI.makeNRouteAlgorithmModeAvailable(true);

			// map and trace are loaded, matching controllers are activated!
			return true;
		}

		// either map or trace aren't loaded yet
		return false;
	}
	
	/**
	 * initialize GPS to link matcher
	 * @return boolean  
	 */
	private boolean initGPSToLinkMatcher() {
		if (isGPSTraceAndStreetMapLoaded()){
			
			// initialize GPS to link matcher
			gpsToLinkMatcher = new GPSToLinkMatcher(this.myMap, gpsTrace, jxMapViewer);
			// successfully initialized
			return true;
		}
		
		// map and GPS trace files, both not loaded
		return false;
	}
	
	private void initSelectedNRoute(boolean useExistingSelectedNRoute) {
		
		// initialize new Selected N Route if existing one shouldn't be used
		if (!useExistingSelectedNRoute) {
			selectedNRoute = new SelectedNRoute(myMap, nRouteAlgorithm, jxMapViewer);
		}
		selectedNRouteMode = true;
		allowNRouteLoadGPSTrace = true;

		// enable "Save" button and "Match" button if N Route is not split
		jxMapMatchGUI.makeNRouteSaveAvailable(true);
		checkAndEnableMatchButton();
	}
	
	private void initSelectedNRoute() {
		// initialize new Selected N Route (don't use old one)
		initSelectedNRoute(false);
	}
	
	/**
	 * initialize N route algorithm
	 * @return boolean
	 */
	private boolean initNRouteAlgorithm() {
		if (isGPSTraceAndStreetMapLoaded()){
			
			// initialize N route algorithm
			nRouteAlgorithm = new NRouteAlgorithm(myMap, gpsTrace, jxMapMatchGUI, jxMapViewer);

			if (!isGPSTraceForSelectedNRoute) {
				// enable/disable buttons
				jxMapMatchGUI.makeNRouteButtonsAvaiable(true,	// Route
														false,	// Match
														false,	// Save
														false,	// Export
														true);	// Reset
		
				// set Selected N Route mode to false
				selectedNRouteMode = false;
				
				// clear flag
				allowNRouteLoadGPSTrace  = false;
			
				// refresh GUI
				if (jxMapMatchGUI.getNRouteAlgorithmMode()) {
					setNRouteAlogrithmMode();
				}
				jxMapKit.repaint();
			}
			
			// successfully initialized
			return true;
		}
		
		// map and GPS trace files, both not loaded
		return false;	
	}
	
	/**
	 * initialize N route algorithm
	 * @return boolean
	 */
	private boolean initMatchGPStoNRouteAlgorithm() {
		if (isSelectedNRouteComplete()){
			
			// initialize match GPS to N route algorithm
			matchGPStoNRouteAlgorithm = new MatchGPStoNRouteAlgorithm(selectedNRoute, gpsTrace, GPS_TO_N_ROUTE_UNMATCHED_LINK_COLOR,
					GPS_TO_N_ROUTE_MATCHED_LINK_COLOR, GPS_TO_N_ROUTE_UNMATCHED_NODE_COLOR, GPS_TO_N_ROUTE_MATCHED_NODE_COLOR,
					jxMapMatchGUI, jxMapViewer);
			
			// successfully initialized
			return true;
		}
		
		// no n route exists
		return false;	
	}
	
	/**
	 * enables/disables route selecting mode according to user choice
	 * made in GUI
	 */
	private void setSelectRouteMode() {
		// get mode of route selection panel
		selectedRouteMode = jxMapMatchGUI.getSelectRouteMode();
		
		// set GUI to chosen mode
		jxMapMatchGUI.setSelectRouteMode(selectedRouteMode);
		
		// enable/disable selected route drawing
		drawSelectedRoute = drawSelectedRoute();
	}
	
	private void setNRouteAlogrithmMode() {
		// get mode of N route algorithm panel by reversing current mode
		isNRouteAlgorithmMode = jxMapMatchGUI.getNRouteAlgorithmMode();
		
		setCheckGPSTraceInBoundary();
		
		// set GUI to chosen mode
		jxMapMatchGUI.setNRouteAlgorithmMode(isNRouteAlgorithmMode, allowNRouteLoadGPSTrace);
		
		// enable/disable route drawing
		drawNRoute = drawNRoute();

	}
	
	private void setCheckGPSTraceInBoundary() {
		
		int firstInB = -1;
		
		if (this.gpsTrace == null) {
			return;
		}
		
		for (int i=0; i < this.gpsTrace.getNrOfNodes(); i++) {
			GPSNode n = this.gpsTrace.getNode(i);
			
			if (this.myMap.osmMinLat <= n.getLat() && n.getLat() <= this.myMap.osmMaxLat && 
					this.myMap.osmMinLon <= n.getLon() && n.getLon() <= this.myMap.osmMaxLon ) {
				
				firstInB = i;
				break;
			}
		}
		
		if (firstInB == -1) {			
			JOptionPane.showInternalMessageDialog(null, "All GPS Trace Points are not in the OSM Boundary", "Message", JOptionPane.CANCEL_OPTION);
			return;
		}
		
		int lastInB = -1;
		
		for (int i = firstInB; i < this.gpsTrace.getNrOfNodes(); i++) {
			GPSNode n = this.gpsTrace.getNode(i);
			
			if (this.myMap.osmMinLat <= n.getLat() && n.getLat() <= this.myMap.osmMaxLat && 
					this.myMap.osmMinLon <= n.getLon() && n.getLon() <= this.myMap.osmMaxLon ) {
				lastInB = i;
			} else {
				break;
			}
		}
		
		if (firstInB != 0 || lastInB != this.gpsTrace.getNrOfNodes() - 1) {
			JOptionPane.showMessageDialog(null, "Not all GPS Trace Points (" + (lastInB - firstInB + 1) + "/" + this.gpsTrace.getNrOfNodes() + ") are in the OSM Boundary: GPS Trace shrinked !", "Message", JOptionPane.CANCEL_OPTION);

			this.gpsTrace.shrinkTrace(firstInB, lastInB);
		}
	}
	
	/**
	 * change state of N route algorithm in background (start, pause, resume and reset)
	 */
	private void changeNRouteAlgorithmState(String state) {
		
		// handle given state
		switch (state) {
			
			// start new N route algorithm
			case "Route":
				startNRouteAlgorithm();
				break;
			
			// pause N route algorithm
			case "Pause":
				pauseNRouteAlgorithm();
				break;
				
			// continue N route algorithm
			case "Resume": 
				resumeNRouteAlgorithm();
				break;
			
			// stop and recess N route algorithm
			case "Reset":
				resetNRouteAlgorithm();
				break;
			
			// no valid state passed
			default:
				Logger.errln(state);
		}	
	}
	
	/**
	 * change state of match GPS to N route algorithm in background (start, pause, resume and reset)
	 */
	private void changeMatchGPStoNRouteAlgorithmState(String state) {
		
		System.out.println(state);
		
		// handle given state
		switch (state) {
			
			// start new N route algorithm
			case "Match":
				startMatchGPStoNRouteAlgorithm();
				break;
			
			// pause N route algorithm
			case "Pause":
				pauseMatchGPStoNRouteAlgorithm();
				break;
				
			// continue N route algorithm
			case "Resume": 
				resumeMatchGPStoNRouteAlgorithm();
				break;
			
			// stop and recess N route algorithm
			case "Reset":
				resetMatchGPStoNRouteAlgorithm();
				break;
			
			// no valid state passed
			default:
				Logger.errln(state);
		}	
	}
	
	private void startNRouteAlgorithm() {
		SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>(){
			@Override
			protected Boolean doInBackground() throws Exception {
				// state of algorithm
				boolean hasFinishedAlgorithm = false;
				

				drawMatchedGPStoNRoute = false;
				
				initNRouteAlgorithm();
				
				try {
					// update n route panel state
					jxMapMatchGUI.setNRouteAlgorithmState(N_ROUTE_RUNNING);

					// execute algorithm in background
					hasFinishedAlgorithm = nRouteAlgorithm.executeNRouteAlgorithm(jxMapMatchGUI.getNRouteSize(),
														   jxMapMatchGUI.getNRouteThreshold());
						
				} catch (Exception e) { 
					System.out.println("Error: startNRouteAlgorithm: " + e.toString());
					return false;
				}
				
				// return algorithm state
				return hasFinishedAlgorithm;
			}
			
			@Override
			protected void done(){
				try {
					// update n route panel state
					jxMapMatchGUI.setNRouteAlgorithmState(N_ROUTE_RECESSED);
					
					// release GUI and activate N Route Algorithm mode again
					jxMapMatchGUI.enableGUI(true);
					
					// check if algorithm has finished until end
					// or was aborted by pressing "Reset" button
					if (get()) { 
						// initialize Selected N Route, allow loading GPS trace
						initSelectedNRoute();
					}
					// otherwise reset N Route algorithm
					else {
						// initialize fresh N Route algorithm
						initNRouteAlgorithm();
					}
					
					// set proper N Route Algorithm mode
					setNRouteAlogrithmMode();
					
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace(); 
					System.out.println("Error: startNRouteAlgorithm: done: \n" + e.toString());
				}catch ( Exception e) {
					e.printStackTrace(); 
					System.out.println("Error: startNRouteAlgorithm: done: \n" + e.toString());
				}
			}
		};
		
		// execute algorithm process in background
		worker.execute();
	}
	
	private void pauseNRouteAlgorithm() {
		nRouteAlgorithm.setNRouteAlgorithmState(N_ROUTE_PAUSED);
		jxMapMatchGUI.setNRouteAlgorithmState(N_ROUTE_PAUSED);
	}
	
	private void resumeNRouteAlgorithm() {
		nRouteAlgorithm.setNRouteAlgorithmState(N_ROUTE_RUNNING);
		jxMapMatchGUI.setNRouteAlgorithmState(N_ROUTE_RUNNING);
	}
	
	private void resetNRouteAlgorithm() {
		// if algorithm is running, send command to reset, after thread
		// is finished, it will automatically call initNRouteAlgorithm function
		if (nRouteAlgorithm.getNRouteAlgorithmState() == N_ROUTE_RUNNING) {
			nRouteAlgorithm.setNRouteAlgorithmState(N_ROUTE_RECESSED);
		}
		// otherwise initialize fresh N Route algorithm
		else {
			initNRouteAlgorithm();
		}
	}
	
	private void startMatchGPStoNRouteAlgorithm() {
		
		// check conditions
		if (!initMatchGPStoNRouteAlgorithm()) {
			return;
		}
		
		SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>(){
			@Override
			protected Boolean doInBackground() throws Exception {
				try {
					// update n route panel state
					jxMapMatchGUI.setMatchGPStoNRouteAlgorithmState(MATCH_GPS_TO_N_ROUTE_RUNNING);
					
					drawMatchedGPStoNRoute = true;
					
					// execute algorithm in background
					// match GPS Nodes to OSM ROute
					matchGPStoNRouteAlgorithm.executeMatchGPStoNRouteAlgorithm(jxMapMatchGUI.getReoderNMatch(),
																			   jxMapMatchGUI.getProjectNMatch());
					
					// reorder matched GPS node on OSM ROute
					ReorderedMatchedGPSNode.reorderMatchedGPSNodes(matchGPStoNRouteAlgorithm.getMatchedNLinks(), matchGPStoNRouteAlgorithm.getMatchedGPSNodes());
					
					// match Datasets to OSM ROute
					myDataset.matchMatchedGPSNode(myMap.DatasetsDown, true, matchGPStoNRouteAlgorithm.getMatchedGPSNodes(), matchGPStoNRouteAlgorithm.getMatchedNLinks(), myMap.CellInfos, jxMapMatchGUI.getUniqueGPS());
					myDataset.matchMatchedGPSNode(myMap.DatasetsUp, false, matchGPStoNRouteAlgorithm.getMatchedGPSNodes(), matchGPStoNRouteAlgorithm.getMatchedNLinks(), myMap.CellInfos, jxMapMatchGUI.getUniqueGPS());

				} catch (Exception e) { 
					return false;
				}
				
				// algorithm run without errors
				return true;
			}
			
			@Override
			protected void done(){
				try {
					// check if loading was successful
					if (get()) {
						
						jxMapMatchGUI.makeNRouteExportAvailable(true);
						
					}
		
					// update n route panel state
					jxMapMatchGUI.setMatchGPStoNRouteAlgorithmState(MATCH_GPS_TO_N_ROUTE_RECESSED);
					
					// release GUI
					jxMapMatchGUI.enableGUI(true);
					
//					// set and initialize selected n route mode
//					selectedNRoute = new SelectedNRoute(streetMap, nRouteAlgorithm, jxMapViewer);
//					selectedNRouteMode = true;
					
				} catch (InterruptedException | ExecutionException e) { e.printStackTrace(); }
			}
		};
		
		// execute algorithm process in background
		worker.execute();
	}
	
	private void pauseMatchGPStoNRouteAlgorithm() {
		matchGPStoNRouteAlgorithm.setMatchGPStoNRouteAlgorithmState(MATCH_GPS_TO_N_ROUTE_PAUSED);
		jxMapMatchGUI.setMatchGPStoNRouteAlgorithmState(N_ROUTE_PAUSED);
	}
	
	private void resumeMatchGPStoNRouteAlgorithm() {
		matchGPStoNRouteAlgorithm.setMatchGPStoNRouteAlgorithmState(MATCH_GPS_TO_N_ROUTE_RUNNING);
		jxMapMatchGUI.setMatchGPStoNRouteAlgorithmState(N_ROUTE_RUNNING);
	}
	
	private void resetMatchGPStoNRouteAlgorithm() {
		matchGPStoNRouteAlgorithm.setMatchGPStoNRouteAlgorithmState(MATCH_GPS_TO_N_ROUTE_RECESSED);
		jxMapMatchGUI.setMatchGPStoNRouteAlgorithmState(N_ROUTE_RECESSED);
		
		// initialize fresh instance of match GPS to N route algorithm class
		initMatchGPStoNRouteAlgorithm();
	}
	
	/**
	 * returns if selected route should be drawn, either if its selected by user or 
	 * program is route select mode
	 * @return
	 */
	private boolean drawSelectedRoute() {
		return (jxMapMatchGUI.getDrawSelectedRoute() || jxMapMatchGUI.getSelectRouteMode());
	}
	
	/**
	 * returns if matched route should be drawn, either if its selected by user or program
	 * is route select mode
	 * @return
	 */
	private boolean drawNRoute() {
		return (jxMapMatchGUI.getDrawNRoute() || jxMapMatchGUI.getNRouteAlgorithmMode());
	}
	
	/**
	 * are GPS trace and routing graph loaded?
	 * @return
	 */
	private boolean isGPSTraceAndStreetMapLoaded() {
//		return ((gpsTrace != null) && (streetMap != null));
		return ((gpsTrace != null) && (myMap != null));
	}
	
	/**
	 * is selected N route complete
	 * @return
	 */
	private boolean isSelectedNRouteComplete() {
		// check if selected N route exists, is not split, and a GPS trace is loaded
		boolean isComplete = (selectedNRoute != null && !selectedNRoute.isNRouteSplit() && gpsTrace != null);
		
		return isComplete;
	}
	
	private void checkAndEnableMatchButton() {
		jxMapMatchGUI.makeNRouteMatchAvailable(isSelectedNRouteComplete());
	}
	
	/**
	 * sets new overlay painter to jxMapView
	 * @param jxMapKit
	 * @param jxMapPainter
	 */
	private void initMapPainter(JXMapKit jxMapKit, final JXMapPainter jxMapPainter) {
		jxMapViewer.setOverlayPainter(new Painter<JXMapViewer>(){
			@Override
			public void paint(Graphics2D g2D, JXMapViewer jxMapViewer, int arg2,
							  int arg3) {
				
				// calculate zoom factor (2 ^ (zoom - 1)), e.g. for zoom = 1 (no zoom) => 2^0 = 1 => no change)
				double zoomFactor = Math.pow(2, jxMapViewer.getZoom()-1);
				
				// draw routing graph?
				if (drawStreetMap){
					jxMapPainter.drawStreetMap(g2D, jxMapViewer, STREET_MAP_COLOR, zoomFactor, myMap);
					//jxMapPainter.drawStreetNodes(g2D, jxMapViewer, streetMap, Color.RED, zoomFactor);
				}
				
				// draw selected route?
				if (drawSelectedRoute) {
					jxMapPainter.drawSelectedRoute(g2D, jxMapViewer, gpsToLinkMatcher.getSelectedRoute(),
							  SELECTABLE_LINK_COLOR, MULTI_SELECTABLE_LINK_COLOR, SELECTED_LINK_COLOR, NON_MATCHED_LINK_COLOR, zoomFactor);
				}
				 
				// draw selectable n route
				if (selectedNRouteMode) {
					jxMapPainter.drawSelectedNRoute(g2D, jxMapViewer, selectedNRoute, N_ROUTE_LINK_COLOR, SELECTABLE_N_ROUTE_COLOR, DELETABLE_N_ROUTE_COLOR, zoomFactor);;
				}
				// draw matched n route?
				else if (drawNRoute && nRouteAlgorithm != null) {
					//jxMapPainter.drawNRoute(g2D, jxMapViewer, nRouteAlgorithm.getNRoute(jxMapMatchGUI.getSelectedNRoute()), N_ROUTE_LINK_COLOR, zoomFactor);
					jxMapPainter.drawNRoute(g2D, jxMapViewer, nRouteAlgorithm.getNRoute(/*jxMapMatchGUI.getSelectedNRoute()*/), N_ROUTE_LINK_COLOR, zoomFactor);
				}

				
				// draw matched GPS to N route nodes/route
				if (drawMatchedGPStoNRoute) {
					jxMapPainter.drawMatchedGPStoNRoute(g2D, jxMapViewer, matchGPStoNRouteAlgorithm.getMatchedNLinks(), matchGPStoNRouteAlgorithm.getReorderedMatchedGPSNodes(), zoomFactor);
					return;
				}
				
				// draw GPS Path?
				if (drawGPSTrace) {
					
					// which one should be drawn, given unmatched trace or matched grace according to selected algorithm mode
					if (jxMapMatchGUI.getSelectRouteMode()) {
						jxMapPainter.drawGPSPath(g2D, jxMapViewer, gpsToLinkMatcher.getMatchedGPSNodes() , gpsToLinkMatcher.getCurrentGPSPointToMatch(), GPS_TRACE_COLOR, GPS_TO_MATCH_COLOR, zoomFactor);
					} else {
						jxMapPainter.drawGPSPath(g2D, jxMapViewer, gpsTrace, GPS_TRACE_COLOR, zoomFactor);
					}
				}
				
			}
		});
	}
	
	/*
	private void setArgs(String... args) {
		// set new arguments
		arguments = args;
	}
	*/
	
	/**
	 * checks if at most two arguments were passed (map & trace file)
	 * and if these files exist
	 * @param args arguments in string array
	 * @return boolean
	 */
	private boolean checkArgs(String args[]) {		
		// maximum two arguments (map and trace file) allowed
		if (args.length == 0 || args.length == 2) {
			
			// check if file(s) exists
			for (String arg : args) {
				
				// create link to file
				File file = new File(arg);
				
				// Does it exists and is it a file
				if (!(file.exists() && file.isFile())) {
					System.out.println("Error: " + file.getPath());
					return false;
				}
			}
			
			// files exists
			return true;
		}
		
		// too many arguments
		return false;
	}

	@Override 
	public void finished(String resultMessage) {
		if (resultMessage == MAP_LOADED || resultMessage == NROUTE_MAP_TO_LOAD || resultMessage == NROUTE_MAP_LOADED) {
			// callback background loading method for further background loadings
			checkArgsAndLoadFiles(resultMessage);
		}
	}
	
	// not needed methods yet
	@Override public void updateStatus(String updateMessage) {}
	@Override public void updateStatus(float percent) {}
	@Override public void updateStatus(String updateMessage, float percent) {}
	@Override public void updateUndefinedStatus() {}
	@Override public void updateUndefinedStatus(String undefinedMessage) {}
	@Override public void updateUndefinedStatus(String undefinedMessage, String updateMessage) {}
	@Override public void finished() {}
}
