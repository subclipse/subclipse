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
package org.tigris.subversion.subclipse.ui.wizards;


import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.model.WorkbenchViewerSorter;
import org.eclipse.ui.views.navigator.ResourceSorter;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * Common superclass for SVN wizard pages. Provides convenience methods
 * for widget creation.
 */
public abstract class SVNWizardPage extends WizardPage {
	protected static final int LABEL_WIDTH_HINT = 400;
	protected static final int LABEL_INDENT_WIDTH = 32;
	protected static final int LIST_HEIGHT_HINT = 100;
	protected static final int SPACER_HEIGHT = 8;

	/**
	 * SVNWizardPage constructor comment.
	 * @param pageName  the name of the page
	 */
	public SVNWizardPage(String pageName) {
		super(pageName);
	}
	/**
	 * SVNWizardPage constructor comment.
	 * @param pageName  the name of the page
	 * @param title  the title of the page
	 * @param titleImage  the image for the page
	 */
	public SVNWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}
	/**
	 * SVNWizardPage constructor comment.
	 * @param pageName  the name of the page
	 * @param title  the title of the page
	 * @param titleImage  the image for the page
	 * @param description the description of the page
	 */
	public SVNWizardPage(String pageName, String title, ImageDescriptor titleImage, String description) {
		super(pageName, title, titleImage);
		setDescription(description);
	}
	/**
	 * Creates a new checkbox instance and sets the default layout data.
	 *
	 * @param group  the composite in which to create the checkbox
	 * @param label  the string to set into the checkbox
	 * @return the new checkbox
	 */ 
	protected Button createCheckBox(Composite group, String label) {
		Button button = new Button(group, SWT.CHECK | SWT.LEFT);
		button.setText(label);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		button.setLayoutData(data);
		return button;
	}
	/**
	 * Utility method that creates a combo box
	 *
	 * @param parent  the parent for the new label
	 * @return the new widget
	 */
	protected Combo createCombo(Composite parent) {
		Combo combo = new Combo(parent, SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		combo.setLayoutData(data);
		return combo;
	}
	/**
	 * Creates composite control and sets the default layout data.
	 *
	 * @param parent  the parent of the new composite
	 * @param numColumns  the number of columns for the new composite
	 * @return the newly-created coposite
	 */
	protected Composite createComposite(Composite parent, int numColumns) {
		Composite composite = new Composite(parent, SWT.NULL);
	
		// GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		composite.setLayout(layout);
	
		// GridData
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);
		return composite;
	}
	/**
	 * Utility method that creates a label instance
	 * and sets the default layout data.
	 *
	 * @param parent  the parent for the new label
	 * @param text  the text for the new label
	 * @return the new label
	 */
	public static Label createLabel(Composite parent, String text) {
		return createIndentedLabel(parent, text, 0);
	}
	/**
	 * Utility method that creates a label instance indented by the specified
	 * number of pixels and sets the default layout data.
	 *
	 * @param parent  the parent for the new label
	 * @param text  the text for the new label
	 * @param indent  the indent in pixels, or 0 for none
	 * @return the new label
	 */
	public static Label createIndentedLabel(Composite parent, String text, int indent) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalIndent = indent;
		label.setLayoutData(data);
		return label;
	}
	/**
	 * Utility method that creates a label instance with word wrap and sets
	 * the default layout data.
	 *
	 * @param parent  the parent for the new label
	 * @param text  the text for the new label
	 * @param indent  the indent in pixels, or 0 for none
	 * @param widthHint  the nominal width of the label
	 * @return the new label
	 */
	protected Label createWrappingLabel(Composite parent, String text, int indent) {
		return createWrappingLabel(parent, text, indent, 1);
	}
	
	protected Label createWrappingLabel(Composite parent, String text, int indent, int horizontalSpan) {
		Label label = new Label(parent, SWT.LEFT | SWT.WRAP);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = horizontalSpan;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalIndent = indent;
		data.grabExcessHorizontalSpace = true;
		data.widthHint = LABEL_WIDTH_HINT;
		label.setLayoutData(data);
		return label;
	}
	
	/**
	 * Create a text field specific for this application
	 *
	 * @param parent  the parent of the new text field
	 * @return the new text field
	 */
	static public Text createTextField(Composite parent) {
		Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = GridData.CENTER;
		data.grabExcessVerticalSpace = false;
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		text.setLayoutData(data);
		return text;
	}
	/**
	 * Utility method to create a radio button
	 * 
	 * @param parent  the parent of the radio button
	 * @param label  the label of the radio button
	 * @param span  the number of columns to span
	 * @return the created radio button
	 */
	protected Button createRadioButton(Composite parent, String label, int span) {
		Button button = new Button(parent, SWT.RADIO);
		button.setText(label);
		GridData data = new GridData();
		data.horizontalSpan = span;
		button.setLayoutData(data);
		return button;
	}
	/**
	 * Utility method to create a full width separator preceeded by a blank space
	 * 
	 * @param parent  the parent of the separator
	 * @param verticalSpace  the vertical whitespace to insert before the label
	 */
	protected void createSeparator(Composite parent, int verticalSpace) {
		// space
		Label label = new Label(parent, SWT.NONE);
		GridData data = new GridData();
		data.heightHint = verticalSpace;
		label.setLayoutData(data);
		// separator
		label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(data);
	}
	
	/**
	 * Creates a ListViewer whose input is an array of IFiles.
	 * 
	 * @param parent  the parent of the viewer
	 * @param title  the text for the title label
	 * @param heightHint  the nominal height of the list
	 * @return the created list viewer
	 */
	public ListViewer createFileListViewer(Composite parent, String title, int heightHint) {
		createLabel(parent, title);
		ListViewer listViewer = new ListViewer(parent, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		listViewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				return (Object[]) inputElement;
			}
			public void dispose() {
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		listViewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((IFile) element).getFullPath().toString();
			}
		});
		listViewer.setSorter(new WorkbenchViewerSorter());

		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = heightHint;
		listViewer.getList().setLayoutData(data);
		return listViewer;
	}

	protected TreeViewer createResourceSelectionTree(Composite composite, int types, int span) {
		TreeViewer tree = new TreeViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		tree.setUseHashlookup(true);
		tree.setContentProvider(getResourceProvider(types));
		tree.setLabelProvider(
			new DecoratingLabelProvider(
				new WorkbenchLabelProvider(), 
				SVNUIPlugin.getPlugin().getWorkbench().getDecoratorManager().getLabelDecorator()));
		tree.setSorter(new ResourceSorter(ResourceSorter.NAME));
		
		GridData data = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		data.heightHint = LIST_HEIGHT_HINT;
		data.horizontalSpan = span;
		tree.getControl().setLayoutData(data);
		return tree;
	}

	/**
	 * Returns a content provider for <code>IResource</code>s that returns 
	 * only children of the given resource type.
	 */
	protected ITreeContentProvider getResourceProvider(final int resourceType) {
		return new WorkbenchContentProvider() {
			public Object[] getChildren(Object o) {
				if (o instanceof IContainer) {
					IResource[] members = null;
					try {
						members = ((IContainer)o).members();
					} catch (CoreException e) {
						//just return an empty set of children
						return new Object[0];
					}
	
					//filter out the desired resource types
					ArrayList results = new ArrayList();
					for (int i = 0; i < members.length; i++) {
						//And the test bits with the resource types to see if they are what we want
						if ((members[i].getType() & resourceType) > 0) {
							results.add(members[i]);
						}
					}
					return results.toArray();
				} else {
					return super.getChildren(o);
				}
			}
		};
	}
/*	
	protected RepositoryManager getRepositoryManager() {
		return SVNUIPlugin.getPlugin().getRepositoryManager();
	}*/
}
