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
package org.tigris.subversion.subclipse.ui.subscriber;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.operations.RevertOperation;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardRevertPage;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;

public class RevertSynchronizeOperation extends SVNSynchronizeOperation {
	private String url;
	private IResource[] resources;
	private IResource[] resourcesToRevert;
	private IResource[] selectedResources;
	private boolean revert;
	private boolean prompted;
	private HashMap statusMap;
	private SvnWizardRevertPage revertPage;

	public RevertSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements, String url, IResource[] resources, HashMap statusMap) {
		super(configuration, elements);
		this.url = url;
		this.resources = resources;
		this.statusMap = statusMap;
	}

	protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
		return true;
	}

	protected void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		if (prompted) return;
		prompted = true;
		getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				if (resources == null || resources.length == 0) {
					revert = false;
					return;
				}
				revertPage = new SvnWizardRevertPage(resources, url, statusMap, true);
				revertPage.setResourceRemoved(SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_IGNORE_HIDDEN_CHANGES));
				SvnWizard wizard = new SvnWizard(revertPage);
				SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
				revert = (dialog.open() == SvnWizardDialog.OK);
				if (revert) resourcesToRevert = revertPage.getSelectedResources();
			}
		});
		if (revert) {
			
			boolean includesExternals = false;
			if (revertPage != null && !revertPage.isResourceRemoved()) {
				for (IResource resource : resourcesToRevert) {
					if (isExternal(resource)) {
						includesExternals = true;
						break;
					}
				}
			}
			
			RevertOperation revertOperation = null;
			if (revertPage != null && !revertPage.isResourceRemoved() && !includesExternals) {
				revertOperation = new RevertOperation(getPart(), selectedResources);
				revertOperation.setRecurse(true);
				revertOperation.setResourcesToRevert(resourcesToRevert);
			} else {
				revertOperation = new RevertOperation(getPart(), resourcesToRevert);
			}
			revertOperation.run();
		}
	}

	public void setSelectedResources(IResource[] selectedResources) {
		this.selectedResources = selectedResources;
	}
	
	private boolean isExternal(IResource resource) {
		IResource parent = resource;
		while (parent != null) {
			 ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(parent);
			 try {
				 LocalResourceStatus status = svnResource.getStatus();
	             if (status.isFileExternal() || SVNStatusKind.EXTERNAL.equals(status.getTextStatus())) {
	            	 return true;
	             }
			} catch (SVNException e) {
				return false;
			}
			 parent = parent.getParent();
		}
		return false;
	}

}
