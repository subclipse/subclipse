/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.sync.notused;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.ThreeWayRemoteTree;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolderTree;
import org.tigris.subversion.subclipse.core.resources.RemoteResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNDirEntry;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * The resource variant tree
 */
public class SVNRemoteTree extends ThreeWayRemoteTree {

	/**
	 * Create the svn remote resource variant tree
	 * 
	 * @param subscriber
	 *            the file system subscriber
	 */
	public SVNRemoteTree(SVNSubscriber subscriber) {
		super(subscriber);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.variants.AbstractResourceVariantTree#fetchMembers(org.eclipse.team.core.variants.IResourceVariant,
	 *      org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IResourceVariant[] fetchMembers(IResourceVariant remote,
			IProgressMonitor progress) throws TeamException {
		ISVNRemoteResource[] children;
		if (remote != null) {
			children = (ISVNRemoteResource[]) ((RemoteResource) remote)
					.members(progress);
		} else {
			children = new ISVNRemoteResource[0];
		}
		IResourceVariant[] result = new IResourceVariant[children.length];
		for (int i = 0; i < children.length; i++) {
			result[i] = (IResourceVariant) children[i];
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.variants.AbstractResourceVariantTree#fetchVariant(org.eclipse.core.resources.IResource,
	 *      int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IResourceVariant fetchVariant(IResource resource, int depth,
			IProgressMonitor monitor) throws TeamException {
		System.out.println("Populating remote tree:" + resource);
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot workspaceRoot = workspace.getRoot();

		ISVNClientAdapter client = SVNProviderPlugin.getPlugin()
				.createSVNClient();
		ISVNLocalResource rootResource = SVNWorkspaceRoot
				.getSVNResourceFor(resource);
		RemoteFolderTree rootVariant = new RemoteFolderTree(null, rootResource
				.getRepository(), rootResource.getUrl(), null, rootResource
				.getStatus().getLastChangedRevision(), null, null);

		Map folders = new HashMap();
		folders.put(resource, new ArrayList());
		Map resourceVariantMap = new HashMap();
		resourceVariantMap.put(resource, rootVariant);
		try {
            // we get the children recursively
			ISVNDirEntry[] entries = client.getList(resource.getLocation()
					.toFile(), SVNRevision.HEAD, true);
			for (int i = 0; i < entries.length; i++) {
				ISVNDirEntry entry = entries[i];
				IPath entryPath = resource.getLocation()
						.append(entry.getPath());
				System.out.println(entryPath);

				IResource memberResource = null;
				IResourceVariant memberVariant = null;
				
                if (entry.getNodeKind() == SVNNodeKind.DIR) {
					memberResource = workspaceRoot
							.getContainerForLocation(entryPath);
					ISVNLocalResource localResource = SVNWorkspaceRoot
							.getSVNResourceFor(memberResource);
					memberVariant = new RemoteFolderTree(null, localResource
							.getRepository(), localResource.getUrl(), null,
							entry.getLastChangedRevision(), entry
									.getLastChangedDate(), entry
									.getLastCommitAuthor());
					folders.put(memberResource, new ArrayList());
					resourceVariantMap.put(memberResource, memberVariant);
				} else if (entry.getNodeKind() == SVNNodeKind.FILE) {
					memberResource = workspaceRoot
							.getFileForLocation(entryPath);
					ISVNLocalResource localResource = SVNWorkspaceRoot
							.getSVNResourceFor(memberResource);
					memberVariant = new RemoteFile(null, localResource
							.getRepository(), localResource.getUrl(), null,
							entry.getLastChangedRevision(), entry
									.getLastChangedDate(), entry
									.getLastCommitAuthor());
				}

				List children = (List) folders.get(memberResource.getParent());
				if (children == null) {
					children = new ArrayList();
					folders.put(memberResource.getParent(), children);
				}
				children.add(memberVariant);
			}

			for (Iterator i = folders.entrySet().iterator(); i.hasNext();) {
				Map.Entry entry = (Map.Entry) i.next();
				IResource r = (IResource) entry.getKey();
				List children = (List) entry.getValue();
				ISVNRemoteResource[] children2 = (ISVNRemoteResource[]) children
						.toArray(new ISVNRemoteResource[children.size()]);
				RemoteFolderTree tree = (RemoteFolderTree) resourceVariantMap
						.get(r);
				tree.setChildren(children2);
			}
		} catch (SVNClientException e) {
			throw SVNException.wrapException(e);
		}
		return rootVariant;
	}
}