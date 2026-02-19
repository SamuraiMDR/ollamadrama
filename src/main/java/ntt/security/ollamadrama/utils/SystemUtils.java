package ntt.security.ollamadrama.utils;

public class SystemUtils {

	public static void halt() {
		System.exit(1);
	}

	public static String getSystemInformation() {
		String systemInfo = System.getProperty("java.vendor") + " " + System.getProperty("java.version") + " on " + System.getProperty("os.name") + " " + System.getProperty("os.version");
		return systemInfo;
	}

	public static void sleepInSeconds(int i) {
		try {
			Thread.sleep(i*1000);
		} catch (InterruptedException e) {
		}
	}
	
	public static void sleepInMilliSeconds(long i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
		}
	}
    
}
