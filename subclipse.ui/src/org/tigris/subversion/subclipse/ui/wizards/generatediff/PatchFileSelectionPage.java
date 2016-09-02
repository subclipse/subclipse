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
package org.tigris.subversion.subclipse.ui.wizards.generatediff;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.util.ResourceSelectionTree;
import org.tigris.subversion.subclipse.ui.util.WorkspaceDialog;



/**
 * Page to select a patch file. Overriding validatePage was necessary to allow
 * entering a file name that already exists.
 */
class PatchFileSelectionPage extends WizardPage {
	private Text filenameCombo;
	private Button browseButton;
	private Button saveInFilesystem;
	private Button saveInWorkspace;
	private Button saveToClipboard;
	private ResourceSelectionTree resourceSelectionTree;
	private IResource[] resources;
	protected Text wsPathText;
    private Button wsBrowseButton;
    private boolean wsBrowsed = false;
    private HashMap statusMap;
	
	public final int CLIPBOARD = 1;
	public final int FILESYSTEM = 2;
	public final int WORKSPACE = 3;
	
	public PatchFileSelectionPage(String pageName, String title, ImageDescriptor image, IStructuredSelection selection, HashMap statusMap) {
		super(pageName, title, image);
		this.statusMap = statusMap;
		Object[] selectedResources = selection.toArray();
		resources = new IResource[selectedResources.length];
		for (int i = 0; i < selectedResources.length; i++)
			resources[i] = (IResource)selectedResources[i];
		setPageComplete(false);
	}
	
	/**
	 * Allow the user to finish if a valid file has been entered. 
	 */
	protected boolean validatePage() {
		boolean valid = false;									
		
		switch (getSaveType()) {
			case WORKSPACE:
				valid = validateWorkspaceLocation();
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
		setPageComplete(valid && getSelectedResources().length > 0);
		return valid;
	}

	private boolean isValidFile(File file) {
		if (!file.isAbsolute()) return false;
		if (file.isDirectory()) return false;
		File parent = file.getParentFile();
		if (parent==null) return false;
		if (!parent.exists()) return false;
		if (!parent.isDirectory()) return false;
		if (!file.exists()) {
			try {
				if (!file.createNewFile()) {
					return false;
				}
				file.delete();
			} catch (IOException e) {
				return false;
			}
		}
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
		if(saveInWorkspace.getSelection() && wsPathText.getText().length() > 0) {
			return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(wsPathText.getText()));
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
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.PATCH_SELECTION_PAGE);
				
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
				setPageComplete(validatePage());
			}
		});
		
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		filenameCombo.addFocusListener(focusListener);

		browseButton = new Button(nameGroup, SWT.NULL);
		browseButton.setText(Policy.bind("GenerateSVNDiff.Browse")); //$NON-NLS-1$
		data = new GridData(GridData.HORIZONTAL_ALIGN_END);
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
		
        final Composite pathGroup = new Composite(composite,SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginWidth = 0;
        pathGroup.setLayout(layout);
        data = new GridData(SWT.FILL, SWT.FILL, true, false);
        pathGroup.setLayoutData(data);
		
        wsPathText= new Text(pathGroup, SWT.BORDER);
        gd= new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalAlignment = GridData.CENTER;
        gd.grabExcessVerticalSpace = false;
        gd.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
        wsPathText.setLayoutData(gd);
        wsPathText.setEditable(false);
        
        wsBrowseButton = new Button(pathGroup, SWT.NULL);
        gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		gd.widthHint = Math.max(widthHint, wsBrowseButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		wsBrowseButton.setLayoutData(gd);
        wsBrowseButton.setText(Policy.bind("GenerateSVNDiff.Browse")); //$NON-NLS-1$	
		wsBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
            	final WorkspaceDialog dialog = new WorkspaceDialog(getShell(), Policy.bind("GenerateSVNDiff.workspaceDialogTitle"), Policy.bind("GenerateSVNDiff.workspaceDialogMessage"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_DIFF), wsPathText); //$NON-NLS-1$ //$NON-NLS-1$		
            	wsBrowsed = true;
            	dialog.open();
            	validatePage();
			}
		});
		
		resourceSelectionTree = new ResourceSelectionTree(composite, SWT.NONE, Policy.bind("GenerateSVNDiff.Changes"), ResourceSelectionTree.dedupeResources(resources), statusMap, null, true, null, null); //$NON-NLS-1$
		((CheckboxTreeViewer)resourceSelectionTree.getTreeViewer()).setAllChecked(true);
        
		resourceSelectionTree.getTreeViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				validatePage();
			}			
		});
		
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
	
	public IResource[] getSelectedResources() {
		return resourceSelectionTree.getSelectedResources();
	}

	/**
	 * Enable and disable controls based on the selected radio button.
	 */
	protected void updateEnablements() {
		int type = getSaveType();

		browseButton.setEnabled(type==FILESYSTEM);
		filenameCombo.setEnabled(type==FILESYSTEM);
        wsPathText.setEnabled(type == WORKSPACE);
        wsBrowseButton.setEnabled(type == WORKSPACE);
        if (type == WORKSPACE)
        	wsBrowsed=false;		
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
     * The following conditions must hold for the file system location to be valid:
     * - a parent must be selected in the workspace tree view
     * - the resource name must be valid 
     */
    private boolean validateWorkspaceLocation() {
    	int type = getSaveType();
        //make sure that the field actually has a filename in it - making
    	//sure that the user has had a chance to browse the workspace first
        if (wsPathText.getText().equals("")){ //$NON-NLS-1$
        	if (type ==WORKSPACE && wsBrowsed)
        		setErrorMessage(Policy.bind("GenerateSVNDiff.validFileName")); //$NON-NLS-1$	
        	return false;
        }
        
        //Make sure that all the segments but the last one (i.e. project + all
        //folders) exist - file doesn't have to exist. It may have happened that
        //some folder refactoring has been done since this path was last saved.
        //
        //Assume that the path will always be in format project/{folders}*/file - this
        //is controlled by the workspace location dialog
        
        
        IPath pathToWorkspaceFile = new Path(wsPathText.getText());
        //Trim file name from path
        IPath containerPath = pathToWorkspaceFile.removeLastSegments(1);
        
        IResource container =ResourcesPlugin.getWorkspace().getRoot().findMember(containerPath);
        if (container == null) {
        	if (type == WORKSPACE)
        		setErrorMessage(Policy.bind("GenerateSVNDiff.validFileName")); //$NON-NLS-1$	
            return false;
        }
        
        return true;
    }	
	
}