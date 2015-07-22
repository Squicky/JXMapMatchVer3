package algorithm;

/**
 * 
 * @author Daniel Sathees Elmo
 * 
 * this class represents an range where a set of GPS points
 * are matched to a specific street link
 *
 */
public class MatchedRange {
	
	private int rangeStartIndex;		// start index of range
	private int rangeEndIndex;			// end index of range
	private boolean matched;			// sets if range belongs to an matched set of GPS points
	
	/**
	 * constructor needs start/end index of an range, and if its matched
	 * @param rangeStartIndex
	 * @param rangeEndIndex
	 * @param matched
	 */
	public MatchedRange(int rangeStartIndex, int rangeEndIndex, boolean matched){
		setRangeStartIndex(rangeStartIndex);
		setRangeEndIndex(rangeEndIndex);
		setMatched(matched);
	}

	/**
	 * set if its matched
	 * @param matched
	 */
	public void setMatched(boolean matched){
		this.matched = matched;
	}
	
	/**
	 * get matched state
	 * @return
	 */
	public boolean getMatched(){
		return matched;
	}
	
	/**
	 * set start index of range
	 * @param rangeStartIndex
	 */
	public void setRangeStartIndex(int rangeStartIndex){
		this.rangeStartIndex = rangeStartIndex;
	}
	
	/**
	 * get start index of range
	 * @return
	 */
	public int getRangeStartIndex(){
		
		if (rangeStartIndex == -1) {
			rangeStartIndex++;
			rangeStartIndex--;
		}
		
		return rangeStartIndex;
	}
	
	public int getRangeStartIndexForClone(){
		return rangeStartIndex;
	}
	
	/**
	 * set end index of range
	 * @param rangeEndIndex
	 */
	public void setRangeEndIndex(int rangeEndIndex){
		this.rangeEndIndex = rangeEndIndex;
	}
	
	/**
	 * get end index of range
	 * @return
	 */
	public int getRangeEndIndex(){
		if (rangeEndIndex == -1) {
			rangeEndIndex++;
			rangeEndIndex--;
		}
		
		return rangeEndIndex;
	}

	public int getRangeEndIndexForClone(){
		return rangeEndIndex;
	}
	
	/**
	 * get size of range
	 * @return
	 */
	public int getRangeSize() {
		if (rangeEndIndex == -1) {
			return 0;
		}
		return Math.abs(rangeEndIndex - rangeStartIndex + 1);
	}
}
