package org.tigris.subversion.subclipse.ui.repository;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;

public class RepositoryFilters {
	public static final ViewerFilter FOLDERS_ONLY = new ViewerFilter() {
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            return !(element instanceof ISVNRemoteFile);
        }
    };
}
