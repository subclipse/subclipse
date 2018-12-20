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

import com.collabnet.subversion.merge.wizards.MergeWizardLastPage;
import com.collabnet.subversion.merge.wizards.MergeWizardMainPage;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchPart;

public interface IMergeInputProvider extends Comparable {

  public void setText(String text);

  public String getText();

  public void setSequence(int sequence);

  public int getSequence();

  public boolean showBestPracticesPage();

  public IWizardPage[] getWizardPages(boolean initializePages);

  public IWizardPage getNextPage(IWizardPage currentPage);

  public boolean performMerge(
      MergeWizardMainPage mainPage, MergeWizardLastPage optionsPage, IWorkbenchPart targetPart);

  public boolean enabledForMultipleSelection();

  public boolean showOptionsPage();

  public boolean hideIgnoreAncestry();

  public boolean hideForce();

  public boolean hideDepth();

  public void setDescription(String description);

  public String getDescription();

  public void setImage(Image image);

  public Image getImage();
}
