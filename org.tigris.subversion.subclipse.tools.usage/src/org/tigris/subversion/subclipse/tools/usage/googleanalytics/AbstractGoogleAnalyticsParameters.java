package org.tigris.subversion.subclipse.tools.usage.googleanalytics;

public abstract class AbstractGoogleAnalyticsParameters implements IGoogleAnalyticsParameters {

	private String accountName;
	private String hostName;
	private String referral;

	public AbstractGoogleAnalyticsParameters(String accountName, String hostName, String referral) {
		this.accountName = accountName;
		this.hostName = hostName;
		this.referral = referral;
	}

	public String getReferral() {
		return referral;
	}

	public String getAccountName() {
		return accountName;
	}
	
	public String getHostname() {
		return hostName;
	}
}
