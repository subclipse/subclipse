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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;

public abstract class MergeWizardWarningPage extends WizardPage {
  private boolean pageShown;

  public MergeWizardWarningPage(String pageName, String title, ImageDescriptor titleImage) {
    super(pageName, title, titleImage);
    setPageComplete(false);
  }

  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) pageShown = true;
  }

  public boolean isPageShown() {
    return pageShown;
  }

  public void setPageShown(boolean pageShown) {
    this.pageShown = pageShown;
  }

  public abstract void performChecks(boolean refreshPage);

  public abstract boolean hasWarnings();
}
