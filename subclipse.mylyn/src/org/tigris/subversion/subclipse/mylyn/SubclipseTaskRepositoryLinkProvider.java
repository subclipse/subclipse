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
import org.eclipse.mylyn.tasks.core.IRepositoryManager;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractTaskRepositoryLinkProvider;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.settings.ProjectProperties;

/**
 * Task Repository link provider based on Subversion properties
 * 
 * @see http://markphip.blogspot.com/2007/01/integrating-subversion-with-your-issue.html
 * 
 * @author Eugene Kuleshov
 */
public class SubclipseTaskRepositoryLinkProvider extends
    AbstractTaskRepositoryLinkProvider {

  public TaskRepository getTaskRepository(IResource resource,
      IRepositoryManager repositoryManager) {
    try {
      ProjectProperties props = ProjectProperties.getProjectProperties(resource);
      if(props!=null) {
        return SubclipseTeamPlugin.getRepository(props.getUrl(), repositoryManager);
      }
    } catch (SVNException ex) {
      // ignore
    }
    return null;
  }

}
