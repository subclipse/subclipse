package org.tigris.subversion.subclipse.core.history;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
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
		Tag[] tagArray = getTags(resource);
		for (int i = 0; i < tagArray.length; i++) tags.add(tagArray[i]);
	}
	
	public TagManager(SVNUrl url) {
		Tag[] tagArray = getTags(url);
		for (int i = 0; i < tagArray.length; i++) tags.add(tagArray[i]);
	}
	
	public Tag[] getTags(int revision) {
		ArrayList revisionTags = new ArrayList();
		Iterator iter = tags.iterator();
		while (iter.hasNext()) {
			Tag tag = (Tag)iter.next();
			if (tag.getRevision() >= revision) {
				revisionTags.add(tag);
			}
		}
		Tag[] tagArray = new Tag[revisionTags.size()];
		revisionTags.toArray(tagArray);
		for (int i = 0; i < tagArray.length; i++) tags.remove(tagArray[i]);
		return tagArray;
	}
	
	public Tag getTag(String revisionNamePathBranch, String tagUrl) {
		boolean branch = false;
		Tag tag = null;
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
		tag = new Tag(revision, name, relativePath, tagUrl);
		tag.setBranch(branch);
		return tag;
	}

	public static String getTagsAsString(Tag[] tags) {
		if (tags == null) return "";
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < tags.length; i++) {
			if (i != 0) stringBuffer.append(", ");
			stringBuffer.append(tags[i].getName());
		}
		return stringBuffer.toString();
	}
	
	public Tag[] getTags(IResource resource) {
		Tag[] tags = getTags(resource, true);
		Arrays.sort(tags);
		return tags;
	}
	
	private Tag[] getTags(IResource resource, boolean checkParents)  {
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
						Tag[] parentTags = getTags(checkResource, false);
						for (int i = 0; i < parentTags.length; i++) {
							if (tags.contains(parentTags[i])) {
								Tag checkTag = (Tag)tags.get(tags.indexOf(parentTags[i]));
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
		Tag[] tagArray = new Tag[tags.size()];
		tags.toArray(tagArray);
		return tagArray;
	}
	
	public Tag[] getTags(SVNUrl url) {
		Tag[] tags = getTags(url, true);
		Arrays.sort(tags);
		return tags;
	}
	
	private Tag[] getTags(SVNUrl url, boolean checkParents)  {
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
		Tag[] tagArray = new Tag[tags.size()];
		tags.toArray(tagArray);
		return tagArray;
	}
	
	private void getTags(ArrayList tags, String propertyValue, String url) {
		StringReader reader = new StringReader(propertyValue);
		BufferedReader bReader = new BufferedReader(reader);
		try {
			String line = bReader.readLine();
			while (line != null) {
				Tag tag = getTag(line, url);
				if (tags.contains(tag)) {
					Tag checkTag = (Tag)tags.get(tags.indexOf(tag));
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
