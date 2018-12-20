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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.commands.GetLogsCommand;
import org.tigris.subversion.subclipse.core.history.AliasManager;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntryChangePath;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.compare.ResourceEditionNode;
import org.tigris.subversion.subclipse.ui.compare.SVNCompareEditorInput;
import org.tigris.subversion.subclipse.ui.history.ChangePathsTreeViewer;
import org.tigris.subversion.subclipse.ui.history.HistoryFolder;
import org.tigris.subversion.subclipse.ui.history.HistoryTableProvider;
import org.tigris.subversion.svnclientadapter.ISVNMergeInfo;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNRevisionRange;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class MergeWizardUnblockRevisionsPage extends WizardPage {
  private MergeWizardStandardPage standardPage;
  private String fromUrl;
  private SVNRevisionRange[] revisionRanges;
  private ArrayList entryArray;

  private IDialogSettings settings;
  private SashForm horizontalSash;
  private SashForm verticalSash;
  private HistoryTableProvider historyTableProvider;
  private ChangePathsTreeViewer changePathsViewer;
  private TableViewer tableHistoryViewer;
  private TextViewer textViewer;
  private Button showCompareButton;
  private CompareViewerSwitchingPane compareViewerPane;
  private ILogEntry[] entries;
  private IResource resource;
  private ISVNLocalResource svnResource;
  private ISVNRepositoryLocation repositoryLocation;
  private AliasManager tagManager;
  private ISVNRemoteResource remoteResource;
  private SVNRevision.Number[] allRevisions;
  private ILogEntry[] rangeEntries;
  private boolean pageShown;

  private SVNCompareEditorInput compareInput;
  private boolean showCompare;
  private Map<String, SVNCompareEditorInput> compareInputMap =
      new HashMap<String, SVNCompareEditorInput>();

  public MergeWizardUnblockRevisionsPage(
      String pageName,
      String title,
      ImageDescriptor titleImage,
      MergeWizardStandardPage standardPage) {
    super(pageName, title, titleImage);
    this.standardPage = standardPage;
    settings = Activator.getDefault().getDialogSettings();
  }

  public void createControl(Composite parent) {
    final MergeWizard wizard = (MergeWizard) getWizard();
    resource = wizard.getResource();
    if (resource != null) {
      svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
      try {
        repositoryLocation = svnResource.getRepository();
      } catch (Exception e1) {
      }
    }

    Composite composite = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    composite.setLayout(layout);
    GridData data = new GridData(GridData.FILL_BOTH);
    composite.setLayoutData(data);

    horizontalSash = new SashForm(composite, SWT.HORIZONTAL);
    horizontalSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    verticalSash = new SashForm(horizontalSash, SWT.VERTICAL);
    GridLayout sashLayout = new GridLayout();
    sashLayout.verticalSpacing = 0;
    sashLayout.marginHeight = 0;
    verticalSash.setLayout(sashLayout);
    verticalSash.setLayoutData(new GridData(GridData.FILL_BOTH));

    Composite historyGroup = new Composite(verticalSash, SWT.NULL);
    GridLayout historyLayout = new GridLayout();
    historyLayout.verticalSpacing = 0;
    historyLayout.marginHeight = 0;
    historyGroup.setLayout(historyLayout);
    historyGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

    historyTableProvider =
        new HistoryTableProvider(
            SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION | SWT.CHECK,
            "MergeWizardUnblockRevisionsPage"); //$NON-NLS-1$
    historyTableProvider.setIncludeMergeRevisions(false);
    historyTableProvider.setIncludeTags(false);
    tableHistoryViewer = historyTableProvider.createTable(historyGroup);
    data = new GridData(GridData.FILL_BOTH);
    data.widthHint = 500;
    data.heightHint = 100;
    tableHistoryViewer.getTable().setLayoutData(data);

    tableHistoryViewer.setContentProvider(
        new IStructuredContentProvider() {
          public void dispose() {}

          public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

          public Object[] getElements(Object inputElement) {
            if (entries == null) return new ILogEntry[0];
            return entries;
          }
        });

    tableHistoryViewer.setInput(fromUrl);
    tableHistoryViewer.addSelectionChangedListener(
        new ISelectionChangedListener() {
          public void selectionChanged(SelectionChangedEvent event) {
            setPageComplete(canFinish());
            ISelection selection = event.getSelection();
            if (selection == null || !(selection instanceof IStructuredSelection)) {
              textViewer.setDocument(new Document("")); // $NON-NLS-1$
              changePathsViewer.setInput(null);
              return;
            }
            IStructuredSelection ss = (IStructuredSelection) selection;
            if (ss.size() != 1) {
              textViewer.setDocument(new Document("")); // $NON-NLS-1$
              changePathsViewer.setInput(null);
              return;
            }
            LogEntry entry = (LogEntry) ss.getFirstElement();
            textViewer.setDocument(new Document(entry.getComment()));
            changePathsViewer.setCurrentLogEntry(entry);
            changePathsViewer.setInput(entry);
          }
        });

    MenuManager menuMgr = new MenuManager();
    Menu menu = menuMgr.createContextMenu(tableHistoryViewer.getTable());
    menuMgr.addMenuListener(
        new IMenuListener() {
          public void menuAboutToShow(IMenuManager menuMgr) {
            if (!tableHistoryViewer.getSelection().isEmpty()) {
              menuMgr.add(new ToggleSelectionAction());
            }
          }
        });
    menuMgr.setRemoveAllWhenShown(true);
    tableHistoryViewer.getTable().setMenu(menu);

    Composite commentGroup = new Composite(verticalSash, SWT.NULL);
    GridLayout commentLayout = new GridLayout();
    commentLayout.verticalSpacing = 0;
    commentLayout.marginHeight = 0;
    commentGroup.setLayout(commentLayout);
    commentGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

    textViewer =
        new TextViewer(
            commentGroup,
            SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.READ_ONLY);
    data = new GridData(GridData.FILL_BOTH);
    data.heightHint = 100;
    data.widthHint = 500;
    textViewer.getControl().setLayoutData(data);

    Composite pathGroup = new Composite(verticalSash, SWT.NULL);
    GridLayout pathLayout = new GridLayout();
    pathLayout.verticalSpacing = 0;
    pathLayout.marginHeight = 0;
    pathGroup.setLayout(pathLayout);
    pathGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

    ViewForm viewerPane = new ViewForm(pathGroup, SWT.BORDER | SWT.FLAT);
    viewerPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
    CLabel toolbarLabel =
        new CLabel(viewerPane, SWT.NONE) {
          public Point computeSize(int wHint, int hHint, boolean changed) {
            return super.computeSize(wHint, Math.max(24, hHint), changed);
          }
        };
    toolbarLabel.setText(Messages.MergeWizardRevisionsPage_2);
    viewerPane.setTopLeft(toolbarLabel);
    ToolBar toolbar = new ToolBar(viewerPane, SWT.FLAT);
    viewerPane.setTopCenter(toolbar);
    ToolBarManager toolbarManager = new ToolBarManager(toolbar);

    toolbarManager.add(new Separator());
    toolbarManager.add(
        new ControlContribution("showCompare") { // $NON-NLS-1$
          @Override
          protected Control createControl(Composite parent) {
            showCompareButton = new Button(parent, SWT.TOGGLE | SWT.FLAT);
            showCompareButton.setImage(SVNUIPlugin.getImage(ISVNUIConstants.IMG_SYNCPANE));
            showCompareButton.setToolTipText(Messages.MergeWizardRevisionsPage_4);
            showCompareButton.setSelection(showCompare);
            showCompareButton.addSelectionListener(
                new SelectionAdapter() {
                  public void widgetSelected(SelectionEvent e) {
                    showComparePane(!showCompare);
                    if (showCompare) {
                      compareRevisions();
                    }
                  }
                });
            return showCompareButton;
          }
        });

    toolbarManager.update(true);

    ChangePathsTreeContentProvider contentProvider = new ChangePathsTreeContentProvider();
    changePathsViewer = new ChangePathsTreeViewer(viewerPane, contentProvider);

    viewerPane.setContent(changePathsViewer.getTree());

    changePathsViewer.addDoubleClickListener(
        new IDoubleClickListener() {
          public void doubleClick(DoubleClickEvent event) {
            compareRevisions();
          }
        });

    changePathsViewer
        .getTree()
        .addSelectionListener(
            new SelectionAdapter() {
              public void widgetSelected(SelectionEvent e) {
                if (showCompare) {
                  compareRevisions();
                }
              }
            });

    setPageComplete(false);

    setMessage(Messages.MergeWizardUnblockRevisionsPage_specifyRevisions);

    try {
      int[] weights = new int[3];
      weights[0] = settings.getInt("MergeWizardRevisionsPageWeights0"); // $NON-NLS-1$
      weights[1] = settings.getInt("MergeWizardRevisionsPageWeights1"); // $NON-NLS-1$
      weights[2] = settings.getInt("MergeWizardRevisionsPageWeights2"); // $NON-NLS-1$
      verticalSash.setWeights(weights);
    } catch (Exception e) {
    }

    compareViewerPane =
        new CompareViewerSwitchingPane(horizontalSash, SWT.BORDER | SWT.FLAT) {
          protected Viewer getViewer(Viewer oldViewer, Object input) {
            CompareConfiguration cc = compareInput.getCompareConfiguration();
            cc.setLeftEditable(false);
            cc.setRightEditable(false);
            cc.setLeftLabel(compareInput.getLeftLabel());
            cc.setRightLabel(compareInput.getRightLabel());
            return CompareUI.findContentViewer(oldViewer, input, this, cc);
          }
        };
    compareViewerPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    try {
      int[] weights = new int[2];
      weights[0] = settings.getInt("MergeWizardRevisionsPageWeightsHorizontal0"); // $NON-NLS-1$
      weights[1] = settings.getInt("MergeWizardRevisionsPageWeightsHorizontal1"); // $NON-NLS-1$
      horizontalSash.setWeights(weights);
    } catch (Exception e) {
    }

    if (!showCompare) {
      horizontalSash.setMaximizedControl(verticalSash);
    } else {
      showCompareButton.setSelection(true);
    }

    setControl(composite);
  }

  public boolean canFinish() {
    return getSelectedRevisions().length > 0;
  }

  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      if (fromUrl == null || !fromUrl.equals(standardPage.getFromUrl())) refresh();

      // This is a hack to get around an initial sorting problem on OSx.
      if (!pageShown) {
        pageShown = true;
        historyTableProvider.setSortColumn(tableHistoryViewer, 0);
        historyTableProvider.setSortColumn(tableHistoryViewer, 0);
      }
    }
  }

  private void setCompareInput(final SVNCompareEditorInput input, boolean run)
      throws InterruptedException, InvocationTargetException {
    if (run) {
      input.run(new NullProgressMonitor());
    }
    compareViewerPane.setInput(null);
    compareViewerPane.setInput(input.getCompareResult());
  }

  public void showComparePane(boolean showCompare) {
    this.showCompare = showCompare;
    if (showCompare) {
      horizontalSash.setMaximizedControl(null);
    } else {
      horizontalSash.setMaximizedControl(verticalSash);
    }
  }

  private void refresh() {
    IRunnableWithProgress runnable =
        new IRunnableWithProgress() {
          public void run(IProgressMonitor monitor)
              throws InvocationTargetException, InterruptedException {
            try {
              monitor.setTaskName(
                  Messages.MergeWizardUnblockRevisionsPage_retrievingRemoteResource);
              monitor.beginTask(
                  Messages.MergeWizardUnblockRevisionsPage_retrievingRemoteResource, 2);
              monitor.worked(1);
              remoteResource = repositoryLocation.getRemoteFile(new SVNUrl(fromUrl));
              monitor.worked(1);
              monitor.done();
            } catch (Exception e) {
              Activator.handleError(e);
            }
          }
        };
    setPageComplete(false);
    setErrorMessage(null);
    fromUrl = standardPage.getFromUrl();
    ISVNMergeInfo mergeInfo = standardPage.getMergeInfo();
    if (mergeInfo == null) {
      entries = new ILogEntry[0];
      return;
    }
    revisionRanges = mergeInfo.getRevisionRange(fromUrl);
    remoteResource = null;
    try {
      getContainer().run(false, false, runnable);
      if (remoteResource == null) {
        setErrorMessage(Messages.MergeWizardUnblockRevisionsPage_errorRetrievingRemoteResource);
        entries = new ILogEntry[0];
      } else getLogEntries();
    } catch (Exception e) {
      setErrorMessage(e.getMessage());
      entries = new ILogEntry[0];
    }
    if (tableHistoryViewer.getInput() == null) tableHistoryViewer.setInput(fromUrl);
    else tableHistoryViewer.refresh();

    setPageComplete(canFinish());
  }

  private void getLogEntries() {
    entryArray = new ArrayList();
    IRunnableWithProgress runnable =
        new IRunnableWithProgress() {
          public void run(IProgressMonitor monitor)
              throws InvocationTargetException, InterruptedException {
            try {
              monitor.setTaskName(
                  Messages.MergeWizardUnblockRevisionsPage_retrievingRevisionLogInfo);
              monitor.beginTask(
                  Messages.MergeWizardUnblockRevisionsPage_retrievingRevisionLogInfo, 3);
              if (SVNUIPlugin.getPlugin()
                  .getPreferenceStore()
                  .getBoolean(ISVNUIConstants.PREF_SHOW_TAGS_IN_REMOTE))
                tagManager = new AliasManager(remoteResource.getUrl());
              SVNRevision pegRevision = remoteResource.getRevision();
              monitor.worked(1);
              for (int i = 0; i < revisionRanges.length; i++) {
                rangeEntries =
                    getLogEntries(
                        pegRevision,
                        revisionRanges[i].getFromRevision(),
                        revisionRanges[i].getToRevision(),
                        true,
                        0,
                        tagManager,
                        true);
                monitor.worked(1);
                for (int j = 0; j < rangeEntries.length; j++) {
                  entryArray.add(rangeEntries[j]);
                }
              }

              entries = new ILogEntry[entryArray.size()];
              entryArray.toArray(entries);
            } catch (Exception e) {
              setErrorMessage(e.getMessage());
              entries = new ILogEntry[0];
            }
            monitor.worked(1);
            monitor.done();
          }
        };
    try {
      getContainer().run(false, false, runnable);
    } catch (Exception e1) {
      Activator.handleError(e1);
    }
    setErrorMessage(standardPage.getErrorMessage());
  }

  private SVNRevision.Number[] getAll() {
    allRevisions = new SVNRevision.Number[entryArray.size()];
    int i = 0;
    Iterator iter = entryArray.iterator();
    while (iter.hasNext()) {
      ILogEntry logEntry = (ILogEntry) iter.next();
      allRevisions[i++] = logEntry.getRevision();
    }
    return allRevisions;
  }

  protected ILogEntry[] getLogEntries(
      SVNRevision pegRevision,
      SVNRevision revisionStart,
      SVNRevision revisionEnd,
      boolean stopOnCopy,
      long limit,
      AliasManager tagManager,
      boolean includeMergedRevisions)
      throws TeamException {
    GetLogsCommand logCmd =
        new GetLogsCommand(
            remoteResource,
            pegRevision,
            revisionStart,
            revisionEnd,
            stopOnCopy,
            limit,
            tagManager,
            includeMergedRevisions);
    logCmd.run(null);
    return logCmd.getLogEntries();
  }

  private SVNRevision.Number[] getSelectedRevisions() {
    ArrayList selectedEntries = new ArrayList();
    TableItem[] items = tableHistoryViewer.getTable().getItems();
    for (int i = 0; i < items.length; i++) {
      if (items[i].getChecked()) {
        ILogEntry entry = (ILogEntry) items[i].getData();
        selectedEntries.add(entry.getRevision());
      }
    }
    SVNRevision.Number[] entryArray = new SVNRevision.Number[selectedEntries.size()];
    selectedEntries.toArray(entryArray);
    return entryArray;
  }

  public SVNRevisionRange[] getRevisions() {
    SVNRevisionRange[] revisionRanges =
        SVNRevisionRange.getRevisions(getSelectedRevisions(), getAll());
    SVNRevisionRange[] reversedRevisionRanges = new SVNRevisionRange[revisionRanges.length];
    for (int i = 0; i < revisionRanges.length; i++) {
      reversedRevisionRanges[i] =
          new SVNRevisionRange(
              revisionRanges[i].getToRevision(), revisionRanges[i].getFromRevision());
    }
    return reversedRevisionRanges;
  }

  public void dispose() {
    if (pageShown) {
      int[] weights = verticalSash.getWeights();
      for (int i = 0; i < weights.length; i++) {
        settings.put("MergeWizardRevisionsPageWeights" + i, weights[i]); // $NON-NLS-1$
      }
      weights = horizontalSash.getWeights();
      for (int i = 0; i < weights.length; i++) {
        settings.put("MergeWizardRevisionsPageWeightsHorizontal" + i, weights[i]); // $NON-NLS-1$
      }
    }
    super.dispose();
  }

  public void setFromUrl(String fromUrl) {
    this.fromUrl = fromUrl;
  }

  private void compareRevisions() {
    IStructuredSelection sel = (IStructuredSelection) changePathsViewer.getSelection();
    Object sel0 = sel.getFirstElement();
    if (sel0 instanceof LogEntryChangePath) {
      LogEntryChangePath logEntryChangePath = (LogEntryChangePath) sel0;
      try {
        if (!logEntryChangePath.getRemoteResource().isContainer()) {
          ISVNRemoteResource left = logEntryChangePath.getRemoteResource();
          compareInput = compareInputMap.get(left.getUrl().toString() + left.getRevision());
          boolean run = compareInput == null;
          if (compareInput == null) {
            SVNRevision.Number selectedRevision = (SVNRevision.Number) left.getRevision();
            SVNRevision.Number previousRevision =
                new SVNRevision.Number(selectedRevision.getNumber() - 1);
            ISVNRemoteResource right =
                new RemoteFile(left.getRepository(), left.getUrl(), previousRevision);
            compareInput =
                new SVNCompareEditorInput(
                    new ResourceEditionNode(left), new ResourceEditionNode(right));
            compareInputMap.put(left.getUrl().toString() + left.getRevision(), compareInput);
          }
          setCompareInput(compareInput, run);
          showComparePane(true);
        }
      } catch (Exception e) {
        MessageDialog.openError(getShell(), Messages.MergeWizardRevisionsPage_5, e.getMessage());
      }
    }
  }

  private class ToggleSelectionAction extends Action {

    public ToggleSelectionAction() {
      super();
      setText("Toggle selection"); // $NON-NLS-1$ 	
    }

    public void run() {
      TableItem[] items = tableHistoryViewer.getTable().getSelection();
      for (int i = 0; i < items.length; i++) items[i].setChecked(!items[i].getChecked());
      setPageComplete(canFinish());
    }
  }

  static class ChangePathsTreeContentProvider implements ITreeContentProvider {

    ChangePathsTreeContentProvider() {}

    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof HistoryFolder) {
        return ((HistoryFolder) parentElement).getChildren();
      }
      return null;
    }

    public Object getParent(Object element) {
      return null;
    }

    public boolean hasChildren(Object element) {
      if (element instanceof HistoryFolder) {
        HistoryFolder folder = (HistoryFolder) element;
        return folder.getChildren().length > 0;
      }
      return false;
    }

    public Object[] getElements(Object inputElement) {
      ILogEntry logEntry = (ILogEntry) inputElement;
      return getGroups(logEntry.getLogEntryChangePaths());
    }

    private Object[] getGroups(LogEntryChangePath[] changePaths) {
      // 1st pass. Collect folder names
      Set folderNames = new HashSet();
      for (int i = 0; i < changePaths.length; i++) {
        folderNames.add(getFolderName(changePaths[i]));
      }

      // 2nd pass. Sorting out explicitly changed folders
      TreeMap folders = new TreeMap();
      for (int i = 0; i < changePaths.length; i++) {
        LogEntryChangePath changePath = changePaths[i];
        String path = changePath.getPath();
        if (folderNames.contains(path)) {
          // changed folder
          HistoryFolder folder = (HistoryFolder) folders.get(path);
          if (folder == null) {
            folder = new HistoryFolder(changePath);
            folders.put(path, folder);
          }
        } else {
          // changed resource
          path = getFolderName(changePath);
          HistoryFolder folder = (HistoryFolder) folders.get(path);
          if (folder == null) {
            folder = new HistoryFolder(path);
            folders.put(path, folder);
          }
          folder.add(changePath);
        }
      }

      return folders.values().toArray(new Object[folders.size()]);
    }

    private String getFolderName(LogEntryChangePath changePath) {
      String path = changePath.getPath();
      int n = path.lastIndexOf('/');
      return n > -1 ? path.substring(0, n) : path;
    }

    public void dispose() {}

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
  }
}
