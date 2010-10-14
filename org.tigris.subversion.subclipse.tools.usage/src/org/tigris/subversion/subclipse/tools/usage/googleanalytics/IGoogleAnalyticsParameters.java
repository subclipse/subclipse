package org.tigris.subversion.subclipse.tools.usage.googleanalytics;

public interface IGoogleAnalyticsParameters {

	public static final char AMPERSAND = '&';
	public static final char EQUALS_SIGN = '=';
	public static final char URL_PARAM_DELIMITER = '?';
	public static final char PLUS_SIGN = '+';
	public static final char DOT = '.';
	public static final char SEMICOLON = ';';
	public static final char PIPE = '|';

	public static final String PARAM_HID = "utmhid";
	public static final String PARAM_PAGE_REQUEST = "utmp";
	public static final String PARAM_ACCOUNT_NAME = "utmac";
	public static final String PARAM_HOST_NAME = "utmhn";
	public static final String PARAM_COOKIES = "utmcc";
	public static final String PARAM_COOKIES_UNIQUE_VISITOR_ID = "__utma";
	public static final String PARAM_COOKIES_SESSION = "__utmb";
	public static final String PARAM_COOKIES_BROWSERSESSION = "__utmc";
	public static final String PARAM_COOKIES_REFERRAL_TYPE = "__utmz";
	public static final String PARAM_COOKIES_UTMCSR = "utmcsr";
	public static final String PARAM_COOKIES_UTMCCN = "utmccn";
	public static final String PARAM_COOKIES_UTMCMD = "utmcmd";
	public static final String PARAM_COOKIES_KEYWORD = "utmctr";
	public static final String PARAM_COOKIES_USERDEFINED = "__utmv";

	public static final String PARAM_REFERRAL = "utmr";
	public static final String PARAM_TRACKING_CODE_VERSION = "utmwv";
	public static final String PARAM_UNIQUE_TRACKING_NUMBER = "utmn";
	public static final String PARAM_LANGUAGE_ENCODING = "utmcs";
	public static final String PARAM_SCREEN_RESOLUTION = "utmsr";
	public static final String PARAM_SCREEN_COLOR_DEPTH = "utmsc";
	public static final String PARAM_PRODUCT_NAME = "utmipn";
	public static final String PARAM_PRODUCT_CODE = "utmipc";
	public static final String PARAM_FLASH_VERSION = "utmfl";
	public static final String PARAM_BROWSER_LANGUAGE = "utmul";
	public static final String PARAM_REPEAT_CAMPAIGN_VISIT = "utmcr";
	public static final String PARAM_PAGE_TITLE = "utmdt";
	public static final String PARAM_GAQ = "gaq";
	public static final String PARAM_AD_CONTENT = "utm_content";
	
	public static final String VALUE_TRACKING_CODE_VERSION = "4.7.2";
	public static final String VALUE_NO_REFERRAL = "0";
	public static final String VALUE_ENCODING_UTF8 = "UTF-8";

	public static final String SCREERESOLUTION_DELIMITER = "x";
	public static final String SCREENCOLORDEPTH_POSTFIX = "-bit";

	public String getAccountName();

	public String getReferral();

	public String getScreenResolution();

	public String getScreenColorDepth();

	public String getBrowserLanguage();

	public String getHostname();

	public String getUserAgent();

	public String getUserId();

	public String getKeyword();

	public String getFirstVisit();

	public String getLastVisit();

	public String getCurrentVisit();

	public long getVisitCount();

	/**
	 * Signals that a visit was executed. The
	 * consequence is that visit timestamps and visit counters get updated
	 * 
	 * @see #getLastVisit()
	 * @see #getCurrentVisit()
	 * @see #getVisitCount()
	 */
	public void visit();
	
	public String getFlashVersion();

	/**
	 * Returns a user defined value that may be queried in Google Analytics.
	 *
	 * @return a user defined value
	 */
	public String getUserDefined();
}
