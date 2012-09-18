/*******************************************************************************
 * Copied from package org.eclipse.compare.internal.CompareDialog, but features
 * regarding writing the resource is taken out.
 *
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.tigris.subversion.subclipse.ui.dialogs;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;


public class CompareDialog extends SubclipseTrayDialog {
		
	private CompareEditorInput compareEditorInput;
    private IDialogSettings settings;

	public CompareDialog(Shell shell, CompareEditorInput input) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		
		Assert.isNotNull(input);
		compareEditorInput= input;

		settings = SVNUIPlugin.getPlugin().getDialogSettings();
	}

	private boolean compareResultOK(CompareEditorInput input) {
		final Shell shell= getShell();
		try {
			// run operation in separate thread and make it canceable
			SVNUIPlugin.getPlugin().getWorkbench().getProgressService().run(true, true, input);
			
			String message= input.getMessage();
			if (message != null) {
				MessageDialog.openError(shell, Policy.bind("CompareDialog.compareFailed"), message); //$NON-NLS-1$
				return false;
			}
			
			if (input.getCompareResult() == null) {
				MessageDialog.openInformation(shell, input.getTitle(), Policy.bind("CompareDialog.noDifferences")); //$NON-NLS-2$ //$NON-NLS-1$
				return false;
			}
			
			return true;

		} catch (InterruptedException x) {
			// cancelled by user		
		} catch (InvocationTargetException x) {
			MessageDialog.openError(shell, Policy.bind("CompareDialog.compareFailed"), x.getTargetException().getMessage()); //$NON-NLS-1$
		}
		return false;
	}
	
	
    protected Point getInitialLocation(Point initialSize) {
	    try {
	        int x = settings.getInt("CompareDialog.location.x"); //$NON-NLS-1$
	        int y = settings.getInt("CompareDialog.location.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
        return super.getInitialLocation(initialSize);
    }
    
    protected Point getInitialSize() {
	    try {
	        int x = settings.getInt("CompareDialog.size.x"); //$NON-NLS-1$
	        int y = settings.getInt("CompareDialog.size.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
	    return new Point(getShell().getSize().x-300, getShell().getSize().y-100);
    }	

	public int open() {
		if (compareResultOK(compareEditorInput))
			return super.open();
		else return IDialogConstants.ABORT_ID;
	}

	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected Control createDialogArea(Composite parent2) {
						
		Composite parent= (Composite) super.createDialogArea(parent2);

		Control c= compareEditorInput.createContents(parent);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));

//		Assert.isTrue(! compareEditorInput.getCompareConfiguration().isLeftEditable());
//		Assert.isTrue(! compareEditorInput.getCompareConfiguration().isRightEditable());

		Shell shell= c.getShell();
		shell.setText(compareEditorInput.getTitle());
		shell.setImage(compareEditorInput.getTitleImage());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.COMPARE_DIALOG);

		applyDialogFont(parent);
		return parent;
	}
		
	protected void cancelPressed() {
	    saveLocation();
	    super.cancelPressed();
	}

	protected void okPressed() {
        saveLocation();
        super.okPressed();
    }
    
	private void saveLocation() {
	    int x = getShell().getLocation().x;
	    int y = getShell().getLocation().y;
	    settings.put("CompareDialog.location.x", x); //$NON-NLS-1$
	    settings.put("CompareDialog.location.y", y); //$NON-NLS-1$
	    x = getShell().getSize().x;
	    y = getShell().getSize().y;
	    settings.put("CompareDialog.size.x", x); //$NON-NLS-1$
	    settings.put("CompareDialog.size.y", y); //$NON-NLS-1$   
	}
}
