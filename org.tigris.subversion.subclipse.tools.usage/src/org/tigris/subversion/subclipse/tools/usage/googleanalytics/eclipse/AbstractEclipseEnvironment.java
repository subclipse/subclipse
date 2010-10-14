package org.tigris.subversion.subclipse.tools.usage.googleanalytics.eclipse;

import java.util.Random;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.tools.usage.googleanalytics.AbstractGoogleAnalyticsParameters;
import org.tigris.subversion.subclipse.tools.usage.googleanalytics.IGoogleAnalyticsParameters;
import org.tigris.subversion.subclipse.tools.usage.internal.SubclipseToolsUsageActivator;
import org.tigris.subversion.subclipse.tools.usage.preferences.IUsageReportPreferenceConstants;
import org.tigris.subversion.subclipse.tools.usage.preferences.UsageReportPreferencesUtils;

public abstract class AbstractEclipseEnvironment extends AbstractGoogleAnalyticsParameters implements
		IEclipseEnvironment {

	private static final String SYSPROP_JAVA_VERSION = "java.version";

	private String screenResolution;
	private String screenColorDepth;
	private Random random;
	private IEclipsePreferences preferences;
	private String firstVisit;
	private String lastVisit;
	private String currentVisit;
	private long visitCount;
	protected IEclipseUserAgent eclipseUserAgent;

	public AbstractEclipseEnvironment(String accountName, String hostName, IEclipsePreferences preferences) {
		this(accountName, hostName, IGoogleAnalyticsParameters.VALUE_NO_REFERRAL, preferences);
	}

	public AbstractEclipseEnvironment(String accountName, String hostName, String referral,
			IEclipsePreferences preferences) {
		super(accountName, hostName, referral);
		this.random = new Random();
		this.preferences = preferences;
		eclipseUserAgent = createEclipseUserAgent();
		initScreenSettings();
		initVisits();
	}

	protected void initScreenSettings() {
		final Display display = getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				screenColorDepth = display.getDepth() + SCREENCOLORDEPTH_POSTFIX;

				Rectangle bounds = display.getBounds();
				screenResolution = bounds.width + SCREERESOLUTION_DELIMITER + bounds.height;
			}
		});
	}

	private void initVisits() {
		String currentTime = String.valueOf(System.currentTimeMillis());
		this.currentVisit = currentTime;
		this.firstVisit = preferences.get(IUsageReportPreferenceConstants.FIRST_VISIT, null);
		if (firstVisit == null) {
			this.firstVisit = currentTime;
			preferences.put(IUsageReportPreferenceConstants.FIRST_VISIT, firstVisit);
		}
		lastVisit = preferences.get(IUsageReportPreferenceConstants.LAST_VISIT, currentTime);
		visitCount = preferences.getLong(IUsageReportPreferenceConstants.VISIT_COUNT, 1);
	}

	protected IEclipseUserAgent createEclipseUserAgent() {
		return new EclipseUserAgent();
	}

	public String getBrowserLanguage() {
		return eclipseUserAgent.getBrowserLanguage();
	}

	public String getScreenResolution() {
		return screenResolution;
	}

	public String getScreenColorDepth() {
		return screenColorDepth;
	}

	protected Display getDisplay() {
		if (PlatformUI.isWorkbenchRunning()) {
			return PlatformUI.getWorkbench().getDisplay();
		}

		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}

	public String getUserAgent() {
		return eclipseUserAgent.toString();
	}

	public String getUserId() {
		String userId = preferences.get(IUsageReportPreferenceConstants.ECLIPSE_INSTANCE_ID, null);
		if (userId == null) {
			userId = createIdentifier();
			preferences.put(IUsageReportPreferenceConstants.ECLIPSE_INSTANCE_ID, userId);
			UsageReportPreferencesUtils.checkedSavePreferences(preferences, SubclipseToolsUsageActivator.getDefault(),
					GoogleAnalyticsEclipseMessages.EclipseEnvironment_Error_SavePreferences);
		}
		return userId;
	}

	/**
	 * Creates an unique identifier.
	 * 
	 * @return the identifier
	 */
	private String createIdentifier() {
		StringBuilder builder = new StringBuilder();
		builder.append(Math.abs(random.nextLong()));
		builder.append(System.currentTimeMillis());
		return builder.toString();
	}

	public abstract String getKeyword();

	public String getCurrentVisit() {
		return currentVisit;
	}

	public String getFirstVisit() {
		return firstVisit;
	}

	public String getLastVisit() {
		return lastVisit;
	}

	public long getVisitCount() {
		return visitCount;
	}

	public void visit() {
		lastVisit = currentVisit;
		preferences.put(IUsageReportPreferenceConstants.LAST_VISIT, lastVisit);
		currentVisit = String.valueOf(System.currentTimeMillis());
		visitCount++;
		preferences.putLong(IUsageReportPreferenceConstants.VISIT_COUNT, visitCount);
		UsageReportPreferencesUtils.checkedSavePreferences(preferences, SubclipseToolsUsageActivator.getDefault(),
				GoogleAnalyticsEclipseMessages.EclipseEnvironment_Error_SavePreferences);
	}

	public String getFlashVersion() {
		return getJavaVersion();
	}

	private String getJavaVersion() {
		return System.getProperty(SYSPROP_JAVA_VERSION);
	}

	public IEclipseUserAgent getEclipseUserAgent() {
		return eclipseUserAgent;
	}

	public String getUserDefined() {
		return getLinuxDistroNameAndVersion();
	}

	protected String getLinuxDistroNameAndVersion() {
		return LinuxSystem.INSTANCE.getDistroNameAndVersion();
	}

}