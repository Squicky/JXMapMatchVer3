package interfaces;

/**
 * @author Daniel Sathees Elmo
 * 
 * This interface helps to keep track of current status by 
 * using call-back functions
 */

public interface StatusUpdate {
	public void updateStatus(String updateMessage); 									// update status by a message
	public void updateStatus(float percent);											// update status by percent
	public void updateStatus(String updateMessage, float percent);  					// update status by message and percent
	
	public void updateUndefinedStatus();												// set update to an undefined state
	public void updateUndefinedStatus(String undefinedMessage);							// same as above, additionally info about undefined state
	public void updateUndefinedStatus(String undefinedMessage, String updateMessage);	// same as above, also updates message
	
	public void finished();																// should be called after work is finished
	public void finished(String resultMessage);											// same as above including a result message
}
