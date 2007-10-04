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
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.ui.operations.RevertOperation;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardRevertPage;

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
				revertPage = new SvnWizardRevertPage(resources, url, statusMap);
				SvnWizard wizard = new SvnWizard(revertPage);
				SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
				revert = (dialog.open() == SvnWizardDialog.OK);
				if (revert) resourcesToRevert = revertPage.getSelectedResources();
			}
		});
		if (revert) {
			RevertOperation revertOperation = null;
			if (revertPage != null && !revertPage.isResourceRemoved()) {
				revertOperation = new RevertOperation(getPart(), selectedResources);
				revertOperation.setRecurse(true);
			} else {
				revertOperation = new RevertOperation(getPart(), resourcesToRevert);
			}
			revertOperation.run();
		}
	}

	public void setSelectedResources(IResource[] selectedResources) {
		this.selectedResources = selectedResources;
	}

}
