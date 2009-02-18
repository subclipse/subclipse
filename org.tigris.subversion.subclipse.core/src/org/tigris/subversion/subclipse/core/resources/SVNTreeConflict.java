package org.tigris.subversion.subclipse.core.resources;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.tigris.subversion.subclipse.core.util.File2Resource;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;

public class SVNTreeConflict implements ISVNTreeConflict, IAdaptable {
	private ISVNStatus status;
	
	public SVNTreeConflict(ISVNStatus status) {
		super();
		this.status = status;
	}

	public SVNConflictDescriptor getConflictDescriptor() {
		return status.getConflictDescriptor();
	}

	public String getDescription() {
		String reason;
		String action;
		String operation;
		switch (status.getConflictDescriptor().getReason()) {
		case SVNConflictDescriptor.Reason.edited:
			reason = "edit"; //$NON-NLS-1$
			break;
		case SVNConflictDescriptor.Reason.obstructed:
			reason = "obstruction"; //$NON-NLS-1$
			break;
		case SVNConflictDescriptor.Reason.deleted:
			reason = "delete"; //$NON-NLS-1$
			break;		
		case SVNConflictDescriptor.Reason.missing:
			reason = "missing"; //$NON-NLS-1$
			break;	
		case SVNConflictDescriptor.Reason.unversioned:
			reason = "unversioned"; //$NON-NLS-1$
			break;
		case SVNConflictDescriptor.Reason.added:
			reason = "add"; //$NON-NLS-1$
			break;					
		default:
			reason = Integer.toString(status.getConflictDescriptor().getReason());
			break;
		}
		switch (status.getConflictDescriptor().getAction()) {
		case SVNConflictDescriptor.Action.edit:
			action = "edit"; //$NON-NLS-1$
			break;
		case SVNConflictDescriptor.Action.add:
			action = "add"; //$NON-NLS-1$
			break;
		case SVNConflictDescriptor.Action.delete:
			action = "delete"; //$NON-NLS-1$
			break;			
		default:
			action = Integer.toString(status.getConflictDescriptor().getAction());
			break;
		}
		switch (status.getConflictDescriptor().getOperation()) {
		case SVNConflictDescriptor.Operation._none:
			operation = "none"; //$NON-NLS-1$
			break;
		case SVNConflictDescriptor.Operation._update:
			operation = "update"; //$NON-NLS-1$
			break;
		case SVNConflictDescriptor.Operation._switch:
			operation = "switch"; //$NON-NLS-1$
			break;	
		case SVNConflictDescriptor.Operation._merge:
			operation = "merge"; //$NON-NLS-1$
			break;			
		default:
			operation = Integer.toString(status.getConflictDescriptor().getOperation());
			break;
		}				
		return "local " + reason + ", incoming " + action + " upon " + operation; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public ISVNStatus getStatus() {
		return status;
	}

	public IResource getResource() {
		return File2Resource.getResource(status.getFile());
	}

	public Object getAdapter(Class adapterType) {
		if (IFile.class == adapterType) {
			IResource resource = getResource();
			if (resource instanceof IFile) {
				IFile file = (IFile)resource;
				return file;
			}
		}
		if (IResource.class == adapterType) {
			return getResource();
		}
		return null;
	}

}
