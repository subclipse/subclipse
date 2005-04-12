/*
 * Created on Apr 22, 2004
 */
package org.tigris.subversion.subclipse.test.core;

import junit.framework.TestCase;

import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * @author mml
 *
 */
public class SVNUrlTest extends TestCase {
	
	
	public void testCanonicalGetParentToString()throws Exception{
		SVNUrl url = new SVNUrl("http://blah.com/svn/blah/");
		assertFalse(url.getParent().toString().endsWith("/"));
		url = new SVNUrl("http://blah.com/svn/blah/A.class");
		assertFalse(url.getParent().toString().endsWith("/"));
	}
}
