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

import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNRevisionRange;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class MergeOptions {
  private SVNUrl fromUrl;
  private SVNUrl toUrl;
  private SVNRevision fromRevision;
  private SVNRevision toRevision;
  private SVNRevisionRange[] revisions;
  private boolean force;
  private boolean ignoreAncestry;
  private boolean recurse;
  private int depth;

  public SVNUrl getFromUrl() {
    return fromUrl;
  }

  public void setFromUrl(SVNUrl fromUrl) {
    this.fromUrl = fromUrl;
  }

  public void setFromUrl(String fromUrl) {
    try {
      this.fromUrl = new SVNUrl(fromUrl);
    } catch (MalformedURLException e) {
    }
  }

  public SVNUrl getToUrl() {
    return toUrl;
  }

  public void setToUrl(SVNUrl toUrl) {
    this.toUrl = toUrl;
  }

  public void setToUrl(String toUrl) {
    try {
      this.toUrl = new SVNUrl(toUrl);
    } catch (MalformedURLException e) {
    }
  }

  public SVNRevision getFromRevision() {
    return fromRevision;
  }

  public void setFromRevision(SVNRevision fromRevision) {
    this.fromRevision = fromRevision;
  }

  public void setFromRevision(String fromRevision) {
    try {
      this.fromRevision = SVNRevision.getRevision(fromRevision);
    } catch (ParseException e) {
    }
  }

  public SVNRevision getToRevision() {
    return toRevision;
  }

  public void setToRevision(SVNRevision toRevision) {
    this.toRevision = toRevision;
  }

  public void setToRevision(String toRevision) {
    try {
      this.toRevision = SVNRevision.getRevision(toRevision);
    } catch (ParseException e) {
    }
  }

  public SVNRevisionRange[] getRevisions() {
    return revisions;
  }

  public void setRevisions(SVNRevisionRange[] revisions) {
    this.revisions = revisions;
  }

  public void setRevisions(String revisions) {
    ArrayList revisionsList = new ArrayList();
    String[] ranges = revisions.split(","); // $NON-NLS-1$
    for (int i = 0; i < ranges.length; i++) revisionsList.add(new SVNRevisionRange(ranges[i]));
    SVNRevisionRange[] revisionsArray = new SVNRevisionRange[revisionsList.size()];
    revisionsList.toArray(revisionsArray);
    setRevisions(revisionsArray);
  }

  public boolean isForce() {
    return force;
  }

  public void setForce(boolean force) {
    this.force = force;
  }

  public boolean isIgnoreAncestry() {
    return ignoreAncestry;
  }

  public void setIgnoreAncestry(boolean ignoreAncestry) {
    this.ignoreAncestry = ignoreAncestry;
  }

  public boolean isRecurse() {
    return recurse;
  }

  public void setRecurse(boolean recurse) {
    this.recurse = recurse;
  }

  public int getDepth() {
    return depth;
  }

  public void setDepth(int depth) {
    this.depth = depth;
  }
}
