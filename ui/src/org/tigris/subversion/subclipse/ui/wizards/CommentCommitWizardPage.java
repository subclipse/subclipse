/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.wizards;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.help.WorkbenchHelp;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.comments.CommitCommentArea;

public class CommentCommitWizardPage extends SVNWizardPage {

	private CommitCommentArea commitCommentArea;

	/**
	 * Constructor for CommentCommitWizardPage.
	 * @param pageName
	 * @param title
	 * @param titleImage
	 * @param description
	 */
	public CommentCommitWizardPage(
		Dialog parentDialog,
		String pageName,
		String title,
		ImageDescriptor titleImage,
		String description) {
			
		super(pageName, title, titleImage, description);
		commitCommentArea = new CommitCommentArea(parentDialog, null);
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		top.setLayout(new GridLayout());
		setControl(top);
		// set F1 help
		WorkbenchHelp.setHelp(top, IHelpContextIds.COMMENT_COMMIT_PAGE_DIALOG);
		commitCommentArea.createArea(top);
        
	}

	/**
	 * Method getComment.
	 * @return String
	 */
	public String getComment() {
		return commitCommentArea.getComment();
	}
}
