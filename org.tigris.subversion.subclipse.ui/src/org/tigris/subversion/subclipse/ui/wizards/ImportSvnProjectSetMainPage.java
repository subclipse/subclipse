package org.tigris.subversion.subclipse.ui.wizards;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.WorkingSetGroup;
import org.tigris.subversion.subclipse.ui.Policy;

public class ImportSvnProjectSetMainPage extends WizardPage {
	private WorkingSetGroup workingSetGroup; 

	public ImportSvnProjectSetMainPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setDescription(Policy.bind("ImportSvnProjectSetMainPage.0")); //$NON-NLS-1$
	}

	public void createControl(Composite parent) {
		Composite outerContainer = new Composite(parent,SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		outerContainer.setLayout(layout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		workingSetGroup = new WorkingSetGroup(
				outerContainer,
				null,
				new String[] { "org.eclipse.ui.resourceWorkingSetPage", //$NON-NLS-1$
						"org.eclipse.jdt.ui.JavaWorkingSetPage" /* JavaWorkingSetUpdater.ID */}); //$NON-NLS-1$
		
		setMessage(Policy.bind("ImportSvnProjectSetMainPage.1")); //$NON-NLS-1$
		setControl(outerContainer);
	}
	
	public IWorkingSet[] getWorkingSets() {
		return workingSetGroup.getSelectedWorkingSets();
	}

}
