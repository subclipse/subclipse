/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.test.core;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.status.StatusCacheComposite;
import org.tigris.subversion.subclipse.test.TestProject;


public class StatusCacheCompositeTest extends TestCase {
	private StatusCacheComposite cache; 
    private IProject project;
    
    public class MyStatus extends LocalResourceStatus {
        public MyStatus(String author) {
            this.lastCommitAuthor = author;
        }
        
        
    }
    
    private IFile getFile(String path) {
        return project.getFile(new Path(path));
    }
    
    private IFolder getFolder(String path) {
    	return project.getFolder(new Path(path));
    }
    
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
        // we create a dummy project
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        project = root.getProject("myProject");
        project.create(null);
        project.open(null);
        
        cache = new StatusCacheComposite();
        cache.addStatus(getFile("test/core/StatusCacheCompositeTest.java"),
                new MyStatus("author1"));
        cache.addStatus(getFile("test/core/CheckoutTest.java"),
                new MyStatus("author2"));
        cache.addStatus(getFile("test/BuildFile.java"),
                new MyStatus("author3"));
        cache.addStatus(getFolder("test/core"),
                new MyStatus("author4"));
        cache.addStatus(getFolder("test"),
                new MyStatus("author5"));
	}
    
    
    
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
        TestProject.waitForIndexer();
		project.delete(true, true, null);
	}
    
    
    public void testGetStatus() throws Exception {
        assertEquals("author1",
                cache.getStatus(getFile("test/core/StatusCacheCompositeTest.java")).getLastCommitAuthor());
        assertEquals("author4",
                cache.getStatus(getFolder("test/core")).getLastCommitAuthor());
    }
    
    public void testRemoveStatusDepthZero() throws Exception {
    	cache.removeStatus(getFolder("test/core"),IResource.DEPTH_ZERO);
        assertNull(cache.getStatus(getFolder("test/core")));
        assertNotNull(cache.getStatus(getFile("test/core/StatusCacheCompositeTest.java")));
    }
    
    public void testRemoveStatusDepthOne() throws Exception {
        cache.removeStatus(getFolder("test"),IResource.DEPTH_ONE);
        assertNull(cache.getStatus(getFolder("test")));
        assertNull(cache.getStatus(getFolder("test/core")));
        assertNotNull(cache.getStatus(getFile("test/core/StatusCacheCompositeTest.java")));
    }

    public void testRemoveStatusDepthInfinite() throws Exception {
        cache.removeStatus(getFolder("test"),IResource.DEPTH_INFINITE);
        assertNull(cache.getStatus(getFolder("test")));
        assertNull(cache.getStatus(getFolder("test/core")));
        assertNull(cache.getStatus(getFile("test/core/StatusCacheCompositeTest.java")));
    }
    
    
}
