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
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class QuestionWizardPage extends WizardPage {
  private String question;

  public QuestionWizardPage(String pageName, String title, String messageText) {
    super(pageName, title, Activator.getDefault().getImageDescriptor(Activator.IMAGE_SVN));
    setMessage(messageText);
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

    Label questionLabel = new Label(composite, SWT.WRAP);
    data = new GridData();
    data.widthHint = 500;
    questionLabel.setLayoutData(data);
    questionLabel.setText(question);

    //		setMessage(messageText);

    setControl(outerContainer);
  }

  public void setQuestion(String question) {
    this.question = question;
  }
}
