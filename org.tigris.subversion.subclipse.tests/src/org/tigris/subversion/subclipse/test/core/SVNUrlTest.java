/*
 * Created on Apr 22, 2004
 */
package org.tigris.subversion.subclipse.test.core;

import junit.framework.TestCase;

import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * @author mml
 *
 */
public class SVNUrlTest extends TestCase {
	
	public void testCanonicalGetParentToString() throws Exception 
	{
		SVNUrl url = new SVNUrl("http://blah.com/svn/blah/");
		assertFalse(url.getParent().toString().endsWith("/"));
		url = new SVNUrl("http://blah.com/svn/blah/A.class");
		assertFalse(url.getParent().toString().endsWith("/"));
	}
	
	public void testUtilGetLastSegment() throws Exception 
	{
		SVNUrl url = new SVNUrl("http://blah.com/svn/blah");
		assertEquals("blah", url.getLastPathSegment());
		assertEquals("blah", Util.getLastSegment(url.toString()));
	}
	
	public void testGetAppendPath() throws Exception
	{
		SVNUrl urlParent = new SVNUrl("http://blah.com/svn/");
		SVNUrl urlAppended = new SVNUrl("http://blah.com/svn/blah/ouch");
		assertEquals(urlAppended.toString(), Util.appendPath(urlParent.toString(), "blah/ouch"));
		assertEquals(urlAppended.toString(), Util.appendPath(urlParent.toString(), "/blah/ouch"));
		assertEquals(urlAppended.toString(), urlParent.appendPath("blah/ouch").toString());
		assertEquals(urlAppended.toString(), urlParent.appendPath("/blah/ouch").toString());
	}
}
