package interfaces;

import java.util.Vector;

import algorithm.MatchedGPSNode;

public interface MatchingGPSObject {

	public long getRefTimeStamp();						// get reference timestamp where measurement started
	public Vector<MatchedGPSNode> getMatchedGPSNodes();	// get vector with matched GPS nodes
	
}
