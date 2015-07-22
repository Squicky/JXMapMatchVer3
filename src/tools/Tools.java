package tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

public class Tools {
	/**
	 * This method scans an container recursive for an matching object type
	 * and set its font
	 * 
	 * @param swingContainer
	 * @param classType
	 * @param font
	 */
	public static void setFontForComponents(Container swingContainer, String classType, Font font ){
		// get all components of container
		Component[] components= swingContainer.getComponents();
	
		// set fonts
		for (Component component : components){
			// if current component equals desired class type, set font
			if(component.getClass().toString().endsWith(classType)){
				component.setFont(font);
			}
			
			// call this method recursive in case current component is another container
			if ((component instanceof JPanel) 		|| (component instanceof JScrollPane)  ||
				(component instanceof JTabbedPane) 	|| (component instanceof JSplitPane)  ||
				(component instanceof JRootPane) 	|| (component instanceof JLayeredPane) ){
				setFontForComponents((Container)component, classType, font);

			}
		}
	}
	
	/**
	 * This method places a window in the middle of the screen with half
	 * of screen's maximum size
	 * 
	 * @param window
	 */
	public static void centerHalfSizeWindow(Window window){
		// calculate
		double width = Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2;
		double height = Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2;
		double xPos = width / 2;
		double yPos = height / 2;
		
		// set position of window
		window.setBounds((int) xPos, (int) yPos, (int) width, (int) height);
	}
	
	/**
	 * This method scans recursive for components and container and enables/disables them
	 * 
	 * @param swingContainer
	 * @param enable
	 */
	public static void enableComponentsInsideContainer(Container swingContainer, boolean enable){
		// get all components of container
		Component[] components= swingContainer.getComponents();
			
		for (Component component : components){
			// enable/disable components
			component.setEnabled(enable);
			
			// call this method recursive in case current component is another container
			if ((component instanceof JPanel)		||	 (component instanceof JScrollPane)  ||
				(component instanceof JTabbedPane) 	|| 	 (component instanceof JSplitPane)   ||
				(component instanceof JRootPane) 	|| (component instanceof JLayeredPane)	 ||
				(component instanceof Container)){
						enableComponentsInsideContainer((Container)component, enable);
			}
		}
	}
	
	/**
	 * changes the standard color of an component
	 * 
	 * @param component
	 * @param red
	 * @param green
	 * @param blue
	 */
	public static void setUIColorForComponent(String component, int red, int green, int blue) {
		// call overload method
		setUIColorForComponent(component, new Color (red, green, blue));
	}
	
	/**
	 * changes the standard color of an component
	 * 
	 * @param component
	 * @param color
	 */
	public static void setUIColorForComponent(String component, Color color) {
		// call UIManager to change color
		UIManager.put (component, color);
	}
	
}
