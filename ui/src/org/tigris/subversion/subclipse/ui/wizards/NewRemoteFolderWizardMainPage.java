/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.wizards;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.DrillDownAdapter;
import org.tigris.subversion.subclipse.ui.repository.model.AllRootsElement;
import org.tigris.subversion.subclipse.ui.repository.model.RemoteContentProvider;

/**
 */
public class NewRemoteFolderWizardMainPage extends SVNWizardPage {

    private Text urlParentText;
    private Text folderNameText;

    private TreeViewer viewer;

    // Drill down adapter
    private DrillDownAdapter drillPart; // Home, back, and "drill into"

    private AllRootsElement root;
    private RemoteContentProvider contentProvider;
	
	/**
	 * NewRemoteFolderWizardMainPage constructor.
	 * 
	 * @param pageName  the name of the page
	 * @param title  the title of the page
	 * @param titleImage  the image for the page
	 */
	public NewRemoteFolderWizardMainPage(
        String pageName, 
        String title, 
        ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}
    
	/**
	 * Creates the UI part of the page.
	 * 
	 * @param parent  the parent of the created widgets
	 */
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 1);
		// set F1 help
//		WorkbenchHelp.setHelp(composite, IHelpContextIds.SHARING_NEW_REPOSITORY_PAGE);

		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				validateFields();
			}
		};
		
	//	Group g = createGroup(composite, Policy.bind("ConfigurationWizardMainPage.Location_1")); //$NON-NLS-1$
		
		createLabel(composite, "Url of remote directory to create :"); // Policy.bind("ConfigurationWizardMainPage.password")); //$NON-NLS-1$
        urlParentText = createTextField(composite);
        urlParentText.addListener(SWT.Selection, listener);
        urlParentText.addListener(SWT.Modify, listener);
        
        viewer = new TreeViewer(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        contentProvider = new RemoteContentProvider();
        viewer.setLabelProvider(new WorkbenchLabelProvider());
//        getSite().setSelectionProvider(viewer);
        root = new AllRootsElement();
        viewer.setInput(root);
        drillPart = new DrillDownAdapter(viewer);

        createLabel(composite, "Folder name :"); // Policy.bind("ConfigurationWizardMainPage.password")); //$NON-NLS-1$
        folderNameText = createTextField(composite);
        folderNameText.addListener(SWT.Selection, listener);
        folderNameText.addListener(SWT.Modify, listener);


		initializeValues();
		validateFields();
        urlParentText.setFocus();
		
		setControl(composite);
	}
	
	protected Group createGroup(Composite parent, String text) {
		Group group = new Group(parent, SWT.NULL);
		group.setText(text);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		//data.widthHint = GROUP_WIDTH;
		
		group.setLayoutData(data);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		return group;
	}
	
	/**
	 * @see SVNWizardPage#finish
	 */
	public boolean finish(IProgressMonitor monitor) {

		return true;
	}

	/**
	 * Initializes states of the controls.
	 */
	private void initializeValues() {
	}

	
	/**
	 * Validates the contents of the editable fields and set page completion 
	 * and error messages appropriately. Call each time url or username is modified 
	 */
	private void validateFields() {
/*		String url = urlCombo.getText();
		if (url.length() == 0) {
			setErrorMessage(null);
			setPageComplete(false);
			return;
		}
		try {
			new URL(url);
		} catch (MalformedURLException e) {
			setErrorMessage(Policy.bind("ConfigurationWizardMainPage.invalidUrl")); //$NON-NLS-1$);
			setPageComplete(false);			
			return;
		}
		setErrorMessage(null);
		setPageComplete(true); */
	}
	
   
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			urlParentText.setFocus();
		}
	}
}
