package dialogelements;

import interfaces.StatusUpdate;

import java.awt.Font;

import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.border.BevelBorder;


/**
 * 
 * @author Daniel Sathees Elmo
 *
 * This class represents a loading window (without frame)
 * and implements the "StatusUpdate" interface
 */
public class JWindowLoading extends JWindow
							implements StatusUpdate {
	
	// serial version UID
	private static final long serialVersionUID = 1L;
	
	// content pane, label for putting status
	JPanel contentPane;
	JLabel jLabelStatus;
	
	/**
	 * class method creates instance of this class
	 * @return reference of window class
	 */
	public static JWindowLoading createLoadingWindow(){
		JWindowLoading jWindowLoading = new JWindowLoading();
		jWindowLoading.setVisible(true);
		return jWindowLoading;
	}

	/**
	 * constructor initialize non-bordered, small loading screen
	 */
	public JWindowLoading(){
		//set window
		super();
		setSize(250,50);
		setLocationRelativeTo(null);
		setAlwaysOnTop(true);
		
		//set content pane and border
		contentPane = new JPanel(new GridBagLayout());
		contentPane.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		setContentPane(contentPane);
		
		//create status JLabel
		jLabelStatus = new JLabel();
		jLabelStatus.setFont(new Font("Arial", Font.BOLD, 12));

		//add to content pane
		contentPane.add(jLabelStatus);		
	}
	
	/**
	 * shut down and close window
	 */
	@Override
	public void finished() {
		setVisible(false);
		dispose();
	}

	//////////////////////// implements update status interface ////////////////////////////////////////////////
	
	/**
	 * displays just progress in percent
	 */
	@Override
	public void updateStatus(float percent) {
		updateStatus(percent + "%");
	}

	/**
	 * displays just current status message
	 */
	@Override
	public void updateStatus(String updateMessage) {
		jLabelStatus.setText(updateMessage);
	}
	
	/**
	 * displays current status message and progress in percent
	 */
	@Override
	public void updateStatus(String updateMessage, float percent) {
		updateStatus(updateMessage + " " + (int) percent + "%");
	}

	/**
	 * shut down and close window by calling overload method, ignore result message
	 */
	@Override
	public void finished(String resultMessage) {
		finished();
	}
	
	// Unimplemented methods, not needed yet
	@Override public void updateUndefinedStatus() {}
	@Override public void updateUndefinedStatus(String undefinedMessage) {}
	@Override public void updateUndefinedStatus(String undefinedMessage, String updateMessage) {}
}
