/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.wizards.sharing;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.comments.CommitCommentArea;
import org.tigris.subversion.subclipse.ui.settings.CommentProperties;
import org.tigris.subversion.subclipse.ui.wizards.SVNWizardPage;

/**
 * The finish page of the sharing wizard
 */
public class SharingWizardFinishPage extends SVNWizardPage {
	private ISVNRepositoryLocationProvider repositoryLocationProvider;

	private CommitCommentArea commitCommentArea;
	private CommentProperties commentProperties;
	
	public SharingWizardFinishPage(String pageName, String title, ImageDescriptor titleImage, ISVNRepositoryLocationProvider repositoryLocationProvider) {
		super(pageName, title, titleImage);
		this.repositoryLocationProvider = repositoryLocationProvider;
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 1);
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.SHARING_FINISH_PAGE);
		Label label = new Label(composite, SWT.LEFT | SWT.WRAP);
		label.setText(Policy.bind("SharingWizardFinishPage.message")); //$NON-NLS-1$
		GridData data = new GridData();
		data.widthHint = 350;
		label.setLayoutData(data);
		IProject project = repositoryLocationProvider.getProject();
        try {
            commentProperties = CommentProperties.getCommentProperties(project);
        } catch (SVNException e) {}
		commitCommentArea = new CommitCommentArea(null, null, commentProperties); //$NON-NLS-1$
		commitCommentArea.setProposedComment(Policy.bind("SharingWizard.initialImport")); //$NON-NLS-1$
		if ((commentProperties != null) && (commentProperties.getMinimumLogMessageSize() != 0)) {
		    ModifyListener modifyListener = new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    setPageComplete(commitCommentArea.getCommentLength() >= commentProperties.getMinimumLogMessageSize());
                }		        
		    };
		    commitCommentArea.setModifyListener(modifyListener);
		}		
		commitCommentArea.createArea(composite);
		setControl(composite);
	}
	
	public String getComment() {
		commitCommentArea.addComment(commitCommentArea.getComment());
		return commitCommentArea.getComment();
	}
}
