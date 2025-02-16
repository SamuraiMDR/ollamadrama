package ntt.security.ollamadrama.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FilesUtils.class);

	public static void appendToFileUNIX(final String outString, final String filePath) throws Exception {
		appendToFile(outString, filePath, "\n");
	}

	public static void appendToFileWindows(final String outString, final String filePath) throws Exception {
		appendToFile(outString, filePath, "\r\n");
	}

	public static void appendToFile(final String outString, final String filePath, final String delimiter) throws Exception {
		Writer out = null;
		try {
			out = new OutputStreamWriter(new FileOutputStream(filePath, true), StandardCharsets.UTF_8);
			out.write(outString + delimiter);
			out.flush();
			out.close();
		} catch (Exception e) {
			throw e;
		} finally {
			IOUtils.closeQuietly(out);
		}
	}
	
	public static void writeToFile(final String outString, final String filePath, final String delimiter) throws Exception {
		Writer out = null;
		try {
			File oldFile = new File(filePath);
			if (oldFile.exists()) {
				if (oldFile.delete() == false) {
					LOGGER.warn("Unable to delete " + oldFile.getAbsolutePath());
				}
			}
			out = new OutputStreamWriter(new FileOutputStream(filePath, true), StandardCharsets.UTF_8);
			out.write(outString + delimiter);
			out.close();
		} catch (Exception e) {
			throw e;
		} finally {
			IOUtils.closeQuietly(out);
		}
	}
	
	public static void writeToFileUNIXNoException(final String outString, final String filePath) {
		try {
			writeToFile(outString, filePath, "\n");
		} catch (Exception e) {
			LOGGER.info("e: " + e.getMessage());
		}
	}
	
	public static void appendToFileUNIXNoException(final String outString, final String filePath) {
		try {
			appendToFile(outString, filePath, "\n");
		} catch (Exception e) {
			LOGGER.info("e: " + e.getMessage());
		}
	}
}
