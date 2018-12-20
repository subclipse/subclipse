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
import com.collabnet.subversion.merge.Messages;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;

public class ResolveConflictWizardPage extends WizardPage {
  private IResource[] resources;
  private Button markResolvedButton;
  private Button chooseUserVersionButton;
  private Button chooseUserVersionForConflictsButton;
  private Button chooseIncomingVersionButton;
  private Button chooseIncomingVersionForConflictsButton;
  private Button chooseBaseVersionButton;
  private IDialogSettings settings;
  private boolean textConflicts;
  private boolean propertyConflicts;
  private boolean treeConflicts;
  private static final String LAST_CHOICE = "ResolveConflictDialog.lastChoice"; // $NON-NLS-1$

  public ResolveConflictWizardPage(String pageName, IResource[] resources) {
    super(
        pageName,
        Messages.ResolveConflictWizardPage_resolveConflict,
        Activator.getDefault().getImageDescriptor(Activator.IMAGE_SVN));
    this.resources = resources;
    settings = Activator.getDefault().getDialogSettings();
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

    Label label = new Label(composite, SWT.WRAP);
    if (resources.length == 1)
      label.setText(
          Messages.ResolveConflictWizardPage_file
              + resources[0].getFullPath().makeRelative().toOSString());
    else label.setText(Messages.ResolveConflictWizardPage_multipleSelected);
    data = new GridData();
    data.widthHint = 500;
    label.setLayoutData(data);

    if (treeConflicts) {
      new Label(composite, SWT.NONE);
      Label treeLabel1 = new Label(composite, SWT.WRAP);
      if (resources.length > 1)
        treeLabel1.setText(Messages.ResolveConflictWizardPage_messageMultiple);
      else treeLabel1.setText(Messages.ResolveConflictWizardPage_message);
      data = new GridData();
      data.widthHint = 500;
      treeLabel1.setLayoutData(data);
    } else if (propertyConflicts) {
      new Label(composite, SWT.NONE);
      Label propertyLabel1 = new Label(composite, SWT.WRAP);
      if (resources.length > 1)
        propertyLabel1.setText(Messages.ResolveConflictWizardPage_messagePropertyMultiple);
      else propertyLabel1.setText(Messages.ResolveConflictWizardPage_messageProperty);
      data = new GridData();
      data.widthHint = 500;
      propertyLabel1.setLayoutData(data);
    }

    new Label(composite, SWT.NONE);

    Group conflictGroup = new Group(composite, SWT.NULL);

    conflictGroup.setText(Messages.ResolveConflictWizardPage_question);
    GridLayout conflictLayout = new GridLayout();
    conflictLayout.numColumns = 1;
    conflictGroup.setLayout(conflictLayout);
    data = new GridData(GridData.FILL_BOTH);
    conflictGroup.setLayoutData(data);

    markResolvedButton = new Button(conflictGroup, SWT.RADIO);
    markResolvedButton.setText(Messages.ResolveConflictWizardPage_conflictsResolved);
    if (treeConflicts) {
      Label treeLabel2 = new Label(conflictGroup, SWT.NONE);
      treeLabel2.setText(Messages.ResolveConflictWizardPage_doNotApplyTreeConflicts);
    } else if (propertyConflicts) {
      Label propertyLabel2 = new Label(conflictGroup, SWT.NONE);
      propertyLabel2.setText(Messages.ResolveConflictWizardPage_doNotApplyProperties);
    }
    chooseUserVersionButton = new Button(conflictGroup, SWT.RADIO);
    chooseUserVersionButton.setText(Messages.ResolveConflictWizardPage_useMine);
    if (!propertyConflicts) {
      chooseUserVersionForConflictsButton = new Button(conflictGroup, SWT.RADIO);
      chooseUserVersionForConflictsButton.setText(Messages.ResolveConflictWizardPage_0);
    }
    chooseIncomingVersionButton = new Button(conflictGroup, SWT.RADIO);
    chooseIncomingVersionButton.setText(Messages.ResolveConflictWizardPage_useIncoming);
    if (!propertyConflicts) {
      chooseIncomingVersionForConflictsButton = new Button(conflictGroup, SWT.RADIO);
      chooseIncomingVersionForConflictsButton.setText(Messages.ResolveConflictWizardPage_1);
    }
    chooseBaseVersionButton = new Button(conflictGroup, SWT.RADIO);
    chooseBaseVersionButton.setText(Messages.ResolveConflictWizardPage_useBase);

    int lastChoice = ISVNConflictResolver.Choice.chooseMerged;
    try {
      lastChoice = settings.getInt(LAST_CHOICE);
    } catch (Exception e) {
    }
    if (lastChoice == ISVNConflictResolver.Choice.chooseMerged)
      markResolvedButton.setSelection(true);
    else if (lastChoice == ISVNConflictResolver.Choice.chooseMine
        && chooseUserVersionForConflictsButton != null)
      chooseUserVersionForConflictsButton.setSelection(true);
    else if (lastChoice == ISVNConflictResolver.Choice.chooseMineFull
        && chooseUserVersionButton != null) chooseUserVersionButton.setSelection(true);
    else if (lastChoice == ISVNConflictResolver.Choice.chooseTheirs
        && chooseIncomingVersionForConflictsButton != null)
      chooseIncomingVersionForConflictsButton.setSelection(true);
    else if (lastChoice == ISVNConflictResolver.Choice.chooseTheirsFull
        && chooseIncomingVersionButton != null) chooseIncomingVersionButton.setSelection(true);
    else if (lastChoice == ISVNConflictResolver.Choice.chooseBase
        && chooseBaseVersionButton != null) chooseBaseVersionButton.setSelection(true);

    SelectionListener selectionListener =
        new SelectionAdapter() {
          public void widgetSelected(SelectionEvent e) {
            if (markResolvedButton.getSelection())
              settings.put(LAST_CHOICE, ISVNConflictResolver.Choice.chooseMerged);
            else if (chooseUserVersionButton.getSelection())
              settings.put(LAST_CHOICE, ISVNConflictResolver.Choice.chooseMineFull);
            else if (chooseUserVersionForConflictsButton != null
                && chooseUserVersionForConflictsButton.getSelection())
              settings.put(LAST_CHOICE, ISVNConflictResolver.Choice.chooseMine);
            else if (chooseIncomingVersionButton.getSelection())
              settings.put(LAST_CHOICE, ISVNConflictResolver.Choice.chooseTheirsFull);
            else if (chooseIncomingVersionForConflictsButton != null
                && chooseIncomingVersionForConflictsButton.getSelection())
              settings.put(LAST_CHOICE, ISVNConflictResolver.Choice.chooseTheirs);
            else if (chooseBaseVersionButton.getSelection())
              settings.put(LAST_CHOICE, ISVNConflictResolver.Choice.chooseBase);
          }
        };
    markResolvedButton.addSelectionListener(selectionListener);
    chooseUserVersionButton.addSelectionListener(selectionListener);
    if (chooseUserVersionForConflictsButton != null)
      chooseUserVersionForConflictsButton.addSelectionListener(selectionListener);
    chooseIncomingVersionButton.addSelectionListener(selectionListener);
    if (chooseIncomingVersionForConflictsButton != null)
      chooseIncomingVersionForConflictsButton.addSelectionListener(selectionListener);
    chooseBaseVersionButton.addSelectionListener(selectionListener);

    if (resources.length == 1)
      setMessage(
          Messages.ResolveConflictWizardPage_decide + resources[0].getName() + "."); // $NON-NLS-1$
    else setMessage(Messages.ResolveConflictWizardPage_decideMultiple);

    setControl(outerContainer);
  }

