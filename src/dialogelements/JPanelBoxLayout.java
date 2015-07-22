package dialogelements;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

/**
 * @author Daniel Sathees Elmo
 * this class extends JPanel class and implements automatically assigned box layout
 * and a optional border
 */

public class JPanelBoxLayout extends JPanel {
	
	// serial version UID
	private static final long serialVersionUID = -4453086129900906880L;
	
	// save size for strut and last strut
	private Dimension strutDimension;
	private Component endStrut;
	
	/**
	 * constructor creates JPanel with adds box layout to it with variable properties for strut width and height
	 * @param orientation
	 * @param strutWidth
	 * @param strutHeight
	 */
	public JPanelBoxLayout(int orientation, int strutWidth, int strutHeight){
		// call super class
		super();
		
		// save strut dimension
		strutDimension = new Dimension(strutWidth, strutHeight);
		
		// set layout according to parameter
		setLayout(new BoxLayout(this, orientation));
		
		// create rigid area for adding some space at end of panel, notice reference
		// cause we need to remove this strut before adding new component
		endStrut = Box.createRigidArea(strutDimension);
		
		// add struts
		super.add(Box.createRigidArea(strutDimension));
		super.add(endStrut);
	}
	
	/**
	 * constructor creates JPanel with adds box layout to it with variable properties for strut width and height
	 * and additionally a border
	 * @param orientation
	 * @param strutWidth
	 * @param strutHeight
	 */
	public JPanelBoxLayout(int orientation, int strutWidth, int strutHeight, Border border)
	{
		// call constructor in this class
		this(orientation, strutWidth, strutHeight);
		
		// add border
		setBorder(border);
	}
	
	/**
	 * constructor creates JPanel with adds box layout to it with variable properties for strut width and height
	 * additionally adds titled border
	 * @param orientation
	 * @param strutWidth
	 * @param strutHeight
	 * @param borderTitle
	 */
	public JPanelBoxLayout(int orientation, int strutWidth, int strutHeight, String borderTitle)
	{
		// call constructor
		this(orientation, strutWidth, strutHeight);
		
		// create titled lower etched border
		Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
		Border titledLowerEtchedBorder = BorderFactory.createTitledBorder(lowerEtchedBorder, borderTitle, TitledBorder.LEFT, TitledBorder.TOP);
		
		// set border
		setBorder(titledLowerEtchedBorder);
	}
	
	/**
	 * add component to panel
	 */
	public Component add(Component comp){
		// keep return value
		Component tmpComp;
		
		// remove strut at the end before adding
		// component, add start/between strut,
		// finally re-add strut
		remove(endStrut);
		tmpComp = super.add(comp);
		super.add(Box.createRigidArea(strutDimension));
		super.add(endStrut);
		
		// return
		return tmpComp;
	}
}
