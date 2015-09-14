package myClasses;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

public class myOSMMap {

	public Map<Long, myOSMNode> nodes = new HashMap<Long, myOSMNode>();
	public long count_nodes = 0;
	
	public TreeMap<Integer, myOSMWay> ways = new TreeMap<Integer, myOSMWay>();
	
	public double osmMinLat;
	public double osmMaxLat;
	public double osmMinLon;
	public double osmMaxLon;
    
    public String osmVersion = "";
    public String osmGenerator = "";
	
	//XML classes
	private XMLInputFactory factory = XMLInputFactory.newInstance(); 
	private XMLStreamReader parser;
	
	//file class to access XML file
	public File osmFile;
	
	//spacer to check XML formation
	private StringBuilder spacer = new StringBuilder();

	private myOSMWay tempWay = new myOSMWay(this);

	private String lastkey = ""; 
	
	private int parseXML_status = 0;
	// 0 = Scan OSM file: search needed nodes of "car" ways
	// 1 = Create nodes and ways
	// 2 = No scan of OSM file; create all nodes and ways
	
	private Map<Integer, Long> nodeIdsOfWay = new HashMap<Integer, Long>();
	private TreeSet<Long> neededNodesIds = new TreeSet<Long>();
	private boolean isBuildingWay = false;
	
	private int anzahl_ways = 0;
	private int anzahl_ways_Building = 0;
	private int anzahl_ways_Car = 0;
	
	public Map<Long, Map<Integer, myEdge>> edges = new HashMap<Long, Map<Integer, myEdge>>();
	
	public Vector<myDataset> DatasetsUp = new Vector<myDataset>(200);
	public Vector<myDataset> DatasetsDown = new Vector<myDataset>(200);
	
	public Vector<myCellInfo> CellInfos = new Vector<myCellInfo>();
	
	public myOSMMap() {
	}
	
	public myOSMMap(File _xmlFile, String netFilePath) {
		loadMapFiles(_xmlFile, netFilePath);
	}
	
	public void loadDatasets(String DatasetFolderPath) {
		DatasetsUp = myDataset.loadDatasetsUp(DatasetFolderPath + "upstream-data.csv");
		DatasetsDown = myDataset.loadDatasetsDown(DatasetFolderPath + "downstream-data.csv");
	}
	
	public void loadCellInfos(String CellInfoFolderPath) {
		CellInfos = myCellInfo.loadCellInfos(CellInfoFolderPath + "cellinfo.txt");
	}
	
	/*
	 * initialization
	 */
	private void init() {
		this.nodes = new HashMap<Long, myOSMNode>();
		this.count_nodes = 0;
		
		this.ways = new TreeMap<Integer, myOSMWay>();
	    
		this.osmVersion = "";
	    this.osmGenerator = "";
		
		//XML classes
	    this.factory = XMLInputFactory.newInstance(); 
		
		//spacer to check XML formation
		this.spacer = new StringBuilder();

		this.tempWay = new myOSMWay(this);

		this.lastkey = ""; 
		
		this.parseXML_status = 3;
		this.nodeIdsOfWay = new HashMap<Integer, Long>();
		this.neededNodesIds = new TreeSet<Long>();
		this.isBuildingWay = false;
		
		this.anzahl_ways = 0;
		this.anzahl_ways_Building = 0;
		this.anzahl_ways_Car = 0;
		
		this.edges = new HashMap<Long, Map<Integer, myEdge>>();		
	}
	
	/*
	 * load osm file (_xmlFile) and netconvert file (netFilePath)
	 */
	public void loadMapFiles(File _xmlFile, String netFilePath) {

		init();
		
		osmFile = _xmlFile;
		
		try {
			
			Map<Integer, myEdge> edgesTemp = myEdge.loadGetEdges(netFilePath);
			
			for (int i = 0 ; i < edgesTemp.size(); i++) {
				
				myEdge e = edgesTemp.get(i);
				
				if (edges.containsKey(e.osmWayId)) {
					
					Map<Integer, myEdge> me = edges.get(e.osmWayId);
					
					me.put(me.size(), e);
					
					edges.put(e.osmWayId, me);
				} else {
					Map<Integer, myEdge> me = new HashMap<Integer, myEdge>();
					
					me.put(me.size(), e);
					
					edges.put(e.osmWayId, me);
				}
			}
			
			parser = factory.createXMLStreamReader( new FileInputStream( osmFile));
		} catch (Exception e) {
			System.err.println("Error: myOSMMap(...) " + e.toString());
		}
		
		if (parseXML_status == 0) {
			parseXML(false);
			
			try {
				parser = factory.createXMLStreamReader( new FileInputStream( osmFile));
			} catch (Exception e) {
				System.err.println("Error: " + e.toString());
			}
			
			parseXML_status = 1;
		}
		
		nodeIdsOfWay.clear();
		isBuildingWay = false;
		
		parseXML(true);

	}

