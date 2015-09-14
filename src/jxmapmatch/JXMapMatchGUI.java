package jxmapmatch;

import interfaces.JXMapMatchGUIInterface;
import interfaces.StatusUpdate;
import java.awt.*;
import java.awt.event.*;
import java.util.EventListener;
import java.util.Hashtable;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ChangeListener;
import org.jdesktop.swingx.*;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import tools.Tools;
import dialogelements.JPanelBoxLayout;
import static algorithm.NRouteAlgorithm.*;
import static algorithm.MatchGPStoNRouteAlgorithm.*;

/**
 * @author Daniel Sathees Elmo
 * @author Adrian Skuballa
 *
 *         This class represents the GUI of JXMapMatcher implements all
 *         functions demanded by JXMapMatchInterface
 */

public class JXMapMatchGUI extends JFrame implements JXMapMatchGUIInterface {

	// serial version UID
	private static final long serialVersionUID = -7436286720561373053L;

	// notice StatusUpdate interface
	private StatusUpdate statusUpdate;

	// is GUI currently enabled?
	private boolean isGUIEnabled = true;

	// JPanels
	private JPanel contentPane; // BorderLayout
	private JPanel mainPanel; // BoxLayout x-axis
	private JPanel jPanelLeft; // BoxLayout y-axis
	private JPanel jPanelRight; // BoxLayout y-axis
	private JPanelBoxLayout jPanelSource; // BoxLayout y-axis
	// private JPanelBoxLayout jPanelMapMatching; // BoxLayout y-axis
	private JPanelBoxLayout jPanelOverlay; // BoxLayout y-axis
	private JPanelBoxLayout jPanelNRoute; // BoxLayout y-axis
	//private JPanelBoxLayout jPanelSelectRoute; // BoxLayout y-axis
	private JPanel jPanelStatusBar; // BoxLayout x-axis

	// JScrollPane
	// private JScrollPane jScrollPaneRight;

	// JLabels
	private JLabel jLabelChooseGraph;
	private JLabel jLabelChooseGPSTrack;
	private JLabel jLabelInfo;
	private JLabel jLabelNRoute;
	//private JLabel jLabelSelectNRoute;
	private JLabel jLabelNRouteSize;
	private JLabel jLabelNRouteTreshold;
	//private JLabel jLabelSelectRoute;
	private JLabel jLabelExport;
	private JLabel jLabelOverlay;

	// JCheckedBox
	private JCheckBox jCheckBoxGPSTrace;
	private JCheckBox jCheckBoxStreetMap;
	private JCheckBox jCheckBoxNRoute;
	//private JCheckBox jCheckBoxSelectedRoute;
	private JCheckBox jCheckBoxProjectNMatch;
	private JCheckBox jCheckBoxReorderNMatch;
	
	private JCheckBox jCheckBoxKMLNorm;
	private JCheckBox jCheckBoxUniqueGPS;
	
	private JCheckBox jCheckBoxNRouteNormalizeMatchedGPSTimeStamp_2;
	private JCheckBox jCheckBoxNormalizeGPSTimeStamp;

	// JButtons
	private JButton jButtonOpenStreetMap;
	private JButton jButtonOpenGPSTrace;
	private JButton jButtonNRouteRoute;
	private JButton jButtonNRouteMatch;
	//private JButton jButtonNRouteOpen;
	//private JButton jButtonNRouteSave;
	private JButton jButtonNRouteExport;
	//private JButton jButtonNRouteReset;
	//private JButton jButtonExport;

	// JComboBoxes
	// private JComboBox<String> jComboBoxMapMatching;

	// JSpinner
	private JSpinner jSpinnerNRouteSize;
	private JSpinner jSpinnerNRouteThreshold;

	// JSlider
	private JSlider jSliderSelectNRoute;

	// JToggleButton
	private JToggleButton jToggleButtonNRouteAlgorithm;
	private JToggleButton jToggleButtonSelectRoute;

	// list all different algorithms
	// private String[] mapMatchingAlgorithms = {"N Route"};

	// JXMapKit & JXMapViewer
	private JXMapKit jxMapKit;
	private JXMapViewer jxMapViewer;

	// default values for custom tile factory initialization
	private static int MIN_ZOOM_DEFAULT = 0;
	private static int MAX_ZOOM_DEFAULT = 17;
	private static int TOTAL_ZOOM_DEFAULT = 17;
	private static int TILE_SIZE_DEFAULT = 256;
	private static boolean X_ORIENTATION_DEFAULT = true;
	private static boolean Y_ORIENTATION_DEFAULT = true;

	// JProgressBar
	private JProgressBar jProgressBar;

	// control components enable state for drawing
	private boolean isDrawStreetMapAvailable = false;
	private boolean isDrawGPSTraceAvailable = false;
	private boolean isDrawNRouteAvailable = false;
	//private boolean isDrawSelectedRouteAvailable = false;

	// control components enable state for "Select Route Mode" matching
	private boolean isSelectRouteModeAvailable = false;

	// control components enable state for "N Route Algorithm" matching...
	private boolean isNRouteAlgorithmAvailable = false;
	// ...and it's subcomponents
	private boolean isNRouteRouteAvailable = false;
	private boolean isNRouteMatchAvailable = false;
	//private boolean isNRouteSaveAvailable = false;
	private boolean isNRouteExportAvailable = false;
	//private boolean isNRouteResetAvailable = false;

	// control components states for matching
	private boolean isSelectRouteModeSet = false;
	private boolean isNRouteAlgorithmModeSet = false;
	private String nRouteAlgorithmState = N_ROUTE_RECESSED;
	private String matchGPStoNRouteAlgorithmState = MATCH_GPS_TO_N_ROUTE_RECESSED;

	// store button captions here <State, Caption>
	private Hashtable<String, String> nRouteAlgorithmMatchButtonCaptions;
	private Hashtable<String, String> matchGPStoNRouteAlgorithmMatchButtonCaptions;

