/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.history;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNProperty;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class AliasManager {
	private ArrayList aliases = new ArrayList();
	
	public AliasManager(IResource resource) {
		Alias[] aliasArray = getAliases(resource);
		Arrays.sort(aliasArray);
		for (int i = 0; i < aliasArray.length; i++) aliases.add(aliasArray[i]);
	}
	
	public AliasManager(IResource resource, boolean checkParents) {
		Alias[] aliasArray = getAliases(resource, checkParents);
		Arrays.sort(aliasArray);
		for (int i = 0; i < aliasArray.length; i++) aliases.add(aliasArray[i]);
	}
	
	public AliasManager(SVNUrl url) {
		Alias[] aliasArray = getAliases(url);
		Arrays.sort(aliasArray);
		for (int i = 0; i < aliasArray.length; i++) aliases.add(aliasArray[i]);
	}
	
	public Alias[] getTags(int revision) {
		ArrayList revisionAliases = new ArrayList();
		Iterator iter = aliases.iterator();
		while (iter.hasNext()) {
			Alias alias = (Alias)iter.next();
			if (alias.getRevision() >= revision && !alias.isBranch()) {
				revisionAliases.add(alias);
			}
		}
		Alias[] aliasArray = new Alias[revisionAliases.size()];
		revisionAliases.toArray(aliasArray);
		for (int i = 0; i < aliasArray.length; i++) aliases.remove(aliasArray[i]);
		return aliasArray;
	}
	
	public Alias[] getTags() {
		ArrayList tags = new ArrayList();
		Iterator iter = aliases.iterator();
		while (iter.hasNext()) {
			Alias tag = (Alias)iter.next();
			if (!tag.isBranch()) {
				tags.add(tag);
			}
		}		
		Alias[] tagArray = new Alias[tags.size()];
		tags.toArray(tagArray);
		return tagArray;
	}
	
	public Alias[] getBranches() {
		ArrayList branches = new ArrayList();
		Iterator iter = aliases.iterator();
		while (iter.hasNext()) {
			Alias branch = (Alias)iter.next();
			if (branch.isBranch()) {
				branches.add(branch);
			}
		}		
		Alias[] branchArray = new Alias[branches.size()];
		branches.toArray(branchArray);
		return branchArray;
	}
	
	public Alias getAlias(String revisionNamePathBranch, String url) {
		boolean branch = false;
		Alias alias = null;
		int index = revisionNamePathBranch.indexOf(",");
		if (index == -1) return null;
		String rev = revisionNamePathBranch.substring(0, index);
		int revision;
		try {
			int revNo = Integer.parseInt(rev);
			revision = revNo;			
		} catch (Exception e) { return null; }
		revisionNamePathBranch = revisionNamePathBranch.substring(index + 1);
		index = revisionNamePathBranch.indexOf(",");
		String name;
		String relativePath = null;
		if (index == -1) name = revisionNamePathBranch;
		else {
			name = revisionNamePathBranch.substring(0, index);
			if (revisionNamePathBranch.length() > index + 1) {
				revisionNamePathBranch = revisionNamePathBranch.substring(index + 1);
				index = revisionNamePathBranch.indexOf(",");
				if (index == -1)
					relativePath = revisionNamePathBranch;
				else {
					relativePath = revisionNamePathBranch.substring(0, index);
					if (revisionNamePathBranch.length() > index + 1)
						branch = revisionNamePathBranch.substring(index + 1).equalsIgnoreCase("branch"); //$NON-NLS-1$
				}
			}
		}
		alias = new Alias(revision, name, relativePath, url);
		alias.setBranch(branch);
		return alias;
	}

	public static String getAliasesAsString(Alias[] aliases) {
		if (aliases == null) return "";
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < aliases.length; i++) {
			if (i != 0) stringBuffer.append(", ");
			stringBuffer.append(aliases[i].getName());
		}
		return stringBuffer.toString();
	}
	
	public static String transformUrl(IResource resource, Alias alias) {
		String aliasUrl = alias.getUrl();
		String a;
        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
        ISVNRepositoryLocation repository = svnResource.getRepository();
		if (svnResource.getUrl().toString().length() <= aliasUrl.length()) 
			a = "";
		else
			a = svnResource.getUrl().toString().substring(aliasUrl.length());
		String b = repository.getUrl().toString();
		String c;
		if (alias.getRelativePath() == null) c = "";
		else c = alias.getRelativePath();			
		return b + c + a;
	}
	
	public Alias[] getAliases(IResource resource) {
		Alias[] aliases = getAliases(resource, true);		
		Arrays.sort(aliases);
		return aliases;
	}
	
	private Alias[] getAliases(IResource resource, boolean checkParents)  {
		ArrayList aliases = new ArrayList();
		if (resource != null) {
			ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			try {
				if (svnResource.isManaged()) {
					ISVNProperty property = null;
					property = svnResource.getSvnProperty("subclipse:tags"); //$NON-NLS-1$
					if (property != null && property.getValue() != null) getAliases(aliases, property.getValue(), svnResource.getUrl().toString());
					if (checkParents) {
						IResource checkResource = resource;
						while (checkResource.getParent() != null) {
							checkResource = checkResource.getParent();
							Alias[] parentAliases = getAliases(checkResource, false);
							for (int i = 0; i < parentAliases.length; i++) {
								if (aliases.contains(parentAliases[i])) {
									Alias checkAlias = (Alias)aliases.get(aliases.indexOf(parentAliases[i]));
									if (parentAliases[i].getRevision() < checkAlias.getRevision()) {
										aliases.remove(checkAlias);
										aliases.add(parentAliases[i]);
									}
								} else aliases.add(parentAliases[i]);
							}
						}
					}
				}
			} catch (SVNException e) {
			}
		}
		Alias[] aliasArray = new Alias[aliases.size()];
		aliases.toArray(aliasArray);
		return aliasArray;
	}
	
	public Alias[] getAliases(SVNUrl url) {
		Alias[] aliases = getAliases(url, true);
		Arrays.sort(aliases);
		return aliases;
	}
	
	private Alias[] getAliases(SVNUrl url, boolean checkParents)  {
		ArrayList aliases = new ArrayList();
		try {
			ISVNClientAdapter client = SVNProviderPlugin.getPlugin().createSVNClient();
			ISVNProperty property = null;
			property = client.propertyGet(url, "subclipse:tags");
			if (property != null && property.getValue() != null) {
				getAliases(aliases, property.getValue(), url.toString());
			} else {
				if (url.getParent() != null && checkParents)
					return getAliases(url.getParent(), checkParents);
			}
		} catch (SVNClientException e) {
		} catch (SVNException e) {
		}
		Alias[] aliasArray = new Alias[aliases.size()];
		aliases.toArray(aliasArray);
		return aliasArray;
	}
	
	private void getAliases(ArrayList aliases, String propertyValue, String url) {
		StringReader reader = new StringReader(propertyValue);
		BufferedReader bReader = new BufferedReader(reader);
		try {
			String line = bReader.readLine();
			while (line != null) {
				Alias alias = getAlias(line, url);
				if (aliases.contains(alias)) {
					Alias checkAlias = (Alias)aliases.get(aliases.indexOf(alias));
					if (alias.getRevision() < checkAlias.getRevision()) {
						aliases.remove(checkAlias);
						aliases.add(alias);
					}					
				} else aliases.add(alias);
				line = bReader.readLine();
			}
			bReader.close();
		} catch (Exception e) {}
	}

}