	/*
	 * return dataset (upstream) nearest to Timestamp
	 */
	public myDataset getDatasetUp (long Timestamp) {
		for (int i = 0; i < DatasetsUp.size(); i++) {
			if (Timestamp <= DatasetsUp.get(i).getTimestamp()) {
				return DatasetsUp.get(i);
			}
		}
		return null;
	}

	/*
	 * return dataset (downstream) nearest to Timestamp
	 */
	public myDataset getDatasetDown (long Timestamp) {
		for (int i = 0; i < DatasetsDown.size(); i++) {
			if (Timestamp <= DatasetsDown.get(i).getTimestamp()) {
				return DatasetsDown.get(i);
			}
		}
		return null;
	}
	
	public int getNrOfAllWayParts() {
		int z = 0;
		for(int i = 0; i < ways.size(); i++) {
				z = z + ways.get(i).WayParts.length;	
		}
		return z;
	}


	public void removeUnusedNotesAndWaysAndSetWayParts() {
		for(int i = (ways.size() - 1); i >= 0 ; i--) {
			ways.get(i).setXYOfNotes();
		}
	}

	
	/**
	 * Parses the XML file to a dynamic osmData Datastructure
	 * @return true if no error
	 */
	private boolean parseXML(boolean showOsmInfo){		
		anzahl_ways = 0;
		anzahl_ways_Building = 0;
		anzahl_ways_Car = 0;

		long index_loop = 0;

		//start reading xml data via "stream"
		try {

			parser_loop:

			while ( parser.hasNext() ) 
			{ 
				index_loop++;
				
				if ((index_loop % 1000000) == 0) {
					System.out.print((new GregorianCalendar()).getTime().toString() + " | " + index_loop);
					System.out.println( " | nodes: " + count_nodes + " | ways: " + anzahl_ways + " | w-building: " + anzahl_ways_Building + " | w-cars: " + anzahl_ways_Car );
				}

				boolean Systemoutprint = false;		

				if (Systemoutprint) System.out.println( "Event: " + parser.getEventType() );
				switch (parser.getEventType()) 
				{ 
					case XMLStreamConstants.START_DOCUMENT: 
						if (Systemoutprint) System.out.println( "START_DOCUMENT: " + parser.getVersion() ); 
						break; 

					case XMLStreamConstants.END_DOCUMENT: 
						if (Systemoutprint) System.out.println( "END_DOCUMENT: " ); 
						parser.close(); 
						break; 
	
					case XMLStreamConstants.NAMESPACE: 
						if (Systemoutprint) System.out.println( "NAMESPACE: " + parser.getNamespaceURI() ); 
						break; 
	
					case XMLStreamConstants.START_ELEMENT: 
						spacer.append( "  " ); 
						if (Systemoutprint) System.out.println( /*spacer + */ "START_ELEMENT: " + parser.getLocalName() + "\n" ); 
	
						if ( parser.getLocalName()=="node") {
							//handle nodes
							nodeHandler();
						}
						else if (parser.getLocalName()=="way") {
							//handle ways
							wayHandler();
							if (Systemoutprint) System.out.println("Way!\n");
						}
						else if (parser.getLocalName()=="nd") {
							//handle node references in ways
							referenceHandler();
						}
						else if (parser.getLocalName()=="tag" ) {
							//handle tags in ways
							tagHandler();
						}
						else if (parser.getLocalName()=="bounds") {
							//handle boundary of the XLM file
							boundsHandler( showOsmInfo );
						}
						else if (parser.getLocalName()=="osm") {
							//handle general OSM info
							osmHandler( showOsmInfo );
						}
						else if (parser.getLocalName()=="relation") {
							// stop parsing file, leave while loop, actually we don't this block at the moment
							parser.close();
							break parser_loop;
						}
						break; 
	
					case XMLStreamConstants.CHARACTERS: 
						if ( ! parser.isWhiteSpace() ){ 
							//System.out.println( spacer + "  CHARACTERS: " + parser.getText() );
							;
						}
						break; 
	
					case XMLStreamConstants.END_ELEMENT:
						// Save way
						if (parser.getLocalName()=="way" && (tempWay.wayNotNeeded == false)) {
							addWay();
						}
	
						//System.out.println( spacer + "END_ELEMENT: " + parser.getLocalName() ); 
						spacer.delete(spacer.length()-2, spacer.length()); 
						break; 
	
					default: 
						break; 
				}

				parser.next(); 
			}
		} catch (Exception e) {
			System.err.println("Error parsing XML File: \n" + e.toString() + "\n" + index_loop);
			return false;
		} 
		return true;
	}