	/**
	 * Create the frame.
	 */
	public JXMapMatchGUI(StatusUpdate statusUpdate) {
		// call constructor of super class
		super("JXMapMatchVer3");

		this.setVisible(true);

		// remind status update interface
		this.statusUpdate = statusUpdate;

		// change toggle button's select color. must be done before creating an
		// instance
		Tools.setUIColorForComponent("ToggleButton.select", new Color(250, 250,	100));

		// initialize frame
		this.statusUpdate.updateStatus("loading main window...");
		initJFrame();

		// initialize left and right panels
		this.statusUpdate.updateStatus("loading main panels...");
		initMainLeftRightPanels();

		// initialize JXMapKit
		this.statusUpdate.updateStatus("loading JXMapKit...");
		initJXMapKit(jPanelLeft);

		// initialize source panel
		this.statusUpdate.updateStatus("loading source panel...");
		initSourcePanel(jPanelRight);

		// initialize Select Route panel
		this.statusUpdate.updateStatus("loading overlay panel...");
		initOverlayPanel(jPanelRight);

		// initialize map matching panel
		this.statusUpdate.updateStatus("loading map matching panel...");
		// initMapMatchingPanel(jPanelRight);

		// initialize N Route panel
		this.statusUpdate.updateStatus("loading N Route panel...");
		// initNRoutePanel(jPanelMapMatching);
		initNRoutePanel(jPanelRight);

		// initialize Select Route panel
		this.statusUpdate.updateStatus("loading route select panel...");
		initSelectRoutePanel(jPanelRight);

		// initialize status bar
		this.statusUpdate.updateStatus("loading status bar...");
		initStatusBar(contentPane);

		// add glue to left panel
		jPanelRight.add(Box.createVerticalGlue());

		// set font for all JLabels
		this.statusUpdate.updateStatus("setting Arial (Plain, 11) as label font...");
		Tools.setFontForComponents(this, "javax.swing.JLabel", new Font("Arial", Font.PLAIN, 11));

		this.statusUpdate.updateStatus("setting scroll bars position...");
		setRightScrollPaneScrollbarPosition();

		// notify update status interface, that we're finished loading
		statusUpdate.finished();

	}

	// //// INIT-METHODS
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void initJFrame() {
		// set frame properties
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Tools.centerHalfSizeWindow(this);
		setExtendedState(MAXIMIZED_BOTH);

		// set content pane (BorderLayout)
		contentPane = new JPanel(new BorderLayout());
		contentPane.setBorder(new EmptyBorder(3, 3, 3, 3));
		setContentPane(contentPane);
	}

	private void initMainLeftRightPanels() {
		// set main
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		// set left pane
		jPanelLeft = new JPanel();
		jPanelLeft.setLayout(new BoxLayout(jPanelLeft, BoxLayout.Y_AXIS));

		// set right pane (add scroll pane as main container)
		jPanelRight = new JPanel();
		// jPanelRight.setPreferredSize(new Dimension(150,10)); //set fixed
		// width for right panel
		// jPanelRight.setLayout(new BoxLayout(jPanelRight, BoxLayout.Y_AXIS));

		jPanelRight.setLayout(new BoxLayout(jPanelRight, BoxLayout.Y_AXIS));
		jPanelRight.setMinimumSize(new Dimension(175, 10));
		jPanelRight.setMaximumSize(new Dimension(175, this.getHeight()));
		jPanelRight.setPreferredSize(new Dimension(175, this.getHeight()));
		jPanelRight.setAlignmentX(LEFT_ALIGNMENT);

		/*
		 * jScrollPaneRight = new JScrollPane(jPanelRight,
		 * ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
		 * ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		 * 
		 * 
		 * // minimum and preferred size needs to be applied to the scroll pane
		 * // otherwise scroll bars won't show/work
		 * jScrollPaneRight.setMinimumSize(new Dimension(175,0));
		 * jScrollPaneRight.setPreferredSize(new Dimension(175,0));
		 * jScrollPaneRight.setMaximumSize(new Dimension(175,
		 * this.getHeight()));
		 */

		// add panels to container
		contentPane.add(mainPanel, BorderLayout.CENTER);

		mainPanel.add(jPanelLeft);
		mainPanel.add(Box.createHorizontalStrut(5)); // add some space between
														// right and left panel
		// mainPanel.add(jScrollPaneRight);
		mainPanel.add(jPanelRight);
	}

	private void initJXMapKit(Container container) {
		// initialize JXMapKit
		jxMapKit = new JXMapKit();
		jxMapKit.setDefaultProvider(org.jdesktop.swingx.JXMapKit.DefaultProviders.OpenStreetMaps);
		jxMapKit.setAlignmentX(LEFT_ALIGNMENT);
		// get map viewer
		jxMapViewer = jxMapKit.getMainMap();
		// add to container
		container.add(jxMapKit);

		setTileFactory("http://tile.openstreetmap.org","x","y","z");
	}

	private void initSourcePanel(Container container) {
		// create source panel
		jPanelSource = new JPanelBoxLayout(BoxLayout.Y_AXIS,
				container.getWidth(), 5, "Choose source files");

		jPanelSource.setAlignmentX(LEFT_ALIGNMENT);
		jPanelSource.setMaximumSize(container.getMaximumSize());
		jPanelSource.setMinimumSize(container.getMinimumSize());

		// create choose graph label
		jLabelChooseGraph = new JLabel("Routing Graph:");
		jLabelChooseGraph.setAlignmentX(CENTER_ALIGNMENT);

		// create choose graph button
		jButtonOpenStreetMap = new JButton("...");
		jButtonOpenStreetMap.setActionCommand("Open routing graph file");
		jButtonOpenStreetMap.setAlignmentX(CENTER_ALIGNMENT);
		jButtonOpenStreetMap.setMaximumSize(new Dimension(150, 25));

		// create choose GPS track label
		jLabelChooseGPSTrack = new JLabel("GPS Track:");
		jLabelChooseGPSTrack.setAlignmentX(CENTER_ALIGNMENT);
		// create choose GPS track button
		jButtonOpenGPSTrace = new JButton("...");
		jButtonOpenGPSTrace.setActionCommand("Open GPS trace file");
		jButtonOpenGPSTrace.setAlignmentX(CENTER_ALIGNMENT);

		jButtonOpenGPSTrace.setMaximumSize(new Dimension(150, 25));

		// add components to source panel
		jPanelSource.add(jLabelChooseGraph);
		jPanelSource.add(jButtonOpenStreetMap);
		jPanelSource.add(jLabelChooseGPSTrack);
		jPanelSource.add(jButtonOpenGPSTrace);

		// add to container
		container.add(jPanelSource);
	}

