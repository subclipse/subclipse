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
import com.collabnet.subversion.merge.dialogs.FilterRevisionsDialog;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
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
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.commands.ISVNCommand;
import org.tigris.subversion.subclipse.core.history.AliasManager;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntryChangePath;
import org.tigris.subversion.subclipse.core.history.Tags;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.SVNUrlWithPegRevision;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.actions.GenerateChangeLogAction;
import org.tigris.subversion.subclipse.ui.compare.ResourceEditionNode;
import org.tigris.subversion.subclipse.ui.compare.SVNCompareEditorInput;
import org.tigris.subversion.subclipse.ui.history.ChangePathsTreeViewer;
import org.tigris.subversion.subclipse.ui.history.HistoryFolder;
import org.tigris.subversion.subclipse.ui.history.HistorySearchViewerFilter;
import org.tigris.subversion.subclipse.ui.history.HistoryTableProvider;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.ISVNMergeinfoLogKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNRevisionRange;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class MergeWizardRevisionsPage extends WizardPage {
  private MergeWizardStandardPage standardPage;
  private String fromUrl;

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
  private IResource[] resources;
  private ISVNLocalResource svnResource;
  private ISVNRepositoryLocation repositoryLocation;
  private AliasManager tagManager;
  private ISVNRemoteResource remoteResource;
  private String message;
  private String pageName;
  private boolean pageShown;
  private GenerateChangeLogAction generateChangeLogAction;
  private Button generateChangeLogButton;
  private Text filterText;
  private boolean noEligibleEntries;

  private Button moreOptionsButton;
  private FilterRevisionsDialog dialog;
  private HistorySearchViewerFilter historySearchFilter;

  private SVNCompareEditorInput compareInput;
  private boolean showCompare;
  private Map<String, SVNCompareEditorInput> compareInputMap =
      new HashMap<String, SVNCompareEditorInput>();

  private Map<SVNRevision.Number, List<IResource>> revisionToResource =
      new HashMap<SVNRevision.Number, List<IResource>>();

  private Set<SVNRevision.Number> selectedRevisions = new HashSet<SVNRevision.Number>();

  private static final String ALL_AUTHORS = Messages.MergeWizardRevisionsPage_0;

  public MergeWizardRevisionsPage(
      String pageName,
      String title,
      ImageDescriptor titleImage,
      MergeWizardStandardPage standardPage,
      String message) {
    super(pageName, title, titleImage);
    this.standardPage = standardPage;
    this.message = message;
    this.pageName = pageName;
    settings = Activator.getDefault().getDialogSettings();
  }

  public MergeWizardRevisionsPage(
      String pageName,
      String title,
      ImageDescriptor titleImage,
      MergeWizardStandardPage standardPage) {
    this(pageName, title, titleImage, standardPage, null);
  }

  public void createControl(Composite parent) {
    final MergeWizard wizard = (MergeWizard) getWizard();

    resources = wizard.getResources();
    if (resources != null && resources.length > 0) {
      svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[0]);
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
    historyLayout.verticalSpacing = 5;
    historyLayout.marginHeight = 0;
    historyGroup.setLayout(historyLayout);
    historyGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

    Composite filterGroup = new Composite(historyGroup, SWT.NULL);
    GridLayout filterLayout = new GridLayout();
    filterLayout.verticalSpacing = 0;
    filterLayout.marginHeight = 0;
    filterLayout.numColumns = 3;
    filterGroup.setLayout(filterLayout);
    filterGroup.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));

    Label filterLabel = new Label(filterGroup, SWT.NULL);
    filterLabel.setText(Messages.MergeWizardRevisionsPage_filter);
    filterText = new Text(filterGroup, SWT.BORDER);
    filterText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));
    filterText.addModifyListener(
        new ModifyListener() {
          public void modifyText(ModifyEvent e) {
            tableHistoryViewer.addFilter(new TextViewFilter());
            tableHistoryViewer.refresh();
            TableItem[] items = tableHistoryViewer.getTable().getItems();
            for (TableItem item : items) {
              ILogEntry entry = adaptToLogEntry(item.getData());
              if (entry != null) {
                SVNRevision.Number revision = entry.getRevision();
                item.setChecked(selectedRevisions.contains(revision));
              }
            }
            showMessage();
          }
        });

    this.moreOptionsButton = new Button(filterGroup, 8);
    this.moreOptionsButton.setText(Messages.MergeWizardRevisionsPage_moreOptions);
    SelectionListener selectionListener = getSelectionListener();
    this.moreOptionsButton.addSelectionListener(selectionListener);

    historyTableProvider =
        new HistoryTableProvider(
            SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION | SWT.CHECK,
            pageName);
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

    tableHistoryViewer.setInput(getUrl());
    tableHistoryViewer.addSelectionChangedListener(
        new ISelectionChangedListener() {
          public void selectionChanged(SelectionChangedEvent event) {
            generateChangeLogButton.setEnabled(!tableHistoryViewer.getSelection().isEmpty());
            TableItem[] items = tableHistoryViewer.getTable().getItems();
            for (TableItem item : items) {
              ILogEntry entry = adaptToLogEntry(item.getData());
              SVNRevision.Number revision = entry.getRevision();
              if (item.getChecked()) {
                selectedRevisions.add(revision);
              } else {
                selectedRevisions.remove(revision);
              }
            }
            if (selectedRevisions.size() == 0) {
              if (message == null) showMessage();
              else setMessage(message);
            } else {
              // check size of entries and table data

              setMessage(
                  selectedRevisions.size() + Messages.MergeWizardRevisionsPage_revisionsSelected);
            }
            setPageComplete(canFinish());
          }
        });
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

    generateChangeLogAction =
        new GenerateChangeLogAction(
            new ISelectionProvider() {
              public void addSelectionChangedListener(ISelectionChangedListener listener) {}

              public ISelection getSelection() {
                return tableHistoryViewer.getSelection();
              }

              public void setSelection(ISelection selection) {}

              public void removeSelectionChangedListener(ISelectionChangedListener listener) {}
            });

    MenuManager menuMgr = new MenuManager();
    Menu menu = menuMgr.createContextMenu(tableHistoryViewer.getTable());
    menuMgr.addMenuListener(
        new IMenuListener() {
          public void menuAboutToShow(IMenuManager menuMgr) {
            if (!tableHistoryViewer.getSelection().isEmpty()) {
              menuMgr.add(new ToggleSelectionAction());
              menuMgr.add(generateChangeLogAction);
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

    setPageComplete(canFinish());

    if (message == null) setMessage(Messages.MergeWizardRevisionsPage_specifyRevisions);
    else setMessage(message);

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

    Composite buttonGroup = new Composite(composite, SWT.NULL);
    GridLayout buttonLayout = new GridLayout();
    buttonLayout.numColumns = 3;
    buttonGroup.setLayout(buttonLayout);
    data = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
    buttonGroup.setLayoutData(data);

    generateChangeLogButton = new Button(buttonGroup, SWT.PUSH);
    generateChangeLogButton.setText(Messages.MergeWizardRevisionsPage_generateChangeLog);
    generateChangeLogButton.setEnabled(false);
    generateChangeLogButton.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            generateChangeLogAction.run();
          }
        });

    if (!showCompare) {
      horizontalSash.setMaximizedControl(verticalSash);
    } else {
      showCompareButton.setSelection(true);
    }

    setControl(composite);
  }

  private SelectionListener getSelectionListener() {
    SelectionListener selectionListener =
        new SelectionAdapter() {

          public void widgetSelected(SelectionEvent e) {

            if (e.getSource() == MergeWizardRevisionsPage.this.moreOptionsButton) {

              if (dialog == null) {
                final TreeSet<String> set = new TreeSet<String>();
                if (entries != null && entries.length > 0) {
                  set.add(ALL_AUTHORS);
                  for (int i = 0; i < entries.length; i++) {
                    if (entries[i].getAuthor() != null) {
                      set.add(entries[i].getAuthor());
                    }
                  }
                  if (set.size() == 2) {
                    set.remove(ALL_AUTHORS);
                  }
                }
                dialog = new FilterRevisionsDialog(getShell(), set);
              }
              dialog.setRemoteResource(MergeWizardRevisionsPage.this.remoteResource);
              int returnCode = dialog.open();
              if (returnCode < 1) {
                clearSearchCriteria();
                historySearchFilter =
                    new HistorySearchViewerFilter(
                        dialog.getUser(),
                        dialog.getComment(),
                        dialog.getStartDate(),
                        dialog.getEndDate(),
                        dialog.getRegExp(),
                        dialog.getStartRevision(),
                        dialog.getEndRevision());
                tableHistoryViewer.setFilters(new ViewerFilter[] {historySearchFilter});
                MergeWizardRevisionsPage.this.tableHistoryViewer.refresh();

                TableItem[] items = tableHistoryViewer.getTable().getItems();
                for (TableItem item : items) {
                  ILogEntry entry = adaptToLogEntry(item.getData());
                  if (entry != null) {
                    historySearchFilter.select(tableHistoryViewer, null, entry);
                  }
                }
                showMessage();
                setPageComplete(canFinish());
              }
            }
          }

          private void clearSearchCriteria() {
            filterText.setText("");
          }
        };
    return selectionListener;
  }

  protected void showMessage() {
    TableItem[] items = tableHistoryViewer.getTable().getItems();
    if ((entries != null && items != null) && (entries.length != items.length)) {
      setMessage(
          Messages.MergeWizardRevisionsPage_specifyRevisions.concat(
              " " + Messages.MergeWizardRevisionsPage_filteredList));
    } else {
      setMessage(Messages.MergeWizardRevisionsPage_specifyRevisions);
    }
  }

  protected ILogEntry adaptToLogEntry(Object element) {
    ILogEntry entry = null;
    if (element instanceof ILogEntry) {
      entry = (ILogEntry) element;
    } else if (element instanceof IAdaptable) {
      entry = (ILogEntry) ((IAdaptable) element).getAdapter(ILogEntry.class);
    }
    return entry;
  }

  public boolean canFinish() {
    return getSelectedRevisions().length > 0;
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      if (((MergeWizard) getWizard()).isRetrieveRevisionsMethodChanged()
          || fromUrl == null
          || !fromUrl.equals(standardPage.getFromUrl())) refresh();
      ((MergeWizard) getWizard()).setRetrieveRevisionsMethodChanged(false);
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
              monitor.setTaskName(Messages.MergeWizardRevisionsPage_retrievingRemoteResource);
              monitor.beginTask(
                  Messages.MergeWizardRevisionsPage_retrievingRemoteResource,
                  IProgressMonitor.UNKNOWN);
              monitor.worked(1);
              remoteResource = repositoryLocation.getRemoteFile(new SVNUrl(getUrl()));
              monitor.done();
            } catch (Exception e) {
              Activator.handleError(e);
            }
          }
        };
    setPageComplete(false);
    setErrorMessage(null);
    fromUrl = standardPage.getFromUrl();
    remoteResource = null;
    try {
      getContainer().run(true, false, runnable);
      if (remoteResource == null) {
        setErrorMessage(Messages.MergeWizardRevisionsPage_errorRetrievingRemoteResource);
        entries = new ILogEntry[0];
      } else {
        getLogEntries(false, false);
        if (noEligibleEntries) {
          setErrorMessage(Messages.MergeWizardRevisionsPage_noEligibleRevisions);
        }
      }
    } catch (Exception e) {
      Activator.handleError(e);
      setErrorMessage(e.getMessage());
      entries = new ILogEntry[0];
    }
    if (tableHistoryViewer.getInput() == null) tableHistoryViewer.setInput(getUrl());
    else tableHistoryViewer.refresh();
  }

  private void getLogEntries(final boolean getAll, final boolean getNext) {
    IRunnableWithProgress runnable =
        new IRunnableWithProgress() {
          public void run(IProgressMonitor monitor)
              throws InvocationTargetException, InterruptedException {
            try {
              monitor.setTaskName(Messages.MergeWizardRevisionsPage_retrievingRevisionLogInfo);
              monitor.beginTask(Messages.MergeWizardRevisionsPage_retrievingRevisionLogInfo, 5);
              if (SVNUIPlugin.getPlugin()
                  .getPreferenceStore()
                  .getBoolean(ISVNUIConstants.PREF_SHOW_TAGS_IN_REMOTE))
                tagManager = new AliasManager(remoteResource.getUrl());
              monitor.worked(1);
              noEligibleEntries = false;
              entries = getEligibleLogEntries(monitor);
              monitor.worked(1);
              if (entries == null || entries.length == 0) noEligibleEntries = true;
              monitor.worked(1);
            } catch (Exception e) {
              Activator.handleError(e);
              Display.getDefault()
                  .asyncExec(
                      new Runnable() {
                        public void run() {
                          setErrorMessage(
                              Messages.MergeWizardChangeSetRevisionsPage_errorRetrievingLogEntries);
                        }
                      });
              entries = new ILogEntry[0];
            }
            monitor.worked(1);
            monitor.done();
          }
        };

    try {
      getContainer().run(true, false, runnable);
    } catch (Exception e1) {
      Activator.handleError(e1);
    }
  }

  @Override
  public boolean isPageComplete() {
    if (!standardPage.selectRevisions()) return true;
    return super.isPageComplete();
  }

  protected ILogEntry[] getEligibleLogEntries(IProgressMonitor monitor) throws TeamException {
    GetEligibleRevisionsCommand logCmd = new GetEligibleRevisionsCommand();
    logCmd.run(monitor);
    return logCmd.getLogEntries();
  }

  private SVNRevision.Number[] getAllRevisions() {
    SVNRevision.Number[] entryArray = new SVNRevision.Number[entries.length];
    for (int i = 0; i < entries.length; i++) {
      entryArray[i] = entries[i].getRevision();
    }
    return entryArray;
  }

  private SVNRevision.Number[] getSelectedRevisions() {
    SVNRevision.Number[] entryArray = new SVNRevision.Number[selectedRevisions.size()];
    selectedRevisions.toArray(entryArray);
    return entryArray;
  }

  public ILogEntry[] getSelectedLogEntries() {
    ArrayList<ILogEntry> selectedEntries = new ArrayList<ILogEntry>();
    TableItem[] items = tableHistoryViewer.getTable().getItems();
    for (int i = 0; i < items.length; i++) {
      if (items[i].getChecked()) {
        ILogEntry entry = (ILogEntry) items[i].getData();
        selectedEntries.add(entry);
      }
    }
    ILogEntry[] entryArray = new ILogEntry[selectedEntries.size()];
    selectedEntries.toArray(entryArray);
    return entryArray;
  }

  public SVNRevisionRange[] getRevisions() {
    return SVNRevisionRange.getRevisions(getSelectedRevisions(), getAllRevisions());
  }

  public Map<SVNRevision.Number, List<IResource>> getRevisionToResource() {
    return revisionToResource;
  }

  @Override
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

  private String getUrl() {
    if (fromUrl != null) {
      try {
        SVNUrlWithPegRevision svnUrlWithPegRevision =
            new SVNUrlWithPegRevision(new SVNUrl(fromUrl));
        SVNUrl svnUrl = svnUrlWithPegRevision.getUrl();
        if (svnUrl != null) return svnUrl.toString();
      } catch (MalformedURLException e) {
      }
    }
    return fromUrl;
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

    @Override
    public void run() {
      TableItem[] items = tableHistoryViewer.getTable().getSelection();
      for (int i = 0; i < items.length; i++) {
        items[i].setChecked(!items[i].getChecked());
        ILogEntry entry = adaptToLogEntry(items[i].getData());
        if (entry != null) {
          SVNRevision.Number revision = entry.getRevision();
          if (items[i].getChecked()) {
            selectedRevisions.add(revision);
          } else {
            selectedRevisions.remove(revision);
          }
        }
      }
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
      Set<String> folderNames = new HashSet<String>();
      for (int i = 0; i < changePaths.length; i++) {
        folderNames.add(getFolderName(changePaths[i]));
      }

      // 2nd pass. Sorting out explicitly changed folders
      TreeMap<String, HistoryFolder> folders = new TreeMap<String, HistoryFolder>();
      for (int i = 0; i < changePaths.length; i++) {
        LogEntryChangePath changePath = changePaths[i];
        String path = changePath.getPath();
        if (folderNames.contains(path)) {
          // changed folder
          HistoryFolder folder = folders.get(path);
          if (folder == null) {
            folder = new HistoryFolder(changePath);
            folders.put(path, folder);
          }
        } else {
          // changed resource
          path = getFolderName(changePath);
          HistoryFolder folder = folders.get(path);
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

  class GetEligibleRevisionsCommand implements ISVNCommand {
    private ILogEntry[] logEntries;

    public GetEligibleRevisionsCommand() {
      super();
    }

    public void run(IProgressMonitor aMonitor) throws SVNException {
      logEntries = null;
      IProgressMonitor monitor = Policy.monitorFor(aMonitor);
      int taskLength;
      if (settings.getBoolean(MergeWizard.LAST_RETRIEVE_ELIGIBLE_REVISIONS_SEPARATELY)) {
        taskLength = standardPage.getUrls().length;
      } else {
        taskLength = IProgressMonitor.UNKNOWN;
      }
      monitor.beginTask(Messages.MergeWizardRevisionsPage_retrievingLogEntries, taskLength);

      ISVNLogMessage[] logMessages = null;
      ISVNClientAdapter client = null;
      try {
        client = remoteResource.getRepository().getSVNClient();
        SVNProviderPlugin.disableConsoleLogging();
        if (resources.length > 1
            && settings.getBoolean(MergeWizard.LAST_RETRIEVE_ELIGIBLE_REVISIONS_SEPARATELY)) {
          logMessages = getCombinedLogMessages(client, monitor);
        } else {
          String commonRoot = ((MergeWizard) getWizard()).getCommonRoot();
          SVNUrl rootUrl = new SVNUrl(commonRoot);
          logMessages =
              client.getMergeinfoLog(
                  ISVNMergeinfoLogKind.eligible,
                  rootUrl,
                  SVNRevision.HEAD,
                  remoteResource.getUrl(),
                  SVNRevision.HEAD,
                  !SVNProviderPlugin.getPlugin().getSVNClientManager().isFetchChangePathOnDemand());
        }
        if (remoteResource.isFolder()) {
          logEntries =
              LogEntry.createLogEntriesFrom(
                  (ISVNRemoteFolder) remoteResource, logMessages, getTags(logMessages));
        } else {
          logEntries =
              LogEntry.createLogEntriesFrom(
                  (ISVNRemoteFile) remoteResource,
                  logMessages,
                  getTags(logMessages),
                  getUrls(logMessages));
        }
      } catch (Exception e) {
        throw SVNException.wrapException(e);
      } finally {
        SVNProviderPlugin.enableConsoleLogging();
        remoteResource.getRepository().returnSVNClient(client);
        monitor.done();
      }
    }

    private ISVNLogMessage[] getCombinedLogMessages(
        ISVNClientAdapter client, IProgressMonitor monitor) throws Exception {
      List<SVNRevision.Number> revisions = new ArrayList<SVNRevision.Number>();
      List<ISVNLogMessage> logMessages = new ArrayList<ISVNLogMessage>();
      SVNUrl[] urls = standardPage.getUrls();
      for (int i = 0; i < resources.length; i++) {
        IResource resource = resources[i];
        monitor.subTask(resource.getName());
        SVNUrl remoteUrl = urls[i];
        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
        SVNUrl url = svnResource.getUrl();
        ISVNLogMessage[] resourceLogMessages = null;
        try {
          resourceLogMessages =
              client.getMergeinfoLog(
                  ISVNMergeinfoLogKind.eligible,
                  url,
                  SVNRevision.HEAD,
                  remoteUrl,
                  SVNRevision.HEAD,
                  !SVNProviderPlugin.getPlugin().getSVNClientManager().isFetchChangePathOnDemand());
        } catch (Exception e) {
          Activator.handleError(e);
        }
        if (resourceLogMessages != null) {
          for (ISVNLogMessage logMessage : resourceLogMessages) {
            if (!revisions.contains(logMessage.getRevision())) {
              revisions.add(logMessage.getRevision());
              logMessages.add(logMessage);
            }
            List<IResource> lst = revisionToResource.get(logMessage.getRevision());
            if (lst == null) {
              lst = new ArrayList<IResource>(resources.length);
              revisionToResource.put(logMessage.getRevision(), lst);
            }
            lst.add(resource);
          }
        }
        monitor.worked(1);
      }
      ISVNLogMessage[] logMessageArray = new ISVNLogMessage[logMessages.size()];
      logMessages.toArray(logMessageArray);
      return logMessageArray;
    }

    private SVNUrl[] fillUrlsWith(SVNUrl[] urls, SVNUrl url) {
      for (int i = 0; i < urls.length; i++) {
        urls[i] = url;
      }
      return urls;
    }

    private SVNUrl[] getUrls(ISVNLogMessage[] logMessages) {
      SVNUrl[] urls = new SVNUrl[logMessages.length];

      SVNUrl rootRepositoryUrl = remoteResource.getRepository().getRepositoryRoot();
      if (rootRepositoryUrl == null) {
        // don't know the root repository url, we consider that resource has never been moved
        // and so that the url was always the same
        return fillUrlsWith(urls, remoteResource.getUrl());
      }

      // we identify the logMessage corresponding to the revision
      // of the remote resource
      int indexRemote = -1;
      for (int i = 0; i < logMessages.length; i++) {
        if (logMessages[i].getRevision().equals(remoteResource.getLastChangedRevision())) {
          indexRemote = i;
          break;
        }
      }
      if (indexRemote > -1) {
        urls[indexRemote] = remoteResource.getUrl();
      }

      // we get the url of more recent revisions
      SVNUrl currentUrl = remoteResource.getUrl();
      for (int i = indexRemote + 1; i < logMessages.length; i++) {
        ISVNLogMessageChangePath[] changePaths = logMessages[i].getChangedPaths();
        for (int j = 0; j < changePaths.length; j++) {
          SVNUrl urlChangedPath = rootRepositoryUrl.appendPath(changePaths[j].getPath());
          if (currentUrl.equals(urlChangedPath)) {
            urls[i] = currentUrl;
            break;
          }
          if (changePaths[j].getCopySrcPath() != null) {
            SVNUrl urlCopyPath = rootRepositoryUrl.appendPath(changePaths[j].getCopySrcPath());
            if (currentUrl.equals(urlCopyPath)) {
              currentUrl = rootRepositoryUrl.appendPath(changePaths[j].getPath());
              urls[i] = currentUrl;
              break;
            }
          }
        }
        if (urls[i] == null) {
          // something went wrong
          return fillUrlsWith(urls, remoteResource.getUrl());
        }
      }

      // we get the url of previous revisions
      currentUrl = remoteResource.getUrl();
      for (int i = indexRemote - 1; i >= 0; i--) {
        ISVNLogMessageChangePath[] changePaths = logMessages[i].getChangedPaths();
        for (int j = 0; j < changePaths.length; j++) {
          SVNUrl urlChangedPath = rootRepositoryUrl.appendPath(changePaths[j].getPath());
          if (currentUrl.equals(urlChangedPath)) {
            urls[i] = currentUrl;

            if (changePaths[j].getCopySrcPath() != null) {
              SVNUrl urlCopyPath = rootRepositoryUrl.appendPath(changePaths[j].getCopySrcPath());
              currentUrl = urlCopyPath;
            }
            break;
          }
        }
        if (urls[i] == null) {
          // something went wrong
          return fillUrlsWith(urls, remoteResource.getUrl());
        }
      }
      return urls;
    }

    private Tags[] getTags(ISVNLogMessage[] logMessages) throws NumberFormatException {
      Tags[] tags = new Tags[logMessages.length];
      for (int i = 0; i < logMessages.length; i++) {
        if (tagManager != null) {
          String rev = logMessages[i].getRevision().toString();
          int revNo = Integer.parseInt(rev);
          tags[i] = new Tags(tagManager.getTags(revNo));
        }
      }
      return tags;
    }

    public ILogEntry[] getLogEntries() {
      return logEntries;
    }
  }

  class TextViewFilter extends ViewerFilter {
    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if (filterText.getText().trim().length() > 0 && element instanceof LogEntry) {
        ILogEntry entry = adaptToLogEntry(element);
        if (entry != null) {
          String filterString = filterText.getText().trim();
          String revision = entry.getRevision().toString();
          if (revision.contains(filterString)) {
            return true;
          }
          String mergedRevisions = entry.getMergedRevisionsAsString();
          if (mergedRevisions != null && mergedRevisions.contains(filterString)) {
            return true;
          }
          String tags = AliasManager.getAliasesAsString(entry.getTags());
          if (tags != null && tags.contains(filterString)) {
            return true;
          }
          Date date = entry.getDate();
          String dateString;
          if (date == null) {
            dateString = ""; // $NON-NLS-1$
          } else {
            dateString = DateFormat.getInstance().format(date);
          }
          if (dateString.contains(filterString)) {
            return true;
          }
          String author;
          if (entry.getAuthor() == null) {
            author = ""; // $NON-NLS-1$
          } else {
            author = entry.getAuthor();
          }
          if (author.contains(filterString)) {
            return true;
          }
          String comment = entry.getComment();
          if (comment != null && comment.contains(filterString)) {
            return true;
          }
          return false;
        }
      }
      return true;
    }
  };
}
