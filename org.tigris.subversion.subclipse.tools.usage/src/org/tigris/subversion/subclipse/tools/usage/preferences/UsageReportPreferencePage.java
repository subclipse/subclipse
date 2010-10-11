package org.tigris.subversion.subclipse.tools.usage.preferences;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.service.prefs.BackingStoreException;
import org.tigris.subversion.subclipse.tools.usage.internal.SubclipseToolsUsageActivator;
import org.tigris.subversion.subclipse.tools.usage.reporting.ReportingMessages;
import org.tigris.subversion.subclipse.tools.usage.util.BrowserUtil;
import org.tigris.subversion.subclipse.tools.usage.util.StatusUtils;

public class UsageReportPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private Button allowReportingButton;

	public UsageReportPreferencePage() {
		super();
	}
	
	protected Control createContents(Composite parent) {
		
		// create the composite
		Composite composite = new Composite(parent, SWT.NULL);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        composite.setLayoutData(gridData);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		
		allowReportingButton = new Button(composite, SWT.CHECK);
		allowReportingButton.setSelection(getPreferenceStore().getBoolean(IUsageReportPreferenceConstants.USAGEREPORT_ENABLED_ID));
		Link link = new Link(composite, SWT.WRAP);
		link.setFont(parent.getFont());
		link.setText(getCheckBoxlabel());
		link.setToolTipText(ReportingMessages.UsageReport_ExplanationPage);
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				BrowserUtil.checkedCreateExternalBrowser(
						ReportingMessages.UsageReport_ExplanationPage,
						SubclipseToolsUsageActivator.PLUGIN_ID,
						SubclipseToolsUsageActivator.getDefault().getLog());
			}
		});
		
		return composite;
	}

	private String getCheckBoxlabel() {
		return PreferencesMessages.UsageReportPreferencePage_AllowReporting;
	}
	

	public void init(IWorkbench workbench) {
		setPreferenceStore(UsageReportPreferences.createPreferenceStore());
	}

	public boolean performOk() {
		getPreferenceStore().setValue(IUsageReportPreferenceConstants.USAGEREPORT_ENABLED_ID, allowReportingButton.getSelection());
		try {
			UsageReportPreferences.flush();
		} catch (BackingStoreException e) {
			IStatus status = StatusUtils.getErrorStatus(SubclipseToolsUsageActivator.PLUGIN_ID,
					getPrefsSaveErrorMessage() , e, null);
			SubclipseToolsUsageActivator.getDefault().getLog().log(status);
		}
		return super.performOk();
	}

	private String getPrefsSaveErrorMessage() {
		return PreferencesMessages.UsageReportPreferencePage_Error_Saving;
	}	
	
}