	/**
	 * Handles OSM Nodes
	 */
	public void nodeHandler() {

		myOSMNode node = new myOSMNode();

		//read node data
		for ( int i = 0; i < parser.getAttributeCount(); i++ ) {
	    	  if (parser.getAttributeLocalName(i)=="id")
	    		  node.id = Long.valueOf(parser.getAttributeValue(i));
	    	  else if (parser.getAttributeLocalName(i)=="lat")
	    		  node.lat = Double.valueOf(parser.getAttributeValue(i)).doubleValue();
	    	  else if (parser.getAttributeLocalName(i)=="lon")
	    		  node.lon = Double.valueOf(parser.getAttributeValue(i)).doubleValue();
	    }

		if (this.parseXML_status == 1) {
			if (this.neededNodesIds.contains(node.id)) {
				nodes.put(node.id, node);
			}
		} else if (this.parseXML_status == 3) {
			nodes.put(node.id, node);
		}
		count_nodes++;
	}

	/**
	 * Handles OSM ways
	 */
	public void wayHandler() {

		tempWay = new myOSMWay(this);
		nodeIdsOfWay.clear();
		this.isBuildingWay = false;

  	  	//now we check all Attributes of the way element
  	  	for ( int i = 0; i < parser.getAttributeCount(); i++ ) {

  	  		if (parser.getAttributeLocalName(i).equals("id")) {

  	  	  		tempWay.id = Long.valueOf(parser.getAttributeValue(i));

  	  		}

  	  	}
 
	}

	/**
	 * Handles XML References
	 */
	public void referenceHandler(){
		//safe all nodes that belongs to a way
		for ( int i = 0; i < parser.getAttributeCount(); i++ ) {
			if (parser.getAttributeLocalName(i)=="ref") {
				long l = Long.valueOf(parser.getAttributeValue(i));
				nodeIdsOfWay.put(nodeIdsOfWay.size(),  l);
			}
			else  {
				System.out.println("should never be called: referenceHandler");
				System.exit(-1);
			}
		}
	}

	/**
	 * Save OSM Way
	 */
	public void addWay()
	{
		anzahl_ways++;
		
		if (this.isBuildingWay == true) {

			anzahl_ways_Building++;
			
		} else {
			
			boolean useOsmParserFile = true;
			
			if (useOsmParserFile == false) {
				tempWay.setMeansOfTransport();
			}
			
			if (useOsmParserFile || tempWay.getMeansOfTransportPermission(myOSMWay.CAR)) {
				
				anzahl_ways_Car++;

				if (this.parseXML_status == 0) {
					for (int i = 0; i < nodeIdsOfWay.size(); i++) {
						long l = nodeIdsOfWay.get(i);
						this.neededNodesIds.add(l);
					}
				} else if (this.parseXML_status == 1 || this.parseXML_status == 3) {
					
					if (nodeIdsOfWay.size() <= 1) {
						System.out.println("Error: Way has only " + nodeIdsOfWay.size() + " refs");
						System.exit(-1);
					}
					
					tempWay.refs = new myOSMNode[nodeIdsOfWay.size()];

					for (int i = 0; i < nodeIdsOfWay.size(); i++) {
						long nodeID = nodeIdsOfWay.get(i);
						
						myOSMNode n = this.nodes.get(nodeID);
						
						if (n == null) {
							System.out.println("Error: n == null: addWay");
							System.exit(-1);
						} else {
							tempWay.refs[i] = n;
						}
					}
					
					tempWay.setWayParts();
					tempWay.map = this;
					this.ways.put(this.ways.size(), tempWay);
				}
			}
		}

		tempWay = new myOSMWay(this);
		this.isBuildingWay = false;
		this.nodeIdsOfWay.clear();
		
	}

