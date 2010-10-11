package org.tigris.subversion.subclipse.tools.usage.reporting;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.tigris.subversion.subclipse.tools.usage.internal.SubclipseToolsUsageActivator;
import org.tigris.subversion.subclipse.tools.usage.util.BrowserUtil;

public class UsageReportEnablementDialog extends Dialog {

	private Button checkBox;
	private boolean reportEnabled;

	public UsageReportEnablementDialog(boolean reportEnabled, Shell parentShell) {
		super(parentShell);
		this.reportEnabled = reportEnabled;
	}

	public UsageReportEnablementDialog(boolean reportEnabled, IShellProvider parentShell) {
		super(parentShell);
		this.reportEnabled = reportEnabled;
	}

	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			this.reportEnabled = checkBox.getSelection();
		} else if (buttonId == IDialogConstants.CANCEL_ID) {
			this.reportEnabled = false;
		}
		super.buttonPressed(buttonId);
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(getDialogTitle());
	}

	private String getDialogTitle() {
		return ReportingMessages.UsageReport_DialogTitle;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		checkBox.setFocus();
		checkBox.setSelection(reportEnabled);
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		// message
		Link link = new Link(composite, SWT.WRAP);
		link.setFont(parent.getFont());
		link.setText(ReportingMessages.UsageReport_DialogMessage);
		link.setToolTipText(ReportingMessages.UsageReport_ExplanationPage);
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				BrowserUtil.checkedCreateExternalBrowser(
						ReportingMessages.UsageReport_ExplanationPage,
						SubclipseToolsUsageActivator.PLUGIN_ID,
						SubclipseToolsUsageActivator.getDefault().getLog());
			}
		});

		// checkbox
		checkBox = new Button(composite, SWT.CHECK);
		checkBox.setText(ReportingMessages.UsageReport_Checkbox_Text);

		return composite;
	}

	public boolean isReportEnabled() {
		return reportEnabled;
	}

}
