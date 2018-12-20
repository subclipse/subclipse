/**
 * ***************************************************************************** Copyright (c) 2009
 * CollabNet. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: CollabNet - initial API and implementation
 * ****************************************************************************
 */
package com.collabnet.subversion.merge;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;

public class MergeAdapterFactory implements IAdapterFactory {

  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (IContainer.class == adapterType) {
      if (adaptableObject instanceof MergeResultsFolder) {
        MergeResultsFolder mergeResultsFolder = (MergeResultsFolder) adaptableObject;
        if (mergeResultsFolder.getFolder() == null) {
          MergeResult mergeResult = mergeResultsFolder.getMergeResult();
          if (mergeResult != null) {
            IResource resource = mergeResult.getResource();
            if (resource instanceof IContainer) {
              IContainer container = (IContainer) resource;
              return container;
            }
          }
        }
        return mergeResultsFolder.getFolder();
      }
    }
    if (IFile.class == adapterType) {
      if (adaptableObject instanceof MergeResult) {
        IResource resource = ((MergeResult) adaptableObject).getResource();
        if (resource instanceof IFile) {
          IFile file = (IFile) resource;
          return file;
        }
      }
    }
    if (IResource.class == adapterType) {
      if (adaptableObject instanceof MergeResult) {
        IResource resource = ((MergeResult) adaptableObject).getResource();
        return resource;
      }
      if (adaptableObject instanceof MergeOutput) {
        IResource resource = ((MergeOutput) adaptableObject).getResource();
        return resource;
      }
      if (adaptableObject instanceof MergeResultsFolder) {
        MergeResultsFolder mergeResultsFolder = (MergeResultsFolder) adaptableObject;
        if (mergeResultsFolder.getFolder() == null) {
          MergeResult mergeResult = mergeResultsFolder.getMergeResult();
          if (mergeResult != null) {
            return mergeResult.getResource();
          }
        }
        return mergeResultsFolder.getFolder();
      }
    }
    return null;
  }

  public Class[] getAdapterList() {
    return new Class[] {IContainer.class, IFile.class, IResource.class};
  }
}
