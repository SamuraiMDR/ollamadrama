package ntt.security.ollamadrama.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateUtils {
	
    @SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(DateUtils.class);
	
	private static final String DATE_FORMAT_NOW_REPORT 		= "yyyy-MM-dd HH:mm:ss";
    
	public static String epochInMilliSecondsToUTC(final long epoch) {
		Date date = new Date(epoch);
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(date);
	}
	
	public static String epochInSecondsToUTC(final long epoch) {
		Date date = new Date(epoch*1000L);
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(date);
	}
	
	public static long secondsToHours(long seconds) {
        if (seconds < 0) {
            return -1;
        }
        return seconds / 3600;
    }

	public static String nowTimeStamp() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW_REPORT);
		return sdf.format(cal.getTime());
	}
	
}
