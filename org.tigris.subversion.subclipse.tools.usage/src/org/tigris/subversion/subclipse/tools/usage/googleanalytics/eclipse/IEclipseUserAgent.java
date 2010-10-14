package org.tigris.subversion.subclipse.tools.usage.googleanalytics.eclipse;

public interface IEclipseUserAgent {

	public String getBrowserLanguage();

	public String getOS();

	/**
	 * Returns the version of the operating system this jre is currently running
	 * on.
	 * 
	 * @return the os version
	 * 
	 * @see <a href="http://lopica.sourceforge.net/os.html">list of os versions
	 *      and os names</a>
	 */
	public String getOSVersion();

	public String getApplicationName();

	public String getApplicationVersion();

}