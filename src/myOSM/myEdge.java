package myOSM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class myEdge {

	public String id_str;
	
	public long osmWayId = -1;
	
	public Boolean reverse_direction = false;
	
	public long startNode = -1;
	
	public long endNode = -1;
	
	public double length = -1;
	
	public static Map<Integer, myEdge> loadGetEdges(String FilePath) {
		
		Map<Integer, myEdge> m = new HashMap<Integer, myEdge>();
		
		String line = "";
		
		String lines[];
		
		try {
			BufferedReader bReader = new BufferedReader( new InputStreamReader( new FileInputStream( new File( FilePath ) ), "UTF-8" ));

			line = bReader.readLine();

			while (line != null) {
				line = line.trim();
				
				if (line.startsWith("<edge id=\"") && false == line.startsWith("<edge id=\":")) {
					
					myEdge e = new myEdge();
					
					line = line.replace("<edge id=\"", "");
					
					lines = line.split("\" from=\"", 2);
					
					e.id_str = lines[0];
					
					if (e.id_str.contains("AddedOnRampEdge") == false && e.id_str.contains("AddedOffRampEdge") == false) {

						line = lines[1];
						
						lines = line.split("\" to=\"", 2);
						
						if (lines[0].contains("AddedOnRampNode") == false && lines[0].contains("AddedOffRampNode") == false ) {
							
							e.startNode = Long.parseLong(lines[0]);
							
							line = lines[1];
							
							lines = line.split("\"", 2);
							
							if (lines[0].contains("AddedOnRampNode") == false && lines[0].contains("AddedOffRampNode") == false ) {

								e.endNode = Long.parseLong(lines[0]);
								
								line = bReader.readLine();
								
								line = line.trim();
								
								lines = line.split("length=\"");
								
								line = lines[1];
								
								lines = line.split("\"", 2);
								
								e.length = Double.parseDouble(lines[0]);
								
								boolean splited = false;
								if (e.id_str.contains("+")) {
									lines = e.id_str.split("\\+");
									e.osmWayId = Long.parseLong(lines[0].split("#")[0]);
									splited = true;
								} else if (e.id_str.contains("_")) {
									lines = e.id_str.split("_");
									e.osmWayId = Long.parseLong(lines[0]);
								} else {
									e.osmWayId = Long.parseLong(e.id_str.split("#")[0]);
								}

								if (e.osmWayId < 0) {
									
									e.osmWayId = e.osmWayId * -1;
									
									e.reverse_direction = true;
								}
								
								m.put(m.size(), e);
								
								if (splited) {
									for (int i=1; i < lines.length; i++) {
										
										myEdge e_temp = new myEdge();
										
										e_temp.endNode = e.endNode;
										e_temp.id_str = e.id_str;
										e_temp.length = e.length;
										e_temp.reverse_direction = e.reverse_direction;
										e_temp.startNode = e.startNode;

										e = e_temp;
										
										e.osmWayId = Long.parseLong(lines[i].split("#")[0]);
										
										if (e.osmWayId < 0) {
											
											e.osmWayId = e.osmWayId * -1;
											
											e.reverse_direction = true;
										}
										
										m.put(m.size(), e);
									}				
								}
							}
						}

					}

				}
				
				line = bReader.readLine();
			}
			
			bReader.close();
		
		} catch (Exception e) {			
			System.out.println("Error: loadGetEdges: \n" + line + "\n" + e.toString());
		}

		return m;
		
	}
	
}
