package logging;

public class Logger {
	// save state if logger is enabled
	private static boolean isLoggerEnabled = true;
	
	public static void setLoggerEnabled(boolean enable) {
		isLoggerEnabled = enable;
	}
	
	public static boolean getLoggerIsEnabled() {
		return isLoggerEnabled;
	}
	
	public static void print(String string) {
		if(isLoggerEnabled) System.out.print(string);
	}
	
	public static void println(String string) {
		if(isLoggerEnabled) System.out.println(string);
	}
	
	public static void err(String errorString) {
		if(isLoggerEnabled) System.err.print(errorString);
	}
	
	public static void errln(String errorString) {
		if(isLoggerEnabled) System.err.println(errorString);
	}
	
}
