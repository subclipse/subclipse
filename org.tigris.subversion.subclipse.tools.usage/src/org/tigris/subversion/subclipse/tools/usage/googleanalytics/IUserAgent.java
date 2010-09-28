package org.tigris.subversion.subclipse.tools.usage.googleanalytics;

public interface IUserAgent {

	public static final char BROWSER_LOCALE_DELIMITER = '-';

	public String getBrowserLanguage();
	public String toString();
}
