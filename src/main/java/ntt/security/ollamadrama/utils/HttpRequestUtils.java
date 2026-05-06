package ntt.security.ollamadrama.utils;

import java.net.ConnectException;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestUtils.class);
	
	public static String getBodyUsingGETUrlRequestOpportunistic(String _url) {
		try {
			Document doc = Jsoup.connect(_url)
					.timeout(2000) // 2 secs
					.ignoreContentType(true).
					get();
			String res = Jsoup.parse(doc.toString()).body().text();
			return res;
		} catch (ConnectException ce) {
			LOGGER.debug("Connection exception: " + ce.getMessage());
		} catch (HttpStatusException he) {
			LOGGER.debug("HTTP status exception: " + he.getMessage());
		} catch (Exception e) {
			LOGGER.debug("e: " + e.getMessage());
		}
		return "";
	}
}
