/**
 * ***************************************************************************** Copyright (c) 2009
 * CollabNet. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: CollabNet - initial API and implementation
 * ****************************************************************************
 */
package com.collabnet.subversion.merge.views;

import com.collabnet.subversion.merge.Activator;
import java.util.HashMap;
import org.eclipse.compare.CompareUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

public class MergeResultsDecorator {
  public static final int ADD = 0;
  public static final int CHANGE = 1;
  public static final int DELETE = 2;
  public static final int MERGE = 3;
  public static final int CONFLICTED_CHANGE = 4;
  public static final int CONFLICTED_DELETE = 5;
  public static final int CONFLICTED_ADD = 6;
  public static final int ERROR = 7;
  public static final int PROPERTY_ADD = 8;
  public static final int PROPERTY_CHANGE = 9;
  public static final int PROPERTY_DELETE = 10;
  public static final int PROPERTY_CONFLICTED_ADD = 11;
  public static final int PROPERTY_CONFLICTED_CHANGE = 12;
  public static final int PROPERTY_CONFLICTED_DELETE = 13;
  public static final int NO_CHANGE = 15;
  public static final int TREE_CONFLICTED = 14;

  private static ImageDescriptor[] fgImages = new ImageDescriptor[15];
  private static HashMap fgMap = new HashMap(20);

  private Image[] fImages = new Image[15];

  static {
    fgImages[ADD] = Activator.getDefault().getImageDescriptor(Activator.IMAGE_OVERLAY_ADD);
    fgImages[CHANGE] = Activator.getDefault().getImageDescriptor(Activator.IMAGE_OVERLAY_CHANGE);
    fgImages[DELETE] = Activator.getDefault().getImageDescriptor(Activator.IMAGE_OVERLAY_DELETE);
    fgImages[MERGE] = Activator.getDefault().getImageDescriptor(Activator.IMAGE_OVERLAY_RESOLVED);
    fgImages[CONFLICTED_CHANGE] =
        Activator.getDefault().getImageDescriptor(Activator.IMAGE_OVERLAY_CONFLICTED_CHANGE);
    fgImages[CONFLICTED_DELETE] =
        Activator.getDefault().getImageDescriptor(Activator.IMAGE_OVERLAY_CONFLICTED_DELETE);
    fgImages[CONFLICTED_ADD] =
        Activator.getDefault().getImageDescriptor(Activator.IMAGE_OVERLAY_CONFLICTED_ADD);
    fgImages[ERROR] = Activator.getDefault().getImageDescriptor(Activator.IMAGE_OVERLAY_ERROR);
    fgImages[PROPERTY_ADD] =
        Activator.getDefault().getImageDescriptor(Activator.IMAGE_OVERLAY_PROPERTY_ADD);
    fgImages[PROPERTY_CHANGE] =
        Activator.getDefault().getImageDescriptor(Activator.IMAGE_OVERLAY_PROPERTY_CHANGE);
    fgImages[PROPERTY_DELETE] =
        Activator.getDefault().getImageDescriptor(Activator.IMAGE_OVERLAY_PROPERTY_DELETE);
    fgImages[PROPERTY_CONFLICTED_ADD] =
        Activator.getDefault().getImageDescriptor(Activator.IMAGE_OVERLAY_PROPERTY_CONFLICTED_ADD);
    fgImages[PROPERTY_CONFLICTED_CHANGE] =
        Activator.getDefault()
            .getImageDescriptor(Activator.IMAGE_OVERLAY_PROPERTY_CONFLICTED_CHANGE);
    fgImages[PROPERTY_CONFLICTED_DELETE] =
        Activator.getDefault()
            .getImageDescriptor(Activator.IMAGE_OVERLAY_PROPERTY_CONFLICTED_DELETE);
    fgImages[TREE_CONFLICTED] =
        Activator.getDefault().getImageDescriptor(Activator.IMAGE_OVERLAY_TREE_CONFLICT);
  }

  public Image getImage(Image base, int kind) {

    Object key = base;

    //		kind &= 15;

    Image[] a = (Image[]) fgMap.get(key);
    if (a == null) {
      a = new Image[15];
      fgMap.put(key, a);
    }
    Image b = a[kind];
    if (b == null) {
      boolean onLeft = kind == PROPERTY_CHANGE;
      b = new DiffImage(base, fgImages[kind], 22, onLeft).createImage();
      CompareUI.disposeOnShutdown(b);
      a[kind] = b;
    }
    return b;
  }

  public void dispose() {
    if (fImages != null) {
      for (int i = 0; i < fImages.length; i++) {
        Image image = fImages[i];
        if (image != null && !image.isDisposed()) image.dispose();
      }
    }
    fImages = null;
  }
}
