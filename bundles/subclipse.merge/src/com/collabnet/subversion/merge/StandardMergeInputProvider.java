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
import com.collabnet.subversion.merge.wizards.MergeWizardRevisionsPage;
import com.collabnet.subversion.merge.wizards.MergeWizardStandardPage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.comments.CommentsManager;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNRevisionRange;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class StandardMergeInputProvider implements IMergeInputProvider {
  private String text;
  private String description;
  private int sequence;
  private Image image;
  private MergeWizardStandardPage standardPage;
  private MergeWizardRevisionsPage revisionsPage;
  private WizardPage[] wizardPages;

  public String getText() {
    return text;
  }

  public int getSequence() {
    return sequence;
  }

  public IWizardPage[] getWizardPages(boolean initializePages) {
    if (wizardPages == null || initializePages) {
      standardPage =
          new MergeWizardStandardPage(
              "standard",
              Messages.StandardMergeInputProvider_selectMergeSource,
              Activator.getDefault()
                  .getImageDescriptor(Activator.IMAGE_MERGE_WIZARD)); // $NON-NLS-1$
      revisionsPage =
          new MergeWizardRevisionsPage(
              "revisions",
              Messages.StandardMergeInputProvider_selectRevisions,
              Activator.getDefault().getImageDescriptor(Activator.IMAGE_MERGE_WIZARD),
              standardPage); //$NON-NLS-1$
      WizardPage[] pages = {standardPage, revisionsPage};
      wizardPages = pages;
    }
    return wizardPages;
  }

  public IWizardPage getNextPage(IWizardPage currentPage) {
    if (currentPage == standardPage && standardPage.selectRevisions()) return revisionsPage;
    else return null;
  }

  public boolean performMerge(
      MergeWizardMainPage mainPage, MergeWizardLastPage optionsPage, IWorkbenchPart targetPart) {

    String commonRoot = standardPage.getCommonRoot(false);
    String mergeFrom = standardPage.getMergeFrom();
    Activator.getDefault().saveMergeSource(mergeFrom, commonRoot);

    IResource[] resources = standardPage.getResources();
    SVNUrl[] urls = standardPage.getUrls();

    SVNRevisionRange[] revisions = null;
    if (standardPage.selectRevisions()) {
      Set<IResource> usedResources = new HashSet<IResource>();

      Map<SVNRevision.Number, List<IResource>> map = revisionsPage.getRevisionToResource();
      revisions = revisionsPage.getRevisions();
      CommentsManager commentsManager =
          SVNUIPlugin.getPlugin().getRepositoryManager().getCommentsManager();
      ILogEntry[] entries = revisionsPage.getSelectedLogEntries();
      for (int i = 0; i < entries.length; i++) {
        commentsManager.addComment(entries[i].getComment());
        if (map.size() > 0) {
          List<IResource> lst = map.get(entries[i].getRevision());
          usedResources.addAll(lst);
        }
      }
      // only filter the urls if the usedResources does have content.
      if (usedResources.size() > 0) {
        List<SVNUrl> urlsList = new ArrayList<SVNUrl>();
        List<IResource> resourcesList = new ArrayList<IResource>();

        for (int i = 0; i < resources.length; i++) {
          if (usedResources.contains(resources[i])) {
            urlsList.add(urls[i]);
            resourcesList.add(resources[i]);
          }
        }
        resources = resourcesList.toArray(new IResource[resourcesList.size()]);
        urls = urlsList.toArray(new SVNUrl[urlsList.size()]);
      }
    }

    MergeOperation mergeOperation =
        new MergeOperation(targetPart, resources, urls, null, urls, null, revisions, null);
    mergeOperation.setForce(optionsPage.isForce());
    mergeOperation.setIgnoreAncestry(optionsPage.isIgnore());
    mergeOperation.setDepth(optionsPage.getDepth());
    mergeOperation.setTextConflictHandling(optionsPage.getTextConflictHandling());
    mergeOperation.setBinaryConflictHandling(optionsPage.getBinaryConflictHandling());
    mergeOperation.setPropertyConflictHandling(optionsPage.getPropertyConflictHandling());
    mergeOperation.setTreeConflictHandling(optionsPage.getTreeConflictHandling());
    try {
      mergeOperation.run();
    } catch (Exception e) {
      Activator.handleError(Messages.StandardMergeInputProvider_error, e);
      MessageDialog.openError(
          Display.getCurrent().getActiveShell(),
          Messages.StandardMergeInputProvider_merge,
          e.getMessage());
      return false;
    }

    return true;
  }

  public void setText(String text) {
    this.text = text;
  }

  public int compareTo(Object compareToObject) {
    if (!(compareToObject instanceof IMergeInputProvider)) return 0;
    IMergeInputProvider compareToInputProvider = (IMergeInputProvider) compareToObject;
    if (getSequence() > compareToInputProvider.getSequence()) return 1;
    else if (compareToInputProvider.getSequence() > getSequence()) return -1;
    return getText().compareTo(compareToInputProvider.getText());
  }

  public boolean enabledForMultipleSelection() {
    return true;
  }

  public boolean showOptionsPage() {
    return true;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean hideDepth() {
    return false;
  }

  public boolean hideForce() {
    return false;
  }

  public boolean hideIgnoreAncestry() {
    return false;
  }

  public Image getImage() {
    return image;
  }

  public void setImage(Image image) {
    this.image = image;
  }

  public void setSequence(int sequence) {
    this.sequence = sequence;
  }

  public boolean showBestPracticesPage() {
    return true;
  }
}
