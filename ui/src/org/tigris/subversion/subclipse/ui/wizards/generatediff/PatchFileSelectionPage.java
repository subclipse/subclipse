/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.wizards.generatediff;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.util.ContainerContentProvider;



/**
 * Page to select a patch file. Overriding validatePage was necessary to allow
 * entering a file name that already exists.
 */
class PatchFileSelectionPage extends WizardPage {
	private Text filenameCombo;
	private Button browseButton;
	
	private TreeViewer treeViewer;
	private IContainer selectedContainer;
	private Text workspaceFilename;
	private Button saveInFilesystem;
	private Button saveInWorkspace;
	private Button saveToClipboard;
	
	public final int CLIPBOARD = 1;
	public final int FILESYSTEM = 2;
	public final int WORKSPACE = 3;
	
	// sizing constants
	private static final int SIZING_SELECTION_PANE_HEIGHT = 125;
	private static final int SIZING_SELECTION_PANE_WIDTH = 200;
	
	public PatchFileSelectionPage(String pageName, String title, ImageDescriptor image, IStructuredSelection selection) {
		super(pageName, title, image);
		
		setPageComplete(false);
	}
	
	/**
	 * Allow the user to finish if a valid file has been entered. 
	 */
	protected boolean validatePage() {
		boolean valid = false;									
		
		switch (getSaveType()) {
			case WORKSPACE:
				if (selectedContainer != null && getWorkspaceFile() != null) {
					valid = true;
				}
				break;
			case FILESYSTEM:
				File file = new File(getFilesystemFile());
				valid = isValidFile(file);
				break;
			case CLIPBOARD:
				valid = true;
				break;
		}
				
		// Avoid draw flicker by clearing error message
		// if all is valid.
		if (valid) {
			setMessage(null);
			setErrorMessage(null);
		} else {
			setErrorMessage(Policy.bind("GenerateSVNDiff.EnterFilename")); //$NON-NLS-1$
		}
		setPageComplete(valid);
		return valid;
	}

	private boolean isValidFile(File file) {
		if (!file.isAbsolute()) return false;
		if (file.isDirectory()) return false;
		File parent = file.getParentFile();
		if (parent==null) return false;
		if (!parent.exists()) return false;
		if (!parent.isDirectory()) return false;
		return true;
	}
	/**
	 * Answers a full path to a file system file or <code>null</code> if the user
	 * selected to save the patch in the workspace. 
	 */
	public String getFilesystemFile() {
		if(saveInFilesystem.getSelection()) {
			return filenameCombo.getText();
		} 
		return null;
	}
	
	/**
	 * Answers a workspace file or <code>null</code> if the user selected to save
	 * the patch outside of the workspace.
	 */
	public IFile getWorkspaceFile() {
		if(saveInWorkspace.getSelection() && selectedContainer !=null) {
			String filename = workspaceFilename.getText();
			if(filename==null || filename.length() == 0) {
				return null;
			}
			return selectedContainer.getFile(new Path(workspaceFilename.getText()));
		}
		return null;
	}

