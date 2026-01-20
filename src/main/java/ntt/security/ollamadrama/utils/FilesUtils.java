package ntt.security.ollamadrama.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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

    public static ArrayList<File> getAllFileNamesInFolderAsFiles(final String f) {
        String folderpath = getExistingFile2(f);
        ArrayList<File> files = new ArrayList<>();
        File folder = new File(folderpath);
        if (folder.exists()) {
            for (final File fileEntry : folder.listFiles()) {
                if (fileEntry.isFile()) files.add(fileEntry);
            }
        }
        return files;
    }
    
    public static String getExistingFile2(final String filePath) {
        File file1 = new File(filePath);
        String secondaryPath = filePath.replaceFirst("/", "");
        File file2 = new File(secondaryPath);
        if (file1.exists()) {
            return filePath;
        } else {
            if (file2.exists()) {
                return secondaryPath;
            }
        }
        return "";
    }
    
    public static String readAllFromFile(String primaryPath) {
        File priFile = new File(primaryPath);
        final Path pth = priFile.toPath();
        if (!priFile.exists()) {
            try {
                LOGGER.error("Found no file at " + priFile.getCanonicalPath());
                SystemUtils.halt();
            } catch (IOException e) {
                LOGGER.error("IO exception while reading file " + primaryPath + ": " + Arrays.toString(e.getStackTrace()));
                SystemUtils.halt();
            }
        }
        try {
            List<String> lines = Files.readAllLines(pth, StandardCharsets.UTF_8);
            String result = StringUtils.join(lines, "\n");
            return result;
        } catch (Exception e) {
            LOGGER.error("IO exception while reading file " + primaryPath + ": " + Arrays.toString(e.getStackTrace()));
            SystemUtils.halt();
        }
        return "";
    }
}