	private void initOverlayPanel(Container container) {
		// create panel
		jPanelOverlay = new JPanelBoxLayout(BoxLayout.Y_AXIS,
				container.getWidth(), 5, "Overlays");
		// JPanel jPanelOverlay = new JPanel();
		// jPanelOverlay.setLayout(new
		// BoxLayout(jPanelOverlay,BoxLayout.Y_AXIS));

		jPanelOverlay.setAlignmentX(LEFT_ALIGNMENT);
		jPanelOverlay.setMaximumSize(container.getMaximumSize());
		jPanelOverlay.setMinimumSize(container.getMinimumSize());

		// create label
		jLabelOverlay = new JLabel("Select overlays:");
		jLabelOverlay.setAlignmentX(CENTER_ALIGNMENT);

		// create check boxes
		jCheckBoxStreetMap = new JCheckBox("Routing Graph");
		jCheckBoxStreetMap.setAlignmentX(CENTER_ALIGNMENT);
		jCheckBoxGPSTrace = new JCheckBox("GPS Trace");
		jCheckBoxGPSTrace.setAlignmentX(CENTER_ALIGNMENT);
		jCheckBoxNRoute = new JCheckBox("Matching Route");
		jCheckBoxNRoute.setAlignmentX(CENTER_ALIGNMENT);
		//jCheckBoxSelectedRoute = new JCheckBox("Selected Route");
		//jCheckBoxSelectedRoute.setAlignmentX(CENTER_ALIGNMENT);

		// add to panel
		jPanelOverlay.add(jLabelOverlay);
		jPanelOverlay.add(jCheckBoxStreetMap);
		jPanelOverlay.add(jCheckBoxGPSTrace);
		// jPanelOverlay.add(jCheckBoxNRoute);
		// jPanelOverlay.add(jCheckBoxSelectedRoute);

		// add to container
		container.add(jPanelOverlay);
	}

	/*
	 * private void initMapMatchingPanel(Container container) { // create panel
	 * jPanelMapMatching = new JPanelBoxLayout(BoxLayout.Y_AXIS,
	 * container.getWidth(), 5, "Map Matching Algorithm");
	 * 
	 * jPanelMapMatching.setAlignmentX(LEFT_ALIGNMENT);
	 * jPanelMapMatching.setMaximumSize(container.getMaximumSize());
	 * jPanelMapMatching.setMinimumSize(container.getMinimumSize());
	 * 
	 * /* // create JComboBox jComboBoxMapMatching = new
	 * JComboBox<String>(mapMatchingAlgorithms);
	 * jComboBoxMapMatching.setMaximumSize(new Dimension (150,20));
	 * jComboBoxMapMatching.setAlignmentX(CENTER_ALIGNMENT);
	 * 
	 * // add components to map matching panel
	 * jPanelMapMatching.add(jComboBoxMapMatching);
	 */
	/*
	 * // add to container container.add(jPanelMapMatching); }
	 */

