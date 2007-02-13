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
package org.tigris.subversion.subclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.util.ListContentProvider;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * Dialog that ask the user to give the root repository url of the given url 
 */
public class ChooseRootUrlDialog extends ListDialog {
	private SVNUrl url;
    
	/**
     * 
	 * @param parent
     * @param url : the url from which we want to get the root url 
	 */
	public ChooseRootUrlDialog(Shell parent, SVNUrl url) {
		super(parent);
        this.url = url;
        
        List list = new ArrayList();
        
        // we want the user can select "no root url", ie a blank url
        list.add(""); // we cannot add null, we would have a NullPointerException //$NON-NLS-1$
        SVNUrl possibleRoot = this.url;
        while (possibleRoot != null) {
            list.add(possibleRoot);
            possibleRoot = possibleRoot.getParent();
        }        
        
        setTitle(Policy.bind("ChooseRootUrlDialog.rootUrlDialogTitle")); //$NON-NLS-1$
        setAddCancelButton(true);
        setLabelProvider(new LabelProvider());
        setMessage(Policy.bind("ChooseRootUrlDialog.chooseRootUrl")); //$NON-NLS-1$
        setContentProvider(new ListContentProvider());
        setInput(list);
	}
    
    /**
     * get the chosen root url 
     * @return
     */
    public SVNUrl getRootUrl() {
        Object result = getResult()[0];
        if ("".equals(result)) { //$NON-NLS-1$
        	return null;
        } else {
        	return (SVNUrl)result;
        }
    }
    
    
}
