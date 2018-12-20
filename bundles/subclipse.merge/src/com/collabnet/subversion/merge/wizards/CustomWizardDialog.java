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
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

public class CustomWizardDialog extends WizardDialog {
  private IDialogSettings settings;

  public CustomWizardDialog(Shell parentShell, IWizard wizard) {
    super(parentShell, wizard);
    settings = Activator.getDefault().getDialogSettings();
  }

  protected void cancelPressed() {
    saveLocation();
    super.cancelPressed();
  }

  protected void okPressed() {
    saveLocation();
    super.okPressed();
  }

  protected Point getInitialLocation(Point initialSize) {
    try {
      int x = settings.getInt(getWizard().getClass().getName() + ".location.x"); // $NON-NLS-1$
      int y = settings.getInt(getWizard().getClass().getName() + ".location.y"); // $NON-NLS-1$
      return new Point(x, y);
    } catch (NumberFormatException e) {
    }
    return super.getInitialLocation(initialSize);
  }

  protected Point getInitialSize() {
    try {
      int x = settings.getInt(getWizard().getClass().getName() + ".size.x"); // $NON-NLS-1$
      int y = settings.getInt(getWizard().getClass().getName() + ".size.y"); // $NON-NLS-1$
      return new Point(x, y);
    } catch (NumberFormatException e) {
    }
    return super.getInitialSize();
  }

  protected void saveLocation() {
    int x = getShell().getLocation().x;
    int y = getShell().getLocation().y;
    settings.put(getWizard().getClass().getName() + ".location.x", x); // $NON-NLS-1$
    settings.put(getWizard().getClass().getName() + ".location.y", y); // $NON-NLS-1$
    x = getShell().getSize().x;
    y = getShell().getSize().y;
    settings.put(getWizard().getClass().getName() + ".size.x", x); // $NON-NLS-1$
    settings.put(getWizard().getClass().getName() + ".size.y", y); // $NON-NLS-1$
  }
}
