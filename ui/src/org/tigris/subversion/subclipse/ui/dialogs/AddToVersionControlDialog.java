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
package org.tigris.subversion.subclipse.ui.dialogs;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.util.DetailsDialog;

/**
 * This dialog allows the user to add a set of resources to version control.
 * They can either all be added or the user can choose which to add from a
 * details list.
 * This dialog is used when user wants to commit some files which are not under revision
 * control
 */
public class AddToVersionControlDialog extends DetailsDialog {

	private static final int WIDTH_HINT = 350;
	private final static int SELECTION_HEIGHT_HINT = 100;
	
	private IResource[] unaddedResources;
	private Object[] resourcesToAdd;
	
	private CheckboxTableViewer listViewer;
	/**
	 * Constructor for AddToVersionControlDialog.
	 * @param parentShell
	 */
	public AddToVersionControlDialog(Shell parentShell, IResource[] unaddedResources) {
		super(parentShell, Policy.bind("AddToVersionControlDialog.title")); //$NON-NLS-1$
		this.unaddedResources = unaddedResources;
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected void createMainDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.ADD_TO_VERSION_CONTROL_DIALOG);
			 
		// add a description label
		if (unaddedResources.length==1) {
			createWrappingLabel(composite, Policy.bind("AddToVersionControlDialog.thereIsAnUnaddedResource", new Integer(unaddedResources.length).toString()));  //$NON-NLS-1$
		} else {
			createWrappingLabel(composite, Policy.bind("AddToVersionControlDialog.thereAreUnaddedResources", new Integer(unaddedResources.length).toString()));  //$NON-NLS-1$
		}
	}

	/**
	 * @see org.eclipse.team.internal.ui.DetailsDialog#createDropDownDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Composite createDropDownDialogArea(Composite parent) {
		// create a composite with standard margins and spacing
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setFont(parent.getFont());
		
		addUnaddedResourcesArea(composite);
		
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.ADD_TO_VERSION_CONTROL_DIALOG);
		
		return composite;
	}

	private void addUnaddedResourcesArea(Composite composite) {
		
		// add a description label
		createWrappingLabel(composite, Policy.bind("ReleaseCommentDialog.unaddedResources")); //$NON-NLS-1$
	
		// add the selectable checkbox list
		listViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = SELECTION_HEIGHT_HINT;
		data.widthHint = WIDTH_HINT;
		listViewer.getTable().setLayoutData(data);

		// set the contents of the list
		listViewer.setLabelProvider(new WorkbenchLabelProvider() {
			protected String decorateText(String input, Object element) {
				if (element instanceof IResource)
					return ((IResource)element).getFullPath().toString();
				else
					return input;
			}
		});
		listViewer.setContentProvider(new WorkbenchContentProvider());
		listViewer.setInput(new AdaptableResourceList(unaddedResources));
		if (resourcesToAdd == null) {
			listViewer.setAllChecked(true);
		} else {
			listViewer.setCheckedElements(resourcesToAdd);
		}
		listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				resourcesToAdd = listViewer.getCheckedElements();
			}
		});
		
		addSelectionButtons(composite);
	}
	
	/**
	 * Add the selection and deselection buttons to the dialog.
	 * @param composite org.eclipse.swt.widgets.Composite
	 */
	private void addSelectionButtons(Composite composite) {
	
		Composite buttonComposite = new Composite(composite, SWT.RIGHT);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		buttonComposite.setLayout(layout);
		GridData data =
			new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		composite.setData(data);
	
		Button selectButton = createButton(buttonComposite, IDialogConstants.SELECT_ALL_ID, Policy.bind("ReleaseCommentDialog.selectAll"), false); //$NON-NLS-1$
		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				listViewer.setAllChecked(true);
				resourcesToAdd = null;
			}
		};
		selectButton.addSelectionListener(listener);
	
		Button deselectButton = createButton(buttonComposite, IDialogConstants.DESELECT_ALL_ID, Policy.bind("ReleaseCommentDialog.deselectAll"), false); //$NON-NLS-1$
		listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				listViewer.setAllChecked(false);
				resourcesToAdd = new Object[0];
	
			}
		};
		deselectButton.addSelectionListener(listener);
	}
	
	/**
	 * @see org.eclipse.team.internal.ui.DetailsDialog#updateEnablements()
	 */
	protected void updateEnablements() {
	}
	
	/**
	 * Returns the resourcesToAdd.
	 * @return IResource[]
	 */
	public IResource[] getResourcesToAdd() {
		if (resourcesToAdd == null) {
			return unaddedResources;
		} else {
			List result = Arrays.asList(resourcesToAdd);
			return (IResource[]) result.toArray(new IResource[result.size()]);
		}
	}
	
	protected static final int LABEL_WIDTH_HINT = 400;
	protected Label createWrappingLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.LEFT | SWT.WRAP);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalIndent = 0;
		data.grabExcessHorizontalSpace = true;
		data.widthHint = LABEL_WIDTH_HINT;
		label.setLayoutData(data);
		return label;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.YES_ID, IDialogConstants.YES_LABEL, true);
		createButton(parent, IDialogConstants.NO_ID, IDialogConstants.NO_LABEL, true);
		super.createButtonsForButtonBar(parent);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.DetailsDialog#includeOkButton()
	 */
	protected boolean includeOkButton() {
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int id) {
		// hijack yes and no buttons to set the correct return
		// codes.
		if(id == IDialogConstants.YES_ID || id == IDialogConstants.NO_ID) {
			setReturnCode(id);
			close();
		} else {
			super.buttonPressed(id);
		}
	}
}
