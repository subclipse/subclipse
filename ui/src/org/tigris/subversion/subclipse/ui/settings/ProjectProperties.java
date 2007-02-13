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
package org.tigris.subversion.subclipse.ui.settings;

import java.util.ArrayList;
import org.eclipse.core.resources.IResource;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.util.LinkList;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

public class ProjectProperties {
    protected String label = "Issue Number:"; //$NON-NLS-1$
    protected String message;
    protected boolean number = false;
    protected String url;
    protected boolean warnIfNoIssue = false;
    protected boolean append = true;
    
    private static final String URL = "://"; //$NON-NLS-1$

    public ProjectProperties() {
        super();
    }

    public boolean isAppend() {
        return append;
    }
    public void setAppend(boolean append) {
        this.append = append;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public boolean isNumber() {
        return number;
    }
    public void setNumber(boolean number) {
        this.number = number;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public boolean isWarnIfNoIssue() {
        return warnIfNoIssue;
    }
    public void setWarnIfNoIssue(boolean warnIfNoIssue) {
        this.warnIfNoIssue = warnIfNoIssue;
    }
    
    public String getResolvedMessage(String issue) {
        if (message == null) return null;
        return message.replaceAll("%BUGID%", issue); //$NON-NLS-1$
    }
    
    public String getResolvedUrl(String issue) {
        if (url == null) return null;
        return url.replaceAll("%BUGID%", issue); //$NON-NLS-1$
    }
    
    // Retrieve hyperlink ranges and url's from commit message.
    public LinkList getLinkList(String commitMessage) {
        ArrayList links = new ArrayList();
        ArrayList urls = new ArrayList();
        String bugID = "%BUGID%"; //$NON-NLS-1$
        if (message != null) {
	        int index = message.indexOf(bugID);
	        if (index != -1) {
	        	String remainder = null;
	        	if (message.length() > index + bugID.length())
	        		remainder = message.substring(index + bugID.length());
	        	else
	        		remainder = "";
		        String tag = message.substring(0, index);
		        index = commitMessage.indexOf(tag);
		        if (index != -1) {
			        index = index + tag.length();
			        int start = index;
			        StringBuffer issue = new StringBuffer();
			        while (index < commitMessage.length()) {
			            if (commitMessage.substring(index, index + 1).equals(",")) { //$NON-NLS-1$
			                int range[] = {start, issue.length()};
			                String url = getResolvedUrl(issue.toString());
			                if ((url != null) && (url.trim().length() > 0)) {
			                    links.add(range);
			                    urls.add(url);
			                }
			                start = index + 1;
			                issue = new StringBuffer();
			            } else {
			                if (commitMessage.substring(index, index + 1).equals("\n") || commitMessage.substring(index, index + 1).equals("\r")) break; //$NON-NLS-1$ //$NON-NLS-2$
			                if (commitMessage.substring(index).trim().equals(remainder.trim())) break;
			                if (commitMessage.substring(index, index + 1).equals(" ")) {
			                    int lineIndex = commitMessage.indexOf("\n", index);
			                    if (lineIndex == -1) lineIndex = commitMessage.indexOf("\r", index);
			                    if (lineIndex != -1) {
			                        if (commitMessage.substring(index, lineIndex - 1).trim().length() == 0)
			                        break;
			                    }
			                }
			                issue.append(commitMessage.substring(index, index + 1));
			            }
			            index++;
			        }  
			        int range[] = {start, issue.length()};
			        String url = getResolvedUrl(issue.toString());
			        if ((url != null) && (url.trim().length() > 0)) { 
			            links.add(range);
			            urls.add(url);
			        }
		        }
	        }
        }
        LinkList urlLinks = getUrls(commitMessage);
        int[][] urlRanges = urlLinks.getLinkRanges();
        String[] urlUrls = urlLinks.getUrls();
        for (int i = 0; i < urlRanges.length; i++) {
            links.add(urlRanges[i]);
            urls.add(urlUrls[i]);
        }
        int[][] linkRanges = new int[links.size()][2];
        links.toArray(linkRanges);
        String[] urlArray = new String[urls.size()];
        urls.toArray(urlArray);
        LinkList linkList = new LinkList(linkRanges, urlArray);
        return linkList;
    }
    
    public static LinkList getUrls(String s) {
        int max = 0;
        int i = -1;
        if (s != null) {
            max = s.length();
            i = s.indexOf(URL);
        }
    	ArrayList linkRanges = new ArrayList();
    	ArrayList links = new ArrayList();
    	while (i != -1) {
    	    while (i != -1) {
    	        if (Character.isWhitespace(s.charAt(i)) || s.substring(i, i + 1).equals("\n")) { //$NON-NLS-1$
    	            i++;
    	            break;
    	        }
    	        i--;
    	    }
    		int start = (i < 0) ? 0 : i;
    		// look for the first whitespace character
    		boolean found = false;
    		i += URL.length();
    		while (!found && i < max) {
    			found = (Character.isWhitespace(s.charAt(i)) || s.substring(i, i + 1).equals("\n")); //$NON-NLS-1$
    			i++;
    		}
    		if (i!=max) i--;
    		linkRanges.add(new int[] {start, i - start});
    		links.add(s.substring(start, i));
    		i = s.indexOf(URL, i);
    	}
    	return new LinkList(
    			(int[][])linkRanges.toArray(new int[linkRanges.size()][2]),
    			(String[])links.toArray(new String[links.size()]));
    }
    
    // Return error message if there are any problems with the issue that was entered.
    public String validateIssue(String issue) {
        if (number) {
           if (!hasOnlyDigits(issue)) return Policy.bind("CommitDialog.number", label); //$NON-NLS-1$
        }
        return null;
    }
    
    // Helper method to test for all numerics and commas.
    private boolean hasOnlyDigits(String s) {
        for (int i=0; i<s.length(); i++) if ((!(s.charAt(i) == ',')) && !Character.isDigit(s.charAt(i))) return false;
        return true;
    }

    
    public String toString() {
       return "bugtraq:label: " + label + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
              "bugtraq:message: " + message + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
              "bugtraq:number: " + number + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
              "bugtraq:url: " + url + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
              "bugtraq:warnifnoissue: " + warnIfNoIssue + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
              "bugtraq:append: " + append; //$NON-NLS-1$
    }
    
    // Get ProjectProperties for selected resource.  First looks at selected resource,
    // then works up through ancestors until a folder with the bugtraq:message property
    // is found.  If none found, returns null.
    public static ProjectProperties getProjectProperties(IResource resource) throws SVNException {
        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
        ISVNProperty property = null;
        ISVNProperty labelProperty = null;
        if (svnResource != null && svnResource.isManaged()) {
            try {
				property = svnResource.getSvnProperty("bugtraq:message"); //$NON-NLS-1$
	            labelProperty = svnResource.getSvnProperty("bugtraq:label"); //$NON-NLS-1$
			} catch (SVNException e) {
			}
        }
        if ((property != null) && (property.getValue() != null) && (property.getValue().trim().length() > 0)) {
            ProjectProperties projectProperties = new ProjectProperties();
            projectProperties.setMessage(property.getValue());
            if ((labelProperty != null) && (labelProperty.getValue() != null) && (labelProperty.getValue().trim().length() != 0)) projectProperties.setLabel(labelProperty.getValue());
            property = svnResource.getSvnProperty("bugtraq:url"); //$NON-NLS-1$
            if (property != null) projectProperties.setUrl(property.getValue()); 
            property = svnResource.getSvnProperty("bugtraq:number"); //$NON-NLS-1$
            if ((property != null) && (property.getValue() != null)) projectProperties.setNumber(property.getValue().equalsIgnoreCase("true")); //$NON-NLS-1$  
            property = svnResource.getSvnProperty("bugtraq:warnifnoissue"); //$NON-NLS-1$
            if ((property != null) && (property.getValue() != null)) projectProperties.setWarnIfNoIssue(property.getValue().equalsIgnoreCase("true")); //$NON-NLS-1$   
            property = svnResource.getSvnProperty("bugtraq:append"); //$NON-NLS-1$
            if ((property != null) && (property.getValue() != null)) projectProperties.setAppend(property.getValue().equalsIgnoreCase("true")); //$NON-NLS-1$                                   
            return projectProperties;           
        }
        IResource checkResource = resource;
        while (checkResource.getParent() != null) {
            checkResource = checkResource.getParent();
            if (checkResource.getParent() == null) return null;
            try {
	            svnResource = SVNWorkspaceRoot.getSVNResourceFor(checkResource);
	            if (svnResource.isManaged())
	                property = svnResource.getSvnProperty("bugtraq:message"); //$NON-NLS-1$
	            if (property != null) return getProjectProperties(checkResource);
            } catch (SVNException e) {
            }
        }
        return null;
    }
    
    public static ProjectProperties getProjectProperties(ISVNRemoteResource remoteResource) {
        return null;
    }
}