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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * This class provides facilities to allow common widget groupings to be shared
 * by mulitple dialogs or wizards.
 */
public abstract class DialogArea {

	protected static final int LABEL_WIDTH_HINT = 400;
	
	protected Dialog parentDialog;
	protected FontMetrics fontMetrics;
	protected IDialogSettings settings;
	
	protected List listeners;
	
	public DialogArea(Dialog parentDialog, IDialogSettings settings) {
		this.listeners = new ArrayList();
		this.parentDialog = parentDialog;
		this.settings = settings;
	}
	
	/**
	 * Listener for property change events. The only event of interest is for
	 * property SELECTED_WORKING_SET which contains the selected working set or
	 * <code>null</code> if none is selected.
	 * 
	 * @param listener
	 */
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	/**
	 * Remove the provided listener from the receiver.
	 * 
	 * @param listener
	 */
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		listeners.remove(listener);
	}

	protected void firePropertyChangeChange(String property, Object oldValue, Object newValue) {
		PropertyChangeEvent event = new PropertyChangeEvent(this, property, oldValue, newValue);
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			IPropertyChangeListener listener = (IPropertyChangeListener) iter.next();
			listener.propertyChange(event);
		}
	}
	
	/**
	 * Code copied from <code>org.eclipse.jface.dialogs.Dialog</code> to obtain
	 * a FontMetrics.
	 *
	 * @param control a control from which to obtain the current font
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog
	 */
	protected void initializeDialogUnits(Control control) {
		// Compute and store a font metric
		GC gc = new GC(control);
		gc.setFont(control.getFont());
		fontMetrics = gc.getFontMetrics();
		gc.dispose();
	}

	public abstract Control createArea(Composite parent);

	/**
	 * Returns the shell.
	 * @return Shell
	 */
	protected Shell getShell() {
		if (parentDialog == null) return Display.getCurrent().getActiveShell();
		return parentDialog.getShell();
	}
	
	protected Button createCheckbox(Composite parent, String label, int span) {
		Button button = new Button(parent, SWT.CHECK | SWT.LEFT);
		button.setText(label);
		button.setFont(parent.getFont());
		GridData data = new GridData();
		data.horizontalSpan = span;
		button.setLayoutData(data);
		return button;
	}
	
	protected Button createButton(Composite parent, String label, int style) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);
		// we need to explicitly set the font to the parent's font for dialogs
		button.setFont(parent.getFont());
		GridData data = new GridData(style);
		int widthHint = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		button.setLayoutData(data);
		return button;
	}

	protected Button createRadioButton(Composite parent, String label, int span) {
		Button button = new Button(parent, SWT.RADIO);
		button.setText(label);
		GridData data = new GridData();
		data.horizontalSpan = span;
		button.setLayoutData(data);
		return button;
	}
	protected Label createWrappingLabel(Composite parent, String text, int horizontalSpan) {
		Label label = new Label(parent, SWT.LEFT | SWT.WRAP);
		label.setText(text);
		label.setFont(parent.getFont());
		GridData data = new GridData();
		data.horizontalSpan = horizontalSpan;
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.widthHint = LABEL_WIDTH_HINT;
		label.setLayoutData(data);
		return label;
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
		
		composite.setFont(parent.getFont());
		
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
	 * Creates composite control and sets the default layout data.
	 *
	 * @param parent  the parent of the new composite
	 * @param numColumns  the number of columns for the new composite
	 * @return the newly-created coposite
	 */
	protected Composite createGrabbingComposite(Composite parent, int numColumns) {
		Composite composite = new Composite(parent, SWT.NULL);
		
		composite.setFont(parent.getFont());
		
		// GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		composite.setLayout(layout);

		// GridData
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		composite.setLayoutData(data);
		return composite;
	}
}
