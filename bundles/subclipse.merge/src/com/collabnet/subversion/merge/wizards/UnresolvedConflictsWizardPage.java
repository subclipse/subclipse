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
import com.collabnet.subversion.merge.Messages;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class UnresolvedConflictsWizardPage extends WizardPage {
  private MergeOutput mergeOutput;

  public UnresolvedConflictsWizardPage(String pageName) {
    super(
        pageName,
        Messages.UnresolvedConflictsWizardPage_title,
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

    Label label = new Label(composite, SWT.WRAP);
    label.setText(
        Messages.UnresolvedConflictsWizardPage_text
            + mergeOutput.getResource().getFullPath().makeRelative().toOSString()
            + "\n\n"
            + Messages.UnresolvedConflictsWizardPage_text2); // $NON-NLS-1$
    data = new GridData();
    data.widthHint = 500;
    label.setLayoutData(data);

    setMessage(Messages.UnresolvedConflictsWizardPage_unresolvedMessage);

    setControl(outerContainer);
  }

  public void setMergeOutput(MergeOutput mergeOutput) {
    this.mergeOutput = mergeOutput;
  }
}
