package org.tigris.subversion.subclipse.ui.conflicts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.OpenFileAction;
import org.tigris.subversion.subclipse.core.resources.SVNTreeConflict;

public class OpenFileInSystemEditorAction extends OpenFileAction {
	private ISelectionProvider selectionProvider;

	public OpenFileInSystemEditorAction(IWorkbenchPage page, ISelectionProvider selectionProvider) {
		super(page);
		this.selectionProvider = selectionProvider;
	}
	
	protected List getSelectedResources() {
		ArrayList openableFiles = new ArrayList();
		IStructuredSelection selection = (IStructuredSelection)selectionProvider.getSelection();
		Iterator iter = selection.iterator();
		while (iter.hasNext()) {
			Object element = iter.next();
			if (element instanceof SVNTreeConflict) {
				SVNTreeConflict treeConflict = (SVNTreeConflict)element;
				IResource resource = treeConflict.getResource();
				if (resource instanceof IFile && resource.exists()) {
					openableFiles.add(resource);
				}
			}
		}
		return openableFiles;
	}

	protected List getSelectedNonResources() {		
		return Collections.EMPTY_LIST;
	}	

}
