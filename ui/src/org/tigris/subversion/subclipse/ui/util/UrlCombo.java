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
package org.tigris.subversion.subclipse.ui.util;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class UrlCombo extends Composite {
    private Combo combo;
    private IDialogSettings settings;
    private String project;
    
    private static final int URL_WIDTH_HINT = 450;

    public UrlCombo(Composite parent, String project) {
        super(parent, SWT.NONE);
        this.project = project;
        createCombo();
    }

    private void createCombo() {
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);
        combo = new Combo(this, SWT.BORDER);
        GridData data = new GridData();
        data.widthHint = URL_WIDTH_HINT;
        combo.setLayoutData(data);
        settings = SVNUIPlugin.getPlugin().getDialogSettings();
        for (int i = 0; i < 5; i++) {
            String url = settings.get("UrlCombo."  + project  + "." + i); //$NON-NLS-1$ //$NON-NLS-2$
            if (url == null) break;
            combo.add(url);
        }
    }

    public Combo getCombo() {
        return combo;
    }
    
    public String getText() {
        return combo.getText().trim();
    }
    
    public void setText(String text) {
        combo.setText(text);
    }
    
    public void saveUrl() {
        ArrayList urls = new ArrayList();
        urls.add(getText());
        for (int i = 0; i < 5; i++) {
            String url = settings.get("UrlCombo." + project + "." + i); //$NON-NLS-1$ //$NON-NLS-2$
            if (url == null) break;
            if (!urls.contains(url)) urls.add(url);
        }
        int i = 0;
        Iterator iter = urls.iterator();
        while (iter.hasNext()) {
            String url = (String)iter.next();
            settings.put("UrlCombo." + project + "." + i++, url); //$NON-NLS-1$ //$NON-NLS-2$
            if (i == 5) break;
        }
    }
}
