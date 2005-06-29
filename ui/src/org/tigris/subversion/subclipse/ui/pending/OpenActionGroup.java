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

package org.tigris.subversion.subclipse.ui.pending;


import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.OpenFileAction;
import org.eclipse.ui.actions.OpenInNewWindowAction;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.views.navigator.ResourceSelectionUtil;
import org.tigris.subversion.subclipse.ui.Policy;

/**
 * This is the action group for the open actions.
 * This class has been taken from org.eclipse.ui.views.navigator and modified
 */
public class OpenActionGroup extends ActionGroup {
    private IViewPart viewPart;
    private OpenFileAction openFileAction;

    /**
     * The id for the Open With submenu.
     */
    public static final String OPEN_WITH_ID = PlatformUI.PLUGIN_ID + ".OpenWithSubMenu"; //$NON-NLS-1$

    public OpenActionGroup(IViewPart site) {
        this.viewPart = site;
        makeActions();
    }

    protected void makeActions() {
        openFileAction = new OpenFileAction(viewPart.getSite().getPage());
    }

    public void fillContextMenu(IMenuManager menu) {
        IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();

        boolean anyResourceSelected =
            !selection.isEmpty()
                && ResourceSelectionUtil.allResourcesAreOfType(
                    selection,
                    IResource.PROJECT | IResource.FOLDER | IResource.FILE);
        boolean onlyFilesSelected =
            !selection.isEmpty() && ResourceSelectionUtil.allResourcesAreOfType(selection, IResource.FILE);

        if (onlyFilesSelected) {
            openFileAction.selectionChanged(selection);
            menu.add(openFileAction);
            fillOpenWithMenu(menu, selection);
        }

        if (anyResourceSelected) {
            addNewWindowAction(menu, selection);
        }
    }

    /**
     * Adds the OpenWith submenu to the context menu.
     * 
     * @param menu the context menu
     * @param selection the current selection
     */
    private void fillOpenWithMenu(IMenuManager menu, IStructuredSelection selection) {

        // Only supported if exactly one file is selected.
        if (selection.size() != 1)
            return;
        Object element = selection.getFirstElement();
        if (!(element instanceof IFile))
            return;

        MenuManager submenu =
            new MenuManager(Policy.bind("OpenActionGroup.openWith"), OPEN_WITH_ID); //$NON-NLS-1$
        submenu.add(new OpenWithMenu(viewPart.getSite().getPage(), (IFile) element));
        menu.add(submenu);
    }

    /**
     * Adds the Open in New Window action to the context menu.
     * 
     * @param menu the context menu
     * @param selection the current selection
     */
    private void addNewWindowAction(IMenuManager menu, IStructuredSelection selection) {

        // Only supported if exactly one container (i.e open project or folder) is selected.
        if (selection.size() != 1)
            return;
        Object element = selection.getFirstElement();
        if (!(element instanceof IContainer))
            return;
        if (element instanceof IProject && !(((IProject)element).isOpen()))
            return;             

        menu.add(new OpenInNewWindowAction(viewPart.getSite().getWorkbenchWindow(), (IContainer) element));
    }

    /**
     * Runs the default action (open file).
     */
    public void runDefaultAction(IStructuredSelection selection) {
        Object element = selection.getFirstElement();
        if (element instanceof IFile) {
            openFileAction.selectionChanged(selection);
            openFileAction.run();
        }
    }
}
