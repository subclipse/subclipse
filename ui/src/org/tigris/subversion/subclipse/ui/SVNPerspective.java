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
package org.tigris.subversion.subclipse.ui;


import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.tigris.subversion.subclipse.ui.repository.RepositoriesView;

public class SVNPerspective implements IPerspectiveFactory {

	/* (Non-javadoc)
	 * Method declared on IPerpsectiveFactory
	 */
	public void createInitialLayout(IPageLayout layout) {
		defineActions(layout);
		defineLayout(layout);
	}

	/**
	 * Defines the initial actions for a page.  
	 */

	public void defineActions(IPageLayout layout) {

		// Add "new wizards". They will be present in File/New menu
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.project"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.file"); //$NON-NLS-1$

		// Add "show views". They will be present in "show view" menu
		layout.addShowViewShortcut(IPageLayout.ID_RES_NAV);
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
		layout.addShowViewShortcut(ISVNUIConstants.HISTORY_VIEW_ID);
		layout.addShowViewShortcut(RepositoriesView.VIEW_ID);
		layout.addShowViewShortcut(ISynchronizeView.VIEW_ID);
		
		// Add  "perspective short cut"
		layout.addPerspectiveShortcut("org.eclipse.ui.resourcePerspective"); //$NON-NLS-1$
		layout.addPerspectiveShortcut("org.eclipse.team.ui.TeamSynchronizingPerspective"); //$NON-NLS-1$
	}

	/**
	 * Defines the initial layout for a page.  
	 */
	public void defineLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		IFolderLayout top =
			layout.createFolder("top", IPageLayout.LEFT, 0.40f, editorArea);	//$NON-NLS-1$
		top.addView(RepositoriesView.VIEW_ID);
		layout.addView(ISVNUIConstants.HISTORY_VIEW_ID, IPageLayout.BOTTOM, 0.70f, editorArea);
		layout.setEditorAreaVisible(true);
	}
}
