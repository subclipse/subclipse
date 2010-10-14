package org.tigris.subversion.subclipse.tools.usage.reporting;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.tigris.subversion.subclipse.tools.usage.internal.SubclipseToolsUsageActivator;
import org.tigris.subversion.subclipse.tools.usage.util.LoggingUtils;
import org.tigris.subversion.subclipse.tools.usage.util.StatusUtils;

public class UsageReportDispatcher implements IStartup {

	public void earlyStartup() {
		Display.getDefault().asyncExec(new Runnable() {

			public void run() {
				try {
					new UsageReport().report();
				} catch (Exception e) {
					IStatus status = StatusUtils.getErrorStatus(SubclipseToolsUsageActivator.PLUGIN_ID,
							"could not start usage reporting", e, null);
					LoggingUtils.log(status, SubclipseToolsUsageActivator.getDefault());
				}
			}
		});
	}
}