	/**
	 * Handler for XML tags
	 */
	public void tagHandler() {
		
		for ( int i = 0; i < parser.getAttributeCount(); i++ ) {
			//as all tags are (k)ey / (v)alue pairs (in this order) we remember the last key and halde the
  		  	//assignment when we find a value.
  		  	if (parser.getAttributeLocalName(i).equals("k")) {		  		
  		  		lastkey = parser.getAttributeValue(i);
  		  	}
  		  	else if (parser.getAttributeLocalName(i).equals("v")){
  		  		if (lastkey.equals("created_by") || lastkey.equals("visible")) {
  		  			//ignore
  		  			lastkey="";
  		  		}	
  		  		else if (lastkey.equals("highway")){
  		  			tempWay.highway = parser.getAttributeValue(i);
  		  			if (tempWay.highway.equals("service")) {
  		  				//System.out.println("Service route, way id="+id);
  		  				tempWay.wayNotNeeded = true;	//we don't need this way
  		  			}
  		  		}
  		  		else if (lastkey.equals("motorcar")) {
		  			tempWay.motorcar = parser.getAttributeValue(i);
		  		}
  		  		else if (lastkey.equals("building")) {
  		  			if (parser.getAttributeValue(i).equals("yes")) {
  		  				this.isBuildingWay = true;
  		  			}
  		  		}
  		  		else if (lastkey.equals("oneway")) {
  		  			String s = parser.getAttributeValue(i);
  		  			
  		  			if (s.equals("yes") || s.equals("-1")) {
  		  				tempWay.onyWay = true;
  		  			} else {
  		  				tempWay.onyWay = Boolean.valueOf(s);
  		  			}
  		  		}
  		  		else if (lastkey.equals("lanes")){
  		  			try {
  	  		  			tempWay.lanes = Integer.parseInt( parser.getAttributeValue(i).split(";")[0] );  		  				
  		  			} catch (NumberFormatException e) {
  		  				tempWay.lanes = 1;
  		  			}
  		  		}
  		  		else if (lastkey.equals("name")){
  		  			tempWay.name = parser.getAttributeValue(i);
  		  		}
  		  		else if (lastkey.equals("railway") && parser.getAttributeValue(i).equals("tram")){
  		  			tempWay.meansOfTransport |= myOSMWay.TRAM;
  		  		}
  		  		else if (lastkey.equals("area") && parser.getAttributeValue(i).equals("yes")) {
  		  			tempWay.wayNotNeeded = true;
		  		}
  		  	}
	    	else {
				System.out.println("should never be called: tagHandler");
	    	}
		}
	}
	
	/**
	 * Handles OSM tag 
	 */
	public void osmHandler(boolean showOsmInfo){
		//read OSM general info
		for ( int i=0; i < parser.getAttributeCount(); i++){
			if (parser.getAttributeLocalName(i).equals("version"))
				osmVersion = parser.getAttributeValue(i);
			else if (parser.getAttributeLocalName(i).equals("generator"))
				osmGenerator = parser.getAttributeValue(i);
		}
		//print these info
		if (showOsmInfo) {
			System.out.println("Parsing "+osmFile.getName()+"...\nOSM-Version: "+osmVersion+"\nGenerator: "+osmGenerator);
		}
		
	}

	/**
	 * handles boundary tag
	 */
	public void boundsHandler(boolean showOsmInfo){
		
  	  	//now we check all Attributes of the bounds
  	  	for ( int i = 0; i < parser.getAttributeCount(); i++ ) {
  	  		if (parser.getAttributeLocalName(i).equals("minlat"))
  	  			osmMinLat = Double.valueOf(parser.getAttributeValue(i));
  	  		else if (parser.getAttributeLocalName(i).equals("maxlat"))
  	  			osmMaxLat = Double.valueOf(parser.getAttributeValue(i));
  	  		else if (parser.getAttributeLocalName(i).equals("minlon"))
  	  			osmMinLon = Double.valueOf(parser.getAttributeValue(i));
  	  		else if (parser.getAttributeLocalName(i).equals("maxlon"))
  	  			osmMaxLon = Double.valueOf(parser.getAttributeValue(i));
  	  		else   
  	  			System.out.println("should never be called: boundsHandler");		    	
  	  	}
  	  
  	  	//print min,max lat/lon of OSM-file
  	  
  	  	if (showOsmInfo) {
  	  		System.out.println("OSM-file boundary min(Lat/Lon),max(Lat/Lon) : ("+
  			  			  osmMinLat+", "+osmMinLon+"),("+osmMaxLat+", "+osmMaxLon+")");
  	  	}  
	}
	
	/*
	 * return vector with all wayParts
	 */
    public Vector<myOSMWayPart> getStreetLinksVector() {
    	// save street link inside this vector
    	Vector<myOSMWayPart> streetLinksVector = new Vector<myOSMWayPart>();
    	
    	// convert street links array to vector and resize it to real size
		for (int i=0; i < ways.size(); i++) {

			myOSMWay w = ways.get(i);
			
	    	Collections.addAll(streetLinksVector, w.WayParts);
	    	streetLinksVector.setSize(this.getNrOfAllWayParts());
    		
		}
    	
    	// return converted street links as vector
    	return streetLinksVector;
    }
}

