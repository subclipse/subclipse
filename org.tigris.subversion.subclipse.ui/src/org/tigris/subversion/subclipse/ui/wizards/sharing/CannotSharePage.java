package org.tigris.subversion.subclipse.ui.wizards.sharing;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.tigris.subversion.subclipse.ui.Messages;
import org.tigris.subversion.subclipse.ui.wizards.SVNWizardPage;

public class CannotSharePage extends SVNWizardPage {
	private IProject project;

	public CannotSharePage(String pageName, String title, ImageDescriptor titleImage, IProject project) {
		super(pageName, title, titleImage);
		this.project = project;
	}

	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 1);
		setControl(composite);
		
		createWrappingLabel(composite, Messages.CannotSharePage_2 + project.getName() + Messages.CannotSharePage_0, 0 /* indent */, 1 /* columns */); 
		
		setErrorMessage(Messages.CannotSharePage_1);

		setPageComplete(false);
	}

}
