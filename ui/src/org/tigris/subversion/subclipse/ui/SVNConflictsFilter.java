/*******************************************************************************
 * Copyright (c) 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;

/**
 * Filter which only shows resources that have Subversion conflicts. Works by checking 
 * if the the resource has an associated SVN conflic warning marker tied to it.
 */
public class SVNConflictsFilter extends ViewerFilter {
    
    public boolean select(Viewer viewer, Object parentElement, Object element) {        
        try {
            // short circuit jars/zips
            if (element instanceof IPackageFragmentRoot && ((IPackageFragmentRoot)element).getKind()==IPackageFragmentRoot.K_BINARY) {
                return false;
            }
            
            IResource resource = null;
            if (element instanceof ICompilationUnit) {
                resource = ((ICompilationUnit)element).getResource();
            }
            else if (element instanceof IFolder) {
                // check if this element has children which are conflicted
                IResource[] resources = ((IFolder)element).members();
                for (int i = 0; i < resources.length; i++ ) {
                    if (select(viewer,element,resources[i])) {
                        return true;
                    }
                }
                // if the children aren't coflicted, the folder itself may be conflicted
                resource = (IResource)element;
            }
            else if (element instanceof IResource) {
                resource = (IResource)element;
            }
            else if (element instanceof IParent) {
                // check the java children first
                IJavaElement[] children = ((IParent)element).getChildren();
                for (int i = 0; i < children.length; i++ ) {
                    if (select(viewer,element,children[i])) {
                        return true;
                    }
                }
                // then check non-java resources
                if (element instanceof IPackageFragment || element instanceof IJavaProject) {
                    Object[] resources = element instanceof IPackageFragment ? ((IPackageFragment)element).getNonJavaResources() :
                                                                               ((IJavaProject)element).getNonJavaResources();
                    for (int i = 0; i < resources.length; i++ ) {
                        if (select(viewer,element,resources[i])) {
                            return true;
                        }                    
                    }
                }
                // If it's a folder, it may have props set on it which may be in conflicted state
                if (element instanceof IJavaElement) {
                    resource = ((IJavaElement)element).getResource();
                } else {
                    return false;
                }
            }
            // filter out ClassPathContainer. Is this too broad a filter?
            else if (element instanceof IWorkbenchAdapter) {
                return false;
            }
            
            if (resource != null ) {
                LocalResourceStatus status = SVNWorkspaceRoot.getSVNResourceFor(resource).getStatus();
                return resource.isAccessible() && (status.isTextConflicted() || status.isPropConflicted());
            }            
        } catch( CoreException ce ) {
            // what can we do here aside from log the exception?
            ce.printStackTrace();
            return true;                        
        }
        return true;
    }
}