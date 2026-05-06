package ntt.security.ollamadrama.agent;

public enum Scheduler {
	ALWAYS(0, ".always"),
	EVERY_HOUR(60, ".every_hour"),
	EVERY_4HOURS(240, ".every_4hours"),
	DAILY(1440, ".daily"),
	WEEKLY(10080, ".weekly"),
	ONCE(Integer.MAX_VALUE, ".once");

	private final int waitMinutes;
	private final String extension;

	Scheduler(int waitMinutes, String extension) {
		this.waitMinutes = waitMinutes;
		this.extension = extension;
	}

	public int getWaitMinutes() {
		return waitMinutes;
	}

	public String getExtension() {
		return extension;
	}

	public static Scheduler fromExtension(String extension) {
		for (Scheduler s : values()) {
			if (s.extension.equals(extension)) {
				return s;
			}
		}
		return null;
	}
}