  public int getConflictResolution() {
    int resolution = ISVNConflictResolver.Choice.postpone;
    if (markResolvedButton.getSelection()) resolution = ISVNConflictResolver.Choice.chooseMerged;
    else if (chooseIncomingVersionButton != null && chooseIncomingVersionButton.getSelection())
      resolution = ISVNConflictResolver.Choice.chooseTheirsFull;
    else if (chooseIncomingVersionForConflictsButton != null
        && chooseIncomingVersionForConflictsButton.getSelection())
      resolution = ISVNConflictResolver.Choice.chooseTheirs;
    else if (chooseUserVersionButton != null && chooseUserVersionButton.getSelection())
      resolution = ISVNConflictResolver.Choice.chooseMineFull;
    else if (chooseUserVersionForConflictsButton != null
        && chooseUserVersionForConflictsButton.getSelection())
      resolution = ISVNConflictResolver.Choice.chooseMine;
    else if (chooseBaseVersionButton.getSelection())
      resolution = ISVNConflictResolver.Choice.chooseBase;
    return resolution;
  }

  public void setTextConflicts(boolean textConflicts) {
    this.textConflicts = textConflicts;
  }

  public void setPropertyConflicts(boolean propertyConflicts) {
    this.propertyConflicts = propertyConflicts;
  }

  public void setTreeConflicts(boolean treeConflicts) {
    this.treeConflicts = treeConflicts;
  }
}
