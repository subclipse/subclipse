package org.tigris.subversion.subclipse.tools.usage.reporting;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.osgi.service.prefs.BackingStoreException;
import org.tigris.subversion.subclipse.tools.usage.googleanalytics.GoogleAnalyticsUrlStrategy;
import org.tigris.subversion.subclipse.tools.usage.googleanalytics.IGoogleAnalyticsParameters;
import org.tigris.subversion.subclipse.tools.usage.http.HttpGetRequest;
import org.tigris.subversion.subclipse.tools.usage.http.IHttpGetRequest;
import org.tigris.subversion.subclipse.tools.usage.internal.SubclipseToolsUsageActivator;
import org.tigris.subversion.subclipse.tools.usage.preferences.GlobalUsageSettings;
import org.tigris.subversion.subclipse.tools.usage.preferences.UsageReportPreferences;
import org.tigris.subversion.subclipse.tools.usage.preferences.UsageReportPreferencesUtils;
import org.tigris.subversion.subclipse.tools.usage.tracker.ILoggingAdapter;
import org.tigris.subversion.subclipse.tools.usage.tracker.ITracker;
import org.tigris.subversion.subclipse.tools.usage.tracker.IURLBuildingStrategy;
import org.tigris.subversion.subclipse.tools.usage.tracker.internal.FocusPoint;
import org.tigris.subversion.subclipse.tools.usage.tracker.internal.IFocusPoint;
import org.tigris.subversion.subclipse.tools.usage.tracker.internal.PluginLogger;
import org.tigris.subversion.subclipse.tools.usage.tracker.internal.SubclipseToolsFocusPoint;
import org.tigris.subversion.subclipse.tools.usage.tracker.internal.Tracker;
import org.tigris.subversion.subclipse.tools.usage.util.StatusUtils;

public class UsageReport {

	private IFocusPoint focusPoint;

	private GlobalUsageSettings globalSettings;

	public UsageReport() {
		focusPoint = new SubclipseToolsFocusPoint("tools") //$NON-NLS-1$ 
				.setChild(new FocusPoint("usage") //$NON-NLS-1$ 
						.setChild(new FocusPoint("action") //$NON-NLS-1$ 
								.setChild(new FocusPoint("wsstartup")))); //$NON-NLS-1$

		globalSettings = new GlobalUsageSettings(SubclipseToolsUsageActivator
				.getDefault());
	}

	public void report() {
		new ReportingJob().schedule();
	}

	private void askUser() {
		UsageReportEnablementDialog dialog = new UsageReportEnablementDialog(
				true,
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		if (dialog.open() == Window.OK) {
			UsageReportPreferences.setEnabled(dialog.isReportEnabled());
			UsageReportPreferences.setAskUser(false);
			flushPreferences();
		}
	}

	private void flushPreferences() {
		try {
			UsageReportPreferences.flush();
		} catch (BackingStoreException e) {
			IStatus status = StatusUtils.getErrorStatus(SubclipseToolsUsageActivator.PLUGIN_ID,
					ReportingMessages.UsageReport_Error_SavePreferences, e, null);
			SubclipseToolsUsageActivator.getDefault().getLog().log(status);
		}
	}

	private void doReport() {
		if (UsageReportPreferences.isEnabled()) {
			getTracker().trackAsynchronously(focusPoint);
		}
	}

	private ITracker getTracker() {
		IGoogleAnalyticsParameters eclipseEnvironment = new ReportingEclipseEnvironment(
				getGoogleAnalyticsAccount()
				, getGoogleAnalyticsHostname()
				, UsageReportPreferencesUtils.getPreferences());
		ILoggingAdapter loggingAdapter = new PluginLogger(SubclipseToolsUsageActivator.getDefault());
		IURLBuildingStrategy urlStrategy = new GoogleAnalyticsUrlStrategy(eclipseEnvironment);
		IHttpGetRequest httpGetRequest = new HttpGetRequest(eclipseEnvironment.getUserAgent(), loggingAdapter);
		return new Tracker(urlStrategy, httpGetRequest, loggingAdapter);
	}

	private String getGoogleAnalyticsAccount() {
		return ReportingMessages.UsageReport_GoogleAnalytics_Account;
	}

	private String getGoogleAnalyticsHostname() {
		return ReportingMessages.UsageReport_HostName;
	}
	private class ReportingJob extends Job {
		private ReportingJob() {
			super(ReportingMessages.UsageReport_Reporting_Usage);
		}

		protected IStatus run(IProgressMonitor monitor) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			monitor.beginTask(ReportingMessages.UsageReport_Querying_Enablement, 2);
			if (globalSettings.isReportingEnabled()) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				monitor.worked(1);
				if (UsageReportPreferences.isAskUser()) {
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					askUserAndReport();
				} else {
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					doReport();
				}
				monitor.worked(2);
				monitor.done();
			}
			return Status.OK_STATUS;
		}

		private void askUserAndReport() {
			Job askUserJob = new AskUserJob();
			askUserJob.addJobChangeListener(new IJobChangeListener() {

				public void sleeping(IJobChangeEvent event) {
					// ignore
				}

				public void scheduled(IJobChangeEvent event) {
					// ignore
				}

				public void running(IJobChangeEvent event) {
					// ignore
				}

				public void done(IJobChangeEvent event) {
					doReport();
				}

				public void awake(IJobChangeEvent event) {
					// ignore
				}

				public void aboutToRun(IJobChangeEvent event) {
					// ignore
				}
			});
			askUserJob.setUser(true);
			askUserJob.setPriority(Job.SHORT);
			askUserJob.schedule();
		}
	}

	private class AskUserJob extends UIJob {
		private AskUserJob() {
			super(ReportingMessages.UsageReport_Asking_User);
		}

		public IStatus runInUIThread(IProgressMonitor monitor) {
			askUser();
			return Status.OK_STATUS;
		}
	}
}
