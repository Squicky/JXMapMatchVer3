package algorithm;

import gps.GPSNode;

import java.awt.Color;

public class ReorderedMatchedGPSNode extends MatchedGPSNode {
	
    // save matched / reordered GPS node index 
    private final static int NO_INDEX = -1;
    private int prevIndex = NO_INDEX;
    private int curIndex = NO_INDEX;
    
	public ReorderedMatchedGPSNode(GPSNode gpsNode, Color color) {
		super(gpsNode, color);
	}
	
	public boolean hasIndexChanged() {
		if (prevIndex == NO_INDEX || curIndex == NO_INDEX)
			return false;
		
		return (prevIndex != curIndex);
	}
	
	public int getPrevIndex() {
		return prevIndex;
	}

	public void setPrevIndex(int prevIndex) {
		this.prevIndex = prevIndex;
	}

	public int getCurIndex() {
		return curIndex;
	}

	public void setCurIndex(int curIndex) {
		this.curIndex = curIndex;
	}
}
