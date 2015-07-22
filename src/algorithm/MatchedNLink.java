package algorithm;

import java.awt.Color;
import java.util.Vector;

import myOSM.myOSMWayPart;

public class MatchedNLink {
	
	public static int objCount = 0;
	public int objID = 0;
	
	private myOSMWayPart streetLink;
	private MatchedRange matchedRange;		// reference to matched range
	
	public Vector<MatchedGPSNode> matchedGPSNodes = new Vector<MatchedGPSNode>();
	
	private Color color;
	
	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public MatchedNLink(myOSMWayPart streetLink, MatchedRange matchedRange, Color color) {
		super();
		
		this.streetLink = streetLink;
		this.matchedRange = matchedRange;
		this.color = color;
		
		this.objID = objCount;
		objCount++;
	}
	
	public MatchedNLink(myOSMWayPart streetLink, Color color) {
		this(streetLink, new MatchedRange(0, 0, false), color);
	}

	public myOSMWayPart getStreetLink() {
		return streetLink;
	}

	public void setStreetLink(myOSMWayPart streetLink) {
		this.streetLink = streetLink;
	}

	public boolean isMatched() {
		return matchedRange.getMatched();
	}

	public void setMatched(boolean matched) {		
		this.matchedRange.setMatched(matched);
	}
	
	/**
	 * set start index of matched range
	 * @param rangeStartIndex
	 */
	public void setRangeStartIndex(int rangeStartIndex) {
		matchedRange.setRangeStartIndex(rangeStartIndex);
	}
	
	/**
	 * get start index of matched range
	 * @return int
	 */
	public int getRangeStartIndex() {
		return matchedRange.getRangeStartIndex();
	}
	
	/**
	 * set end index of matched range 
	 * @param rangeEndIndex
	 */
	public void setRangeEndIndex(int rangeEndIndex) {
		matchedRange.setRangeEndIndex(rangeEndIndex);
	}
	
	/**
	 * get end index of matched range
	 * @param int
	 */
	public int getRangeEndIndex() {
		return matchedRange.getRangeEndIndex();
	}
	
	/**
	 * get range size
	 * @return int
	 */
	public int getRangeSize() {
		return matchedRange.getRangeSize();
	}
}
