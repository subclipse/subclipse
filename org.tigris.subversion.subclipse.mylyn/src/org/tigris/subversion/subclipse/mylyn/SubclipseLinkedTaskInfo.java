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
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.TaskRepositoryManager;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.team.ui.AbstractTaskReference;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.core.subscribers.CheckedInChangeSet;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.history.LogEntry;
import org.tigris.subversion.subclipse.core.sync.SVNStatusSyncInfo;
import org.tigris.subversion.subclipse.ui.settings.ProjectProperties;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.ISVNProperty;
import org.tigris.subversion.svnclientadapter.SVNRevision;


/**
 * <code>ILinkedTaskInfo</code> implementation for lazy loading of the Subversion info.
 *
 * @author Eugene Kuleshov
 */
class SubclipseLinkedTaskInfo extends AbstractTaskReference {
  private IResource resource;
  private CheckedInChangeSet changeSet;
  private LogEntry logEntry;
  
  private String repositoryUrl;
  private String taskFullUrl;
  private String comment;

  SubclipseLinkedTaskInfo(IResource resource, CheckedInChangeSet changeSet) {
    this.resource = resource;
    this.changeSet = changeSet;
  }

  SubclipseLinkedTaskInfo(LogEntry logEntry) {
    this.logEntry = logEntry;
    this.comment = logEntry.getComment();
  }

  public String getRepositoryUrl() {
    if(repositoryUrl==null) {
      init();
    }
    return repositoryUrl;
  }

  public String getTaskUrl() {
    if(taskFullUrl==null) {
      init();
    }          
    return taskFullUrl;
  }

  public AbstractTask getTask() {
    return null;
  }

  public String getTaskId() {
    return null;
  }

  public String getText() {
    if(comment==null && changeSet!=null) {
      try {
        SyncInfoTree syncInfoSet = changeSet.getSyncInfoSet();
        SVNStatusSyncInfo info = (SVNStatusSyncInfo) syncInfoSet.getSyncInfo(resource);
        ISVNRemoteResource remoteResource = (ISVNRemoteResource) info.getRemote();
        
        SVNRevision rev = remoteResource.getLastChangedRevision();
        ISVNLogMessage[] messages = remoteResource.getLogMessages(rev, rev,
            SVNRevision.START, false, false, 1, false);
        comment = messages[0].getMessage();
      } catch (TeamException ex) {
        comment = changeSet.getComment();
      }
    }
    return comment;
  }
  
  private void init() {
    TaskRepositoryManager repositoryManager = TasksUiPlugin.getRepositoryManager(); 

    String[] urls = null;
    ProjectProperties props = null;
    try {
      if(resource!=null) {
        props = ProjectProperties.getProjectProperties(resource);
      } else if(logEntry!=null) {
        ISVNResource svnres = logEntry.getResource();
        if(svnres!=null) {
          if(svnres.getResource()!=null) {
            props = ProjectProperties.getProjectProperties(svnres.getResource());
          } else {
            ISVNClientAdapter client = SVNProviderPlugin.getPlugin()
                .getSVNClientManager().getSVNClient();
            SVNProviderPlugin.disableConsoleLogging(); 
            ISVNProperty[] properties = client.getProperties(svnres.getUrl());
            SVNProviderPlugin.enableConsoleLogging(); 
            for (int i = 0; i < properties.length; i++) {
              ISVNProperty property = properties[i];
              if ("bugtraq:url".equals(property.getName())) {
                repositoryUrl = SubclipseTeamPlugin.getRepository(
                    property.getValue(), repositoryManager).getRepositoryUrl();
                // comments?
              }
            }
          }
        }
      }
    } catch (Exception ex) {
      // ignore?
        SVNProviderPlugin.enableConsoleLogging(); 
    }
    if (props != null) {
      if (repositoryUrl == null) {
        repositoryUrl = SubclipseTeamPlugin.getRepository(props.getUrl(),
            repositoryManager).getRepositoryUrl();
      }
      urls = props.getLinkList(getText()).getUrls();
    }
    
    if (urls == null || urls.length == 0) {
      urls = ProjectProperties.getUrls(getText()).getUrls();
    }
    if (urls != null && urls.length > 0) {
      taskFullUrl = urls[0];
    }
  }
  
}

