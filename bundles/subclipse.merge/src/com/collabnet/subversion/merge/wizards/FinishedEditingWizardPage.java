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
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;

public class FinishedEditingWizardPage extends WizardPage {
  private boolean propertyConflict;
  protected Button yesButton;
  protected Button noButton;
  private Group optionGroup;
  private Button markConflictedButton;
  private Button chooseUserVersionButton;
  private Button chooseUserVersionForConflictsButton;
  private Button chooseIncomingVersionButton;
  private Button chooseIncomingVersionForConflictsButton;
  private Button chooseBaseVersionButton;

  public FinishedEditingWizardPage(String pageName, boolean propertyConflict) {
    super(
        pageName,
        Messages.FinishedEditingWizardPage_resolve,
        Activator.getDefault().getImageDescriptor(Activator.IMAGE_SVN));
    this.propertyConflict = propertyConflict;
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

    yesButton = new Button(composite, SWT.RADIO);
    yesButton.setText(Messages.FinishedEditingWizardPage_yes);
    noButton = new Button(composite, SWT.RADIO);
    noButton.setText(Messages.FinishedEditingWizardPage_no);
    noButton.setSelection(true);

    optionGroup = new Group(composite, SWT.NONE);
    optionGroup.setText(Messages.FinishedEditingWizardPage_question);
    GridLayout optionLayout = new GridLayout();
    optionLayout.numColumns = 1;
    optionGroup.setLayout(optionLayout);
    data = new GridData(GridData.FILL_BOTH);
    optionGroup.setLayoutData(data);

    markConflictedButton = new Button(optionGroup, SWT.RADIO);
    markConflictedButton.setText(Messages.FinishedEditingWizardPage_mark);
    chooseUserVersionButton = new Button(optionGroup, SWT.RADIO);
    chooseUserVersionButton.setText(Messages.FinishedEditingWizardPage_useMine);
    if (!propertyConflict) {
      chooseUserVersionForConflictsButton = new Button(optionGroup, SWT.RADIO);
      chooseUserVersionForConflictsButton.setText(Messages.FinishedEditingWizardPage_0);
    }
    chooseIncomingVersionButton = new Button(optionGroup, SWT.RADIO);
    chooseIncomingVersionButton.setText(Messages.FinishedEditingWizardPage_useIncoming);
    if (!propertyConflict) {
      chooseIncomingVersionForConflictsButton = new Button(optionGroup, SWT.RADIO);
      chooseIncomingVersionForConflictsButton.setText(Messages.FinishedEditingWizardPage_1);
    }
    chooseBaseVersionButton = new Button(optionGroup, SWT.RADIO);
    chooseBaseVersionButton.setText(Messages.FinishedEditingWizardPage_useBase);
    markConflictedButton.setSelection(true);

    SelectionListener selectionListener =
        new SelectionAdapter() {
          public void widgetSelected(SelectionEvent e) {
            optionGroup.setVisible(noButton.getSelection());
          }
        };
    yesButton.addSelectionListener(selectionListener);
    noButton.addSelectionListener(selectionListener);

    setMessage(Messages.FinishedEditingWizardPage_confirmDone);

    setControl(outerContainer);
  }

  public int getResolution() {
    if (yesButton.getSelection()) return ISVNConflictResolver.Choice.chooseMerged;
    else {
      if (chooseUserVersionButton.getSelection()) return ISVNConflictResolver.Choice.chooseMineFull;
      else if (chooseIncomingVersionButton.getSelection())
        return ISVNConflictResolver.Choice.chooseTheirsFull;
      else if (chooseBaseVersionButton.getSelection())
        return ISVNConflictResolver.Choice.chooseBase;
      else if (chooseUserVersionForConflictsButton != null
          && chooseUserVersionForConflictsButton.getSelection())
        return ISVNConflictResolver.Choice.chooseMine;
      else if (chooseIncomingVersionForConflictsButton != null
          && chooseIncomingVersionForConflictsButton.getSelection())
        return ISVNConflictResolver.Choice.chooseTheirs;
      else return ISVNConflictResolver.Choice.postpone;
    }
  }
}
