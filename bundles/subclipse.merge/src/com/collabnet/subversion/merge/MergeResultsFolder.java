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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class MergeResultsFolder implements IPropertySource {
  private IContainer folder;
  private boolean compressed;
  private int rootFolderLength;
  private MergeOutput mergeOutput;
  private MergeResult mergeResult;

  public static String P_ID_RESOURCE = "resource"; // $NON-NLS-1$
  public static String P_RESOURCE = Messages.MergeResultsFolder_resource;
  public static String P_ID_TEXT_STATUS = "textSts"; // $NON-NLS-1$
  public static String P_TEXT_STATUS = Messages.MergeResultsFolder_textStatus;
  public static String P_ID_PROPERTY_STATUS = "propSts"; // $NON-NLS-1$
  public static String P_PROPERTY_STATUS = Messages.MergeResultsFolder_propsStatus;
  public static List descriptors;

  static {
    descriptors = new ArrayList();
    descriptors.add(new PropertyDescriptor(P_ID_RESOURCE, P_RESOURCE));
    descriptors.add(new PropertyDescriptor(P_ID_TEXT_STATUS, P_TEXT_STATUS));
    descriptors.add(new PropertyDescriptor(P_ID_PROPERTY_STATUS, P_PROPERTY_STATUS));
  }

  public IContainer getFolder() {
    return folder;
  }

  public void setFolder(IContainer folder) {
    this.folder = folder;
  }

  public boolean isCompressed() {
    return compressed;
  }

  public void setCompressed(boolean compressed) {
    this.compressed = compressed;
  }

  public void setRootFolderLength(int rootFolderLength) {
    this.rootFolderLength = rootFolderLength;
  }

  public String toString() {
    if (compressed) {
      if (folder.getFullPath().makeRelative().toString().length() > rootFolderLength)
        return folder.getFullPath().makeRelative().toString().substring(rootFolderLength + 1);
      else {
        return mergeOutput.getResource().getName();
      }
    } else return folder.getName();
  }

  public MergeOutput getMergeOutput() {
    return mergeOutput;
  }

  public void setMergeOutput(MergeOutput mergeOutput) {
    this.mergeOutput = mergeOutput;
  }

  public MergeResult[] getMergeResults(boolean conflictsOnly) {
    MergeResult[] mergeResults = mergeOutput.getMergeResults(conflictsOnly);
    ArrayList mergeResultList = new ArrayList();
    boolean resultAdded = false;
    for (int i = 0; i < mergeResults.length; i++) {
      if (mergeResults[i].getResource() instanceof IFile) {
        IContainer parent = mergeResults[i].getResource().getParent();
        if (parent.getFullPath().toString().equals(folder.getFullPath().toString())) {
          mergeResultList.add(mergeResults[i]);
          resultAdded = true;
        } else {
          if (resultAdded) break;
        }
      }
    }
    MergeResult[] mergeResultArray = new MergeResult[mergeResultList.size()];
    mergeResultList.toArray(mergeResultArray);
    return mergeResultArray;
  }

  public MergeResult getMergeResult() {
    return mergeResult;
  }

  public void setMergeResult(MergeResult mergeResult) {
    this.mergeResult = mergeResult;
  }

  public boolean equals(Object obj) {
    if (obj instanceof MergeResultsFolder) {
      MergeResultsFolder compareFolder = (MergeResultsFolder) obj;
      return compareFolder
          .getFolder()
          .getFullPath()
          .toString()
          .equals(folder.getFullPath().toString());
    }
    if (obj instanceof IContainer) {
      IContainer compareFolder = (IContainer) obj;
      return compareFolder.getFullPath().toString().equals(folder.getFullPath().toString());
    }
    return super.equals(obj);
  }

  public int hashCode() {
    return folder.getFullPath().toString().hashCode();
  }

  public int getRootFolderLength() {
    return rootFolderLength;
  }

  public Object getEditableValue() {
    return folder.getFullPath().toString();
  }

  public IPropertyDescriptor[] getPropertyDescriptors() {
    return (IPropertyDescriptor[])
        getDescriptors().toArray(new IPropertyDescriptor[getDescriptors().size()]);
  }

  private static List getDescriptors() {
    return descriptors;
  }

  public Object getPropertyValue(Object id) {
    if (P_ID_RESOURCE.equals(id)) return folder.getFullPath().toString();
    if (mergeResult == null) return Messages.MergeResultsFolder_noChange;
    if (P_ID_TEXT_STATUS.equals(id)) {
      if (mergeResult.getAction().equals(MergeResult.ACTION_ADD))
        return Messages.MergeResultsFolder_added;
      if (mergeResult.getAction().equals(MergeResult.ACTION_CHANGE))
        return Messages.MergeResultsFolder_modified;
      if (mergeResult.getAction().equals(MergeResult.ACTION_DELETE))
        return Messages.MergeResultsFolder_deleted;
      if (mergeResult.getAction().equals(MergeResult.ACTION_MERGE))
        return Messages.MergeResultsFolder_merged;
      if (mergeResult.getAction().equals(MergeResult.ACTION_SKIP))
        return Messages.MergeResultsFolder_skipped;
      if (mergeResult.getAction().equals(MergeResult.ACTION_CONFLICT)) {
        if (mergeResult.isResolved())
          return Messages.MergeResultsFolder_resolvedConflict
              + SVNConflictResolver.getResolutionDescription(mergeResult.getConflictResolution())
              + ")"; //$NON-NLS-1$
        else return Messages.MergeResultsFolder_conflicted;
      }
    }
    if (P_ID_PROPERTY_STATUS.equals(id)) {
      if (mergeResult.getPropertyAction().equals(MergeResult.ACTION_ADD))
        return Messages.MergeResultsFolder_added;
      if (mergeResult.getPropertyAction().equals(MergeResult.ACTION_CHANGE))
        return Messages.MergeResultsFolder_modified;
      if (mergeResult.getPropertyAction().equals(MergeResult.ACTION_DELETE))
        return Messages.MergeResultsFolder_deleted;
      if (mergeResult.getPropertyAction().equals(MergeResult.ACTION_MERGE))
        return Messages.MergeResultsFolder_merged;
      if (mergeResult.getPropertyAction().equals(MergeResult.ACTION_SKIP))
        return Messages.MergeResultsFolder_skipped;
      if (mergeResult.getPropertyAction().equals(MergeResult.ACTION_CONFLICT)) {
        if (mergeResult.isPropertyResolved())
          return Messages.MergeResultsFolder_resolvedConflict
              + SVNConflictResolver.getResolutionDescription(mergeResult.getPropertyResolution())
              + ")"; //$NON-NLS-1$
        else return Messages.MergeResultsFolder_conflicted;
      }
    }
    return null;
  }

  public boolean isPropertySet(Object id) {
    return false;
  }

  public void resetPropertyValue(Object id) {}

  public void setPropertyValue(Object id, Object value) {}
}
