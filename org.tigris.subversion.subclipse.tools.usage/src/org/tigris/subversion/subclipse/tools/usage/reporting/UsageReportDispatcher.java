package org.tigris.subversion.subclipse.tools.usage.reporting;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;

public class UsageReportDispatcher implements IStartup {

	public void earlyStartup() {
		Display.getDefault().asyncExec(new Runnable() {

			public void run() {
				new UsageReport().report();
			}
		});
	}
}
