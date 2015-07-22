package dialogelements;

import java.awt.Component;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * @author Daniel Sathees Elmo
 * 
 * this class provides file open/save dialog and supports file filter
 */

public class JFileDialog {
	// file chooser class
	private JFileChooser jFileChooser;
		
	// parent component of file chooser dialog
	private Component parentComponent;
		
	// selected file
	private File selectedFile;
	
	/**
	 * constructor method, allows just one file extension & description
	 * @param parentComponent this component gets blocked during file dialog is active
	 * @param fileExtension without "." notation
	 * @param fileDescription
	 * @throws Exception
	 */
	public JFileDialog(Component parentComponent, String fileExtension, String fileDescription) {
		// call other constructor with converted strings
		this(parentComponent, convertStringToArray(fileExtension), convertStringToArray(fileDescription), "");
	}
		
	/**
	 * constructor method, allows multiple file extensions & descriptions through arrays (both
	 * arrays must have the same size!)
	 * @param parentComponent
	 * @param fileExtensions without "." notation
	 * @param fileDescriptions
	 * @throws Exception
	 */
	public JFileDialog(Component parentComponent, final String[] fileExtensions, final String[] fileDescriptions, String CurrentDirectory) {
		// save parent component
		this.parentComponent = parentComponent;
		
		if (CurrentDirectory == null || CurrentDirectory.isEmpty()) {
			CurrentDirectory = "";
		}
		
		// create file chooser instance, overwrite approve selection method in order to avoid
		// overwritten files by accident
		jFileChooser = new JFileChooser(CurrentDirectory) {  
			// generated serial version UID
			private static final long serialVersionUID = 1L;
			
			/**
			 * includes an confirm dialog
			 */
			@Override 
			public void approveSelection() {
				// get current selected file
				File file = getSelectedFile();
				// check if it's exists and dialog type
				if(file.exists() && getDialogType() == SAVE_DIALOG) {
					// ask user and handle choices
					int dialogResult = JOptionPane.showConfirmDialog (this, "The file " + file.getName() + " already exists, overwrite?", "Existing file",
	            												  JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					// do nothing in case user want not to overwrite file
					if (dialogResult == JOptionPane.NO_OPTION)
						return;
				}
				// otherwise call super method in order to continue normal behavior of super class
				super.approveSelection();
			}
			
			/**
			 * adds automatically file extension if necessary
			 */
			@Override
			public File getSelectedFile() {
				// get selected file via super call
				File selectedFile = super.getSelectedFile();
				// if file has not matching extension, add so
				try {
					String chosenFileExtension = "." + ((FileNameExtensionFilter)jFileChooser.getFileFilter()).getExtensions()[0];
					if (!selectedFile.getName().endsWith(chosenFileExtension)) {
						selectedFile = new File(selectedFile.getAbsolutePath() + chosenFileExtension);
					}
				} catch (NullPointerException e) {
					//System.err.println("JFileDialog.JFileChooser.getSelectedFile(): NullPointer Exception");
				} 
				// return adjusted File
				return selectedFile;
			}
		};
		
		// don't allow to select all files
		jFileChooser.setAcceptAllFileFilterUsed(false);
		
		// create and assign file filter for each given file extension
		try {
			// set first (default) file filter, we need the from FileFilter derived class FileNameExtensionFilter
			// cause FileFilter is not able to return its file extension!
			jFileChooser.setFileFilter(new FileNameExtensionFilter(fileDescriptions[0], fileExtensions[0]));

			// add further file filters if necessary
			for (int i=1; i < fileExtensions.length; i++) {
				jFileChooser.addChoosableFileFilter(new FileNameExtensionFilter(fileDescriptions[i], fileExtensions[i]));
			}
		}
		catch (IndexOutOfBoundsException e) {
			System.err.println("Size of file descriptions array is smaller than file extensions array");
		}
	}
	
	public void setCurrentDirectory(String dir) {
//		jFileChooser.setCurrentDirectory(arg0);
	}
	
	/**
	 * shows open dialog and saves selected file
	 * @return
	 */
	public boolean showOpenDialog() {
		// show open dialog and return if file was chosen or not
		return getApproveOption(jFileChooser.showOpenDialog(parentComponent));
	}
	
	/**
	 * shows save dialog and saves selected file
	 * @return
	 */
	public boolean showSaveDialog() {
		// show open dialog and return if file was chosen or not
		return getApproveOption(jFileChooser.showSaveDialog(parentComponent));
	}
	
	/**
	 * checks state of last used file dialog
	 * @param state of last call
	 * @return if user selected a file or not
	 */
	private boolean getApproveOption(int state) {
		// exploit file dialog, save/set selected file
		if (state == JFileChooser.APPROVE_OPTION){
			selectedFile = jFileChooser.getSelectedFile();
			jFileChooser.setSelectedFile(selectedFile);
			return true;
		}
		// aborted by user
		return false;
	}
	
	/**
	 * returns whole path of selected file
	 * @return
	 */
	public String getSelectedFilePath() {
		// return full file path
		try {
			return selectedFile.getAbsolutePath();
		} catch(NullPointerException e){
			return "";
		}
	}
	
	/**
	 * get just filename of selected file 
	 * @return
	 */
	public String getSelectedFileName() {
		// return just file name
		try {
			return selectedFile.getName();
		} catch(NullPointerException e){
			return "";
		}
	}
	
	/**
	 * get an instance of selected file
	 * @return
	 */
	public File getSelectedFile() {
		// return file object
		return selectedFile; 
	}
	
	/**
	 * converts an single string to an array
	 * @param string
	 * @return
	 */
	private static String[] convertStringToArray(String string) {
		String stringArray[] = { string };
		return stringArray;
	}
}
