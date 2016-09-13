/*******************************************************************************
 * Copyright (c) 2004, 2010 Subclipse project and others.
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
import java.util.Iterator;
import java.util.List;

import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNProperty;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;


public class UnversionedCustomProperty {
  
  final private String name;
  final private String value;
  
  public UnversionedCustomProperty(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() { return name; }
  
  public String getValue() { return value; }  

  public static UnversionedCustomProperty[] getSvnRevisionProperties(final ISVNRemoteResource remoteResource, final SVNRevision revision,
      final SVNRevision peg)  throws SVNException {
    return getSvnRevisionProperties(remoteResource, revision, peg, true);
  }
  
  public static UnversionedCustomProperty[] getSvnRevisionProperties(final ISVNRemoteResource remoteResource, final SVNRevision revision,
      final SVNRevision peg, final boolean customOnly)  throws SVNException {
    
    SVNUrl url= remoteResource.getUrl(); 
 
    ISVNProperty[] props = getSvnRevisionProperties(url, revision, peg);
    List temp = new ArrayList();
    for (int i = 0; i < props.length; i++) {
      final String name = props[i].getName();
      final String value = props[i].getValue();
      // TODO: pull these from svnPropertyTypes prefixes rather than hardcoding
      if (customOnly && (name.startsWith("svn:") || name.startsWith("bugtraq:") || name.startsWith("tsvn:"))) 
        continue;
      UnversionedCustomProperty ucp = new UnversionedCustomProperty(name, value);
      temp.add(ucp);    	
    }
    UnversionedCustomProperty[] ret = new UnversionedCustomProperty[temp.size()];
    int i = 0;
    Iterator iter = temp.iterator();
    while (iter.hasNext()) {
    	ret[i++] = (UnversionedCustomProperty)iter.next();
    }
    return ret;
  }
  
  public static ISVNProperty[] getSvnRevisionProperties(final SVNUrl url, final SVNRevision revision, final SVNRevision peg) throws SVNException {
      return getSvnRevisionProperties(url, revision, peg, true);
  }
  
  public static ISVNProperty[] getSvnRevisionProperties(final SVNUrl url, final SVNRevision revision, final SVNRevision peg, final boolean customOnly) throws SVNException {
    ISVNClientAdapter svnClient = null;
	try {
        svnClient = SVNProviderPlugin.getPlugin().getSVNClient();
        SVNProviderPlugin.disableConsoleLogging(); 
        ISVNProperty[] props = svnClient.getRevProperties(url, (SVNRevision.Number)revision);
        if (customOnly) 
          return stripNonCustom(props);
        return props;
    } catch (SVNClientException e) {
        throw SVNException.wrapException(e); 
    } finally {
        SVNProviderPlugin.enableConsoleLogging();
        SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(svnClient);
    }
  }  
  
  private static ISVNProperty[] stripNonCustom(final ISVNProperty[] propsIn) {
	  List temp = new ArrayList();
	  for (int i = 0; i < propsIn.length; i++) {
	      String name = propsIn[i].getName();
	      if (name.startsWith("svn:") || name.startsWith("bugtraq:") || name.startsWith("tsvn:")) 
	        continue;
	      temp.add(propsIn[i]);		  
	  }
	  ISVNProperty[] ret = new ISVNProperty[temp.size()];
	  int i = 0;
	  Iterator iter = temp.iterator();
	  while (iter.hasNext()) {
	    ret[i++] = (ISVNProperty)iter.next();
	  }
	  return ret;
  }
  
  private static final String NL = System.getProperty("line.separator");
  
  public static String asMultilineString(UnversionedCustomProperty[] props) {	  
	StringBuffer sb = new StringBuffer();
	for (int i = 0; i < props.length; i++) {
		if (props[i] == null) break;
		sb.append(props[i].getName() + ": " + props[i].getValue() + NL);
	}
	return sb.toString();
  }
  
}
