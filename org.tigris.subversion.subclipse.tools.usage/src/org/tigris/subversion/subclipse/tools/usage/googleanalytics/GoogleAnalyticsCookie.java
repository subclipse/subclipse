package org.tigris.subversion.subclipse.tools.usage.googleanalytics;

public class GoogleAnalyticsCookie {

	private CharSequence value;
	private String identifier;
	private char[] delimiters;

	public GoogleAnalyticsCookie(String identifier, CharSequence value, char[] delimiters) {
		this.identifier = identifier;
		this.value = value;
		this.delimiters = delimiters;
	}

	public GoogleAnalyticsCookie(String identifier, CharSequence value) {
		this(identifier, value, new char[] {(char)-1});
	}

	public void appendTo(StringBuilder builder) {
		if (identifier != null && identifier.length() > 0 && value != null && value.length() > 0) {
			builder.append(identifier)
					.append(IGoogleAnalyticsParameters.EQUALS_SIGN)
					.append(value);
			for (int i = 0; i < delimiters.length; i++) {
				builder.append(delimiters[i]);
			}
		}
	}
}
