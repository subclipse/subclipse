/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.tigris.subversion.subclipse.ui.internal;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;



/**
 * 
 */
public class SWTUtils {
	
	public static final int MARGINS_DEFAULT= -1;
	public static final int MARGINS_NONE= 0;
	public static final int MARGINS_DIALOG= 1;

	public static PreferenceLinkArea createPreferenceLink(IWorkbenchPreferenceContainer container, Composite parent, String pageId, String text) {
        final PreferenceLinkArea area = new PreferenceLinkArea(parent, SWT.NONE, pageId, text, container, null);
        return area;
	}
	
    public static GridData createGridData(int width, int height, boolean hFill, boolean vFill) {
        return createGridData(width, height, hFill ? SWT.FILL : SWT.BEGINNING, vFill ? SWT.FILL : SWT.CENTER, hFill, vFill);
    }
    
    public static GridData createGridData(int width, int height, int hAlign, int vAlign, boolean hGrab, boolean vGrab) {
        final GridData gd= new GridData(hAlign, vAlign, hGrab, vGrab);
        gd.widthHint= width;
        gd.heightHint= height;
        return gd;
    }

    public static GridData createHFillGridData() {
        return createHFillGridData(1);
    }
    
    public static GridData createHFillGridData(int span) {
        final GridData gd= createGridData(0, SWT.DEFAULT, SWT.FILL, SWT.CENTER, true, false);
        gd.horizontalSpan= span;
        return gd;
    }

    public static Composite createHFillComposite(Composite parent, int margins) {
        return createHFillComposite(parent, margins, 1);
    }
    