	private void initNRoutePanel(Container container) {
		// initialize hash table containing captions for match button
		// according to algorithm state
		nRouteAlgorithmMatchButtonCaptions = new Hashtable<String, String>();
		nRouteAlgorithmMatchButtonCaptions.put(N_ROUTE_RUNNING, "Pause");
		nRouteAlgorithmMatchButtonCaptions.put(N_ROUTE_PAUSED, "Resume");
		nRouteAlgorithmMatchButtonCaptions.put(N_ROUTE_RECESSED, "Route");

		// the same for GPS to N Link matching
		matchGPStoNRouteAlgorithmMatchButtonCaptions = new Hashtable<String, String>();
		matchGPStoNRouteAlgorithmMatchButtonCaptions.put(
				MATCH_GPS_TO_N_ROUTE_RUNNING, "Pause");
		matchGPStoNRouteAlgorithmMatchButtonCaptions.put(
				MATCH_GPS_TO_N_ROUTE_PAUSED, "Resume");
		matchGPStoNRouteAlgorithmMatchButtonCaptions.put(
				MATCH_GPS_TO_N_ROUTE_RECESSED, "Match");

		// create panel
		// jPanelNRoute = new JPanelBoxLayout(BoxLayout.Y_AXIS,
		// container.getWidth(), 5);
		jPanelNRoute = new JPanelBoxLayout(BoxLayout.Y_AXIS,
				container.getWidth(), 5, "Map Matching Algorithm");

		jPanelNRoute.setAlignmentX(LEFT_ALIGNMENT);
		jPanelNRoute.setMaximumSize(container.getMaximumSize());
		jPanelNRoute.setMinimumSize(container.getMinimumSize());

		// create label
		jLabelNRoute = new JLabel("N Route Algorithm");
		jLabelNRoute.setAlignmentX(CENTER_ALIGNMENT);
		// create toggle button
		jToggleButtonNRouteAlgorithm = new JToggleButton("N Route");
		jToggleButtonNRouteAlgorithm.setActionCommand("N Route Algorithm");
		jToggleButtonNRouteAlgorithm.setAlignmentX(CENTER_ALIGNMENT);
		// create label
		//jLabelSelectNRoute = new JLabel("Choose route: 1");
		//jLabelSelectNRoute.setAlignmentX(CENTER_ALIGNMENT);
		// create JSlider
		jSliderSelectNRoute = new JSlider(JSlider.HORIZONTAL, 1, 7, 1);
		jSliderSelectNRoute.setMaximumSize(new Dimension(150, 20));
		jSliderSelectNRoute.setAlignmentX(CENTER_ALIGNMENT);
		// create label
		jLabelNRouteSize = new JLabel("max. container size");
		jLabelNRouteSize.setAlignmentX(CENTER_ALIGNMENT);
		// create JSpinner and its eligible model (number model)
		SpinnerModel spinnerNumberModelMaxContainerSize = new SpinnerNumberModel(
				DEFAULT_N_ROUTE_SIZE, // initial value
				MIN_N_ROUTE_SIZE, // min
				MAX_N_ROUTE_SIZE, // max
				1); // step
		jSpinnerNRouteSize = new JSpinner(spinnerNumberModelMaxContainerSize);
		jSpinnerNRouteSize.setMaximumSize(new Dimension(50, 22));
		jSpinnerNRouteSize.setAlignmentX(CENTER_ALIGNMENT);
		// create label
		jLabelNRouteTreshold = new JLabel("Treshold ("
				+ MIN_INTERSECTION_REACHED_THRESHOLD + " - "
				+ MAX_INTERSECTION_REACHED_THRESHOLD + ")");
		jLabelNRouteTreshold.setAlignmentX(CENTER_ALIGNMENT);
		// create JSpinner and its eligible model (number model)
		SpinnerModel spinnerNumberModelTreshold = new SpinnerNumberModel(
				DEFAULT_INTERSECTION_REACHED_THRESHOLD, // init
				MIN_INTERSECTION_REACHED_THRESHOLD, // min
				MAX_INTERSECTION_REACHED_THRESHOLD, // max
				0.1); // step
		jSpinnerNRouteThreshold = new JSpinner(spinnerNumberModelTreshold);
		jSpinnerNRouteThreshold.setMaximumSize(new Dimension(50, 22));
		jSpinnerNRouteThreshold.setAlignmentX(CENTER_ALIGNMENT);

		// create check boxes
		jCheckBoxReorderNMatch = new JCheckBox("Reorder");
		jCheckBoxReorderNMatch.setAlignmentX(CENTER_ALIGNMENT);
		jCheckBoxProjectNMatch = new JCheckBox("Project");
		jCheckBoxProjectNMatch.setAlignmentX(CENTER_ALIGNMENT);

		jCheckBoxKMLNorm = new JCheckBox("KML Norm");
		this.setKMLNorm(true);
		jCheckBoxKMLNorm.setAlignmentX(CENTER_ALIGNMENT);
		
		jCheckBoxUniqueGPS = new JCheckBox("Unique GPS");
		jCheckBoxUniqueGPS.setToolTipText("different matched neighboring measured value");
		setUniqueGPS(true);
		jCheckBoxUniqueGPS.setAlignmentX(CENTER_ALIGNMENT);
		
		// create JButtons
		jButtonNRouteRoute = new JButton("Route");
		jButtonNRouteRoute.setActionCommand("change N route algorithm state");
		jButtonNRouteMatch = new JButton("Match");
		jButtonNRouteMatch.setActionCommand("change match GPS to N route algorithm state");
		//jButtonNRouteOpen = new JButton("Open");
		//jButtonNRouteOpen.setActionCommand("open N route");
		jButtonNRouteExport = new JButton("Export");
		jButtonNRouteExport.setActionCommand("export N match");
		//jButtonNRouteSave = new JButton("Save");
		//jButtonNRouteSave.setActionCommand("save N route");
		//jButtonNRouteReset = new JButton("Reset");
		//jButtonNRouteReset.setActionCommand("change N route algorithm state");

		// create check box
		jCheckBoxNRouteNormalizeMatchedGPSTimeStamp_2 = new JCheckBox(
				"Normalize");
		jCheckBoxNRouteNormalizeMatchedGPSTimeStamp_2
				.setAlignmentX(CENTER_ALIGNMENT);

		// create horizontal boxes
		Box horBox1 = Box.createHorizontalBox();
		Box horBox2 = Box.createHorizontalBox();
		Box horBox2_1 = Box.createHorizontalBox();
		Box horBox3 = Box.createHorizontalBox();
		//Box horBox4 = Box.createHorizontalBox();
		Box horBox5 = Box.createHorizontalBox();
		
		horBox1.add(jButtonNRouteRoute);
		horBox2.add(jButtonNRouteMatch);
		//horBox2.add(jButtonNRouteOpen);
		horBox3.add(jButtonNRouteExport);
		//horBox3.add(jButtonNRouteSave);
		
		horBox5.add(jCheckBoxKMLNorm);
		
		horBox2_1.add(jCheckBoxUniqueGPS);
		
		//horBox4.add(jButtonNRouteReset);

		// add to panel
		jPanelNRoute.add(jLabelNRoute);
		jPanelNRoute.add(jToggleButtonNRouteAlgorithm);
		// jPanelNRoute.add(jLabelSelectNRoute);
		// jPanelNRoute.add(jSliderSelectNRoute);
		jPanelNRoute.add(jLabelNRouteSize);
		jPanelNRoute.add(jSpinnerNRouteSize);
		// jPanelNRoute.add(jLabelNRouteTreshold);
		// jPanelNRoute.add(jSpinnerNRouteThreshold);
		//jPanelNRoute.add(jCheckBoxReorderNMatch);
		//jPanelNRoute.add(jCheckBoxProjectNMatch);
		
		jPanelNRoute.add(horBox1);
		jPanelNRoute.add(horBox2);
		jPanelNRoute.add(horBox2_1);
		jPanelNRoute.add(horBox3);
		//jPanelNRoute.add(horBox4);
		jPanelNRoute.add(horBox5);
		
		// jPanelNRoute.add(jCheckBoxNRouteNormalizeMatchedGPSTimeStamp_2);

		// add panel to container
		container.add(jPanelNRoute);
	}

	private void initSelectRoutePanel(Container container) {
		// create panel
		/*
		jPanelSelectRoute = new JPanelBoxLayout(BoxLayout.Y_AXIS, container.getWidth(), 5, "Route Select");

		jPanelSelectRoute.setAlignmentX(LEFT_ALIGNMENT);
		jPanelSelectRoute.setMaximumSize(container.getMaximumSize());
		jPanelSelectRoute.setMinimumSize(container.getMinimumSize());
		 */

		// create label
		//jLabelSelectRoute = new JLabel("Select Route manually");
		//jLabelSelectRoute.setAlignmentX(CENTER_ALIGNMENT);
		// create toggle button
		jToggleButtonSelectRoute = new JToggleButton("Select Route");
		jToggleButtonSelectRoute.setActionCommand("Select Route");
		jToggleButtonSelectRoute.setAlignmentX(CENTER_ALIGNMENT);
		// create label
		jLabelExport = new JLabel("Save matched GPS Nodes");
		jLabelExport.setAlignmentX(CENTER_ALIGNMENT);
		// create button
		//jButtonExport = new JButton("Export");
		//jButtonExport.setActionCommand("Export matched GPS Nodes to file");
		//jButtonExport.setAlignmentX(CENTER_ALIGNMENT);
		// create check box
		jCheckBoxNormalizeGPSTimeStamp = new JCheckBox("Normalize");
		jCheckBoxNormalizeGPSTimeStamp.setAlignmentX(CENTER_ALIGNMENT);
		// add to panel
		/*
		jPanelSelectRoute.add(jLabelSelectRoute);
		jPanelSelectRoute.add(jToggleButtonSelectRoute);
		jPanelSelectRoute.add(jLabelExport);
		jPanelSelectRoute.add(jButtonExport);
		jPanelSelectRoute.add(jCheckBoxNormalizeGPSTimeStamp);
		*/

		// add to container
		// container.add(jPanelSelectRoute);
	}

