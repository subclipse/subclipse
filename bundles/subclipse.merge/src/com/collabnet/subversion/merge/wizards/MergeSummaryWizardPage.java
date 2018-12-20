/**
 * ***************************************************************************** Copyright (c) 2009
 * CollabNet. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: CollabNet - initial API and implementation
 * ****************************************************************************
 */
package com.collabnet.subversion.merge.wizards;

import com.collabnet.subversion.merge.Activator;
import com.collabnet.subversion.merge.MergeOutput;
import com.collabnet.subversion.merge.MergeSummaryResult;
import com.collabnet.subversion.merge.Messages;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class MergeSummaryWizardPage extends WizardPage {
  private MergeOutput[] mergeOutputs;
  private String resolvedConflicts = "0"; // $NON-NLS-1$
  private String skippedFiles = "0"; // $NON-NLS-1$
  private String skippedFolders = "0"; // $NON-NLS-1$
  private String fileConflicts = "0"; // $NON-NLS-1$
  private String treeConflicts = "0"; // $NON-NLS-1$
  private String resolvedTreeConflicts = "0"; // $NON-NLS-1$
  private String propertyConflicts = "0"; // $NON-NLS-1$
  private String resolvedPropertyConflicts = "0"; // $NON-NLS-1$
  private String fileMerges = "0"; // $NON-NLS-1$
  private String propertyMerges = "0"; // $NON-NLS-1$
  private String fileUpdates = "0"; // $NON-NLS-1$
  private String propertyUpdates = "0"; // $NON-NLS-1$
  private String fileDeletes = "0"; // $NON-NLS-1$
  private String fileAdds = "0"; // $NON-NLS-1$
  private String fileExisting = "0"; // $NON-NLS-1$
  private boolean resumed = false;

  public MergeSummaryWizardPage(String pageName) {
    super(
        pageName,
        Messages.MergeSummaryWizardPage_title,
        Activator.getDefault().getImageDescriptor(Activator.IMAGE_SVN));
  }

  public void createControl(Composite parent) {
    Composite outerContainer = new Composite(parent, SWT.NONE);
    GridLayout outerLayout = new GridLayout();
    outerLayout.numColumns = 1;
    outerContainer.setLayout(outerLayout);
    outerContainer.setLayoutData(
        new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

    Composite composite = new Composite(outerContainer, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    composite.setLayout(layout);
    GridData data = new GridData(GridData.FILL_BOTH);
    composite.setLayoutData(data);

    int resolvedConflictsTotal = 0;
    int skippedFilesTotal = 0;
    int skippedFoldersTotal = 0;
    int fileConflictsTotal = 0;
    int treeConflictsTotal = 0;
    int resolvedTreeConflictsTotal = 0;
    int propertyConflictsTotal = 0;
    int resolvedPropertyConflictsTotal = 0;
    int fileMergesTotal = 0;
    int propertyMergesTotal = 0;
    int fileUpdatesTotal = 0;
    int propertyUpdatesTotal = 0;
    int fileDeletesTotal = 0;
    int fileAddsTotal = 0;
    int fileExistingTotal = 0;

    for (int i = 0; i < mergeOutputs.length; i++) {
      MergeSummaryResult[] mergeSummaryResults = mergeOutputs[i].getMergeSummaryResults();
      for (int j = 0; j < mergeSummaryResults.length; j++) {
        String category = mergeSummaryResults[j].getCategory();
        int number = Integer.parseInt(mergeSummaryResults[j].getNumber());
        if (mergeSummaryResults[j].getType() == MergeSummaryResult.TREE) {
          if (category.startsWith("Resolved conflicts"))
            resolvedTreeConflictsTotal += number; // $NON-NLS-1$
          else if (category.startsWith("Tree conflicts"))
            treeConflictsTotal += number; // $NON-NLS-1$
        } else if (mergeSummaryResults[j].getType() == MergeSummaryResult.FILE) {
          if (category.startsWith("Resolved conflicts"))
            resolvedConflictsTotal += number; // $NON-NLS-1$
          else if (category.startsWith("Skipped files")) skippedFilesTotal += number; // $NON-NLS-1$
          else if (category.startsWith("Skipped folders"))
            skippedFoldersTotal += number; // $NON-NLS-1$
          else if (category.startsWith("Conflicts")) fileConflictsTotal += number; // $NON-NLS-1$
          else if (category.startsWith("Merged")) fileMergesTotal += number; // $NON-NLS-1$
          else if (category.startsWith("Updated")) fileUpdatesTotal += number; // $NON-NLS-1$
          else if (category.startsWith("Deleted")) fileDeletesTotal += number; // $NON-NLS-1$
          else if (category.startsWith("Added")) fileAddsTotal += number; // $NON-NLS-1$
          else if (category.startsWith("Existing")) fileExistingTotal += number; // $NON-NLS-1$
        } else {
          if (category.startsWith("Conflicts")) propertyConflictsTotal += number; // $NON-NLS-1$
          else if (category.startsWith("Resolved conflicts"))
            resolvedPropertyConflictsTotal += number; // $NON-NLS-1$
          else if (category.startsWith("Merged")) propertyMergesTotal += number; // $NON-NLS-1$
          else if (category.startsWith("Updated")) propertyUpdatesTotal += number; // $NON-NLS-1$
        }
      }
    }

    resolvedConflicts = Integer.toString(resolvedConflictsTotal);
    resolvedPropertyConflicts = Integer.toString(resolvedPropertyConflictsTotal);
    skippedFiles = Integer.toString(skippedFilesTotal);
    skippedFolders = Integer.toString(skippedFoldersTotal);
    fileConflicts = Integer.toString(fileConflictsTotal);
    treeConflicts = Integer.toString(treeConflictsTotal);
    resolvedTreeConflicts = Integer.toString(resolvedTreeConflictsTotal);
    propertyConflicts = Integer.toString(propertyConflictsTotal);
    fileMerges = Integer.toString(fileMergesTotal);
    propertyMerges = Integer.toString(propertyMergesTotal);
    fileUpdates = Integer.toString(fileUpdatesTotal);
    propertyUpdates = Integer.toString(propertyUpdatesTotal);
    fileDeletes = Integer.toString(fileDeletesTotal);
    fileAdds = Integer.toString(fileAddsTotal);
    fileExisting = Integer.toString(fileExistingTotal);

    if (resumed) {
      Label label = new Label(composite, SWT.WRAP);
      label.setText(Messages.MergeSummaryWizardPage_note);
      data = new GridData();
      data.widthHint = 500;
      label.setLayoutData(data);

      new Label(composite, SWT.NONE);
    }

    Group fileGroup = new Group(composite, SWT.NULL);
    fileGroup.setText(Messages.MergeSummaryWizardPage_fileStatistics);
    GridLayout fileLayout = new GridLayout();
    fileLayout.numColumns = 2;
    fileGroup.setLayout(fileLayout);
    data = new GridData(GridData.FILL_BOTH);
    fileGroup.setLayoutData(data);

    Label fileUpdateLabel = new Label(fileGroup, SWT.NONE);
    fileUpdateLabel.setText(Messages.MergeSummaryWizardPage_updated);
    data = new GridData();
    data.horizontalAlignment = GridData.END;
    fileUpdateLabel.setLayoutData(data);
    Text fileUpdateText = new Text(fileGroup, SWT.NONE);
    fileUpdateText.setEditable(false);
    data = new GridData();
    data.widthHint = 100;
    fileUpdateText.setLayoutData(data);
    fileUpdateText.setText(fileUpdates);

    Label fileAddLabel = new Label(fileGroup, SWT.NONE);
    fileAddLabel.setText(Messages.MergeSummaryWizardPage_added);
    data = new GridData();
    data.horizontalAlignment = GridData.END;
    fileAddLabel.setLayoutData(data);
    Text fileAddText = new Text(fileGroup, SWT.NONE);
    fileAddText.setEditable(false);
    data = new GridData();
    data.widthHint = 100;
    fileAddText.setLayoutData(data);
    fileAddText.setText(fileAdds);

    Label fileExistingLabel = new Label(fileGroup, SWT.NONE);
    fileExistingLabel.setText(Messages.MergeSummaryWizardPage_existing);
    data = new GridData();
    data.horizontalAlignment = GridData.END;
    fileExistingLabel.setLayoutData(data);
    Text fileExistingText = new Text(fileGroup, SWT.NONE);
    fileExistingText.setEditable(false);
    data = new GridData();
    data.widthHint = 100;
    fileExistingText.setLayoutData(data);
    fileExistingText.setText(fileExisting);

    Label fileDeleteLabel = new Label(fileGroup, SWT.NONE);
    fileDeleteLabel.setText(Messages.MergeSummaryWizardPage_deleted);
    data = new GridData();
    data.horizontalAlignment = GridData.END;
    fileDeleteLabel.setLayoutData(data);
    Text fileDeleteText = new Text(fileGroup, SWT.NONE);
    fileDeleteText.setEditable(false);
    data = new GridData();
    data.widthHint = 100;
    fileDeleteText.setLayoutData(data);
    fileDeleteText.setText(fileDeletes);

    Label fileMergeLabel = new Label(fileGroup, SWT.NONE);
    fileMergeLabel.setText(Messages.MergeSummaryWizardPage_merged);
    data = new GridData();
    data.horizontalAlignment = GridData.END;
    fileMergeLabel.setLayoutData(data);
    Text fileMergeText = new Text(fileGroup, SWT.NONE);
    fileMergeText.setEditable(false);
    data = new GridData();
    data.widthHint = 100;
    fileMergeText.setLayoutData(data);
    fileMergeText.setText(fileMerges);

    Label fileConflictLabel = new Label(fileGroup, SWT.NONE);
    fileConflictLabel.setText(Messages.MergeSummaryWizardPage_conflicts);
    data = new GridData();
    data.horizontalAlignment = GridData.END;
    fileConflictLabel.setLayoutData(data);
    Text fileConflictText = new Text(fileGroup, SWT.NONE);
    fileConflictText.setEditable(false);
    data = new GridData();
    data.widthHint = 100;
    fileConflictText.setLayoutData(data);
    fileConflictText.setText(fileConflicts);

    Label fileResolvedLabel = new Label(fileGroup, SWT.NONE);
    fileResolvedLabel.setText(Messages.MergeSummaryWizardPage_resolvedConflicts);
    data = new GridData();
    data.horizontalAlignment = GridData.END;
    fileResolvedLabel.setLayoutData(data);
    Text fileResolvedText = new Text(fileGroup, SWT.NONE);
    fileResolvedText.setEditable(false);
    data = new GridData();
    data.widthHint = 100;
    fileResolvedText.setLayoutData(data);
    fileResolvedText.setText(resolvedConflicts);

    Label skippedFilesLabel = new Label(fileGroup, SWT.NONE);
    skippedFilesLabel.setText(Messages.MergeSummaryWizardPage_skippedFiles);
    data = new GridData();
    data.horizontalAlignment = GridData.END;
    skippedFilesLabel.setLayoutData(data);
    Text skippedFilesText = new Text(fileGroup, SWT.NONE);
    skippedFilesText.setEditable(false);
    data = new GridData();
    data.widthHint = 100;
    skippedFilesText.setLayoutData(data);
    skippedFilesText.setText(skippedFiles);

    Label skippedFoldersLabel = new Label(fileGroup, SWT.NONE);
    skippedFoldersLabel.setText(Messages.MergeSummaryWizardPage_skippedFolders);
    data = new GridData();
    data.horizontalAlignment = GridData.END;
    skippedFoldersLabel.setLayoutData(data);
    Text skippedFoldersText = new Text(fileGroup, SWT.NONE);
    skippedFoldersText.setEditable(false);
    data = new GridData();
    data.widthHint = 100;
    skippedFoldersText.setLayoutData(data);
    skippedFoldersText.setText(skippedFolders);

    Group propertyGroup = new Group(composite, SWT.NULL);
    propertyGroup.setText(Messages.MergeSummaryWizardPage_propertyStatistics);
    GridLayout propertyLayout = new GridLayout();
    propertyLayout.numColumns = 2;
    propertyGroup.setLayout(propertyLayout);
    data = new GridData(GridData.FILL_BOTH);
    propertyGroup.setLayoutData(data);

    Label propertyUpdateLabel = new Label(propertyGroup, SWT.NONE);
    propertyUpdateLabel.setText(Messages.MergeSummaryWizardPage_updated);
    data = new GridData();
    data.horizontalAlignment = GridData.END;
    propertyUpdateLabel.setLayoutData(data);
    Text propertyUpdateText = new Text(propertyGroup, SWT.NONE);
    propertyUpdateText.setEditable(false);
    data = new GridData();
    data.widthHint = 100;
    propertyUpdateText.setLayoutData(data);
    propertyUpdateText.setText(propertyUpdates);

    Label propertyMergeLabel = new Label(propertyGroup, SWT.NONE);
    propertyMergeLabel.setText(Messages.MergeSummaryWizardPage_merged);
    data = new GridData();
    data.horizontalAlignment = GridData.END;
    propertyMergeLabel.setLayoutData(data);
    Text propertyMergeText = new Text(propertyGroup, SWT.NONE);
    propertyMergeText.setEditable(false);
    data = new GridData();
    data.widthHint = 100;
    propertyMergeText.setLayoutData(data);
    propertyMergeText.setText(propertyMerges);

    Label propertyConflictLabel = new Label(propertyGroup, SWT.NONE);
    propertyConflictLabel.setText(Messages.MergeSummaryWizardPage_conflicts);
    data = new GridData();
    data.horizontalAlignment = GridData.END;
    propertyConflictLabel.setLayoutData(data);
    Text propertyConflictText = new Text(propertyGroup, SWT.NONE);
    propertyConflictText.setEditable(false);
    data = new GridData();
    data.widthHint = 100;
    propertyConflictText.setLayoutData(data);
    propertyConflictText.setText(propertyConflicts);

    Label propertyResolvedLabel = new Label(propertyGroup, SWT.NONE);
    propertyResolvedLabel.setText(Messages.MergeSummaryWizardPage_resolvedConflicts);
    data = new GridData();
    data.horizontalAlignment = GridData.END;
    propertyResolvedLabel.setLayoutData(data);
    Text propertyResolvedText = new Text(propertyGroup, SWT.NONE);
    propertyResolvedText.setEditable(false);
    data = new GridData();
    data.widthHint = 100;
    propertyResolvedText.setLayoutData(data);
    propertyResolvedText.setText(resolvedPropertyConflicts);

    if (treeConflictsTotal > 0) {
      Group treeGroup = new Group(composite, SWT.NULL);
      treeGroup.setText(Messages.MergeSummaryWizardPage_treeConflictStatistics);
      GridLayout treeLayout = new GridLayout();
      treeLayout.numColumns = 2;
      treeGroup.setLayout(treeLayout);
      data = new GridData(GridData.FILL_BOTH);
      treeGroup.setLayoutData(data);

      Label treeConflictLabel = new Label(treeGroup, SWT.NONE);
      treeConflictLabel.setText(Messages.MergeSummaryWizardPage_0);
      data = new GridData();
      data.horizontalAlignment = GridData.END;
      treeConflictLabel.setLayoutData(data);
      Text treeConflictText = new Text(treeGroup, SWT.NONE);
      treeConflictText.setEditable(false);
      data = new GridData();
      data.widthHint = 100;
      treeConflictText.setLayoutData(data);
      treeConflictText.setText(treeConflicts);

      Label resolvedTreeConflictLabel = new Label(treeGroup, SWT.NONE);
      resolvedTreeConflictLabel.setText(Messages.MergeSummaryWizardPage_1);
      data = new GridData();
      data.horizontalAlignment = GridData.END;
      resolvedTreeConflictLabel.setLayoutData(data);
      Text resolvedTreeConflictText = new Text(treeGroup, SWT.NONE);
      resolvedTreeConflictText.setEditable(false);
      data = new GridData();
      data.widthHint = 100;
      resolvedTreeConflictText.setLayoutData(data);
      resolvedTreeConflictText.setText(resolvedTreeConflicts);
    }

    if (mergeOutputs.length == 1)
      setMessage(
          Messages.MergeSummaryWizardPage_messageMultiple
              + mergeOutputs[0].getResource().getFullPath().makeRelative().toOSString());
    else setMessage(Messages.MergeSummaryWizardPage_message);

    setControl(outerContainer);
  }

  public void setMergeOutputs(MergeOutput[] mergeOutputs) {
    this.mergeOutputs = mergeOutputs;
  }

  public void setResumed(boolean resumed) {
    this.resumed = resumed;
  }
}
