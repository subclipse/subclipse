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
import com.collabnet.subversion.merge.IMergeInputProvider;
import com.collabnet.subversion.merge.Messages;
import com.collabnet.subversion.merge.SVNConflictResolver;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.tigris.subversion.subclipse.ui.DepthComboHelper;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;

public class MergeWizardLastPage extends WizardPage {
  private Button ignoreButton;
  private Button forceButton;
  private Label depthLabel;
  private Combo depthCombo;

  private Button textConflictPromptButton;
  private Button textConflictMarkButton;

  private Button propertyConflictPromptButton;
  private Button propertyConflictMarkButton;

  private Button binaryConflictPromptButton;
  private Button binaryConflictMarkButton;
  private Button binaryConflictUserButton;
  private Button binaryConflictIncomingButton;

  private Button treeConflictPromptButton;
  private Button treeConflictMarkButton;
  private Button treeConflictResolveButton;

  private IDialogSettings settings;

  private static final String LAST_TEXT_CONFLICT_CHOICE =
      "MergeWizardMainPage.lastTextConflictChoice"; //$NON-NLS-1$
  private static final String LAST_PROPERTY_CONFLICT_CHOICE =
      "MergeWizardMainPage.lastPropertyConflictChoice"; //$NON-NLS-1$
  private static final String LAST_BINARY_CONFLICT_CHOICE =
      "MergeWizardMainPage.lastBinaryConflictChoice"; //$NON-NLS-1$
  private static final String LAST_TREE_CONFLICT_CHOICE =
      "MergeWizardMainPage.lastTreeConflictChoice"; //$NON-NLS-1$

  public MergeWizardLastPage(String pageName, String title, ImageDescriptor titleImage) {
    super(pageName, title, titleImage);
    settings = Activator.getDefault().getDialogSettings();
  }

