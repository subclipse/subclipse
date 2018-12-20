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

import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;

public class ConflictResolution {
  private SVNConflictDescriptor conflictDescriptor;
  private int resolution;
  private String mergedPath;
  private boolean applyToAll;
  public static final int FILE_EDITOR = 20;
  public static final int CONFLICT_EDITOR = 21;

  public ConflictResolution(SVNConflictDescriptor conflictDescriptor, int resolution) {
    super();
    this.conflictDescriptor = conflictDescriptor;
    this.resolution = resolution;
  }

  public SVNConflictDescriptor getConflictDescriptor() {
    return conflictDescriptor;
  }

  public int getResolution() {
    return resolution;
  }

  public boolean isResolved() {
    return resolution != ISVNConflictResolver.Choice.postpone;
  }

  public String getMergedPath() {
    if (mergedPath == null) return conflictDescriptor.getMergedPath();
    else return mergedPath;
  }

  public void setMergedPath(String mergedPath) {
    this.mergedPath = mergedPath;
  }

  public boolean isApplyToAll() {
    return applyToAll;
  }

  public void setApplyToAll(boolean applyToAll) {
    this.applyToAll = applyToAll;
  }
}
