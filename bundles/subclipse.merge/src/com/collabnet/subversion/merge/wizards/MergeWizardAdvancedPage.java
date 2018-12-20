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
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.SVNUrlWithPegRevision;
import org.tigris.subversion.subclipse.ui.dialogs.ChooseUrlDialog;
import org.tigris.subversion.subclipse.ui.dialogs.HistoryDialog;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class MergeWizardAdvancedPage extends WizardPage {
  private Combo fromCombo;
  private Button selectFromButton;
  private Button fromHeadButton;
  private Button fromRevisionButton;
  private Text fromRevisionText;
  private Button selectFromRevisionButton;
  private Button useFromButton;
  private Combo toCombo;
  private Button selectToButton;
  private Button toHeadButton;
  private Button toRevisionButton;
  private Text toRevisionText;
  private Button selectToRevisionButton;

  private IResource resource;
  private IResource[] resources;
  private ISVNLocalResource svnResource;
  private SVNUrl fromUrl;
  private SVNRevision fromRevision;
  private SVNUrl toUrl;
  private SVNRevision toRevision;

  private String[] urlStrings;
  private String commonRoot;
  private MergeResource[] mergeResources;

  private String[] mergeInfoPaths;
  private String repositoryLocation;
  private boolean combosInitialized;

  private Table fromTable;
  private TableViewer fromViewer;

  private Label toResourcesLabel;
  private Table toTable;
  private TableViewer toViewer;

  private String[] columnHeaders = {Messages.MergeWizardAdvancedPage_resource};
  private ColumnLayoutData columnLayouts[] = {new ColumnWeightData(100, 100, true)};

  public MergeWizardAdvancedPage(String pageName, String title, ImageDescriptor titleImage) {
    super(pageName, title, titleImage);
  }

  public void createControl(Composite parent) {
    MergeWizard wizard = (MergeWizard) getWizard();
    resource = wizard.getResource();
    resources = wizard.getResources();

    if (resource != null) svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);

    Composite outerContainer = new Composite(parent, SWT.NONE);
    outerContainer.setLayout(new GridLayout());
    outerContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

    Group fromGroup = new Group(outerContainer, SWT.NONE);
    fromGroup.setText(Messages.MergeWizardAdvancedPage_from);
    GridLayout fromLayout = new GridLayout();
    fromLayout.numColumns = 5;
    fromGroup.setLayout(fromLayout);
    fromGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

    fromCombo = new Combo(fromGroup, SWT.BORDER);
    GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
    data.horizontalSpan = 4;
    fromCombo.setLayoutData(data);

    if (resources != null) {
      commonRoot = getCommonRoot();

      if (commonRoot != null) fromCombo.setText(commonRoot);
    }

    selectFromButton = new Button(fromGroup, SWT.PUSH);
    selectFromButton.setText(Messages.MergeWizardAdvancedPage_select);

    if (resources != null && resources.length > 1) {
      Label label = new Label(fromGroup, SWT.NONE);
      label.setText(Messages.MergeWizardAdvancedPage_resources);
      data = new GridData();
      data.horizontalSpan = 5;
      label.setLayoutData(data);

      fromTable = new Table(fromGroup, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
      fromTable.setLinesVisible(false);
      fromTable.setHeaderVisible(false);
      data = new GridData(GridData.FILL_HORIZONTAL);
      data.heightHint = 75;
      data.horizontalSpan = 5;
      fromTable.setLayoutData(data);
      TableLayout tableLayout = new TableLayout();
      fromTable.setLayout(tableLayout);
      fromViewer = new TableViewer(fromTable);
      fromViewer.setContentProvider(new MergeContentProvider());
      ILabelDecorator decorator =
          PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
      fromViewer.setLabelProvider(
          new TableDecoratingLabelProvider(
              new MergeLabelProvider(MergeLabelProvider.FROM), decorator));
      for (int i = 0; i < columnHeaders.length; i++) {
        tableLayout.addColumnData(columnLayouts[i]);
        TableColumn tc = new TableColumn(fromTable, SWT.NONE, i);
        tc.setResizable(columnLayouts[i].resizable);
        tc.setText(columnHeaders[i]);
      }
      fromViewer.setInput(this);
    }

    fromHeadButton = new Button(fromGroup, SWT.RADIO);
    fromHeadButton.setText(Messages.MergeWizardAdvancedPage_headRevision);

    fromRevisionButton = new Button(fromGroup, SWT.RADIO);
    fromRevisionButton.setText(Messages.MergeWizardAdvancedPage_revision);

    fromRevisionButton.setSelection(true);

    fromRevisionText = new Text(fromGroup, SWT.BORDER);
    data = new GridData();
    data.widthHint = 40;
    fromRevisionText.setLayoutData(data);

    selectFromRevisionButton = new Button(fromGroup, SWT.PUSH);
    selectFromRevisionButton.setText(Messages.MergeWizardAdvancedPage_select2);

    Group toGroup = new Group(outerContainer, SWT.NONE);
    toGroup.setText(Messages.MergeWizardAdvancedPage_to);
    GridLayout toLayout = new GridLayout();
    toLayout.numColumns = 5;
    toGroup.setLayout(toLayout);
    toGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

    useFromButton = new Button(toGroup, SWT.CHECK);
    useFromButton.setText(Messages.MergeWizardAdvancedPage_useFrom);
    data = new GridData();
    data.horizontalSpan = 5;
    useFromButton.setLayoutData(data);
    useFromButton.setSelection(true);

    toCombo = new Combo(toGroup, SWT.BORDER);
    data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
    data.horizontalSpan = 4;
    toCombo.setLayoutData(data);
    toCombo.setVisible(false);
    toCombo.setText(fromCombo.getText());

    selectToButton = new Button(toGroup, SWT.PUSH);
    selectToButton.setText(Messages.MergeWizardAdvancedPage_select3);
    selectToButton.setVisible(false);

    if (resources != null && resources.length > 1) {
      toResourcesLabel = new Label(toGroup, SWT.NONE);
      toResourcesLabel.setText(Messages.MergeWizardAdvancedPage_resources);
      data = new GridData();
      data.horizontalSpan = 5;
      toResourcesLabel.setLayoutData(data);
      toResourcesLabel.setVisible(false);

      toTable = new Table(toGroup, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
      toTable.setLinesVisible(false);
      toTable.setHeaderVisible(false);
      data = new GridData(GridData.FILL_HORIZONTAL);
      data.heightHint = 75;
      data.horizontalSpan = 5;
      toTable.setLayoutData(data);
      TableLayout tableLayout = new TableLayout();
      toTable.setLayout(tableLayout);
      toViewer = new TableViewer(toTable);
      toViewer.setContentProvider(new MergeContentProvider());
      ILabelDecorator decorator =
          PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
      toViewer.setLabelProvider(
          new TableDecoratingLabelProvider(
              new MergeLabelProvider(MergeLabelProvider.TO), decorator));
      for (int i = 0; i < columnHeaders.length; i++) {
        tableLayout.addColumnData(columnLayouts[i]);
        TableColumn tc = new TableColumn(toTable, SWT.NONE, i);
        tc.setResizable(columnLayouts[i].resizable);
        tc.setText(columnHeaders[i]);
      }
      toViewer.setInput(this);
      toTable.setVisible(false);
    }

    toHeadButton = new Button(toGroup, SWT.RADIO);
    toHeadButton.setText(Messages.MergeWizardAdvancedPage_headRevision2);

    toRevisionButton = new Button(toGroup, SWT.RADIO);
    toRevisionButton.setText(Messages.MergeWizardAdvancedPage_revision2);

    toRevisionButton.setSelection(true);

    toRevisionText = new Text(toGroup, SWT.BORDER);
    data = new GridData();
    data.widthHint = 40;
    toRevisionText.setLayoutData(data);

    selectToRevisionButton = new Button(toGroup, SWT.PUSH);
    selectToRevisionButton.setText(Messages.MergeWizardAdvancedPage_select4);

    SelectionListener selectionListener = getSelectionListener();
    selectFromButton.addSelectionListener(selectionListener);
    fromHeadButton.addSelectionListener(selectionListener);
    fromRevisionButton.addSelectionListener(selectionListener);
    selectFromRevisionButton.addSelectionListener(selectionListener);
    useFromButton.addSelectionListener(selectionListener);
    selectToButton.addSelectionListener(selectionListener);
    toHeadButton.addSelectionListener(selectionListener);
    toRevisionButton.addSelectionListener(selectionListener);
    selectToRevisionButton.addSelectionListener(selectionListener);

    ModifyListener modifyListener =
        new ModifyListener() {
          public void modifyText(ModifyEvent e) {
            if (e.getSource() == fromCombo && useFromButton.getSelection())
              toCombo.setText(fromCombo.getText());
            if (e.getSource() == fromCombo && fromViewer != null) fromViewer.refresh();
            if (e.getSource() == toCombo && toViewer != null) toViewer.refresh();
            setPageComplete(canFinish());
          }
        };

    fromCombo.addModifyListener(modifyListener);
    toCombo.addModifyListener(modifyListener);
    fromRevisionText.addModifyListener(modifyListener);
    toRevisionText.addModifyListener(modifyListener);

    FocusListener focusListener =
        new FocusAdapter() {
          public void focusGained(FocusEvent e) {
            ((Text) e.getSource()).selectAll();
          }

          public void focusLost(FocusEvent e) {
            ((Text) e.getSource()).setText(((Text) e.getSource()).getText());
          }
        };
    fromRevisionText.addFocusListener(focusListener);
    toRevisionText.addFocusListener(focusListener);

    setPageComplete(canFinish());

    setMessage(Messages.MergeWizardAdvancedPage_message);

    setControl(outerContainer);
  }

  private SelectionListener getSelectionListener() {
    SelectionListener selectionListener =
        new SelectionAdapter() {
          public void widgetSelected(SelectionEvent e) {
            if (e.getSource() == selectFromButton) {
              ChooseUrlDialog dialog = new ChooseUrlDialog(getShell(), resource);
              dialog.setIncludeBranchesAndTags(resources.length == 1);
              if ((dialog.open() == ChooseUrlDialog.OK) && (dialog.getUrl() != null)) {
                fromCombo.setText(dialog.getUrl());
                if (useFromButton.getSelection()) toCombo.setText(dialog.getUrl());
              }
            } else if (e.getSource() == fromHeadButton || e.getSource() == fromRevisionButton) {
              fromRevisionText.setEnabled(fromRevisionButton.getSelection());
              selectFromRevisionButton.setEnabled(fromRevisionButton.getSelection());
              if (fromRevisionButton.getSelection()) fromRevisionText.setFocus();
            } else if (e.getSource() == selectFromRevisionButton) {
              showLog(fromRevisionText);
            } else if (e.getSource() == useFromButton) {
              Activator.getDefault()
                  .getDialogSettings()
                  .put(
                      "mergeUseFromDeselected_" + fromCombo.getText(),
                      !useFromButton.getSelection());
              if (useFromButton.getSelection()) toCombo.setText(fromCombo.getText());
              toCombo.setVisible(!useFromButton.getSelection());
              selectToButton.setVisible(!useFromButton.getSelection());
              if (toResourcesLabel != null)
                toResourcesLabel.setVisible(!useFromButton.getSelection());
              if (toTable != null) toTable.setVisible(!useFromButton.getSelection());
            } else if (e.getSource() == selectToButton) {
              ChooseUrlDialog dialog = new ChooseUrlDialog(getShell(), resource);
              dialog.setIncludeBranchesAndTags(resources.length == 1);
              if ((dialog.open() == ChooseUrlDialog.OK) && (dialog.getUrl() != null)) {
                toCombo.setText(dialog.getUrl());
              }
            } else if (e.getSource() == toHeadButton || e.getSource() == toRevisionButton) {
              toRevisionText.setEnabled(toRevisionButton.getSelection());
              selectToRevisionButton.setEnabled(toRevisionButton.getSelection());
              if (toRevisionButton.getSelection()) toRevisionText.setFocus();
            } else if (e.getSource() == selectToRevisionButton) {
              showLog(toRevisionText);
            }
            setPageComplete(canFinish());
          }
        };
    return selectionListener;
  }

  private boolean canFinish() {
    setErrorMessage(null);
    if (fromCombo.getText().trim().length() == 0) return false;
    if (!validateUrl(fromCombo.getText().trim())) {
      setErrorMessage(Messages.MergeWizardAdvancedPage_invalidFromUrl);
      return false;
    }
    if (!useFromButton.getSelection() && toCombo.getText().trim().length() == 0) return false;
    if (!useFromButton.getSelection() && !validateUrl(toCombo.getText().trim())) {
      setErrorMessage(Messages.MergeWizardAdvancedPage_invalidToUrl);
      return false;
    }
    if (fromRevisionButton.getSelection() && fromRevisionText.getText().trim().length() == 0)
      return false;
    if (fromRevisionButton.getSelection() && !validateRevision(fromRevisionText.getText().trim())) {
      setErrorMessage(Messages.MergeWizardAdvancedPage_invalidFromRevision);
      return false;
    }
    if (toRevisionButton.getSelection() && toRevisionText.getText().trim().length() == 0)
      return false;
    if (toRevisionButton.getSelection() && !validateRevision(toRevisionText.getText().trim())) {
      setErrorMessage(Messages.MergeWizardAdvancedPage_invalidToRevision);
      return false;
    }
    return true;
  }

  public void setVisible(boolean visible) {
    if (visible && !combosInitialized) {
      initializeLocations();
    }
    super.setVisible(visible);
  }

  public String getMergeFrom() {
    return fromCombo.getText().trim();
  }

  public String getMergeTarget() {
    return toCombo.getText().trim();
  }

  public String getCommonRoot(boolean calculateRoot) {
    if (calculateRoot) return getCommonRoot();
    else return commonRoot;
  }

  private void initializeLocations() {
    combosInitialized = true;
    MergeWizard wizard = (MergeWizard) getWizard();
    resource = wizard.getResource();
    resources = wizard.getResources();
    svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
    mergeInfoPaths = null;
    repositoryLocation = svnResource.getRepository().getLocation();
    if (((MergeWizard) getWizard()).suggestMergeSources()) {
      IRunnableWithProgress runnable =
          new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
              monitor.setTaskName(Messages.MergeWizardStandardPage_retrievingMergeSourceInfo);
              monitor.beginTask(
                  Messages.MergeWizardStandardPage_retrievingMergeSourceInfo,
                  IProgressMonitor.UNKNOWN);
              monitor.subTask(""); // $NON-NLS-1$
              ISVNClientAdapter svnClient = null;
              try {
                svnClient = svnResource.getRepository().getSVNClient();
                try {
                  mergeInfoPaths =
                      svnClient.suggestMergeSources(new SVNUrl(commonRoot), SVNRevision.HEAD);
                } catch (Exception e1) {
                }
              } catch (Exception e) {
                Activator.handleError(e);
              } finally {
                svnResource.getRepository().returnSVNClient(svnClient);
              }
              monitor.done();
            }
          };
      try {
        getContainer().run(true, false, runnable);
      } catch (Exception e2) {
        Activator.handleError(e2);
      }
    }
    boolean valueAdded = false;
    List<String> fromUrls = new ArrayList<String>();
    if (mergeInfoPaths != null) {
      for (int i = 0; i < mergeInfoPaths.length; i++) {
        String url = mergeInfoPaths[i].substring(repositoryLocation.length());
        if (!fromUrls.contains(url)) fromUrls.add(url);
        valueAdded = true;
      }
    }
    String previousFromUrls = null;
    String previousFromUrl = null;
    try {
      previousFromUrls =
          Activator.getDefault().getDialogSettings().get("mergeFromUrls_" + commonRoot);
    } catch (Exception e) {
    }
    if (previousFromUrls != null) {
      String[] urls = previousFromUrls.split("\\,");
      for (String url : urls) {
        if (!fromUrls.contains(url)) fromUrls.add(url);
        valueAdded = true;
      }
      if (urls.length > 0) previousFromUrl = urls[0];
    }

    if (!valueAdded && commonRoot != null) {
      fromUrls.add(commonRoot.substring(repositoryLocation.length()));
    }

    for (String url : fromUrls) {
      fromCombo.add(url);
      toCombo.add(url);
    }

    if (previousFromUrl != null) fromCombo.setText(previousFromUrl);
    else if (fromCombo.getItemCount() > 0) fromCombo.setText(fromCombo.getItem(0));
    if (fromCombo.getText() != null && fromCombo.getText().length() > 0) {
      try {
        String previousToUrl =
            Activator.getDefault().getDialogSettings().get("mergeToUrl_" + fromCombo.getText());
        if (previousToUrl != null) {
          if (toCombo.indexOf(previousToUrl) == -1) {
            toCombo.add(previousToUrl);
          }
          try {
            boolean useFromDeselected =
                Activator.getDefault()
                    .getDialogSettings()
                    .getBoolean("mergeUseFromDeselected_" + fromCombo.getText());
            if (useFromDeselected) {
              useFromButton.setSelection(false);
            }
          } catch (Exception e) {
          }
          if (!useFromButton.getSelection()) {
            toCombo.setVisible(true);
            toCombo.setText(previousToUrl);
          }
        }
      } catch (Exception e) {
      }
    }
  }

  private String getUrl(String url) {
    if (url != null) {
      try {
        SVNUrlWithPegRevision svnUrlWithPegRevision = new SVNUrlWithPegRevision(new SVNUrl(url));
        SVNUrl svnUrl = svnUrlWithPegRevision.getUrl();
        if (svnUrl != null) return svnUrl.toString();
      } catch (MalformedURLException e) {
      }
    }
    return url;
  }

  private void showLog(Text text) {
    ISVNRemoteResource remoteResource = null;
    if (text == fromRevisionText) {
      try {
        fromUrl = new SVNUrl(getUrl(getFrom()));
        remoteResource = svnResource.getRepository().getRemoteFile(fromUrl);
      } catch (Exception e) {
        Activator.handleError(Messages.MergeWizardAdvancedPage_showLogError, e);
        MessageDialog.openError(getShell(), Messages.MergeWizardAdvancedPage_showLog, e.toString());
        return;
      }
      if (remoteResource == null) {
        MessageDialog.openError(
            getShell(),
            Messages.MergeWizardAdvancedPage_showLog,
            Messages.MergeWizardAdvancedPage_invalidUrl + getFrom());
        return;
      }
    }
    if (text == toRevisionText) {
      try {
        toUrl = new SVNUrl(getUrl(getTo()));
        remoteResource = svnResource.getRepository().getRemoteFile(toUrl);
      } catch (Exception e) {
        Activator.handleError(Messages.MergeWizardAdvancedPage_showLogError, e);
        MessageDialog.openError(getShell(), Messages.MergeWizardAdvancedPage_showLog, e.toString());
        return;
      }
      if (remoteResource == null) {
        MessageDialog.openError(
            getShell(),
            Messages.MergeWizardAdvancedPage_showLog,
            Messages.MergeWizardAdvancedPage_invalidUrl + getTo());
        return;
      }
    }
    HistoryDialog dialog = null;
    if ((text == fromRevisionText) || (text == toRevisionText))
      dialog = new HistoryDialog(getShell(), remoteResource);
    else dialog = new HistoryDialog(getShell(), resource);
    if (dialog.open() == HistoryDialog.CANCEL) return;
    ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
    if (selectedEntries.length == 0) return;
    if ((text != null) && useFromButton.getSelection()) {
      fromRevisionText.setText(
          Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber() - 1));
      fromRevisionButton.setSelection(true);
      fromHeadButton.setSelection(false);
      toRevisionText.setText(Long.toString(selectedEntries[0].getRevision().getNumber()));
      toRevisionButton.setSelection(true);
      toHeadButton.setSelection(false);
      fromRevisionText.setEnabled(true);
      toRevisionText.setEnabled(true);
      return;
    }
    if ((text == fromRevisionText)
        || ((text == null) && (fromRevisionText.getText().trim().length() == 0))) {
      fromRevisionText.setText(Long.toString(selectedEntries[0].getRevision().getNumber()));
      fromRevisionButton.setSelection(true);
      fromHeadButton.setSelection(false);
      fromRevisionText.setEnabled(true);
    }
    if (text == toRevisionText) {
      toRevisionText.setText(Long.toString(selectedEntries[0].getRevision().getNumber()));
      toRevisionButton.setSelection(true);
      toHeadButton.setSelection(false);
      toRevisionText.setEnabled(true);
    }
  }

  private boolean validateUrl(String url) {
    if (!url.startsWith("/")) { // $NON-NLS-1$
      try {
        new SVNUrl(url);
      } catch (MalformedURLException e) {
        return false;
      }
    }
    return true;
  }

  private boolean validateRevision(String revision) {
    try {
      SVNRevision.getRevision(revision);
    } catch (ParseException e1) {
      return false;
    }
    return true;
  }

  public IResource getResource() {
    return resource;
  }

  public IResource[] getResources() {
    return resources;
  }

  private String getFrom() {
    if (fromCombo.getText().startsWith("/")) { // $NON-NLS-1$
      return svnResource.getRepository().getUrl().toString() + fromCombo.getText().trim();
    }
    return fromCombo.getText().trim();
  }

  public SVNUrl getFromUrl() {
    try {
      fromUrl = new SVNUrl(getFrom());
    } catch (MalformedURLException e) {
      Activator.handleError(e);
    }
    return fromUrl;
  }

  public SVNUrl[] getFromUrls() {
    if (resources.length == 1) {
      SVNUrl[] urls = {getFromUrl()};
      return urls;
    }
    SVNUrl[] urls = new SVNUrl[mergeResources.length];
    for (int i = 0; i < mergeResources.length; i++) {
      try {
        if (getFrom().endsWith("/"))
          urls[i] = new SVNUrl(getFrom() + mergeResources[i].getPartialPath()); // $NON-NLS-1$
        else
          urls[i] = new SVNUrl(getFrom() + "/" + mergeResources[i].getPartialPath()); // $NON-NLS-1$
      } catch (MalformedURLException e) {
        Activator.handleError(e);
      }
    }
    return urls;
  }

  private String getTo() {
    if (toCombo.getText().startsWith("/")) { // $NON-NLS-1$
      return svnResource.getRepository().getUrl().toString() + toCombo.getText().trim();
    }
    return toCombo.getText().trim();
  }

  public SVNUrl getToUrl() {
    try {
      toUrl = new SVNUrl(getTo());
    } catch (MalformedURLException e) {
      Activator.handleError(e);
    }
    return toUrl;
  }

  public SVNUrl[] getToUrls() {
    if (resources.length == 1) {
      SVNUrl[] urls = {getToUrl()};
      return urls;
    }
    SVNUrl[] urls = new SVNUrl[mergeResources.length];
    for (int i = 0; i < mergeResources.length; i++) {
      try {
        if (getTo().endsWith("/"))
          urls[i] = new SVNUrl(getTo() + mergeResources[i].getPartialPath()); // $NON-NLS-1$
        else
          urls[i] = new SVNUrl(getTo() + "/" + mergeResources[i].getPartialPath()); // $NON-NLS-1$
      } catch (MalformedURLException e) {
        Activator.handleError(e);
      }
    }
    return urls;
  }

  public SVNRevision getFromRevision() {
    if (fromHeadButton.getSelection()) fromRevision = SVNRevision.HEAD;
    else {
      try {
        fromRevision = SVNRevision.getRevision(fromRevisionText.getText().trim());
      } catch (ParseException e) {
        Activator.handleError(e);
      }
    }
    return fromRevision;
  }

  public SVNRevision getToRevision() {
    if (toHeadButton.getSelection()) toRevision = SVNRevision.HEAD;
    else {
      try {
        toRevision = SVNRevision.getRevision(toRevisionText.getText().trim());
      } catch (ParseException e) {
        Activator.handleError(e);
      }
    }
    return toRevision;
  }

  private String getCommonRoot() {
    commonRoot = ((MergeWizard) getWizard()).getCommonRoot();
    urlStrings = ((MergeWizard) getWizard()).getUrlStrings();
    mergeResources = new MergeResource[resources.length];
    for (int i = 0; i < resources.length; i++) {
      if (urlStrings[i].length() <= commonRoot.length())
        mergeResources[i] = new MergeResource(resources[i], commonRoot);
      else
        mergeResources[i] =
            new MergeResource(resources[i], urlStrings[i].substring(commonRoot.length() + 1));
    }

    return commonRoot;
  }

  private class MergeResource implements IAdaptable {
    private IResource resource;
    private String partialPath;

    public MergeResource(IResource resource, String partialPath) {
      this.resource = resource;
      this.partialPath = partialPath;
    }

    public IResource getResource() {
      return resource;
    }

    public void setResource(IResource resource) {
      this.resource = resource;
    }

    public String getPartialPath() {
      return partialPath;
    }

    public void setPartialPath(String partialPath) {
      this.partialPath = partialPath;
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class adapter) {
      if (IResource.class == adapter) return resource;
      return null;
    }
  }

  class MergeLabelProvider extends LabelProvider implements ITableLabelProvider {
    WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();

    private int type;
    public static final int FROM = 0;
    public static final int TO = 1;

    public MergeLabelProvider(int type) {
      super();
      this.type = type;
    }

    public String getColumnText(Object element, int columnIndex) {
      return getText(element);
    }

    public String getText(Object element) {
      MergeResource mergeResource = (MergeResource) element;
      if (type == FROM)
        return mergeResource.getPartialPath()
            + " ["
            + fromCombo.getText()
            + "/"
            + mergeResource.getPartialPath()
            + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      else
        return mergeResource.getPartialPath()
            + " ["
            + toCombo.getText()
            + "/"
            + mergeResource.getPartialPath()
            + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    public Image getColumnImage(Object element, int columnIndex) {
      return getImage(element);
    }

    public Image getImage(Object element) {
      MergeResource mergeResource = (MergeResource) element;
      return workbenchLabelProvider.getImage(mergeResource.getResource());
    }
  }

  class MergeContentProvider implements IStructuredContentProvider {
    public void dispose() {}

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

    public Object[] getElements(Object obj) {
      return mergeResources;
    }
  }

  class TableDecoratingLabelProvider extends DecoratingLabelProvider
      implements ITableLabelProvider {

    ITableLabelProvider provider;
    ILabelDecorator decorator;

    public TableDecoratingLabelProvider(ILabelProvider provider, ILabelDecorator decorator) {
      super(provider, decorator);
      this.provider = (ITableLabelProvider) provider;
      this.decorator = decorator;
    }

    public Image getColumnImage(Object element, int columnIndex) {
      Image image = provider.getColumnImage(element, columnIndex);
      if (decorator != null) {
        Image decorated = decorator.decorateImage(image, element);
        if (decorated != null) {
          return decorated;
        }
      }
      return image;
    }

    public String getColumnText(Object element, int columnIndex) {
      String text = provider.getColumnText(element, columnIndex);
      return text;
    }
  }
}