  public void createControl(Composite parent) {
    Composite outerContainer = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    outerContainer.setLayout(layout);
    outerContainer.setLayoutData(
        new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

    Group conflictGroup = new Group(outerContainer, SWT.NONE);
    conflictGroup.setText(Messages.MergeWizardLastPage_conflictHandling);
    GridLayout conflictLayout = new GridLayout();
    conflictLayout.numColumns = 1;
    conflictGroup.setLayout(conflictLayout);
    conflictGroup.setLayoutData(
        new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

    Group textGroup = new Group(conflictGroup, SWT.NONE);
    textGroup.setText(Messages.MergeWizardLastPage_textFiles);
    GridLayout textLayout = new GridLayout();
    textLayout.numColumns = 1;
    textGroup.setLayout(textLayout);
    textGroup.setLayoutData(
        new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

    textConflictPromptButton = new Button(textGroup, SWT.RADIO);
    textConflictPromptButton.setText(Messages.MergeWizardLastPage_prompt);
    textConflictMarkButton = new Button(textGroup, SWT.RADIO);
    textConflictMarkButton.setText(Messages.MergeWizardLastPage_mark);

    Group binaryGroup = new Group(conflictGroup, SWT.NONE);
    binaryGroup.setText(Messages.MergeWizardLastPage_binaryFiles);
    GridLayout binaryLayout = new GridLayout();
    binaryLayout.numColumns = 1;
    binaryGroup.setLayout(binaryLayout);
    binaryGroup.setLayoutData(
        new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

    binaryConflictPromptButton = new Button(binaryGroup, SWT.RADIO);
    binaryConflictPromptButton.setText(Messages.MergeWizardLastPage_prompt2);
    binaryConflictMarkButton = new Button(binaryGroup, SWT.RADIO);
    binaryConflictMarkButton.setText(Messages.MergeWizardLastPage_mark2);
    binaryConflictUserButton = new Button(binaryGroup, SWT.RADIO);
    binaryConflictUserButton.setText(Messages.MergeWizardLastPage_useMine);
    binaryConflictIncomingButton = new Button(binaryGroup, SWT.RADIO);
    binaryConflictIncomingButton.setText(Messages.MergeWizardLastPage_useIncoming);

    Group propertyGroup = new Group(conflictGroup, SWT.NONE);
    propertyGroup.setText(Messages.MergeWizardLastPage_propertyConflicts);
    GridLayout propertyLayout = new GridLayout();
    propertyLayout.numColumns = 1;
    propertyGroup.setLayout(propertyLayout);
    propertyGroup.setLayoutData(
        new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

    propertyConflictPromptButton = new Button(propertyGroup, SWT.RADIO);
    propertyConflictPromptButton.setText(Messages.MergeWizardLastPage_prompt3);
    propertyConflictMarkButton = new Button(propertyGroup, SWT.RADIO);
    propertyConflictMarkButton.setText(Messages.MergeWizardLastPage_mark3);

    Group treeConflictGroup = new Group(conflictGroup, SWT.NONE);
    treeConflictGroup.setText(Messages.MergeWizardLastPage_0);
    GridLayout treeConflictLayout = new GridLayout();
    treeConflictLayout.numColumns = 1;
    treeConflictGroup.setLayout(treeConflictLayout);
    treeConflictGroup.setLayoutData(
        new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

    treeConflictPromptButton = new Button(treeConflictGroup, SWT.RADIO);
    treeConflictPromptButton.setText(Messages.MergeWizardLastPage_1);
    treeConflictMarkButton = new Button(treeConflictGroup, SWT.RADIO);
    treeConflictMarkButton.setText(Messages.MergeWizardLastPage_2);
    treeConflictResolveButton = new Button(treeConflictGroup, SWT.RADIO);
    treeConflictResolveButton.setText(Messages.MergeWizardLastPage_3);

    Group optionsGroup = new Group(outerContainer, SWT.NONE);
    optionsGroup.setText(Messages.MergeWizardLastPage_mergeOptions);
    GridLayout optionsLayout = new GridLayout();
    optionsLayout.numColumns = 2;
    optionsGroup.setLayout(optionsLayout);
    optionsGroup.setLayoutData(
        new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

    ignoreButton = new Button(optionsGroup, SWT.CHECK);
    ignoreButton.setText(Messages.MergeWizardLastPage_ignoreAncestry);
    GridData data = new GridData();
    data.horizontalSpan = 2;
    ignoreButton.setLayoutData(data);

    forceButton = new Button(optionsGroup, SWT.CHECK);
    forceButton.setText(Messages.MergeWizardLastPage_allUnversionedObstructions);
    data = new GridData();
    data.horizontalSpan = 2;
    forceButton.setLayoutData(data);

    depthLabel = new Label(optionsGroup, SWT.NONE);
    depthLabel.setText(Messages.MergeWizardLastPage_depth);
    depthCombo = new Combo(optionsGroup, SWT.READ_ONLY);
    DepthComboHelper.addDepths(depthCombo, true, ISVNUIConstants.DEPTH_UNKNOWN);

    SelectionListener conflictSelectionListener =
        new SelectionAdapter() {
          public void widgetSelected(SelectionEvent e) {
            if (textConflictMarkButton.getSelection())
              settings.put(LAST_TEXT_CONFLICT_CHOICE, ISVNConflictResolver.Choice.postpone);
            else settings.put(LAST_TEXT_CONFLICT_CHOICE, ISVNConflictResolver.Choice.chooseMerged);
            if (propertyConflictMarkButton.getSelection())
              settings.put(LAST_PROPERTY_CONFLICT_CHOICE, ISVNConflictResolver.Choice.postpone);
            else
              settings.put(LAST_PROPERTY_CONFLICT_CHOICE, ISVNConflictResolver.Choice.chooseMerged);
            if (binaryConflictIncomingButton.getSelection())
              settings.put(
                  LAST_BINARY_CONFLICT_CHOICE, ISVNConflictResolver.Choice.chooseTheirsFull);
            else if (binaryConflictUserButton.getSelection())
              settings.put(LAST_BINARY_CONFLICT_CHOICE, ISVNConflictResolver.Choice.chooseMineFull);
            else if (binaryConflictMarkButton.getSelection())
              settings.put(LAST_BINARY_CONFLICT_CHOICE, ISVNConflictResolver.Choice.postpone);
            else
              settings.put(LAST_BINARY_CONFLICT_CHOICE, ISVNConflictResolver.Choice.chooseMerged);
            if (treeConflictPromptButton.getSelection())
              settings.put(LAST_TREE_CONFLICT_CHOICE, SVNConflictResolver.PROMPT);
            else if (treeConflictResolveButton.getSelection())
              settings.put(LAST_TREE_CONFLICT_CHOICE, ISVNConflictResolver.Choice.chooseMerged);
            else settings.put(LAST_TREE_CONFLICT_CHOICE, ISVNConflictResolver.Choice.postpone);
          }
        };

    textConflictMarkButton.addSelectionListener(conflictSelectionListener);
    textConflictPromptButton.addSelectionListener(conflictSelectionListener);
    propertyConflictMarkButton.addSelectionListener(conflictSelectionListener);
    propertyConflictPromptButton.addSelectionListener(conflictSelectionListener);
    binaryConflictIncomingButton.addSelectionListener(conflictSelectionListener);
    binaryConflictUserButton.addSelectionListener(conflictSelectionListener);
    binaryConflictMarkButton.addSelectionListener(conflictSelectionListener);
    binaryConflictPromptButton.addSelectionListener(conflictSelectionListener);
    treeConflictMarkButton.addSelectionListener(conflictSelectionListener);
    treeConflictPromptButton.addSelectionListener(conflictSelectionListener);
    treeConflictResolveButton.addSelectionListener(conflictSelectionListener);

    initializeSelections();

    setPageComplete(true);

    setMessage(Messages.MergeWizardLastPage_message);

    setControl(outerContainer);
  }

  public boolean isIgnore() {
    return ignoreButton.getSelection();
  }

  public boolean isForce() {
    return forceButton.getSelection();
  }

  public int getDepth() {
    return DepthComboHelper.getDepth(depthCombo);
  }

  private void initializeSelections() {
    int lastTextConflictChoice = ISVNConflictResolver.Choice.chooseMerged;
    try {
      lastTextConflictChoice = settings.getInt(LAST_TEXT_CONFLICT_CHOICE);
    } catch (Exception e) {
    }

    switch (lastTextConflictChoice) {
      case ISVNConflictResolver.Choice.chooseMerged:
        textConflictPromptButton.setSelection(true);
        break;
      case ISVNConflictResolver.Choice.postpone:
        textConflictMarkButton.setSelection(true);
        break;
      default:
        textConflictPromptButton.setSelection(true);
        break;
    }

    int lastPropertyConflictChoice = lastTextConflictChoice;
    try {
      lastPropertyConflictChoice = settings.getInt(LAST_PROPERTY_CONFLICT_CHOICE);
    } catch (Exception e) {
    }

    switch (lastPropertyConflictChoice) {
      case ISVNConflictResolver.Choice.chooseMerged:
        propertyConflictPromptButton.setSelection(true);
        break;
      case ISVNConflictResolver.Choice.postpone:
        propertyConflictMarkButton.setSelection(true);
        break;
      default:
        propertyConflictPromptButton.setSelection(true);
        break;
    }

    int lastBinaryConflictChoice = ISVNConflictResolver.Choice.chooseMerged;
    try {
      lastBinaryConflictChoice = settings.getInt(LAST_BINARY_CONFLICT_CHOICE);
    } catch (Exception e) {
    }

    switch (lastBinaryConflictChoice) {
      case ISVNConflictResolver.Choice.chooseMerged:
        binaryConflictPromptButton.setSelection(true);
        break;
      case ISVNConflictResolver.Choice.postpone:
        binaryConflictMarkButton.setSelection(true);
        break;
      case ISVNConflictResolver.Choice.chooseMineFull:
        binaryConflictUserButton.setSelection(true);
        break;
      case ISVNConflictResolver.Choice.chooseTheirsFull:
        binaryConflictIncomingButton.setSelection(true);
        break;
      default:
        binaryConflictPromptButton.setSelection(true);
        break;
    }

    int lastTreeConflictChoice = ISVNConflictResolver.Choice.postpone;
    try {
      lastTreeConflictChoice = settings.getInt(LAST_TREE_CONFLICT_CHOICE);
    } catch (Exception e) {
    }

    switch (lastTreeConflictChoice) {
      case SVNConflictResolver.PROMPT:
        treeConflictPromptButton.setSelection(true);
        break;
      case ISVNConflictResolver.Choice.chooseMerged:
        treeConflictResolveButton.setSelection(true);
        break;
      default:
        treeConflictMarkButton.setSelection(true);
        break;
    }
  }

  public int getTextConflictHandling() {
    if (textConflictMarkButton.getSelection()) return ISVNConflictResolver.Choice.postpone;
    else return ISVNConflictResolver.Choice.chooseMerged;
  }

  public int getBinaryConflictHandling() {
    if (binaryConflictIncomingButton.getSelection())
      return ISVNConflictResolver.Choice.chooseTheirsFull;
    else if (binaryConflictUserButton.getSelection())
      return ISVNConflictResolver.Choice.chooseMineFull;
    else if (binaryConflictMarkButton.getSelection()) return ISVNConflictResolver.Choice.postpone;
    else return ISVNConflictResolver.Choice.chooseMerged;
  }

  public int getPropertyConflictHandling() {
    if (propertyConflictMarkButton.getSelection()) return ISVNConflictResolver.Choice.postpone;
    else return ISVNConflictResolver.Choice.chooseMerged;
  }

  public int getTreeConflictHandling() {
    if (treeConflictMarkButton.getSelection()) return ISVNConflictResolver.Choice.postpone;
    else if (treeConflictResolveButton.getSelection())
      return ISVNConflictResolver.Choice.chooseMerged;
    else return SVNConflictResolver.PROMPT;
  }

  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      IMergeInputProvider selectedMergeInputProvider =
          ((MergeWizard) getWizard()).getSelectedMergeInputProvider();
      ignoreButton.setEnabled(!selectedMergeInputProvider.hideIgnoreAncestry());
      forceButton.setEnabled(!selectedMergeInputProvider.hideForce());
      depthLabel.setEnabled(!selectedMergeInputProvider.hideDepth());
      depthCombo.setEnabled(!selectedMergeInputProvider.hideDepth());
    }
  }
}
