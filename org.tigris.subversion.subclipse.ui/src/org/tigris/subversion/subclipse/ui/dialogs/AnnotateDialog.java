/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.dialogs;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.ShowAnnotationOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class AnnotateDialog extends TrayDialog {
  private IWorkbenchPart targetPart;
  private ISVNRemoteFile remoteFile;
  private Text fromRevisionText;
  private Button fromLogButton;
  private Button headButton;
  private Button revisionButton;
  private Text toRevisionText;
  private Button toLogButton;
  private Button okButton;
  private boolean success;
  
  private boolean includeMergedRevisions = true;

  public AnnotateDialog(Shell parentShell, IWorkbenchPart targetPart, ISVNRemoteFile remoteFile) {
    super(parentShell);
    setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.MAX | SWT.RESIZE | getDefaultOrientation());
    this.remoteFile = remoteFile;
    this.targetPart = targetPart;
  }
  
  protected Control createDialogArea(Composite parent) {
    getShell().setText(Policy.bind("AnnotateDialog.title")); //$NON-NLS-1$
    Composite composite = new Composite(parent, SWT.NULL);
    GridLayout gridLayout = new GridLayout(2,false);
    gridLayout.marginHeight = 10;
    gridLayout.marginWidth = 10;
    composite.setLayout(gridLayout);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));
    
    Label urlLabel = new Label(composite, SWT.NONE);
    urlLabel.setLayoutData(new GridData());
    urlLabel.setText(Policy.bind("AnnotateDialog.url")); //$NON-NLS-1$
    
    Text urlText = new Text(composite, SWT.NONE);
    GridData urlTextData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    urlTextData.widthHint = 600;
    urlText.setLayoutData(urlTextData);
    urlText.setEditable(false);
    urlText.setText(remoteFile.getUrl().toString());
    
    Group fromGroup = new Group(composite, SWT.NULL);
    fromGroup.setText(Policy.bind("AnnotateDialog.from")); //$NON-NLS-1$
    GridLayout fromLayout = new GridLayout();
    fromLayout.numColumns = 3;
    fromGroup.setLayout(fromLayout);
    fromGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
    
    Label fromRevisionLabel = new Label(fromGroup, SWT.NONE);
    fromRevisionLabel.setText(Policy.bind("AnnotateDialog.revision")); //$NON-NLS-1$
    fromRevisionText = new Text(fromGroup, SWT.BORDER);
    fromRevisionText.setLayoutData(new GridData(40, SWT.DEFAULT));
    fromRevisionText.setText("1"); //$NON-NLS-1$
    
    fromLogButton = new Button(fromGroup, SWT.PUSH);
    fromLogButton.setText(Policy.bind("AnnotateDialog.showLog")); //$NON-NLS-1$
    fromLogButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog(e.getSource());
            }
    });
    
    Group toGroup = new Group(composite, SWT.NULL);
    toGroup.setText(Policy.bind("AnnotateDialog.to")); //$NON-NLS-1$
    toGroup.setLayout(new GridLayout(3, false));
    toGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
    
    headButton = new Button(toGroup, SWT.RADIO);
    headButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.head")); //$NON-NLS-1$
    headButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 3, 1));
    
    revisionButton = new Button(toGroup, SWT.RADIO);
    revisionButton.setText(Policy.bind("AnnotateDialog.toRevision")); //$NON-NLS-1$
    
    headButton.setSelection(true);
    
    toRevisionText = new Text(toGroup, SWT.BORDER);
    toRevisionText.setLayoutData(new GridData(40, SWT.DEFAULT));
    toRevisionText.setEnabled(false);
    
    toLogButton = new Button(toGroup, SWT.PUSH);
    toLogButton.setText(Policy.bind("AnnotateDialog.showToLog")); //$NON-NLS-1$
    toLogButton.setEnabled(false);
    toLogButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog(e.getSource());
            }
    });     
    
    ModifyListener modifyListener = new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        setOkButtonStatus();
      }   
    };
    
    fromRevisionText.addModifyListener(modifyListener);
    toRevisionText.addModifyListener(modifyListener);
    
    SelectionListener selectionListener = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        toRevisionText.setEnabled(revisionButton.getSelection());
        toLogButton.setEnabled(revisionButton.getSelection());
        if (revisionButton.getSelection()) {
          toRevisionText.selectAll();
          toRevisionText.setFocus();
        }
        setOkButtonStatus();
      }
    };
    
    headButton.addSelectionListener(selectionListener);
    revisionButton.addSelectionListener(selectionListener);
    
    // set F1 help
    PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.ANNOTATE_DIALOG);  

    fromRevisionText.selectAll();
    fromRevisionText.setFocus();
    
    FocusListener focusListener = new FocusAdapter() {
      public void focusGained(FocusEvent e) {
        ((Text)e.getSource()).selectAll();
      }
      public void focusLost(FocusEvent e) {
        ((Text)e.getSource()).setText(((Text)e.getSource()).getText());
      }         
    };
    fromRevisionText.addFocusListener(focusListener);
    toRevisionText.addFocusListener(focusListener); 
    
    return composite;
  }
  
  protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        Button button = super.createButton(parent, id, label, defaultButton);
    if (id == IDialogConstants.OK_ID) {
      okButton = button; 
    }
        return button;
    }
  
  protected void okPressed() {
    success = true;
    BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
      public void run() {
        try {
          int fromRevisionInt = Integer.parseInt(fromRevisionText.getText().trim());
          long fromRevisionLong = fromRevisionInt;
          SVNRevision fromRevision = new SVNRevision.Number(fromRevisionLong);
          SVNRevision toRevision = null;
          if (headButton.getSelection()) toRevision = SVNRevision.HEAD;
          else {
            int toRevisionInt = Integer.parseInt(toRevisionText.getText().trim());
            long toRevisionLong = toRevisionInt;
            toRevision = new SVNRevision.Number(toRevisionLong);
          }

          new ShowAnnotationOperation(targetPart, remoteFile, fromRevision, toRevision, includeMergedRevisions).run();
        } catch (Exception e) {
          MessageDialog.openError(getShell(), Policy.bind("AnnotateDialog.title"), e.getMessage());
          success = false;
        }
      }     
    });
    if (!success) return;
    super.okPressed();
  }

  private void setOkButtonStatus() {
    boolean canFinish = true;
    if (fromRevisionText.getText().trim().length() == 0) canFinish = false;
    if (revisionButton.getSelection() && toRevisionText.getText().trim().length() == 0) canFinish = false;
    okButton.setEnabled(canFinish);
  }
  
  private void showLog(Object sourceButton) {
    HistoryDialog dialog = new HistoryDialog(getShell(), remoteFile);
    if (dialog.open() == HistoryDialog.CANCEL) return;
        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
        if (selectedEntries.length == 0) return;
        if (sourceButton == fromLogButton) fromRevisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));
        else toRevisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));
        setOkButtonStatus();
  }

}