	private void initStatusBar(Container container) {
		// create panel
		jPanelStatusBar = new JPanel(new BorderLayout());

		// initialize info label at the bottom of the Frame
		jLabelInfo = new JLabel();

		// create progress bar
		jProgressBar = new JProgressBar();
		jProgressBar.setPreferredSize(new Dimension(150, 20));
		jProgressBar.setStringPainted(true);
		jProgressBar.setVisible(false);

		// add to status bar
		jPanelStatusBar.add(jLabelInfo, BorderLayout.WEST);
		jPanelStatusBar.add(Box.createRigidArea(new Dimension(0, 20)),
				BorderLayout.CENTER); // keeps status bar's height constant
		jPanelStatusBar.add(jProgressBar, BorderLayout.EAST);

		// add status bar to container (bottom position)
		container.add(jPanelStatusBar, BorderLayout.SOUTH);
	}

	private void setRightScrollPaneScrollbarPosition() {
		// get and set mid value of horizontal scroll bar maximum as new value
		// middle position seems to be max value / 4
		// int midHorizontalPos = (int)
		// (jScrollPaneRight.getHorizontalScrollBar().getMaximum() / 4.0);
		// jScrollPaneRight.getHorizontalScrollBar().setValue(midHorizontalPos);
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * return instance of jxMapKit
	 */
	@Override
	public JXMapKit getJXMapKit() {
		// return reference to JXMapKit
		return jxMapKit;
	}

	/**
	 * set custom tile factory for map representation with custom values for
	 * zoom, size and orientation
	 */
	@Override
	public void setTileFactory(String baseURL, String xParam, String yParam,
			String zParam, int minZoom, int maxZoom, final int totalZoom,
			int tileSize, boolean defaultXOrientation,
			boolean defaultYOrientation) {

		// initialize custom tile factory
		TileFactoryInfo tileFactoryInfo = new TileFactoryInfo(minZoom, // minimum
																		// Zoom-Level
				maxZoom, // maximum zoom-Level
				totalZoom, // total map-zoom
				tileSize, // tile-size
				defaultXOrientation, // normal x-orientation
				defaultYOrientation, // normal y-orientation
				baseURL, xParam, yParam, zParam // URL
		) {
			@Override
			public String getTileUrl(int x, int y, int z) {
				return this.baseURL + (totalZoom - z) + "/" + x + "/" + y + ".png";
			}
		};
		
		// add new tile factory to our map kit
		jxMapKit.setTileFactory(new DefaultTileFactory(tileFactoryInfo));
	}

	/**
	 * set custom tile factory for map representation with default values for
	 * zoom, size and orientation
	 */

	@Override
	public void setTileFactory(String url, String xParam, String yParam, String zParam) {
		// call overloaded method with default values
		
		if (url.endsWith("/") == false) {
			url = url + "/";
		}
		
		setTileFactory(url, xParam, yParam, zParam, MIN_ZOOM_DEFAULT,
				MAX_ZOOM_DEFAULT, TOTAL_ZOOM_DEFAULT, TILE_SIZE_DEFAULT,
				X_ORIENTATION_DEFAULT, Y_ORIENTATION_DEFAULT);
	}

	/**
	 * register event listener, cast listener class if necessary
	 * 
	 * @param eventListener
	 */
	@Override
	public void setEventListener(EventListener eventListener) {
		// add listener for buttons
		jButtonOpenGPSTrace.addActionListener((ActionListener) eventListener);
		jButtonOpenStreetMap.addActionListener((ActionListener) eventListener);
		jButtonNRouteRoute.addActionListener((ActionListener) eventListener);
		//jButtonNRouteReset.addActionListener((ActionListener) eventListener);
		//jButtonNRouteOpen.addActionListener((ActionListener) eventListener);
		jButtonNRouteExport.addActionListener((ActionListener) eventListener);
		//jButtonNRouteSave.addActionListener((ActionListener) eventListener);
		jButtonNRouteMatch.addActionListener((ActionListener) eventListener);
		// add listener for check boxes
		jCheckBoxStreetMap.addItemListener((ItemListener) eventListener);
		jCheckBoxGPSTrace.addItemListener((ItemListener) eventListener);
		jCheckBoxNRoute.addItemListener((ItemListener) eventListener);
		//jCheckBoxSelectedRoute.addItemListener((ItemListener) eventListener);
		// add listeners for toggle buttons
		jToggleButtonSelectRoute
				.addActionListener((ActionListener) eventListener);
		jToggleButtonNRouteAlgorithm
				.addActionListener((ActionListener) eventListener);
		// add listener for sliders
		jSliderSelectNRoute.addChangeListener((ChangeListener) eventListener);
		// add listener for spinners
		jSpinnerNRouteSize.addChangeListener((ChangeListener) eventListener);
		// add listener for matched GPS nodes export
		//jButtonExport.addActionListener((ActionListener) eventListener);
		// add listeners for JXMapKit
		jxMapKit.getMainMap().addMouseListener((MouseListener) eventListener);
		jxMapKit.getMainMap().addMouseMotionListener(
				(MouseMotionListener) eventListener);
		jxMapKit.getMainMap().addKeyListener((KeyListener) eventListener);
	}

	// /////////////////////////////////////////// GUI ENABLE/DISABLE
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * enables/disables GUI in which included panels separately gets regarded
	 * according to their state
	 */
	@Override
	public void enableGUI(boolean enable) {
		// enable/disable whole GUI
		isGUIEnabled = enable;
		Tools.enableComponentsInsideContainer(this, enable);

		// enable/disable overlay panel according to state
		enableOverLayPanel(enable);
		enableNRouteAlgorithmPanel(enable);
		enableSelectRoutePanel(enable);

		// except status bar, must always be enabled
		Tools.enableComponentsInsideContainer(jPanelStatusBar, true);

		// set hour glass mouse cursor if GUI gets disabled, otherwise restore
		// normal cursor
		setCursor(enable ? null : Cursor
				.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	/**
	 * enables/disables drawing components according to their permission to be
	 * enabled
	 * 
	 * @param enable
	 */
	private void enableOverLayPanel(boolean enable) {
		// enable/disable overlay panel relevant to state
		jCheckBoxStreetMap.setEnabled((enable && isGUIEnabled && isDrawStreetMapAvailable));
		jCheckBoxGPSTrace.setEnabled((enable && isGUIEnabled && isDrawGPSTraceAvailable));
		jCheckBoxNRoute.setEnabled((enable && isGUIEnabled && isDrawNRouteAvailable));
		//jCheckBoxSelectedRoute.setEnabled((enable && isGUIEnabled && isDrawSelectedRouteAvailable));
	}

	/**
	 * enables/disables manual route selecting panel according to their
	 * permission to be enabled
	 * 
	 * @param enable
	 */
	private void enableSelectRoutePanel(boolean enable) {
		//Tools.enableComponentsInsideContainer(jPanelSelectRoute, (enable && isGUIEnabled && isSelectRouteModeAvailable));
	}

	/**
	 * enables/disables manual route selecting panel according to their
	 * permission to be enabled
	 * 
	 * @param enable
	 */
	private void enableNRouteAlgorithmPanel(boolean enable) {
		// check conditions for activating
		boolean enableInGeneral = (enable && isGUIEnabled && isNRouteAlgorithmAvailable);
		boolean enableToogleButton = (enableInGeneral && (nRouteAlgorithmState == N_ROUTE_RECESSED));
		boolean enableComponents = enableInGeneral && isNRouteAlgorithmModeSet;
		boolean enableSpinner = (enableComponents && (nRouteAlgorithmState == N_ROUTE_RECESSED));
		boolean enableRoute = (enableComponents && isNRouteRouteAvailable);
		boolean enableMatch = (enableComponents && isNRouteMatchAvailable);
		//boolean enableOpen = (enableComponents && (nRouteAlgorithmState == N_ROUTE_RECESSED || nRouteAlgorithmState == N_ROUTE_PAUSED));
		//boolean enableSave = (enableComponents && isNRouteSaveAvailable);
		boolean enableExport = (enableComponents && isNRouteExportAvailable);
		//boolean enableReset = (enableComponents && isNRouteResetAvailable);

		// enable components if N route algorithm mode is activated
		//Tools.enableComponentsInsideContainer(jPanelNRoute, enableComponents);

		// consider toggle button, spinner and buttons separately considering n
		// route algorithm state
		jToggleButtonNRouteAlgorithm.setEnabled(enableToogleButton);
		jSpinnerNRouteSize.setEnabled(enableSpinner);
		jSpinnerNRouteThreshold.setEnabled(enableSpinner);
		jButtonNRouteRoute.setEnabled(enableRoute);
		jButtonNRouteMatch.setEnabled(enableMatch);
		//jButtonNRouteOpen.setEnabled(enableOpen);
		//jButtonNRouteSave.setEnabled(enableSave);
		jButtonNRouteExport.setEnabled(enableExport);
		jCheckBoxNRouteNormalizeMatchedGPSTimeStamp_2.setEnabled(enableExport);
		//jButtonNRouteReset.setEnabled(enableReset);
		
		jCheckBoxKMLNorm.setEnabled(enableExport);
		
		jCheckBoxUniqueGPS.setEnabled(enableMatch);

		// get caption of match button according to current match GPS/n route
		// algorithm states
		jButtonNRouteRoute.setText(nRouteAlgorithmMatchButtonCaptions.get(nRouteAlgorithmState));
		jButtonNRouteMatch.setText(matchGPStoNRouteAlgorithmMatchButtonCaptions.get(matchGPStoNRouteAlgorithmState));
	}

	/*
	 * private void enableNRouteAlgorithmPanel(boolean enable) { // check
	 * conditions for activating boolean enableToogleButton = (enable &&
	 * isGUIEnabled && isNRouteAlgorithmAvailable); boolean enableComponents =
	 * enableToogleButton && isNRouteAlgorithmModeSet;
	 * 
	 * // enable components if N route algorithm mode is activated
	 * Tools.enableComponentsInsideContainer(jPanelNRoute, enableComponents);
	 * 
	 * // consider toggle button seperately cause n route algorithm mode must
	 * not be set jToggleButtonNRouteAlgorithm.setEnabled(enableToogleButton); }
	 */

	/**
	 * prepares the rest of the GUI for a map matching modus by disabling not
	 * needed components
	 * 
	 * @param enable
	 */
	private void enableComponentsForMatchingOperations(boolean enable) {
		// enable/disable whole GUI
		Tools.enableComponentsInsideContainer(this, !enable);
		// except jxMapKit, overlay panel and status panel
		enableBasicElements();
	}

	private void enableComponentsForMatchingOperations(boolean enable, boolean allowLoadMap, boolean allowLoadGPSTrace) {
		enableComponentsForMatchingOperations(enable);
		//jButtonOpenStreetMap.setEnabled(allowLoadMap);
		//jButtonOpenGPSTrace.setEnabled(allowLoadGPSTrace);
	}

	/**
	 * this method enables basic elements of the GUI like status bar and
	 * JXMapKit
	 */
	private void enableBasicElements() {
		// keep jxMapKit, scroll pane and status panel enabled
		enableOverLayPanel(true);
		// jScrollPaneRight.setEnabled(true);
		jPanelRight.setEnabled(true);
		Tools.enableComponentsInsideContainer(jxMapKit, true);
		Tools.enableComponentsInsideContainer(jPanelStatusBar, true);
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * set caption of GPS-trace-loading Button
	 */
	@Override
	public void setGPSButtonText(String text) {
		jButtonOpenGPSTrace.setText(text);
	}

	/**
	 * set caption of Map-loading Button
	 */
	@Override
	public void setStreetMapButtonText(String text) {
		jButtonOpenStreetMap.setText(text); // set text
	}

	@Override
	public void makeDrawStreetMapAvailable(boolean enable) {
		isDrawStreetMapAvailable = enable;
		jCheckBoxStreetMap.setEnabled(enable && isGUIEnabled);
	}

	@Override
	public void makeDrawGPSTraceAvailable(boolean enable) {
		isDrawGPSTraceAvailable = enable;
		jCheckBoxGPSTrace.setEnabled(enable && isGUIEnabled);
	}

	
	@Override
	public void makeDrawSelectedRouteAvailable(boolean enable) {
		//isDrawSelectedRouteAvailable = enable;
		//jCheckBoxSelectedRoute.setEnabled(enable && isGUIEnabled);
	}

	@Override
	public void makeDrawNRouteAvailable(boolean enable) {
		isDrawNRouteAvailable = enable;
		jCheckBoxNRoute.setEnabled(enable && isGUIEnabled);
	}

	@Override
	public void makeSelectRouteModeAvailable(boolean enable) {
		isSelectRouteModeAvailable = enable;
		enableSelectRoutePanel(isSelectRouteModeAvailable);
		// Tools.enableComponentsInsideContainer(jPanelSelectRoute, (enable &&
		// isGUIEnabled));
	}

	@Override
	public void makeNRouteAlgorithmModeAvailable(boolean enable) {
		isNRouteAlgorithmAvailable = enable;
		enableNRouteAlgorithmPanel(isNRouteAlgorithmAvailable);
	}

	@Override
	public void makeNRouteRouteAvailable(boolean enable) {
		isNRouteRouteAvailable = enable;
		// update N Route panel
		enableNRouteAlgorithmPanel(isNRouteAlgorithmAvailable);
	}

	@Override
	public void makeNRouteMatchAvailable(boolean enable) {
		isNRouteMatchAvailable = enable;
		// update N Route panel
		enableNRouteAlgorithmPanel(isNRouteAlgorithmAvailable);
	}

	@Override
	public void makeNRouteSaveAvailable(boolean enable) {
		//isNRouteSaveAvailable = enable;
		// update N Route panel
		enableNRouteAlgorithmPanel(isNRouteAlgorithmAvailable);
	}

	@Override
	public void makeNRouteExportAvailable(boolean enable) {
		isNRouteExportAvailable = enable;
		// update N Route panel
		enableNRouteAlgorithmPanel(isNRouteAlgorithmAvailable);
	}

	@Override
	public void makeNRouteResetAvailable(boolean enable) {
		//isNRouteResetAvailable = enable;
		// update N Route panel
		enableNRouteAlgorithmPanel(isNRouteAlgorithmAvailable);
	}

	@Override
	public void makeNRouteButtonsAvaiable(boolean route, boolean match, boolean save, boolean export, boolean reset) {
		isNRouteRouteAvailable = route;
		isNRouteMatchAvailable = match;
		//isNRouteSaveAvailable = save;
		isNRouteExportAvailable = export;
		//isNRouteResetAvailable = reset;
		// update N Route panel
		enableNRouteAlgorithmPanel(isNRouteAlgorithmAvailable);
	}

	@Override
	public void setDrawStreetMap(boolean enabled) {
		jCheckBoxStreetMap.setSelected(enabled);
	}

	@Override
	public void setDrawGPSTrace(boolean enabled) {
		jCheckBoxGPSTrace.setSelected(enabled);
	}

	@Override
	public void setDrawNRoute(boolean enabled) {
		jCheckBoxNRoute.setSelected(enabled);
	}

	@Override
	public void setDrawSelectedRoute(boolean enabled) {
		//jCheckBoxSelectedRoute.setSelected(enabled);
	}

	@Override
	public boolean getDrawStreetMap() {
		return jCheckBoxStreetMap.isSelected(); // return status of check box
	}

	@Override
	public boolean getDrawGPSTrace() {
		return jCheckBoxGPSTrace.isSelected(); // return status of check box
	}

	@Override
	public boolean getDrawNRoute() {
		return jCheckBoxNRoute.isSelected(); // return status of check box
	}

	@Override
	public boolean getDrawSelectedRoute() {
		//return jCheckBoxSelectedRoute.isSelected(); // return status of check box
		return false;
	}

	@Override
	public void setNRouteAlgorithmMode(boolean set) {
		// don't allow loading GPS trace (default)
		setNRouteAlgorithmMode(set, false);
	}

	@Override
	public void setNRouteAlgorithmMode(boolean set, boolean allowLoadGPSTrace) {
		// save state
		isNRouteAlgorithmModeSet = set;
		// enable/disable components for matching operations
		enableComponentsForMatchingOperations(isNRouteAlgorithmModeSet, !set, !set || allowLoadGPSTrace);
		
		// disable select route panel
		enableSelectRoutePanel(!isNRouteAlgorithmModeSet);
		// enable/disable n route algorithm panel / check box for route drawing
		enableNRouteAlgorithmPanel(true);
		jCheckBoxNRoute.setEnabled(!isNRouteAlgorithmModeSet);
	}

	@Override
	public boolean getNRouteAlgorithmMode() {
		return jToggleButtonNRouteAlgorithm.isSelected();
	}

	@Override
	public void setSelectRouteMode(boolean set) {
		// save state
		isSelectRouteModeSet = set;
		// enable/disable components for matching operations
		enableComponentsForMatchingOperations(isSelectRouteModeSet);
		// disable/enable N route algorithm panel if this mode is
		// activated/deactivated
		enableNRouteAlgorithmPanel(!isSelectRouteModeSet);
		// enable/disable select route panel / check box for route drawing
		enableSelectRoutePanel(true);
		//jCheckBoxSelectedRoute.setEnabled(!isSelectRouteModeSet);
	}

	@Override
	public boolean getSelectRouteMode() {
		return jToggleButtonSelectRoute.isSelected();
	}

	@Override
	public boolean setNRouteAlgorithmState(String state) {
		// first of all check global state, if n route algorithm mode is set
		if (isNRouteAlgorithmModeSet) {

			// check if valid state constant was passed as parameter
			if ((state == N_ROUTE_RUNNING) || (state == N_ROUTE_PAUSED)
					|| (state == N_ROUTE_RECESSED)) {

				// save state
				nRouteAlgorithmState = state;

				// update N route algorithm panel
				enableNRouteAlgorithmPanel(true);

				// state successfully updated
				return true;
			}
		}

		// no valid state was given or N route algorithm mode was not set
		return false;
	}

	@Override
	public void setNormalizeNMatchedGPSTimeStamp(boolean enable) {
		jCheckBoxNRouteNormalizeMatchedGPSTimeStamp_2.setSelected(enable);
	}

	@Override
	public boolean getNormalizeNMatchedGPSTimeStamp() {
		return jCheckBoxNRouteNormalizeMatchedGPSTimeStamp_2.isSelected();
	}

	@Override
	public void setNormalizeGPSTimeStamp(boolean selected) {
		jCheckBoxNormalizeGPSTimeStamp.setSelected(selected);
	}

	@Override
	public boolean getNormalizeGPSTimeStamp() {
		return jCheckBoxNormalizeGPSTimeStamp.isSelected();
	}

	public Point getTranslatedMousePos(MouseEvent mouseEvent) {
		// get position of visible rectangle relative to whole map
		Rectangle rect = jxMapViewer.getViewportBounds();

		// get zoom factor = 2^(zoomFromJXMap - 1)
		double zoomFactor = Math.pow(2, jxMapViewer.getZoom() - 1);

		// return absolute x- and y-position = (rect.x/y + mouse.x/y) * zoom
		// factor
		return new Point(
				(int) ((rect.getX() + mouseEvent.getX()) * zoomFactor),
				(int) ((rect.getY() + mouseEvent.getY()) * zoomFactor));
	}

	@Override
	public int getTranslatedMousePosX(MouseEvent mouseEvent) {
		return getTranslatedMousePos(mouseEvent).x;
	}

	@Override
	public int getTranslatedMousePosY(MouseEvent mouseEvent) {
		return getTranslatedMousePos(mouseEvent).y;
	}

	@Override
	public void updateStatus(String updateMessage, float percent) {
		// show progress
		updateStatus(updateMessage);
		updateStatus(percent);
	}

	@Override
	public void updateStatus(String updateMessage) {
		// write status to info label
		jLabelInfo.setText(updateMessage);
	}

	@Override
	public void updateStatus(float percent) {
		// show working state via progress bar, make visible and turn off
		// intermediate mode
		if (jProgressBar.isIndeterminate())
			jProgressBar.setIndeterminate(false);
		if (!jProgressBar.isVisible())
			jProgressBar.setVisible(true);
		jProgressBar.setValue((int) percent);
		jProgressBar.setString((int) percent + "%");
	}

	@Override
	public void finished() {
		// clear status bar after work is done
		finished("");
	}

	@Override
	public void finished(String resultMessage) {
		updateStatus(resultMessage);
		jProgressBar.setVisible(false);
	}

	@Override
	public void updateUndefinedStatus(String undefinedMessage) {
		updateUndefinedStatus();
		jProgressBar.setString(undefinedMessage);
	}

	@Override
	public void updateUndefinedStatus(String undefinedMessage,
			String updateMessage) {
		updateUndefinedStatus(undefinedMessage);
		updateStatus(updateMessage);
	}

	@Override
	public void updateUndefinedStatus() {
		if (!jProgressBar.isIndeterminate())
			jProgressBar.setIndeterminate(true);
		if (!jProgressBar.isVisible())
			jProgressBar.setVisible(true);
	}

	// ///////////////////////////////////////////////////////////////// N Route
	// Panel
	// //////////////////////////////////////////////////////////////////////////

	@Override
	public void setNRouteSize(int nRouteSize) {
		// verify / normalize value
		if (nRouteSize > MAX_N_ROUTE_SIZE)
			nRouteSize = MAX_N_ROUTE_SIZE;
		if (nRouteSize < MIN_N_ROUTE_SIZE)
			nRouteSize = MIN_N_ROUTE_SIZE;

		// adjust spinner
		jSpinnerNRouteSize.setValue(nRouteSize);

		// update other settings
		updateNRouteSettings();
	}

	@Override
	public int getNRouteSize() {
		return ((Integer) jSpinnerNRouteSize.getValue()).intValue();
	}

	@Override
	public void setSelectedNRoute(int selectedNRoute) {
		jSliderSelectNRoute.setValue(selectedNRoute);

		// update other settings
		updateNRouteSettings();
	}

	@Override
	public int getSelectedNRoute() {
		return jSliderSelectNRoute.getValue() - 1;
	}

	@Override
	public void updateNRouteSettings() {
		// update slider and label
		jSliderSelectNRoute.setMaximum(((Integer) jSpinnerNRouteSize.getValue()).intValue());
		//jLabelSelectNRoute.setText("Choose route: "	+ jSliderSelectNRoute.getValue());
	}

	@Override
	public void setNRouteThreshold(double threshold) {
		// normalize given threshold value
		threshold = (threshold <= MAX_INTERSECTION_REACHED_THRESHOLD) ? threshold
				: MAX_INTERSECTION_REACHED_THRESHOLD;
		threshold = (threshold >= MIN_INTERSECTION_REACHED_THRESHOLD) ? threshold
				: MIN_INTERSECTION_REACHED_THRESHOLD;

		// set value to spinner
		jSpinnerNRouteThreshold.setValue(threshold);
	}

	@Override
	public double getNRouteThreshold() {
		// get double value of set intersection reached threshold
		return ((Double) jSpinnerNRouteThreshold.getValue()).doubleValue();
	}

	@Override
	public boolean setMatchGPStoNRouteAlgorithmState(String state) {
		// first of all check global state, if n route algorithm mode is set
		if (isNRouteAlgorithmModeSet) {

			// check if valid state constant was passed as parameter
			if ((state == N_ROUTE_RUNNING) || (state == N_ROUTE_PAUSED)
					|| (state == N_ROUTE_RECESSED)) {

				// save state
				nRouteAlgorithmState = state;

				// update N route algorithm panel
				enableNRouteAlgorithmPanel(true);

				// state successfully updated
				return true;
			}
		}

		// no valid state was given or N route algorithm mode was not set
		return false;
	}

	@Override
	public void setReorderNMatch(boolean enable) {
		jCheckBoxReorderNMatch.setSelected(enable);
	}

	@Override
	public boolean getReoderNMatch() {
		return jCheckBoxReorderNMatch.isSelected();
	}

	@Override
	public void setKMLNorm(boolean enable) {
		jCheckBoxKMLNorm.setSelected(enable);
	}

	@Override
	public boolean getKMLNorm() {
		return jCheckBoxKMLNorm.isSelected();
	}
	
	@Override
	public void setUniqueGPS(boolean enable) {
		jCheckBoxUniqueGPS.setSelected(enable);
	}

	@Override
	public boolean getUniqueGPS() {
		return jCheckBoxUniqueGPS.isSelected();
	}

	
	@Override
	public void setProjectNMatch(boolean enable) {
		jCheckBoxProjectNMatch.setSelected(enable);
	}

	@Override
	public boolean getProjectNMatch() {
		return jCheckBoxProjectNMatch.isSelected();
	}

}