    public static Composite createHFillComposite(Composite parent, int margins, int columns) {
        final Composite composite= new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());
        composite.setLayoutData(createHFillGridData());
        composite.setLayout(createGridLayout(columns, new PixelConverter(parent), margins));
        return composite;
    }
    
    public static Composite createHVFillComposite(Composite parent, int margins) {
        return createHVFillComposite(parent, margins, 1);
    }
    
    public static Composite createHVFillComposite(Composite parent, int margins, int columns) {
        final Composite composite= new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());
        composite.setLayoutData(createHVFillGridData());
        composite.setLayout(createGridLayout(columns, new PixelConverter(parent), margins));
        return composite;
    }

    
    /**
     * Groups
     */
    
    public static Group createHFillGroup(Composite parent, String text, int margins) {
        return createHFillGroup(parent, text, margins, 1);
    }
    
    public static Group createHFillGroup(Composite parent, String text, int margins, int rows) {
        final Group group= new Group(parent, SWT.NONE);
        group.setFont(parent.getFont());
        group.setLayoutData(createHFillGridData());
        if (text != null)
            group.setText(text);
        group.setLayout(createGridLayout(rows, new PixelConverter(parent), margins));
        return group;
    }
    
    public static Group createHVFillGroup(Composite parent, String text, int margins) {
        return createHVFillGroup(parent, text, margins, 1);
    }
    
    public static Group createHVFillGroup(Composite parent, String text, int margins, int rows) {
        final Group group= new Group(parent, SWT.NONE);
        group.setFont(parent.getFont());
        group.setLayoutData(createHVFillGridData());
        if (text != null)
            group.setText(text);
        group.setLayout(createGridLayout(rows, new PixelConverter(parent), margins));
        return group;
    }
    

    /**
     * Grid data
     */
    
    public static GridData createHVFillGridData() {
        return createHVFillGridData(1);
    }
    
    public static GridData createHVFillGridData(int span) {
        final GridData gd= createGridData(0, 0, true, true);
        gd.horizontalSpan= span;
        return gd;
    }

    
    /**
	 * Create a grid layout with the specified number of columns and the
	 * standard spacings.
	 * 
	 * @param numColumns
	 *                the number of columns
	 * @param converter
	 *                the pixel converter
	 * @param margins
	 *                One of <code>MARGINS_DEFAULT</code>,
	 *                <code>MARGINS_NONE</code> or <code>MARGINS_DIALOG</code>.
	 * @return the grid layout
	 */
    public static GridLayout createGridLayout(int numColumns, PixelConverter converter, int margins) {
    	Assert.isTrue(margins == MARGINS_DEFAULT || margins == MARGINS_NONE || margins == MARGINS_DIALOG);
    	
        final GridLayout layout= new GridLayout(numColumns, false);
        layout.horizontalSpacing= converter.convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing= converter.convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        
        switch (margins) {
        case MARGINS_NONE:
            layout.marginLeft= layout.marginRight= 0;
            layout.marginTop= layout.marginBottom= 0;
            break;
        case MARGINS_DIALOG:
            layout.marginLeft= layout.marginRight= converter.convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
            layout.marginTop= layout.marginBottom= converter.convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
            break;
        case MARGINS_DEFAULT:
            layout.marginLeft= layout.marginRight= layout.marginWidth;
            layout.marginTop= layout.marginBottom= layout.marginHeight;
        }
        layout.marginWidth= layout.marginHeight= 0;
        return layout;
    }
    
    
    public static Label createLabel(Composite parent, String message) {
        return createLabel(parent, message, 1);
    }

    public static Label createLabel(Composite parent, String message, int span) {
        final Label label= new Label(parent, SWT.WRAP);
        if (message != null)
        	label.setText(message);
        label.setLayoutData(createHFillGridData(span));
        return label;
    }
    
    public static Button createCheckBox(Composite parent, String message) {
        return createCheckBox(parent, message, 1);
    }

    public static Button createCheckBox(Composite parent, String message, int span) {
        final Button button= new Button(parent, SWT.CHECK);
        button.setText(message);
        button.setLayoutData(createHFillGridData(span));
        return button;
    }
    
    public static Button createRadioButton(Composite parent, String message) {
        return createRadioButton(parent, message, 1);
    }

    public static Button createRadioButton(Composite parent, String message, int span) {
        final Button button= new Button(parent, SWT.RADIO);
        button.setText(message);
        button.setLayoutData(createHFillGridData(span));
        return button;
    }

    
    public static Text createText(Composite parent) {
        return createText(parent, 1);
    }

    public static Text createText(Composite parent, int span) {
        final Text text= new Text(parent, SWT.SINGLE | SWT.BORDER);
        text.setLayoutData(createHFillGridData(span));
        return text;
    }
    

    public static Control createPlaceholder(Composite parent, int heightInChars, int span) {
        Assert.isTrue(heightInChars > 0);
        final Control placeHolder= new Composite(parent, SWT.NONE);
        final GridData gd= new GridData(SWT.BEGINNING, SWT.TOP, false, false);
        gd.heightHint= new PixelConverter(parent).convertHeightInCharsToPixels(heightInChars);
        gd.horizontalSpan= span;
        placeHolder.setLayoutData(gd);
        return placeHolder;
    }

    
    public static Control createPlaceholder(Composite parent, int heightInChars) {
        return createPlaceholder(parent, heightInChars, 1);
    }
    
    public static PixelConverter createDialogPixelConverter(Control control) {
    	Dialog.applyDialogFont(control);
    	return new PixelConverter(control);
    }
    
	public static int calculateControlSize(PixelConverter converter, Control [] controls) {
		return calculateControlSize(converter, controls, 0, controls.length - 1);
	}

	public static int calculateControlSize(PixelConverter converter, Control [] controls, int start, int end) {
		int minimum= converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		for (int i = start; i <= end; i++) {
			final int length= controls[i].computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
			if (minimum < length)
				minimum= length;
		}
		return minimum;
	}
	
	public static void equalizeControls(PixelConverter converter, Control [] controls) {
		equalizeControls(converter, controls, 0, controls.length - 1);
	}

	public static void equalizeControls(PixelConverter converter, Control [] controls, int start, int end) {
		final int size= calculateControlSize(converter, controls, start, end);
		for (int i = start; i <= end; i++) {
			final Control button= controls[i];
			if (button.getLayoutData() instanceof GridData) {
				((GridData)button.getLayoutData()).widthHint= size;
			}
		}
	}

	public static int getWidthInCharsForLongest(PixelConverter converter, String [] strings) {
		int minimum= 0;
		for (int i = 0; i < strings.length; i++) {
			final int length= converter.convertWidthInCharsToPixels(strings[i].length());
			if (minimum < length)
				minimum= length;
		}
		return minimum;
	}
}
