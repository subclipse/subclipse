package org.tigris.subversion.subclipse.tools.usage.util;

public class StringUtils {
	private static final String LINE_SEPARATOR_KEY = "line.separator";

	public StringUtils() {
	}	

	public static String getLineSeparator() {
		return System.getProperty(LINE_SEPARATOR_KEY);
	}
}
