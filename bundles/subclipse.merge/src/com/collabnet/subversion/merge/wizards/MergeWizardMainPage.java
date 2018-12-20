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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

public class MergeWizardMainPage extends WizardPage {
  private Composite outerContainer;
  private Group descriptionGroup;
  private Label descriptionImage;
  private Label descriptionLabel;
  private Button bestPracticesButton;
  private IMergeInputProvider[] mergeInputProviders;
  private IMergeInputProvider selectedMergeInputProvider;
  private IDialogSettings settings;

  private static final String LAST_INPUT_PROVIDER =
      "MergeWizardMainPage.lastInputProvider"; //$NON-NLS-1$

  public MergeWizardMainPage(
      String pageName,
      String title,
      ImageDescriptor titleImage,
      IMergeInputProvider[] mergeInputProviders) {
    super(pageName, title, titleImage);
    this.mergeInputProviders = mergeInputProviders;
    setPageComplete(true);
    settings = Activator.getDefault().getDialogSettings();
  }

  public void createControl(Composite parent) {
    outerContainer = new Composite(parent, SWT.NONE);
    outerContainer.setLayout(new GridLayout());
    outerContainer.setLayoutData(
        new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

    Group inputGroup = new Group(outerContainer, SWT.NONE);
    inputGroup.setText(Messages.MergeWizardMainPage_mergeInput);
    GridLayout inputLayout = new GridLayout();
    inputLayout.numColumns = 1;
    inputGroup.setLayout(inputLayout);
    inputGroup.setLayoutData(
        new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

    getInputProviders(inputGroup);

    createDescriptionArea();

    bestPracticesButton = new Button(outerContainer, SWT.CHECK);
    bestPracticesButton.setText(Messages.MergeWizardMainPage_bestPractice);
    bestPracticesButton.setSelection(true);
    bestPracticesButton.setVisible(selectedMergeInputProvider.showBestPracticesPage());

    setMessage(Messages.MergeWizardMainPage_specifyType);

    setControl(outerContainer);
  }

  private void createDescriptionArea() {
    boolean needsRedraw = descriptionGroup != null;
    if (descriptionGroup == null) {
      descriptionGroup = new Group(outerContainer, SWT.NONE);
      GridLayout descriptionLayout = new GridLayout();
      descriptionLayout.numColumns = 1;
      descriptionGroup.setLayout(descriptionLayout);
      descriptionGroup.setLayoutData(
          new GridData(
              GridData.GRAB_HORIZONTAL
                  | GridData.HORIZONTAL_ALIGN_FILL
                  | GridData.GRAB_VERTICAL
                  | GridData.VERTICAL_ALIGN_FILL));
    }
    descriptionGroup.setText(selectedMergeInputProvider.getText());
    if (descriptionLabel != null) descriptionLabel.dispose();
    if (descriptionImage != null) descriptionImage.dispose();
    if (selectedMergeInputProvider.getImage() != null) {
      descriptionImage = new Label(descriptionGroup, SWT.NONE);
      GridData data = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
      descriptionImage.setLayoutData(data);
      descriptionImage.setImage(selectedMergeInputProvider.getImage());
    }
    descriptionLabel = new Label(descriptionGroup, SWT.WRAP);
    GridData data =
        new GridData(
            GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_VERTICAL
                | GridData.VERTICAL_ALIGN_FILL);
    data.widthHint = 500;
    descriptionLabel.setLayoutData(data);
    descriptionLabel.setText(selectedMergeInputProvider.getDescription());
    if (needsRedraw) {
      descriptionGroup.layout(true);
      descriptionGroup.redraw();
    }
  }

  private void getInputProviders(Group inputGroup) {
    final MergeWizard mergeWizard = (MergeWizard) getWizard();
    SelectionListener selectionListener =
        new SelectionAdapter() {
          public void widgetSelected(SelectionEvent e) {
            Button button = (Button) e.getSource();
            Integer integer = (Integer) button.getData();
            selectedMergeInputProvider = mergeInputProviders[integer.intValue()];
            settings.put(LAST_INPUT_PROVIDER, selectedMergeInputProvider.getText());
            createDescriptionArea();
            bestPracticesButton.setVisible(selectedMergeInputProvider.showBestPracticesPage());
            boolean needsChecks = mergeWizard.getBestPracticesPage().needsChecks();
            mergeWizard.getBestPracticesPage().setNeedsChecks(false);
            setPageComplete(true);
            mergeWizard.getBestPracticesPage().setNeedsChecks(needsChecks);
          }
        };
    String lastInputProvider = settings.get(LAST_INPUT_PROVIDER);
    for (int i = 0; i < mergeInputProviders.length; i++) {
      Button inputButton = new Button(inputGroup, SWT.RADIO);
      inputButton.setText(mergeInputProviders[i].getText()); // $NON-NLS-1$
      inputButton.setData(new Integer(i));
      if (!mergeInputProviders[i].enabledForMultipleSelection()
          && mergeWizard.getResources().length > 1) {
        inputButton.setEnabled(false);
      } else {
        if (lastInputProvider != null
            && mergeInputProviders[i].getText().equals(lastInputProvider)) {
          inputButton.setSelection(true);
          selectedMergeInputProvider = mergeInputProviders[i];
        }
        if (lastInputProvider == null && i == 0) {
          inputButton.setSelection(true);
          selectedMergeInputProvider = mergeInputProviders[i];
        }
      }
      inputButton.addSelectionListener(selectionListener);
    }
    if (selectedMergeInputProvider == null) selectedMergeInputProvider = mergeInputProviders[0];
  }

  public IMergeInputProvider getSelectedMergeInputProvider() {
    return selectedMergeInputProvider;
  }

  public boolean showBestPracticesPage() {
    return selectedMergeInputProvider.showBestPracticesPage() && bestPracticesButton.getSelection();
  }
}
