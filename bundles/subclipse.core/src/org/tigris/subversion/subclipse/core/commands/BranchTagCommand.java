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
package org.tigris.subversion.subclipse.core.commands;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.client.OperationProgressNotifyListener;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class BranchTagCommand implements ISVNCommand {
	private SVNUrl[] sourceUrls;
    private SVNUrl destinationUrl;
    // selected resources
	private IResource[] resources;

	private boolean createOnServer;
    private String message;
    private SVNRevision revision;
    private boolean makeParents;
    private ISVNClientAdapter svnClient;
    
    private SVNWorkspaceRoot root;
    
    private boolean multipleTransactions = true;
    private Map<String, SVNUrl> urlMap = new HashMap<String, SVNUrl>();

	public BranchTagCommand(SVNWorkspaceRoot root, IResource[] resources, SVNUrl[] sourceUrls, SVNUrl destinationUrl, String message, boolean createOnServer, SVNRevision revision) {
        super();
        this.root = root;
        this.resources = resources;
        this.sourceUrls = sourceUrls;
        this.destinationUrl = destinationUrl;
        this.createOnServer = createOnServer;
        this.message = message;
        this.revision = revision;
    }
    
    public BranchTagCommand(ISVNClientAdapter svnClient, IResource[] resources, SVNUrl[] sourceUrls, SVNUrl destinationUrl, String message, boolean createOnServer, SVNRevision revision) {
        super();
        this.svnClient = svnClient;
        this.resources = resources;
        this.sourceUrls = sourceUrls;
        this.destinationUrl = destinationUrl;
        this.createOnServer = createOnServer;
        this.message = message;
        this.revision = revision;        
    }

    public void run(IProgressMonitor monitor) throws SVNException {
    	 boolean clientPassed = svnClient != null;
        try {
            monitor.beginTask(null, 100);       
            if (!clientPassed) {
            	svnClient = root.getRepository().getSVNClient();
            }
            OperationManager.getInstance().beginOperation(svnClient, new OperationProgressNotifyListener(monitor, svnClient));
            if (createOnServer) {
            	boolean copyAsChild = sourceUrls.length > 1;
            	String commonRoot = null;
            	if (copyAsChild) {
            		commonRoot = getCommonRoot();
            	}
            	if (!multipleTransactions || !copyAsChild || destinationUrl.toString().startsWith(commonRoot)) {
            		svnClient.copy(sourceUrls, destinationUrl, message, revision, copyAsChild, makeParents);
            		multipleTransactions = false;
            	} else {
            		for (SVNUrl sourceUrl : sourceUrls) {
            			String fromUrl = sourceUrl.toString();
            			String uncommonPortion = fromUrl.substring(commonRoot.length());
            			String toUrl = destinationUrl.toString() + uncommonPortion;
            			SVNUrl destination = new SVNUrl(toUrl);
            			SVNUrl[] source = { sourceUrl };
            			urlMap.put(fromUrl, destination);
            			svnClient.copy(source, destination, message, revision, copyAsChild, makeParents);
            		}
            	}
            }
            else {
            	File[] files = new File[resources.length];
            	for (int i = 0; i < resources.length; i++) {
            		files[i] = resources[i].getLocation().toFile();
            	}
            	boolean copyAsChild = files.length > 1;
            	String commonRoot = null;
            	if (copyAsChild) {
            		commonRoot = getCommonRoot();
            	}
            	if (!multipleTransactions || !copyAsChild || destinationUrl.toString().startsWith(commonRoot))  
            		try {
            			svnClient.copy(files, destinationUrl, message, copyAsChild, makeParents);
            			multipleTransactions = false;
            		} catch (IllegalArgumentException ex) {
            			// Ignore.  Bug in JavaHL results in this error when parent directories are created, even though copy succeeds.
            		}
            	else {
            		for (int i = 0; i < sourceUrls.length; i++) {
            			String fromUrl = sourceUrls[i].toString();
            			String uncommonPortion = fromUrl.substring(commonRoot.length());
            			String toUrl = destinationUrl.toString() + uncommonPortion;
            			SVNUrl destination = new SVNUrl(toUrl);
            			File[] source = { files[i] };
            			try {
            				urlMap.put(fromUrl, destination);
            				svnClient.copy(source, destination, message, copyAsChild, makeParents);
            			} catch (IllegalArgumentException ex) {
            				// Ignore.  Bug in JavaHL results in this error when parent directories are created, even though copy succeeds.
            			}
            		}            		
            	}
            }
            monitor.worked(100);
        } catch (Exception e) {
            throw SVNException.wrapException(e);
        } finally {
        	if (!clientPassed) {
        		root.getRepository().returnSVNClient(svnClient);
        	}
            OperationManager.getInstance().endOperation();
            monitor.done();
        }                
    }

	public void setMakeParents(boolean makeParents) {
		this.makeParents = makeParents;
	}
	
    public void setMultipleTransactions(boolean multipleTransactions) {
		this.multipleTransactions = multipleTransactions;
	}
    
    // For switch
    public SVNUrl getDestinationUrl(String sourceUrl) {
    	if (!multipleTransactions) {
    		if (sourceUrls.length == 1) {
    			return destinationUrl;
    		} else {
    			String uncommonPortion = sourceUrl.substring(getCommonRoot().length());
    			String toUrl = destinationUrl.toString() + uncommonPortion;
    			try {
					return new SVNUrl(toUrl);
				} catch (MalformedURLException e) {
					return destinationUrl;
				}   			
    		}
    	}
    	else return (SVNUrl)urlMap.get(sourceUrl);
    }
	
	private String getCommonRoot() {
		String commonRoot = null;
		String urlString = sourceUrls[0].toString();
    	tag1:
        	for (int i = 0; i < urlString.length(); i++) {
        		String partialPath = urlString.substring(0, i+1);
        		if (partialPath.endsWith("/")) {
    	    		for (int j = 1; j < sourceUrls.length; j++) {
    	    			if (!sourceUrls[j].toString().startsWith(partialPath)) break tag1;
    	    		}
    	    		commonRoot = partialPath.substring(0, i);
        		}
        	}
		return commonRoot;
	}

}
