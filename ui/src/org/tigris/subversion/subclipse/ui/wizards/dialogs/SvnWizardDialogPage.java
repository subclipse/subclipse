package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public abstract class SvnWizardDialogPage extends WizardPage {

	public SvnWizardDialogPage(String pageName, String title) {
		this(pageName, title, SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_SVN));
	}

	public SvnWizardDialogPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	public void createControl(Composite parent) {
		Composite outerContainer = new Composite(parent,SWT.NONE);
		GridLayout outerLayout = new GridLayout();
		outerLayout.numColumns = 1;
		outerLayout.marginHeight = 0;
		outerLayout.marginWidth = 0;
		outerContainer.setLayout(outerLayout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		createControls(outerContainer);
		
		setMessage();
		
		setControl(outerContainer);
	}
	
	public abstract void createControls(Composite outerContainer);
	
	public abstract void createButtonsForButtonBar(Composite parent, SvnWizardDialog wizardDialog);
	
	public abstract String getWindowTitle();
	
	public abstract void setMessage();
	
	public abstract boolean performFinish();
	
	public abstract boolean performCancel();
	
	public abstract void saveSettings();
	
	protected static final int LABEL_WIDTH_HINT = 400;
	protected Label createWrappingLabel(Composite parent) {
		Label label = new Label(parent, SWT.LEFT | SWT.WRAP);
		GridData data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalIndent = 0;
		data.grabExcessHorizontalSpace = true;
		data.widthHint = LABEL_WIDTH_HINT;
		label.setLayoutData(data);
		return label;
	}	

}