	/**
	 * Allow the user to chose to save the patch to the workspace or outside
	 * of the workspace.
	 */
	public void createControl(Composite parent) {
		
		Composite composite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout();
		composite.setLayout(layout);
		composite.setLayoutData(new GridData());
		setControl(composite);
		initializeDialogUnits(composite);

		// set F1 help
		WorkbenchHelp.setHelp(composite, IHelpContextIds.PATCH_SELECTION_PAGE);
				
		// Clipboard
		saveToClipboard= new Button(composite, SWT.RADIO);
		saveToClipboard.setText(Policy.bind("GenerateSVNDiff.SaveToClipboard")); //$NON-NLS-1$
		saveToClipboard.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				validatePage();
				updateEnablements();
			}
		});
		
		// File System
		saveInFilesystem= new Button(composite, SWT.RADIO);
		saveInFilesystem.setText(Policy.bind("GenerateSVNDiff.SaveInFileSystem")); //$NON-NLS-1$
		saveInFilesystem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				validatePage();
				updateEnablements();
			}
		});

		Composite nameGroup = new Composite(composite,SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		nameGroup.setLayout(layout);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		nameGroup.setLayoutData(data);
		
		filenameCombo= new Text(nameGroup, SWT.BORDER);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		filenameCombo.setLayoutData(gd);
		filenameCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});

		browseButton = new Button(nameGroup, SWT.NULL);
		browseButton.setText(Policy.bind("GenerateSVNDiff.Browse")); //$NON-NLS-1$
		data = new GridData(GridData.HORIZONTAL_ALIGN_END);
		data.heightHint = convertVerticalDLUsToPixels(IDialogConstants.BUTTON_HEIGHT);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		browseButton.setLayoutData(data);
		browseButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				FileDialog d = new FileDialog(getShell(), SWT.PRIMARY_MODAL | SWT.SAVE);
				d.setText(Policy.bind("GenerateSVNDiff.SavePatchAs")); //$NON-NLS-1$
				d.setFileName(Policy.bind("GenerateSVNDiff.patchTxt")); //$NON-NLS-1$
				String file = d.open();
				if(file!=null) {
					IPath path = new Path(file);
					setFilesystemFilename(path.toOSString());
				}			
			}
		});			
		
		// Workspace
		saveInWorkspace= new Button(composite, SWT.RADIO);
		saveInWorkspace.setText(Policy.bind("GenerateSVNDiff.SaveInWorkspace")); //$NON-NLS-1$
		saveInWorkspace.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				validatePage();
				updateEnablements();
			}
		});
		
		createTreeViewer(composite);		
		saveToClipboard.setSelection(true);
		validatePage();
		updateEnablements();
	}
	
	/**
	 * Sets the file name in the file system text.
	 */
	protected void setFilesystemFilename(String filename) {
		filenameCombo.setText(filename);
	}
	
	/**
	 * Create the tree viewer that shows the container available in the workspace. The user
	 * can then enter a filename in the text box below the viewer.
	 */
	protected void createTreeViewer(Composite parent) {
		// Create tree viewer inside drill down.
		new Label(parent, SWT.LEFT).setText(Policy.bind("GenerateSVNDiff.SelectFolderAndFilename"));		 //$NON-NLS-1$
		
		treeViewer = new TreeViewer(parent, SWT.BORDER);
		ContainerContentProvider cp = new ContainerContentProvider();
		cp.showClosedProjects(false);
		GridData data = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL |
							  		  GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		
		data.widthHint = SIZING_SELECTION_PANE_WIDTH;
		data.heightHint = SIZING_SELECTION_PANE_HEIGHT;
				
		treeViewer.getTree().setLayoutData(data);
		treeViewer.setContentProvider(cp);
		treeViewer.setLabelProvider(new WorkbenchLabelProvider());
		treeViewer.addSelectionChangedListener(
			new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					IStructuredSelection selection = (IStructuredSelection)event.getSelection();
					containerSelectionChanged((IContainer) selection.getFirstElement()); // allow null
					validatePage();
				}
			});
		
		treeViewer.addDoubleClickListener(
			new IDoubleClickListener() {
				public void doubleClick(DoubleClickEvent event) {
					ISelection selection = event.getSelection();
					if (selection instanceof IStructuredSelection) {
						Object item = ((IStructuredSelection)selection).getFirstElement();
						if (treeViewer.getExpandedState(item))
							treeViewer.collapseToLevel(item, 1);
						else
							treeViewer.expandToLevel(item, 1);
					}
				}
			});
	
		// This has to be done after the viewer has been laid out
		treeViewer.setInput(ResourcesPlugin.getWorkspace());
		
		// name group
		Composite nameGroup = new Composite(parent,SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		nameGroup.setLayout(layout);
		data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		nameGroup.setLayoutData(data);
	
		Label label = new Label(nameGroup,SWT.NONE);
		label.setText(Policy.bind("GenerateSVNDiff.FileName")); //$NON-NLS-1$
	
		// resource name entry field
		workspaceFilename = new Text(nameGroup,SWT.BORDER);
		data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		workspaceFilename.setLayoutData(data);
		workspaceFilename.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});
	}

	/**
	 * Enable and disable controls based on the selected radio button.
	 */
	protected void updateEnablements() {
		int type = getSaveType();

		browseButton.setEnabled(type==FILESYSTEM);
		filenameCombo.setEnabled(type==FILESYSTEM);
		treeViewer.getTree().setEnabled(type==WORKSPACE);
		workspaceFilename.setEnabled(type==WORKSPACE);
	}
	
	/**
	 * Answers the type of the patch file destination.
	 */		
	public int getSaveType() {
		if(saveInFilesystem.getSelection()) {
			return FILESYSTEM;
		} else if(saveToClipboard.getSelection()) {
			return CLIPBOARD;
		} else {
			return WORKSPACE;
		}
	}
	
	/**
	 * Remember the container selected in the tree viewer.
	 */
	public void containerSelectionChanged(IContainer container) {
		selectedContainer = container;
	}
}