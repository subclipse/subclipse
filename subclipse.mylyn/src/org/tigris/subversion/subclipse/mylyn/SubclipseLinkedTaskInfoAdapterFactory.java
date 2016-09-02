/*******************************************************************************
 * Copyright (c) 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eugene Kuleshov - initial API and implementation
 ******************************************************************************/

package org.tigris.subversion.subclipse.mylyn;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.mylyn.team.ui.AbstractTaskReference;
import org.eclipse.team.internal.core.subscribers.CheckedInChangeSet;
import org.tigris.subversion.subclipse.core.history.LogEntry;
import org.tigris.subversion.subclipse.ui.subscriber.SVNChangeSetCollector;


/**
 * <code>AdapterFactory</code> for adapting to <code>AbstractTaskReference</code>. 
 * 
 * @author Eugene Kuleshov
 */
public class SubclipseLinkedTaskInfoAdapterFactory implements IAdapterFactory {

  private static final Class[] ADAPTER_TYPES = new Class[] { AbstractTaskReference.class };


  public Class[] getAdapterList() {
    return ADAPTER_TYPES;
  }
  
  public Object getAdapter(Object object, Class adapterType) {
    if(adapterType != AbstractTaskReference.class) {
      return null;
    }
    
    if(object instanceof LogEntry) {
      return adaptSubclipseLogEntry((LogEntry) object);
    }
    
    if(object instanceof SVNChangeSetCollector.SVNCheckedInChangeSet) {
      return adaptSubclipseChangeset((CheckedInChangeSet) object);
    }
    
    return null;
  }

  private static AbstractTaskReference adaptSubclipseLogEntry(LogEntry logEntry) {
    return new SubclipseLinkedTaskInfo(logEntry);
  }

  private static AbstractTaskReference adaptSubclipseChangeset(final CheckedInChangeSet set) {
    IResource[] resources = set.getResources();
    IResource res = resources[0];
    return new SubclipseLinkedTaskInfo(res, set);
  }
  
}

