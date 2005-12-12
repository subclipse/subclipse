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

public class TagManager {
	private ArrayList tags = new ArrayList();
	
	public TagManager(IResource resource) {
		Alias[] tagArray = getTags(resource);
		for (int i = 0; i < tagArray.length; i++) tags.add(tagArray[i]);
	}
	
	public TagManager(SVNUrl url) {
		Alias[] tagArray = getTags(url);
		for (int i = 0; i < tagArray.length; i++) tags.add(tagArray[i]);
	}
	
	public Alias[] getTags(int revision) {
		ArrayList revisionTags = new ArrayList();
		Iterator iter = tags.iterator();
		while (iter.hasNext()) {
			Alias tag = (Alias)iter.next();
			if (tag.getRevision() >= revision) {
				revisionTags.add(tag);
			}
		}
		Alias[] tagArray = new Alias[revisionTags.size()];
		revisionTags.toArray(tagArray);
		for (int i = 0; i < tagArray.length; i++) tags.remove(tagArray[i]);
		return tagArray;
	}
	
	public Alias[] getTagTags() {
		ArrayList tagTags = new ArrayList();
		Iterator iter = tags.iterator();
		while (iter.hasNext()) {
			Alias tag = (Alias)iter.next();
			if (!tag.isBranch()) {
				tagTags.add(tag);
			}
		}		
		Alias[] tagArray = new Alias[tagTags.size()];
		tagTags.toArray(tagArray);
		return tagArray;
	}
	
	public Alias[] getBranchTags() {
		ArrayList branchTags = new ArrayList();
		Iterator iter = tags.iterator();
		while (iter.hasNext()) {
			Alias tag = (Alias)iter.next();
			if (tag.isBranch()) {
				branchTags.add(tag);
			}
		}		
		Alias[] tagArray = new Alias[branchTags.size()];
		branchTags.toArray(tagArray);
		return tagArray;
	}
	
	public Alias getTag(String revisionNamePathBranch, String tagUrl) {
		boolean branch = false;
		Alias tag = null;
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
		tag = new Alias(revision, name, relativePath, tagUrl);
		tag.setBranch(branch);
		return tag;
	}

	public static String getTagsAsString(Alias[] tags) {
		if (tags == null) return "";
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < tags.length; i++) {
			if (i != 0) stringBuffer.append(", ");
			stringBuffer.append(tags[i].getName());
		}
		return stringBuffer.toString();
	}
	
	public static String transformUrl(IResource resource, Alias tag) {
		String tagUrl = tag.getUrl();
		String a;
        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
        ISVNRepositoryLocation repository = svnResource.getRepository();
		if (svnResource.getUrl().toString().length() <= tagUrl.length()) 
			a = "";
		else
			a = svnResource.getUrl().toString().substring(tagUrl.length());
		String b = repository.getUrl().toString();
		String c;
		if (tag.getRelativePath() == null) c = "";
		else c = tag.getRelativePath();			
		return b + c + a;
	}
	
	public Alias[] getTags(IResource resource) {
		Alias[] tags = getTags(resource, true);		
		Arrays.sort(tags);
		return tags;
	}
	
	private Alias[] getTags(IResource resource, boolean checkParents)  {
		ArrayList tags = new ArrayList();
		ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
		try {
			if (svnResource.isManaged()) {
				ISVNProperty property = null;
				property = svnResource.getSvnProperty("subclipse:tags"); //$NON-NLS-1$
				if (property != null && property.getValue() != null) getTags(tags, property.getValue(), svnResource.getUrl().toString());
				if (checkParents) {
					IResource checkResource = resource;
					while (checkResource.getParent() != null) {
						checkResource = checkResource.getParent();
						Alias[] parentTags = getTags(checkResource, false);
						for (int i = 0; i < parentTags.length; i++) {
							if (tags.contains(parentTags[i])) {
								Alias checkTag = (Alias)tags.get(tags.indexOf(parentTags[i]));
								if (parentTags[i].getRevision() < checkTag.getRevision()) {
									tags.remove(checkTag);
									tags.add(parentTags[i]);
								}
							} else tags.add(parentTags[i]);
						}
					}
				}
			}
		} catch (SVNException e) {
		}
		Alias[] tagArray = new Alias[tags.size()];
		tags.toArray(tagArray);
		return tagArray;
	}
	
	public Alias[] getTags(SVNUrl url) {
		Alias[] tags = getTags(url, true);
		Arrays.sort(tags);
		return tags;
	}
	
	private Alias[] getTags(SVNUrl url, boolean checkParents)  {
		ArrayList tags = new ArrayList();
		try {
			ISVNClientAdapter client = SVNProviderPlugin.getPlugin().createSVNClient();
			ISVNProperty property = null;
			property = client.propertyGet(url, "subclipse:tags");
			if (property != null && property.getValue() != null) {
				getTags(tags, property.getValue(), url.toString());
			} else {
				if (url.getParent() != null && checkParents)
					return getTags(url.getParent(), checkParents);
			}
		} catch (SVNClientException e) {
		} catch (SVNException e) {
		}
		Alias[] tagArray = new Alias[tags.size()];
		tags.toArray(tagArray);
		return tagArray;
	}
	
	private void getTags(ArrayList tags, String propertyValue, String url) {
		StringReader reader = new StringReader(propertyValue);
		BufferedReader bReader = new BufferedReader(reader);
		try {
			String line = bReader.readLine();
			while (line != null) {
				Alias tag = getTag(line, url);
				if (tags.contains(tag)) {
					Alias checkTag = (Alias)tags.get(tags.indexOf(tag));
					if (tag.getRevision() < checkTag.getRevision()) {
						tags.remove(checkTag);
						tags.add(tag);
					}					
				} else tags.add(tag);
				line = bReader.readLine();
			}
			bReader.close();
		} catch (Exception e) {}
	}

}
