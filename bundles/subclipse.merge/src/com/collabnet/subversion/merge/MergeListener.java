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

import com.collabnet.subversion.merge.views.MergeResultsView;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class MergeListener implements ISVNNotifyListener {
  private Date mergeDate;
  private ArrayList mergeResults = new ArrayList();
  private ArrayList mergeSummaryResults = new ArrayList();
  private boolean logging = true;
  private MergeResult lastMergeResult;
  private MergeOutput mergeOutput;
  private boolean viewShown = false;
  private MergeResultsView view;
  private IResource resource;
  private MergeOptions mergeOptions;
  private SVNConflictResolver conflictResolver;
  private boolean loggingFileStatistics = false;
  private boolean loggingPropertyStatistics = false;
  private boolean loggingConflictStatistics = false;
  private ArrayList skips = new ArrayList();
  private boolean resumed;

  public MergeListener(
      IResource resource,
      MergeOptions mergeOptions,
      SVNConflictResolver conflictResolver,
      MergeOutput mergeOutput) {
    super();
    this.resource = resource;
    this.mergeOptions = mergeOptions;
    this.conflictResolver = conflictResolver;
    this.mergeOutput = mergeOutput;
    if (this.mergeOutput == null) this.mergeOutput = new MergeOutput();
    else resumed = true;
    setWorkspaceInformation();
  }

  private void setWorkspaceInformation() {
    ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
    if (svnResource == null) return;
    if (svnResource.getUrl() != null) mergeOutput.setWorkspaceUrl(svnResource.getUrl().toString());
    try {
      if (svnResource.getRevision() != null)
        mergeOutput.setWorkspaceRevision(
            ((SVNRevision.Number) svnResource.getRevision()).getNumber());
    } catch (SVNException e) {
    }
  }

  public void logCommandLine(String commandLine) {
    if (!resumed) {
      mergeDate = new Date();
      mergeOutput.setResource(resource);
      mergeOutput.setMergeOptions(mergeOptions);
      mergeOutput.setMergeCommand(commandLine);
      mergeOutput.setMergeResults(new MergeResult[0]);
      mergeOutput.setMergeSummaryResults(new MergeSummaryResult[0]);
      mergeOutput.setMergeDate(mergeDate);
    } else {
      MergeResult[] previousResults = mergeOutput.getMergeResults();
      for (int i = 0; i < previousResults.length; i++) {
        mergeResults.add(previousResults[i]);
      }
      MergeSummaryResult[] previousSummaries = mergeOutput.getMergeSummaryResults();
      for (int i = 0; i < previousSummaries.length; i++) {
        mergeSummaryResults.add(previousSummaries[i]);
      }
    }
  }

  public void logCompleted(String message) {
    logging = false;
    if (conflictResolver.getResolvedConflictCount(SVNConflictDescriptor.Kind.text) != 0) {
      MergeSummaryResult mergeSummaryResult =
          new MergeSummaryResult(
              MergeSummaryResult.FILE,
              "Resolved conflicts",
              Integer.toString(
                  conflictResolver.getResolvedConflictCount(
                      SVNConflictDescriptor.Kind.text))); // $NON-NLS-1$
      mergeSummaryResults.add(mergeSummaryResult);
    }
    if (conflictResolver.getResolvedConflictCount(SVNConflictDescriptor.Kind.property) != 0) {
      MergeSummaryResult mergeSummaryResult =
          new MergeSummaryResult(
              MergeSummaryResult.PROPERTY,
              "Resolved conflicts",
              Integer.toString(
                  conflictResolver.getResolvedConflictCount(
                      SVNConflictDescriptor.Kind.property))); // $NON-NLS-1$
      mergeSummaryResults.add(mergeSummaryResult);
    }
    if (conflictResolver.getResolvedConflictCount(MergeSummaryResult.TREE) != 0) {
      MergeSummaryResult mergeSummaryResult =
          new MergeSummaryResult(
              MergeSummaryResult.TREE,
              "Resolved conflicts",
              Integer.toString(
                  conflictResolver.getResolvedConflictCount(
                      MergeSummaryResult.TREE))); // $NON-NLS-1$
      mergeSummaryResults.add(mergeSummaryResult);
    }
    int skippedFiles = 0;
    int skippedFolders = 0;
    Iterator iter = skips.iterator();
    while (iter.hasNext()) {
      MergeResult skipped = (MergeResult) iter.next();
      if (skipped.getType() == MergeResult.FILE) skippedFiles++;
      else skippedFolders++;
    }
    if (skippedFiles > 0) {
      MergeSummaryResult mergeSummaryResult =
          new MergeSummaryResult(
              MergeSummaryResult.FILE,
              "Skipped files",
              Integer.toString(skippedFiles)); // $NON-NLS-1$
      mergeSummaryResults.add(mergeSummaryResult);
    }
    if (skippedFolders > 0) {
      MergeSummaryResult mergeSummaryResult =
          new MergeSummaryResult(
              MergeSummaryResult.FILE,
              "Skipped folders",
              Integer.toString(skippedFolders)); // $NON-NLS-1$
      mergeSummaryResults.add(mergeSummaryResult);
    }
    if (skippedFiles > 0 || skippedFolders > 0) refreshView();
  }

  public void logError(String message) {
    if (logging) {
      // Ignore messages with no action code.  See artf190.
      if (message.substring(0, 3).trim().length() > 0) {
        if (message.substring(3, 4).equals(" "))
          addMergeResult(
              message.substring(0, 1),
              message.substring(1, 2),
              message.substring(2, 3),
              message.substring(4),
              true); //$NON-NLS-1$
        else if (message.substring(2, 3).equals(" "))
          addMergeResult(
              message.substring(0, 1),
              message.substring(1, 2),
              " ",
              message.substring(3),
              true); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }
  }

  public void logMessage(String message) {

    if (message.indexOf("--- Merging r") != -1) { // $NON-NLS-1$
      updateRevisionRange(message);
      return;
    }
    if (message.indexOf("--- Merging") != -1) return; // $NON-NLS-1$
    if (logging) {

      if (message.startsWith("Merge info recorded for")) {
        addMergeResult(" ", "U", " ", message.substring(24), false);
        return;
      }

      // Ignore messages with no action code.  See artf190.
      if (message.substring(0, 3).trim().length() > 0) {
        if (message.substring(3, 4).equals(" "))
          addMergeResult(
              message.substring(0, 1),
              message.substring(1, 2),
              message.substring(2, 3),
              message.substring(4),
              false); //$NON-NLS-1$
        else if (message.substring(2, 3).equals(" "))
          addMergeResult(
              message.substring(0, 1),
              message.substring(1, 2),
              " ",
              message.substring(3),
              false); //$NON-NLS-1$ //$NON-NLS-2$
        if (message.startsWith("Skipped "))
          addMergeResult(
              MergeResult.ACTION_SKIP,
              " ",
              " ",
              message.substring(8),
              false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }
    }
    if (!logging) {
      if (message.indexOf("File Statistics:") != -1) { // $NON-NLS-1$
        loggingFileStatistics = true;
        return;
      }
      if (message.indexOf("Property Statistics:") != -1) { // $NON-NLS-1$
        loggingFileStatistics = false;
        loggingPropertyStatistics = true;
        return;
      }
      if (message.indexOf("Conflict Statistics:") != -1) { // $NON-NLS-1$
        loggingFileStatistics = false;
        loggingPropertyStatistics = false;
        loggingConflictStatistics = true;
        return;
      }
      if (loggingFileStatistics || loggingPropertyStatistics || loggingConflictStatistics) {
        int index = message.indexOf(":"); // $NON-NLS-1$
        if (index != -1) {
          int type;
          String category;
          if (loggingConflictStatistics) {
            if (message.startsWith("File conflicts:")) { // $NON-NLS-1$
              type = MergeSummaryResult.FILE;
              category = "Conflicts"; // $NON-NLS-1$
            } else if (message.startsWith("Property conflicts:")) { // $NON-NLS-1$
              type = MergeSummaryResult.PROPERTY;
              category = "Conflicts"; // $NON-NLS-1$
            } else {
              type = MergeSummaryResult.TREE;
              category = "Tree conflicts"; // $NON-NLS-1$
            }
          } else {
            if (loggingPropertyStatistics) type = MergeSummaryResult.PROPERTY;
            else type = MergeSummaryResult.FILE;
            category = message.substring(0, index);
          }
          String number = message.substring(index + 1).trim();
          MergeSummaryResult mergeSummaryResult = new MergeSummaryResult(type, category, number);
          mergeSummaryResults.add(mergeSummaryResult);
          refreshView();
        }
      }
    }
  }

  private void updateRevisionRange(String message) {
    int mergingIndex = message.indexOf("--- Merging r"); // $NON-NLS-1$
    if (mergingIndex == -1) return;
    int lowerStart = mergingIndex + 13;
    int blankIndex = message.indexOf(" ", lowerStart); // $NON-NLS-1$
    if (blankIndex == -1) return;
    String lower = message.substring(lowerStart, blankIndex);
    int lowerRevision = Integer.parseInt(lower);
    if (mergeOutput.getLowerRevision() == 0 || lowerRevision < mergeOutput.getLowerRevision())
      mergeOutput.setLowerRevision(lowerRevision);
    if (lowerRevision > mergeOutput.getUpperRevision()) mergeOutput.setUpperRevision(lowerRevision);
    int throughIndex = message.indexOf(" through r"); // $NON-NLS-1$
    if (throughIndex == -1) return;
    int upperStart = throughIndex + 10;
    blankIndex = message.indexOf(" ", upperStart); // $NON-NLS-1$
    if (blankIndex == -1) return;
    String upper = message.substring(upperStart, blankIndex);
    int upperRevision = Integer.parseInt(upper);
    if (mergeOutput.getUpperRevision() == 0 || upperRevision > mergeOutput.getUpperRevision())
      mergeOutput.setUpperRevision(upperRevision);
  }

  public void logRevision(long revision, String path) {}

  public void onNotify(File path, SVNNodeKind kind) {
    if (lastMergeResult != null
        && lastMergeResult
            .getPath()
            .equals(path.toString().replaceAll("\\\\", "/"))) // $NON-NLS-1$ //$NON-NLS-2$
    lastMergeResult.setType(kind.toInt());
  }

  private void addMergeResult(
      String action, String propertyAction, String treeConflictAction, String path, boolean error) {
    boolean add = true;
    if (action.equals(MergeResult.ACTION_SKIP))
      lastMergeResult =
          new SkippedMergeResult(action, propertyAction, treeConflictAction, path, error);
    else
      lastMergeResult =
          new AdaptableMergeResult(action, propertyAction, treeConflictAction, path, error);
    int index = mergeResults.indexOf(lastMergeResult);
    if (index != -1) {
      lastMergeResult = (MergeResult) mergeResults.get(index);
      lastMergeResult.setAction(action);
      lastMergeResult.setPropertyAction(propertyAction);
      lastMergeResult.setTreeConflictAction(treeConflictAction);
      lastMergeResult.setError(error);
      if (lastMergeResult.isConflicted()) {
        lastMergeResult.setConflictResolution(" "); // $NON-NLS-1$
        lastMergeResult.setPropertyResolution(" "); // $NON-NLS-1$
      }
      if (lastMergeResult.hasTreeConflict()) {
        lastMergeResult.setTreeConflictResolution(" "); // $NON-NLS-1$
      }
      add = false;
    }
    if (add) {
      mergeResults.add(lastMergeResult);
      if (lastMergeResult.getAction().equals(MergeResult.ACTION_SKIP)) {
        setType(lastMergeResult);
        skips.add(lastMergeResult);
      } else {
        if (lastMergeResult.hasTreeConflict()) setType(lastMergeResult);
      }
    }
    if (!lastMergeResult.isConflicted()
        && (conflictResolver.getTextHandling() != ISVNConflictResolver.Choice.postpone
            || conflictResolver.getBinaryHandling() != ISVNConflictResolver.Choice.postpone)) {
      updateConflictStatus(lastMergeResult);
    }
    refreshView();
  }

  private void setType(MergeResult mergeResult) {
    File file = new File(mergeResult.getPath());
    if (file.getName().indexOf(".") == -1) mergeResult.setType(MergeResult.FOLDER); // $NON-NLS-1$
    else mergeResult.setType(MergeResult.FILE);
  }

  private void updateConflictStatus(MergeResult mergeResult) {
    ConflictResolution[] conflictResolutions = conflictResolver.getConflictResolutions();
    for (int i = 0; i < conflictResolutions.length; i++) {
      if (conflictResolutions[i].getConflictDescriptor().getPath().equals(mergeResult.getPath())) {
        if (conflictResolutions[i].isResolved()) {
          mergeResult.setAction(MergeResult.ACTION_CONFLICT);
          mergeResult.setError(true);
          mergeResult.setConflictResolution(
              Integer.toString(conflictResolutions[i].getResolution()));
        }
      }
    }
  }

  private MergeResult[] getMergeResults() {
    MergeResult[] mergeResultArray = new MergeResult[mergeResults.size()];
    mergeResults.toArray(mergeResultArray);
    return mergeResultArray;
  }

  private MergeSummaryResult[] getMergeSummaryResults() {
    MergeSummaryResult[] mergeSummaryResultArray =
        new MergeSummaryResult[mergeSummaryResults.size()];
    mergeSummaryResults.toArray(mergeSummaryResultArray);
    return mergeSummaryResultArray;
  }

  public MergeOutput getMergeOutput() {
    return mergeOutput;
  }

  public void setCommand(int command) {}

  private void refreshView() {
    MergeOutput.setInProgress(true);
    mergeOutput.setMergeResults(getMergeResults());
    mergeOutput.setMergeSummaryResults(getMergeSummaryResults());
    mergeOutput.store();
    if (!viewShown) {
      viewShown = true;
      Display.getDefault()
          .asyncExec(
              new Runnable() {
                public void run() {
                  try {
                    view =
                        (MergeResultsView)
                            Activator.getDefault()
                                .getWorkbench()
                                .getActiveWorkbenchWindow()
                                .getActivePage()
                                .showView(MergeResultsView.ID);
                    view.getTreeViewer().setExpandedState(mergeOutput, true);
                  } catch (PartInitException e) {
                    Activator.handleError(e);
                  }
                }
              });
    }
    if (view != null) {
      view.refreshAsync(mergeOutput);
    }
    MergeOutput.setInProgress(false);
  }
}
