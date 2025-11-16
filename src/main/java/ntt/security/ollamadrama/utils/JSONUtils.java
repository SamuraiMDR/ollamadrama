package ntt.security.ollamadrama.utils;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(JSONUtils.class);

	public static <T> T createPOJOFromJSONOpportunistic(final String jsonSTR, Class<T> valueType) {
		ObjectMapper mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		T pojo = null;
		try {
			pojo = mapper.readValue(jsonSTR, valueType);
		} catch (Exception e) {
			LOGGER.info("Exception e: " + e.getMessage());
		}
		return pojo;
	}
	
	public static <T> T createPOJOFromJSONOpportunistic(final String jsonSTR, Class<T> valueType, ObjectMapper mapper) {
		T pojo = null;
		try {
			pojo = mapper.readValue(jsonSTR, valueType);
		} catch (Exception e) {
		}
		return pojo;
	}
	
	public static String createJSONFromPOJO(Object o) {
		String jsonString = "";
		try {
			jsonString = JSON.toJSONString(o);
		} catch (Exception e) {
			LOGGER.error("Exception during JSON parsing: " + e.getClass() + ": " + e.getMessage(), e);
		}
		return jsonString;
	}

}
