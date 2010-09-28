package org.tigris.subversion.subclipse.tools.usage.googleanalytics;

import java.io.UnsupportedEncodingException;

import org.tigris.subversion.subclipse.tools.usage.reporting.SubclipseComponents;
import org.tigris.subversion.subclipse.tools.usage.tracker.IURLBuildingStrategy;
import org.tigris.subversion.subclipse.tools.usage.tracker.internal.IFocusPoint;
import org.tigris.subversion.subclipse.tools.usage.util.HttpEncodingUtils;

public class GoogleAnalyticsUrlStrategy implements IURLBuildingStrategy {

	private static final String TRACKING_URL = "http://www.google-analytics.com/__utm.gif";

	private IGoogleAnalyticsParameters googleParameters;

	public GoogleAnalyticsUrlStrategy(IGoogleAnalyticsParameters googleAnalyticsParameters) {
		this.googleParameters = googleAnalyticsParameters;
	}

	public String build(IFocusPoint focusPoint) throws UnsupportedEncodingException {
		StringBuilder builder = new StringBuilder(TRACKING_URL)
				.append(IGoogleAnalyticsParameters.URL_PARAM_DELIMITER);
		appendParameter(IGoogleAnalyticsParameters.PARAM_TRACKING_CODE_VERSION,
				IGoogleAnalyticsParameters.VALUE_TRACKING_CODE_VERSION, builder);
		appendParameter(IGoogleAnalyticsParameters.PARAM_UNIQUE_TRACKING_NUMBER, getRandomNumber(), builder);
		appendParameter(IGoogleAnalyticsParameters.PARAM_HOST_NAME, googleParameters.getHostname(), builder);
		appendParameter(IGoogleAnalyticsParameters.PARAM_LANGUAGE_ENCODING,
				IGoogleAnalyticsParameters.VALUE_ENCODING_UTF8, builder);
		appendParameter(IGoogleAnalyticsParameters.PARAM_SCREEN_RESOLUTION, googleParameters.getScreenResolution(),
				builder);
		appendParameter(IGoogleAnalyticsParameters.PARAM_SCREEN_COLOR_DEPTH, googleParameters.getScreenColorDepth(),
				builder);
		appendParameter(IGoogleAnalyticsParameters.PARAM_BROWSER_LANGUAGE, googleParameters.getBrowserLanguage(),
				builder);
		String cookies = getCookies();
		String page = "Subclipse";
		if (SubclipseComponents.getSubclipseVersion() != null) {
			page = page + " " + SubclipseComponents.getSubclipseVersion();
		}
		
		appendParameter(IGoogleAnalyticsParameters.PARAM_PAGE_TITLE, page, builder);
//		appendParameter(IGoogleAnalyticsParameters.PARAM_PAGE_TITLE, focusPoint.getTitle(), builder);
		
		appendParameter(IGoogleAnalyticsParameters.PARAM_FLASH_VERSION, googleParameters.getFlashVersion(), builder);
		appendParameter(IGoogleAnalyticsParameters.PARAM_REFERRAL, googleParameters.getReferral(), builder);
		appendParameter(IGoogleAnalyticsParameters.PARAM_PAGE_REQUEST, focusPoint.getURI(), builder);

		appendParameter(IGoogleAnalyticsParameters.PARAM_ACCOUNT_NAME, googleParameters.getAccountName(), builder);
		appendParameter(IGoogleAnalyticsParameters.PARAM_COOKIES, cookies, builder);
		appendParameter(IGoogleAnalyticsParameters.PARAM_AD_CONTENT, googleParameters.getAdContent(), builder);
		appendParameter(IGoogleAnalyticsParameters.PARAM_GAQ, "1", false, builder);
		
		googleParameters.visit(); // update visit timestamps and count

		return builder.toString();
	}

	private String getCookies() {
		StringBuilder builder = new StringBuilder();

		/**
		 * unique visitor id cookie has to be unique per eclipse installation
		 */
		char[] plusDelimiter = { IGoogleAnalyticsParameters.PLUS_SIGN };
		new GoogleAnalyticsCookie(IGoogleAnalyticsParameters.PARAM_COOKIES_UNIQUE_VISITOR_ID,
				new StringBuilder().append("999.")
						.append(googleParameters.getUserId()).append(IGoogleAnalyticsParameters.DOT)
						.append(googleParameters.getFirstVisit()).append(IGoogleAnalyticsParameters.DOT)
						.append(googleParameters.getLastVisit()).append(IGoogleAnalyticsParameters.DOT)
						.append(googleParameters.getCurrentVisit()).append(IGoogleAnalyticsParameters.DOT)
						.append(googleParameters.getVisitCount())
						.append(IGoogleAnalyticsParameters.SEMICOLON),
				plusDelimiter)
				.appendTo(builder);		

		new GoogleAnalyticsCookie(IGoogleAnalyticsParameters.PARAM_COOKIES_REFERRAL_TYPE,
						new StringBuilder()
								.append("999.")
								.append(googleParameters.getFirstVisit())
								.append(IGoogleAnalyticsParameters.DOT)
								.append("1.1."))
				.appendTo(builder);

		char[] pipeDelimiter = { IGoogleAnalyticsParameters.PIPE };
		new GoogleAnalyticsCookie(IGoogleAnalyticsParameters.PARAM_COOKIES_UTMCSR,
				"(direct)",
				pipeDelimiter)
		.appendTo(builder);

		new GoogleAnalyticsCookie(IGoogleAnalyticsParameters.PARAM_COOKIES_UTMCCN,
				"(direct)",
				pipeDelimiter)
		.appendTo(builder);

		new GoogleAnalyticsCookie(IGoogleAnalyticsParameters.PARAM_COOKIES_UTMCMD,
				"(none)",
				pipeDelimiter)
		.appendTo(builder);		

		new GoogleAnalyticsCookie(IGoogleAnalyticsParameters.PARAM_COOKIES_KEYWORD,
					googleParameters.getKeyword())
				.appendTo(builder);

		builder.append(IGoogleAnalyticsParameters.SEMICOLON);

		return HttpEncodingUtils.checkedEncodeUtf8(builder.toString());
	}

	private String getRandomNumber() {
		return Integer.toString((int) (Math.random() * 0x7fffffff));
	}

	private void appendParameter(String name, String value, StringBuilder builder) {
		appendParameter(name, value, true, builder);
	}

	private void appendParameter(String name, String value, boolean appendAmpersand, StringBuilder builder) {
		builder.append(name)
				.append(IGoogleAnalyticsParameters.EQUALS_SIGN)
				.append(value);
		if (appendAmpersand) {
			builder.append(IGoogleAnalyticsParameters.AMPERSAND);
		}
	}
}
