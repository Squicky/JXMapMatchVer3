package jxmapmatch;

import java.awt.EventQueue;
import logging.Logger;
import dialogelements.JWindowLoading;

/**
 * @author Daniel Sathees Elmo
 * 
 * This class launch the application by showing a splash screen
 * during all components are loaded
 */

public class JXMapMatch extends Thread{

	private static JWindowLoading jWindowLoading;
	private static String[] arguments;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		System.setProperty("java.net.useSystemProxies","true");

		/*
		Properties systemSettings = System.getProperties();
		systemSettings.put("http.proxyHost", "172.20.150.211");
		systemSettings.put("http.proxyPort", "8080");
		System.setProperties(systemSettings);
		*/
		
		// save arguments
		arguments = args;
		
		// enable/disable logger
		Logger.setLoggerEnabled(true);
		
		// start program
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					// create loading window
					jWindowLoading = JWindowLoading.createLoadingWindow();
					// start JXMapMatch on new thread
					JXMapMatch jxMapMatch = new JXMapMatch();
					jxMapMatch.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}


	/**
	 * load and show GUI
	 */
	@Override
	public void run(){
		// create GUI and show loading screen
		JXMapMatchGUI jxMapMatchGUI = new JXMapMatchGUI(jWindowLoading);
		// create controller for GUI, add listener to controller
		@SuppressWarnings("unused")
		JXMapMatchController jxMapMatchController = new JXMapMatchController(jxMapMatchGUI, arguments);
		// show GUI after all
		jxMapMatchGUI.setVisible(true);
	}

}
