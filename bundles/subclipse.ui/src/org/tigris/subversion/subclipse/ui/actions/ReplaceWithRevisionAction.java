/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.SaveablePartDialog;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.compare.SVNCompareRevisionsInput;

/**
 * Displays a compare dialog and allows the same behavior as the compare. In addition
 * a replace button is added to the dialog that will replace the local with the currently 
 * selected revision.
 * 
 * @since 3.0
 */
public class ReplaceWithRevisionAction extends CompareWithRevisionAction {
	
	protected static final int REPLACE_ID = 10;
	private SVNCompareRevisionsInput input;
	
	protected class ReplaceCompareDialog extends SaveablePartDialog {
		private Button replaceButton;
		
		public ReplaceCompareDialog(Shell shell, SVNCompareRevisionsInput input) {
			super(shell, input);	
			// Don't allow editing of the merge viewers in the replace
			input.getCompareConfiguration().setLeftEditable(false);
			input.getCompareConfiguration().setRightEditable(false);
		}
		
		/**
		 * Add the replace button to the dialog.
		 */
		protected void createButtonsForButtonBar(Composite parent) {
			replaceButton = createButton(parent, REPLACE_ID, Policy.bind("ReplaceWithRevisionAction.replace"), true); //$NON-NLS-1$
			replaceButton.setEnabled(false);
			input.getViewer().addSelectionChangedListener(
				new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent e) {
						ISelection s= e.getSelection();
						replaceButton.setEnabled(s != null && ! s.isEmpty());
					}
				}
			);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false); //$NON-NLS-1$
			// Don't call super because we don't want the OK button to appear
		}
		
		/**
		 * If the replace button was pressed.
		 */
		protected void buttonPressed(int buttonId) {
			if(buttonId == REPLACE_ID) {
				try {
					input.replaceLocalWithCurrentlySelectedRevision();
				} catch (CoreException e) {
					handle(e);
				}
				buttonId = IDialogConstants.OK_ID;
			}
			super.buttonPressed(buttonId);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CompareWithRevisionAction#createCompareDialog(org.eclipse.swt.widgets.Shell, org.eclipse.team.internal.ccvs.ui.CVSCompareRevisionsInput)
	 */
	protected SaveablePartDialog createCompareDialog(Shell shell, SVNCompareRevisionsInput input) {
		this.input = input;
		return new ReplaceCompareDialog(shell, input); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CompareWithRevisionAction#getActionTitle()
	 */
	protected String getActionTitle() {
		return Policy.bind("ReplaceWithRevisionAction.title"); //$NON-NLS-1$
	}
}
