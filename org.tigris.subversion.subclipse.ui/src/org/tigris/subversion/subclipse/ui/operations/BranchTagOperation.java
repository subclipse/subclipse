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
package org.tigris.subversion.subclipse.ui.operations;

import java.util.HashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.BranchTagCommand;
import org.tigris.subversion.subclipse.core.commands.GetRemoteResourceCommand;
import org.tigris.subversion.subclipse.core.commands.SwitchToUrlCommand;
import org.tigris.subversion.subclipse.core.history.Alias;
import org.tigris.subversion.subclipse.core.history.AliasManager;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class BranchTagOperation extends RepositoryProviderOperation {
    private SVNUrl[] sourceUrls;
    private SVNUrl destinationUrl;
    private SVNRevision revision;
    private boolean createOnServer;
    private boolean makeParents;
    private String message;
    private Alias newAlias;
	private boolean switchAfterTagBranch;
	private boolean branchCreated = false;
	private boolean multipleTransactions = true;

    public BranchTagOperation(IWorkbenchPart part, IResource[] resources, SVNUrl[] sourceUrls, SVNUrl destinationUrl, boolean createOnServer, SVNRevision revision, String message) {
        super(part, resources);
        this.sourceUrls = sourceUrls;
        this.destinationUrl = destinationUrl;
        this.createOnServer = createOnServer;
        this.revision = revision;
        this.message = message;
    }
    
    protected String getTaskName() {
        return Policy.bind("BranchTagOperation.taskName"); //$NON-NLS-1$;
    }

    protected String getTaskName(SVNTeamProvider provider) {
        return Policy.bind("BranchTagOperation.0", provider.getProject().getName()); //$NON-NLS-1$  
    }

    protected void execute(SVNTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws SVNException, InterruptedException {
    	if (branchCreated) return;
        branchCreated = true;
    	monitor.beginTask(null, 100);
		try {			
	    	BranchTagCommand command = new BranchTagCommand(provider.getSVNWorkspaceRoot(),getResources(), sourceUrls, destinationUrl, message, createOnServer, revision);
	        command.setMakeParents(makeParents);
	        command.setMultipleTransactions(multipleTransactions);
	    	command.run(Policy.subMonitorFor(monitor,1000));
	        if (newAlias != null) updateBranchTagProperty(resources[0]);
	        if(switchAfterTagBranch) {
	        	for (int i = 0; i < sourceUrls.length; i++) {
//		        	String lastPathSegment = sourceUrls[i].getLastPathSegment();
//		        	SVNUrl switchDestinationUrl = destinationUrl.appendPath(lastPathSegment);
	        		SVNUrl switchDestinationUrl = command.getDestinationUrl(sourceUrls[i].toString());
		        	
		        	// the copy command's destination URL can either be a path to an existing directory
		        	// or a path to a new directory. In the former case the last path segment of the
		        	// source path is automatically created at the destination
		        	GetRemoteResourceCommand getRemoteResourceCommand = new GetRemoteResourceCommand(provider.getSVNWorkspaceRoot().getRepository(), switchDestinationUrl, SVNRevision.HEAD);
		        	try {
		        		getRemoteResourceCommand.run(null);
		        	} catch(SVNException e) {
		        		if(e.getStatus().getCode() == TeamException.UNABLE) {
		        			switchDestinationUrl = destinationUrl;
		        		} else {
		        			throw e;
		        		}
		        	}
		        	resources = getResources();
			        SwitchToUrlCommand switchToUrlCommand = new SwitchToUrlCommand(provider.getSVNWorkspaceRoot(), resources[i], switchDestinationUrl, SVNRevision.HEAD);
			        switchToUrlCommand.run(Policy.subMonitorFor(monitor,100));
	        	}
	        }
		} catch (SVNException e) {
		    collectStatus(e.getStatus());
		} finally {
			monitor.done();
		}         
    }
    
	protected ISchedulingRule getSchedulingRule(SVNTeamProvider provider) {
		IResource[] resources = getResources();
		if (resources == null) return super.getSchedulingRule(provider);
		IResourceRuleFactory ruleFactory = provider.getRuleFactory();
		HashSet rules = new HashSet();
		for (int i = 0; i < resources.length; i++) {
			rules.add(ruleFactory.modifyRule(resources[i].getProject()));
		}
		return MultiRule.combine((ISchedulingRule[]) rules.toArray(new ISchedulingRule[rules.size()]));
	}
    
    private void updateBranchTagProperty(IResource resource) {
		AliasManager aliasManager = new AliasManager(resource, false);
		Alias[] branchAliases = aliasManager.getBranches();
		Alias[] tagAliases = aliasManager.getTags();
		StringBuffer propertyValue = new StringBuffer();
		for (int i = 0; i < branchAliases.length; i++) {
			if (branchAliases[i].getRevision() > 0) {
				if (propertyValue.length() > 0) propertyValue.append("\n"); //$NON-NLS-1$
				Alias branch = branchAliases[i];
				propertyValue.append(branch.getRevision() + "," + branch.getName()); //$NON-NLS-1$
				if (branch.getRelativePath() != null) propertyValue.append("," + branch.getRelativePath()); //$NON-NLS-1$
				if (branch.isBranch()) propertyValue.append(",branch"); //$NON-NLS-1$
				else propertyValue.append(",tag"); //$NON-NLS-1$			
			}
		}
		for (int i = 0; i < tagAliases.length; i++) {
			if (tagAliases[i].getRevision() > 0) {
				if (propertyValue.length() > 0) propertyValue.append("\n"); //$NON-NLS-1$
				Alias tag = tagAliases[i];
				propertyValue.append(tag.getRevision() + "," + tag.getName()); //$NON-NLS-1$
				if (tag.getRelativePath() != null) propertyValue.append("," + tag.getRelativePath()); //$NON-NLS-1$
				if (tag.isBranch()) propertyValue.append(",branch"); //$NON-NLS-1$
				else propertyValue.append(",tag"); //$NON-NLS-1$
			}
		}
		if (propertyValue.length() > 0) propertyValue.append("\n"); //$NON-NLS-1$
		propertyValue.append(newAlias.getRevision() + "," + newAlias.getName() + "," + newAlias.getRelativePath());
		if (newAlias.isBranch()) propertyValue.append(",branch"); //$NON-NLS-1$
		else propertyValue.append(",tag"); //$NON-NLS-1$	
		ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);		
		try {
			svnResource.setSvnProperty("subclipse:tags", propertyValue.toString(), false); //$NON-NLS-1$	
		} catch (SVNException e) {}    	
    }

	public void setNewAlias(Alias newAlias) {
		this.newAlias = newAlias;
	}
	
	public void switchAfterTagBranchOperation(boolean switchAfterTagBranchOperation) {
		this.switchAfterTagBranch = switchAfterTagBranchOperation;
	}

	public void setMakeParents(boolean makeParents) {
		this.makeParents = makeParents;
	}
	
	public void setMultipleTransactions(boolean multipleTransactions) {
		this.multipleTransactions = multipleTransactions;
	}

}
