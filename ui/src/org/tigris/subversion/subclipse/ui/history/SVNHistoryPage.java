/*******************************************************************************
 * Copyright (c) 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.history;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.team.ui.history.IHistoryPageSite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.tigris.subversion.subclipse.core.IResourceStateChangeListener;
import org.tigris.subversion.subclipse.core.ISVNLocalFile;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNStatus;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.ChangeCommitPropertiesCommand;
import org.tigris.subversion.subclipse.core.commands.GetLogsCommand;
import org.tigris.subversion.subclipse.core.history.AliasManager;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntryChangePath;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.actions.OpenRemoteFileAction;
import org.tigris.subversion.subclipse.ui.actions.WorkspaceAction;
import org.tigris.subversion.subclipse.ui.console.TextViewerAction;
import org.tigris.subversion.subclipse.ui.dialogs.BranchTagDialog;
import org.tigris.subversion.subclipse.ui.dialogs.HistorySearchDialog;
import org.tigris.subversion.subclipse.ui.dialogs.SetCommitPropertiesDialog;
import org.tigris.subversion.subclipse.ui.internal.Utils;
import org.tigris.subversion.subclipse.ui.operations.BranchTagOperation;
import org.tigris.subversion.subclipse.ui.operations.MergeOperation;
import org.tigris.subversion.subclipse.ui.operations.ReplaceOperation;
import org.tigris.subversion.subclipse.ui.settings.ProjectProperties;
import org.tigris.subversion.subclipse.ui.util.EmptySearchViewerFilter;
import org.tigris.subversion.subclipse.ui.util.LinkList;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * <code>IHistoryPage</code> for generic history view 
 * 
 * @author Eugene Kuleshov (migration from legacy history view)
 */
public class SVNHistoryPage extends HistoryPage implements IResourceStateChangeListener, KeyListener {

  private SashForm svnHistoryPageControl;
  private SashForm innerSashForm;
  private HistorySearchDialog historySearchDialog;

  HistoryTableProvider historyTableProvider;
  TableViewer tableHistoryViewer;
  StructuredViewer changePathsViewer;
  TextViewer textViewer;

  private boolean showComments;
  private boolean showAffectedPaths;
  private boolean wrapCommentsText;
  boolean shutdown = false;

  private ProjectProperties projectProperties;

  // cached for efficiency
  ILogEntry[] entries;
  LogEntryChangePath[] currentLogEntryChangePath;
  ILogEntry lastEntry;
  SVNRevision revisionStart = SVNRevision.HEAD;

  AbstractFetchJob fetchLogEntriesJob = null;
  AbstractFetchJob fetchAllLogEntriesJob = null;
  AbstractFetchJob fetchNextLogEntriesJob = null;
  FetchChangePathJob fetchChangePathJob = null;
  AliasManager tagManager;

  IResource resource;
  ISVNRemoteResource remoteResource;
  ISelection selection;

  private IAction searchAction;
  private IAction clearSearchAction;
  private IAction getNextAction;
  private IAction getAllAction;
  private IAction toggleStopOnCopyAction;
  private IAction toggleShowComments;
  private IAction toggleWrapCommentsAction;
  private IAction toggleShowAffectedPathsAction;

  private IAction openAction;
  private IAction getContentsAction;
  private IAction updateToRevisionAction;
  private IAction openChangedPathAction;
  private IAction showDifferencesAsUnifiedDiffAction;
  private IAction createTagFromRevisionAction;
  private IAction setCommitPropertiesAction;
  private IAction revertChangesAction;
  private IAction refreshAction;

  private ToggleAffectedPathsOptionAction[] toggleAffectedPathsLayoutActions;
  private ToggleAffectedPathsOptionAction[] toggleAffectedPathsModeActions;

  private TextViewerAction copyAction;
  private TextViewerAction selectAllAction;

  private LinkList linkList;
  private Cursor handCursor;
  private Cursor busyCursor;

  
  public SVNHistoryPage(Object object) {
	  SVNProviderPlugin.addResourceStateChangeListener(this);
  }

  public void dispose() {
    super.dispose();

    SVNProviderPlugin.removeResourceStateChangeListener(this);

    if(busyCursor!=null) {
      busyCursor.dispose();
    }
    if(handCursor!=null) {
      handCursor.dispose();
    }
  }
  
  public Control getControl() {
    return svnHistoryPageControl;
  }

  public void setFocus() {
    // TODO Auto-generated method stub

  }

  public String getDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getName() {
    return remoteResource == null ? null : remoteResource.getRepositoryRelativePath() + " in "
        + remoteResource.getRepository();
  }

  public boolean isValidInput(Object object) {
    if(object instanceof IResource) {
      RepositoryProvider provider = RepositoryProvider.getProvider(((IResource) object).getProject());
      return provider instanceof SVNTeamProvider;
    } else if(object instanceof ISVNRemoteResource) {
      return true;
    }

    // TODO
    // } else if(object instanceof CVSFileRevision) {
    // return true;
    // } else if(object instanceof CVSLocalFileRevision) {
    // return true;

    return false;
  }

  public void refresh() {
    entries = null;
    lastEntry = null;
    revisionStart = SVNRevision.HEAD;
    // show a Busy Cursor during refresh
    BusyIndicator.showWhile(tableHistoryViewer.getTable().getDisplay(), new Runnable() {
      public void run() {
        if(resource != null) {
          try {
        	  remoteResource = SVNWorkspaceRoot.getBaseResourceFor(resource);
        	  historyTableProvider.setRemoteResource(remoteResource);
              projectProperties = ProjectProperties.getProjectProperties(resource);
          } catch(SVNException e) {
          }
        }
        tableHistoryViewer.refresh();
       	tableHistoryViewer.resetFilters();
       	getClearSearchAction().setEnabled(false);
      }
    });
  }

  public Object getAdapter(Class adapter) {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean inputSet() {
    Object input = getInput();

    if(input instanceof IResource) {
      IResource res = (IResource) input;
      RepositoryProvider teamProvider = RepositoryProvider.getProvider(res.getProject(), SVNProviderPlugin.getTypeId());
      if(teamProvider != null) {
        try {
          ISVNLocalResource localResource = SVNWorkspaceRoot.getSVNResourceFor(res);
          LocalResourceStatus localResourceStatus = (localResource != null) ? localResource.getStatus() : null;
          if(localResource != null && localResourceStatus.isManaged() && (!localResourceStatus.isAdded() || localResourceStatus.isCopied())) {
            this.resource = res;
            this.remoteResource = localResource.getBaseResource();

            this.projectProperties = ProjectProperties.getProjectProperties(res);
            this.historyTableProvider.setRemoteResource(this.remoteResource);
            this.tableHistoryViewer.setInput(this.remoteResource);
        	this.tableHistoryViewer.resetFilters();
        	getClearSearchAction().setEnabled(false);
            // setContentDescription(Policy.bind("HistoryView.titleWithArgument",
            // baseResource.getName())); //$NON-NLS-1$
            // setTitleToolTip(baseResource.getRepositoryRelativePath());
            return true;
          }
        } catch(TeamException e) {
          SVNUIPlugin.openError(getSite().getShell(), null, null, e);
        }
      }

    } else if(input instanceof ISVNRemoteResource) {
      this.resource = null;
      this.remoteResource = (ISVNRemoteResource) input;

      this.projectProperties = ProjectProperties.getProjectProperties(this.remoteResource);
      this.historyTableProvider.setRemoteResource(this.remoteResource);
      this.tableHistoryViewer.setInput(this.remoteResource);
  	  this.tableHistoryViewer.resetFilters();
  	  getClearSearchAction().setEnabled(false);
      // setContentDescription(Policy.bind("HistoryView.titleWithArgument",
      // remoteResource.getName())); //$NON-NLS-1$
      // setTitleToolTip(remoteResource.getRepositoryRelativePath());
      return true;

    }

    return false;
  }

  public void createControl(Composite parent) {
    this.busyCursor = new Cursor(parent.getDisplay(), SWT.CURSOR_WAIT);
    this.handCursor = new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);

    IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
    this.showComments = store.getBoolean(ISVNUIConstants.PREF_SHOW_COMMENTS);
    this.wrapCommentsText = store.getBoolean(ISVNUIConstants.PREF_WRAP_COMMENTS);
    this.showAffectedPaths = store.getBoolean(ISVNUIConstants.PREF_SHOW_PATHS);

    this.svnHistoryPageControl = new SashForm(parent, SWT.VERTICAL);
    this.svnHistoryPageControl.setLayoutData(new GridData(GridData.FILL_BOTH));

    this.toggleAffectedPathsModeActions = new ToggleAffectedPathsOptionAction[] {
        new ToggleAffectedPathsOptionAction(this, "HistoryView.affectedPathsFlatLayout", 
            ISVNUIConstants.IMG_AFFECTED_PATHS_FLAT_MODE, 
            ISVNUIConstants.PREF_AFFECTED_PATHS_MODE, 
            ISVNUIConstants.MODE_FLAT),
        new ToggleAffectedPathsOptionAction(this, "HistoryView.affectedPathsCompressedLayout", 
            ISVNUIConstants.IMG_AFFECTED_PATHS_COMPRESSED_MODE, 
            ISVNUIConstants.PREF_AFFECTED_PATHS_MODE, 
            ISVNUIConstants.MODE_COMPRESSED),
      };
    
    this.toggleAffectedPathsLayoutActions = new ToggleAffectedPathsOptionAction[] {
        new ToggleAffectedPathsOptionAction(this, "HistoryView.affectedPathsHorizontalLayout", 
            ISVNUIConstants.IMG_AFFECTED_PATHS_HORIZONTAL_LAYOUT, 
            ISVNUIConstants.PREF_AFFECTED_PATHS_LAYOUT, 
            ISVNUIConstants.LAYOUT_HORIZONTAL),
        new ToggleAffectedPathsOptionAction(this, "HistoryView.affectedPathsVerticalLayout", 
            ISVNUIConstants.IMG_AFFECTED_PATHS_VERTICAL_LAYOUT, 
            ISVNUIConstants.PREF_AFFECTED_PATHS_LAYOUT, 
            ISVNUIConstants.LAYOUT_VERTICAL),
      };
    
    createTableHistory(svnHistoryPageControl);
    createAffectedPathsViewer();
    contributeActions();

    svnHistoryPageControl.setWeights(new int[] { 70, 30});

    // set F1 help
    // PlatformUI.getWorkbench().getHelpSystem().setHelp(svnHistoryPageControl,
    // IHelpContextIds.RESOURCE_HISTORY_VIEW);
    // initDragAndDrop();

    // add listener for editor page activation - this is to support editor
    // linking
    // getSite().getPage().addPartListener(partListener);
    // getSite().getPage().addPartListener(partListener2);
  }

  protected void createTableHistory(Composite parent) {
    this.historyTableProvider = new HistoryTableProvider();
    this.tableHistoryViewer = historyTableProvider.createTable(parent);
    this.tableHistoryViewer.getTable().addKeyListener(this);
    // set the content provider for the table
    this.tableHistoryViewer.setContentProvider(new IStructuredContentProvider() {

      public Object[] getElements(Object inputElement) {
        // Short-circuit to optimize
        if(entries != null)
          return entries;

        if( !(inputElement instanceof ISVNRemoteResource))
          return null;
        final ISVNRemoteResource remoteResource = (ISVNRemoteResource) inputElement;

        IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
        int entriesToFetch = store.getInt(ISVNUIConstants.PREF_LOG_ENTRIES_TO_FETCH);
        if (entriesToFetch > 0)
        	fetchLogEntriesJob = new FetchLogEntriesJob();
        else
        	fetchLogEntriesJob = new FetchAllLogEntriesJob();
        if(fetchLogEntriesJob.getState() != Job.NONE) {
          fetchLogEntriesJob.cancel();
          try {
            fetchLogEntriesJob.join();
          } catch(InterruptedException e) {
            SVNUIPlugin.log(new SVNException(
                Policy.bind("HistoryView.errorFetchingEntries", remoteResource.getName()), e)); //$NON-NLS-1$
          }
        }
        fetchLogEntriesJob.setRemoteFile(remoteResource);
        Utils.schedule(fetchLogEntriesJob, SVNUIPlugin.getPlugin().getWorkbench().getActiveWorkbenchWindow()
            .getActivePage().getActivePart().getSite());

        return new Object[ 0];
      }

      public void dispose() {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        entries = null;
        lastEntry = null;
        revisionStart = SVNRevision.HEAD;
      }
    });

    // set the selectionchanged listener for the table
    // updates the comments and affected paths when selection changes
    this.tableHistoryViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      private ILogEntry currentLogEntry;

      public void selectionChanged(SelectionChangedEvent event) {
        ISelection selection = event.getSelection();
        ILogEntry logEntry = getLogEntry((IStructuredSelection) selection);
        if(logEntry != currentLogEntry) {
          this.currentLogEntry = logEntry;
          updatePanels(selection);
        }

        SVNHistoryPage.this.selection = selection;
      }
    });

    // Double click open action
    this.tableHistoryViewer.getTable().addListener(SWT.DefaultSelection, new Listener() {
      public void handleEvent(Event e) {
        getOpenRemoteFileAction().run();
      }
    });

    // Contribute actions to popup menu for the table
    {
      MenuManager menuMgr = new MenuManager();
      Menu menu = menuMgr.createContextMenu(tableHistoryViewer.getTable());
      menuMgr.addMenuListener(new IMenuListener() {
        public void menuAboutToShow(IMenuManager menuMgr) {
          fillTableMenu(menuMgr);
        }
      });
      menuMgr.setRemoveAllWhenShown(true);
      tableHistoryViewer.getTable().setMenu(menu);
      getHistoryPageSite().getPart().getSite().registerContextMenu(menuMgr, tableHistoryViewer);
    }
  }

  private void fillTableMenu(IMenuManager manager) {
    // file actions go first (view file)
    manager.add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
    // Add the "Add to Workspace" action if 1 revision is selected.
    ISelection sel = tableHistoryViewer.getSelection();
    if( !sel.isEmpty()) {
      if(sel instanceof IStructuredSelection) {
        if(((IStructuredSelection) sel).size() == 1) {
          if(resource != null && resource instanceof IFile) {
            manager.add(getGetContentsAction());
            manager.add(getUpdateToRevisionAction());
          }
          manager.add(getShowDifferencesAsUnifiedDiffAction());
          // if (resource != null) {
          manager.add(getCreateTagFromRevisionAction());
          // }
          manager.add(getSetCommitPropertiesAction());
        }
        if(resource != null)
          manager.add(getRevertChangesAction());
        
        manager.add(new Separator("exportImportGroup")); //$NON-NLS-1$
      }
    }
    manager.add(new Separator("additions")); //$NON-NLS-1$
    manager.add(getRefreshAction());
    manager.add(new Separator("additions-end")); //$NON-NLS-1$
  }

  public void createAffectedPathsViewer() {
    int[] weights = null;
    weights = svnHistoryPageControl.getWeights();
    if(innerSashForm != null) {
      innerSashForm.dispose();
    }
    if(changePathsViewer != null) {
      changePathsViewer.getControl().dispose();
    }
    
    IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
    int mode = store.getInt(ISVNUIConstants.PREF_AFFECTED_PATHS_MODE);
    int layout = store.getInt(ISVNUIConstants.PREF_AFFECTED_PATHS_LAYOUT);
    
    if(layout==ISVNUIConstants.LAYOUT_HORIZONTAL) {
      innerSashForm = new SashForm(svnHistoryPageControl, SWT.HORIZONTAL);
    } else {
      innerSashForm = new SashForm(svnHistoryPageControl, SWT.VERTICAL);
      createText(innerSashForm);
    }

    switch(mode) {
      case ISVNUIConstants.MODE_COMPRESSED:
        changePathsViewer = new ChangePathsTreeViewer(innerSashForm, this);
        break;
      default:
        changePathsViewer = new ChangePathsTableProvider(innerSashForm, this);
        break;
    }
    
    changePathsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        SVNHistoryPage.this.selection = changePathsViewer.getSelection();
      }
    });

    changePathsViewer.getControl().addListener(SWT.DefaultSelection, new Listener() {
      public void handleEvent(Event e) {
        getOpenChangedPathAction().run();
      }
    });

    if(layout==ISVNUIConstants.LAYOUT_HORIZONTAL) {
      createText(innerSashForm);
    }
    
    setViewerVisibility();
    
    innerSashForm.layout();
    if(weights!=null && weights.length==2) {
      svnHistoryPageControl.setWeights(weights);
    }
    svnHistoryPageControl.layout();

    updatePanels(tableHistoryViewer.getSelection());
  }

  /**
   * Create the TextViewer for the logEntry comments
   */
  protected void createText(Composite parent) {
    // this.textViewer = new TextViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY);
    SourceViewer result = new SourceViewer(parent, null, null, true, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY);
    result.getTextWidget().setIndent(2);
    result.configure(new TextSourceViewerConfiguration(EditorsUI.getPreferenceStore()) {
      public Map getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
        return Collections.singletonMap("org.eclipse.ui.DefaultTextEditor", //$NON-NLS-1$
            new IAdaptable() {
              public Object getAdapter(Class adapter) {
                if(adapter==IResource.class && getInput() instanceof IResource) {
                  return getInput();
                } else if(adapter==ISVNRemoteResource.class && getInput() instanceof ISVNRemoteResource) {
                  return getInput();
                }
                return Platform.getAdapterManager().getAdapter(SVNHistoryPage.this, adapter);
              }
            });
      }
      
      public int getHyperlinkStateMask(ISourceViewer sourceViewer) {
        return SWT.NONE;
      }
      
      public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
        IHyperlinkDetector[] detectors = super.getHyperlinkDetectors(sourceViewer);
        IHyperlinkDetector[] newDetectors;
        if(detectors==null) {
          newDetectors = new IHyperlinkDetector[1];
        } else {
          newDetectors = new IHyperlinkDetector[detectors.length + 1];
          System.arraycopy(detectors, 0, newDetectors, 0, detectors.length);
        }
        
        newDetectors[newDetectors.length - 1] = new IHyperlinkDetector() {
          public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
              IRegion region, boolean canShowMultipleHyperlinks) {
            if(linkList==null || !linkList.isLinkAt(region.getOffset())) {
              return null;
            }

            final String linkUrl = linkList.getLinkAt(region.getOffset());
            final int[] linkRange = linkList.getLinkRange(region.getOffset());
            
            return new IHyperlink[] { new IHyperlink() {
              public IRegion getHyperlinkRegion() {
                return new Region(linkRange[0], linkRange[1]);
              }
              
              public void open() {
                try {
                  URL url = new URL(linkUrl);
                  PlatformUI.getWorkbench().getBrowserSupport().createBrowser("Subclipse").openURL(url);
                } catch (Exception e1) {
                  Program.launch(linkUrl);
                }
              }

              public String getHyperlinkText() {
                return null;
              }

              public String getTypeLabel() {
                return null;
              }
            }};
          }
        };
        
        return newDetectors;
      }
    });
    
    this.textViewer = result;
    
    this.textViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        copyAction.update();
      }
    });

    Font font = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry().get(
        ISVNUIConstants.SVN_COMMENT_FONT);
    if(font != null) {
      this.textViewer.getTextWidget().setFont(font);
    }

    // Create actions for the text editor (copy and select all)
    copyAction = new TextViewerAction(this.textViewer, ITextOperationTarget.COPY);
    copyAction.setText(Policy.bind("HistoryView.copy")); //$NON-NLS-1$

    selectAllAction = new TextViewerAction(this.textViewer, ITextOperationTarget.SELECT_ALL);
    selectAllAction.setText(Policy.bind("HistoryView.selectAll")); //$NON-NLS-1$

    IHistoryPageSite parentSite = getHistoryPageSite();
    IPageSite pageSite = parentSite.getWorkbenchPageSite();
    IActionBars actionBars = pageSite.getActionBars();

    actionBars.setGlobalActionHandler(ITextEditorActionConstants.COPY, copyAction);
    actionBars.setGlobalActionHandler(ITextEditorActionConstants.SELECT_ALL, selectAllAction);
    actionBars.updateActionBars();

    // Contribute actions to popup menu for the comments area
    {
      MenuManager menuMgr = new MenuManager();
      menuMgr.setRemoveAllWhenShown(true);
      menuMgr.addMenuListener(new IMenuListener() {
        public void menuAboutToShow(IMenuManager menuMgr) {
          menuMgr.add(copyAction);
          menuMgr.add(selectAllAction);
        }
      });

      StyledText text = this.textViewer.getTextWidget();
      Menu menu = menuMgr.createContextMenu(text);
      text.setMenu(menu);
    }
  }

  private void contributeActions() {
    SVNUIPlugin plugin = SVNUIPlugin.getPlugin();
    final IPreferenceStore store = plugin.getPreferenceStore();

    toggleShowComments = new Action(Policy.bind("HistoryView.showComments"), //$NON-NLS-1$
        SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_COMMENTS)) {
      public void run() {
        showComments = isChecked();
        setViewerVisibility();
        store.setValue(ISVNUIConstants.PREF_SHOW_COMMENTS, showComments);
      }
    };
    toggleShowComments.setChecked(showComments);
    // PlatformUI.getWorkbench().getHelpSystem().setHelp(toggleTextAction,
    // IHelpContextIds.SHOW_COMMENT_IN_HISTORY_ACTION);

    // Toggle wrap comments action
    toggleWrapCommentsAction = new Action(Policy.bind("HistoryView.wrapComments")) { //$NON-NLS-1$
      public void run() {
        wrapCommentsText = isChecked();
        setViewerVisibility();
        store.setValue(ISVNUIConstants.PREF_WRAP_COMMENTS, wrapCommentsText);
      }
    };
    toggleWrapCommentsAction.setChecked(wrapCommentsText);
    // PlatformUI.getWorkbench().getHelpSystem().setHelp(toggleTextWrapAction,
    // IHelpContextIds.SHOW_TAGS_IN_HISTORY_ACTION);

    // Toggle path visible action
    toggleShowAffectedPathsAction = new Action(Policy.bind("HistoryView.showAffectedPaths"), //$NON-NLS-1$
        SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_AFFECTED_PATHS_FLAT_MODE)) {
      public void run() {
        showAffectedPaths = isChecked();
        setViewerVisibility();
        store.setValue(ISVNUIConstants.PREF_SHOW_PATHS, showAffectedPaths);
      }
    };
    toggleShowAffectedPathsAction.setChecked(showAffectedPaths);
    // PlatformUI.getWorkbench().getHelpSystem().setHelp(toggleListAction,
    // IHelpContextIds.SHOW_TAGS_IN_HISTORY_ACTION);

    // Toggle stop on copy action
    toggleStopOnCopyAction = new Action(Policy.bind("HistoryView.stopOnCopy")) { //$NON-NLS-1$
      public void run() {
        refresh();
        SVNUIPlugin.getPlugin().getPreferenceStore().setValue(ISVNUIConstants.PREF_STOP_ON_COPY,
            toggleStopOnCopyAction.isChecked());
      }
    };
    toggleStopOnCopyAction.setChecked(store.getBoolean(ISVNUIConstants.PREF_STOP_ON_COPY));
    
    IHistoryPageSite parentSite = getHistoryPageSite();
    IPageSite pageSite = parentSite.getWorkbenchPageSite();
    IActionBars actionBars = pageSite.getActionBars();

    // Contribute toggle text visible to the toolbar drop-down
    IMenuManager actionBarsMenu = actionBars.getMenuManager();
    actionBarsMenu.add(getGetNextAction());
    actionBarsMenu.add(getGetAllAction());
    actionBarsMenu.add(toggleStopOnCopyAction);
    actionBarsMenu.add(new Separator());
    actionBarsMenu.add(toggleWrapCommentsAction);
    actionBarsMenu.add(new Separator());
    actionBarsMenu.add(toggleShowComments);
    actionBarsMenu.add(toggleShowAffectedPathsAction);
    actionBarsMenu.add(new Separator());
    actionBarsMenu.add(toggleAffectedPathsModeActions[0]);
    actionBarsMenu.add(toggleAffectedPathsModeActions[1]);
    actionBarsMenu.add(new Separator());
    actionBarsMenu.add(toggleAffectedPathsLayoutActions[0]);
    actionBarsMenu.add(toggleAffectedPathsLayoutActions[1]);
    
    // Create the local tool bar
    IToolBarManager tbm = actionBars.getToolBarManager();
    // tbm.add(getRefreshAction());
    tbm.add(new Separator());
    tbm.add(getSearchAction());
    tbm.add(getClearSearchAction());
    tbm.add(new Separator());
    tbm.add(toggleShowComments);
    tbm.add(toggleShowAffectedPathsAction);
    tbm.add(new Separator());
    tbm.add(getGetNextAction());
    tbm.add(getGetAllAction());
    // tbm.add(getLinkWithEditorAction());
    tbm.update(false);
    
    actionBars.updateActionBars();  
  }

  ILogEntry getLogEntry(IStructuredSelection ss) {
    if(ss.getFirstElement() instanceof LogEntryChangePath) {
      return ((LogEntryChangePath) ss.getFirstElement()).getLogEntry();
    }
    return (ILogEntry) ss.getFirstElement();
  }

  void updatePanels(ISelection selection) {
    if(selection == null || !(selection instanceof IStructuredSelection)) {
      textViewer.setDocument(new Document("")); //$NON-NLS-1$
      changePathsViewer.setInput(null);
      return;
    }
    IStructuredSelection ss = (IStructuredSelection) selection;
    if(ss.size() != 1) {
      textViewer.setDocument(new Document("")); //$NON-NLS-1$
      changePathsViewer.setInput(null);
      return;
    }
    LogEntry entry = (LogEntry) ss.getFirstElement();
    textViewer.setDocument(new Document(entry.getComment()));
    StyledText text = textViewer.getTextWidget();
    
    // TODO move this logic into the hyperlink detector created in createText()
    if(projectProperties == null) {
      linkList = ProjectProperties.getUrls(entry.getComment());
    } else {
      linkList = projectProperties.getLinkList(entry.getComment());
    }
    if(linkList != null) {
      int[][] linkRanges = linkList.getLinkRanges();
      // String[] urls = linkList.getUrls();
      for(int i = 0; i < linkRanges.length; i++) {
        text.setStyleRange(new StyleRange(linkRanges[ i][ 0], linkRanges[ i][ 1], 
            JFaceColors.getHyperlinkText(Display.getCurrent()), null));
      }
    }
    changePathsViewer.setInput(entry);
  }

  void setViewerVisibility() {
    if(showComments && showAffectedPaths) {
      svnHistoryPageControl.setMaximizedControl(null);
      innerSashForm.setMaximizedControl(null);
    } else if(showComments) {
      svnHistoryPageControl.setMaximizedControl(null);
      innerSashForm.setMaximizedControl(textViewer.getTextWidget());
    } else if(showAffectedPaths) {
      svnHistoryPageControl.setMaximizedControl(null);
      innerSashForm.setMaximizedControl(changePathsViewer.getControl());
    } else {
      svnHistoryPageControl.setMaximizedControl(tableHistoryViewer.getControl());
    }
    changePathsViewer.refresh();
    textViewer.getTextWidget().setWordWrap(wrapCommentsText);
  }

  void setCurrentLogEntryChangePath(final LogEntryChangePath[] currentLogEntryChangePath) {
    this.currentLogEntryChangePath = currentLogEntryChangePath;
    if( !shutdown) {
      // Getting the changePaths
      /*
       * final SVNRevision.Number revisionId =
       * remoteResource.getLastChangedRevision();
       */
      getSite().getShell().getDisplay().asyncExec(new Runnable() {
        public void run() {
          if(currentLogEntryChangePath != null && changePathsViewer != null
              && !changePathsViewer.getControl().isDisposed()) {
            // once we got the changePath, we refresh the table
            changePathsViewer.refresh();
            // selectRevision(revisionId);
          }
        }
      });
    }
  }

  /**
   * Select the revision in the receiver.
   */
  public void selectRevision(SVNRevision.Number revision) {
    if(entries == null) {
      return;
    }

    ILogEntry entry = null;
    for(int i = 0; i < entries.length; i++) {
      if(entries[ i].getRevision().equals(revision)) {
        entry = entries[ i];
        break;
      }
    }

    if(entry != null) {
      IStructuredSelection selection = new StructuredSelection(entry);
      tableHistoryViewer.setSelection(selection, true);
    }
  }

  public void scheduleFetchChangePathJob(ILogEntry logEntry) {
    if(fetchChangePathJob == null) {
      fetchChangePathJob = new FetchChangePathJob();
    }
    if(fetchChangePathJob.getState() != Job.NONE) {
      fetchChangePathJob.cancel();
      try {
        fetchChangePathJob.join();
      } catch(InterruptedException e) {
        e.printStackTrace();
        // SVNUIPlugin.log(new
        // SVNException(Policy.bind("HistoryView.errorFetchingEntries",
        // remoteResource.getName()), e)); //$NON-NLS-1$
      }
    }
    fetchChangePathJob.setLogEntry(logEntry);
    Utils.schedule(fetchChangePathJob, getSite());
  }

  public boolean isShowChangePaths() {
	  return showAffectedPaths;
  }

  private IAction getOpenRemoteFileAction() {
    if(openAction == null) {
      openAction = new Action() {
        public void run() {
          OpenRemoteFileAction delegate = new OpenRemoteFileAction();
          delegate.init(this);
          delegate.selectionChanged(this, tableHistoryViewer.getSelection());
          if(isEnabled()) {
            try {
              // disableEditorActivation = true;
              delegate.run(this);
            } finally {
              // disableEditorActivation = false;
            }
          }
        }
      };
    }
    return openAction;
  }

  // open changed Path (double-click)
  private IAction getOpenChangedPathAction() {
    if(openChangedPathAction == null) {
      openChangedPathAction = new Action() {
        public void run() {
          OpenRemoteFileAction delegate = new OpenRemoteFileAction();
          delegate.init(this);
          delegate.selectionChanged(this, changePathsViewer.getSelection());
          if(isEnabled()) {
            try {
              // disableEditorActivation = true;
              delegate.run(this);
            } finally {
              // disableEditorActivation = false;
            }
          }
        }
      };
    }
    return openChangedPathAction;

  }

  // get contents Action (context menu)
  private IAction getGetContentsAction() {
    if(getContentsAction == null) {
      getContentsAction = getContextMenuAction(Policy.bind("HistoryView.getContentsAction"), new IWorkspaceRunnable() { //$NON-NLS-1$
            public void run(IProgressMonitor monitor) throws CoreException {
              ISelection selection = getSelection();
              if( !(selection instanceof IStructuredSelection))
                return;
              IStructuredSelection ss = (IStructuredSelection) selection;
              ISVNRemoteFile remoteFile = (ISVNRemoteFile) getLogEntry(ss).getRemoteResource();
              monitor.beginTask(null, 100);
              try {
                if(remoteFile != null) {
                  if(confirmOverwrite()) {
                    InputStream in = ((IResourceVariant) remoteFile).getStorage(new SubProgressMonitor(monitor, 50))
                        .getContents();
                    IFile file = (IFile) resource;
                    file.setContents(in, false, true, new SubProgressMonitor(monitor, 50));
                  }
                }
              } catch(TeamException e) {
                throw new CoreException(e.getStatus());
              } finally {
                monitor.done();
              }
            }
          });
      PlatformUI.getWorkbench().getHelpSystem().setHelp(getContentsAction, IHelpContextIds.GET_FILE_CONTENTS_ACTION);
    }
    return getContentsAction;
  }

  // get differences as unified diff action (context menu)
  private IAction getShowDifferencesAsUnifiedDiffAction() {
    if(showDifferencesAsUnifiedDiffAction == null) {
      showDifferencesAsUnifiedDiffAction = new Action(
          Policy.bind("HistoryView.showDifferences"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MENU_DIFF)) { //$NON-NLS-1$
        public void run() {
          ISelection selection = getSelection();
          if( !(selection instanceof IStructuredSelection))
            return;
          ILogEntry currentSelection = getLogEntry((IStructuredSelection) selection);
          FileDialog dialog = new FileDialog(getSite().getShell(), SWT.SAVE);
          dialog.setText("Select Unified Diff Output File");
          dialog.setFileName("revision" + currentSelection.getRevision().getNumber() + ".diff"); //$NON-NLS-1$
          String outFile = dialog.open();
          if(outFile != null) {
            final SVNUrl url = currentSelection.getResource().getUrl();
            final SVNRevision oldUrlRevision = new SVNRevision.Number(currentSelection.getRevision().getNumber() - 1);
            final SVNRevision newUrlRevision = currentSelection.getRevision();
            final File file = new File(outFile);
            if(file.exists()) {
              if( !MessageDialog.openQuestion(getSite().getShell(), Policy.bind("HistoryView.showDifferences"), Policy
                  .bind("HistoryView.overwriteOutfile", file.getName())))
                return;
            }
            BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
              public void run() {
                try {
                  ISVNClientAdapter client = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
                  client.diff(url, oldUrlRevision, newUrlRevision, file, true);
                } catch(Exception e) {
                  MessageDialog.openError(getSite().getShell(), Policy.bind("HistoryView.showDifferences"), e
                      .getMessage());
                }
              }
            });
          }
        }
      };
    }
    return showDifferencesAsUnifiedDiffAction;
  }

  // update to the selected revision (context menu)
  private IAction getUpdateToRevisionAction() {
    if(updateToRevisionAction == null) {
      updateToRevisionAction = getContextMenuAction(
          Policy.bind("HistoryView.getRevisionAction"), new IWorkspaceRunnable() { //$NON-NLS-1$
            public void run(IProgressMonitor monitor) throws CoreException {
              ISelection selection = getSelection();
              if( !(selection instanceof IStructuredSelection))
                return;
              IStructuredSelection ss = (IStructuredSelection) selection;
              ISVNRemoteFile remoteFile = (ISVNRemoteFile) getLogEntry(ss).getRemoteResource();
              try {
                if(remoteFile != null) {
                  if(confirmOverwrite()) {
                    IFile file = (IFile) resource;
                    new ReplaceOperation(getSite().getPage().getActivePart(), file, remoteFile.getLastChangedRevision())
                        .run(monitor);
                    historyTableProvider.setRemoteResource(remoteFile);
                    Display.getDefault().asyncExec(new Runnable() {
                      public void run() {
                        tableHistoryViewer.refresh();
                      }
                    });
                  }
                }
              } catch(InvocationTargetException e) {
                throw new CoreException(new SVNStatus(IStatus.ERROR, 0, e.getMessage()));
              } catch(InterruptedException e) {
                // Cancelled by user
              }
            }
          });
      PlatformUI.getWorkbench().getHelpSystem().setHelp(updateToRevisionAction,
          IHelpContextIds.GET_FILE_REVISION_ACTION);
    }
    return updateToRevisionAction;
  }

  // get create tag from revision action (context menu)
  private IAction getCreateTagFromRevisionAction() {
    if(createTagFromRevisionAction == null) {
      createTagFromRevisionAction = new Action(
          Policy.bind("HistoryView.createTagFromRevision"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MENU_BRANCHTAG)) { //$NON-NLS-1$
        public void run() {
          ISelection selection = getSelection();
          if( !(selection instanceof IStructuredSelection))
            return;
          ILogEntry currentSelection = getLogEntry((IStructuredSelection) selection);
          BranchTagDialog dialog;
          if(resource == null)
            dialog = new BranchTagDialog(getSite().getShell(), historyTableProvider.getRemoteResource());
          else
            dialog = new BranchTagDialog(getSite().getShell(), resource);
          dialog.setRevisionNumber(currentSelection.getRevision().getNumber());
          if(dialog.open() == BranchTagDialog.CANCEL)
            return;
          final SVNUrl sourceUrl = dialog.getUrl();
          final SVNUrl destinationUrl = dialog.getToUrl();
          final String message = dialog.getComment();
          final SVNRevision revision = dialog.getRevision();
          boolean createOnServer = dialog.isCreateOnServer();
          IResource[] resources = { resource};
          try {
            if(resource == null) {
              BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
                public void run() {
                  try {
                    ISVNClientAdapter client = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
                    client.copy(sourceUrl, destinationUrl, message, revision);
                  } catch(Exception e) {
                    MessageDialog.openError(getSite().getShell(), Policy.bind("HistoryView.createTagFromRevision"), e
                        .getMessage());
                  }
                }
              });
            } else {
              new BranchTagOperation(getSite().getPage().getActivePart(), resources, sourceUrl, destinationUrl,
                  createOnServer, dialog.getRevision(), message).run();
            }
          } catch(Exception e) {
            MessageDialog.openError(getSite().getShell(), Policy.bind("HistoryView.createTagFromRevision"), e
                .getMessage());
          }
        }
      };
    }
    return createTagFromRevisionAction;
  }

  private IAction getSetCommitPropertiesAction() {
    // set Action (context menu)
    if(setCommitPropertiesAction == null) {
      setCommitPropertiesAction = new Action(Policy.bind("HistoryView.setCommitProperties")) {
        public void run() {
          try {
            final ISelection selection = getSelection();
            if( !(selection instanceof IStructuredSelection))
              return;
            final ILogEntry ourSelection = getLogEntry((IStructuredSelection) selection);

            // Failing that, try the resource originally selected by the user if
            // from the Team menu

            // TODO: Search all paths from currentSelection and find the
            // shortest path and
            // get the resources for that instance (in order to get the 'best'
            // "bugtraq" properties)
            final ProjectProperties projectProperties = (resource != null) ? ProjectProperties
                .getProjectProperties(resource) : ProjectProperties.getProjectProperties(ourSelection
                .getRemoteResource()); // will return null!

            final ISVNResource svnResource = ourSelection.getRemoteResource() != null ? ourSelection
                .getRemoteResource() : ourSelection.getResource();

            SetCommitPropertiesDialog dialog = new SetCommitPropertiesDialog(getSite().getShell(), ourSelection
                .getRevision(), resource, projectProperties);
            // Set previous text - the text to edit
            dialog.setOldAuthor(ourSelection.getAuthor());
            dialog.setOldComment(ourSelection.getComment());

            boolean doCommit = (dialog.open() == Window.OK);
            if(doCommit) {
              final String author;
              final String commitComment;
              if(ourSelection.getAuthor().equals(dialog.getAuthor()))
                author = null;
              else
                author = dialog.getAuthor();
              if(ourSelection.getComment().equals(dialog.getComment()))
                commitComment = null;
              else
                commitComment = dialog.getComment();

              final ChangeCommitPropertiesCommand command = new ChangeCommitPropertiesCommand(svnResource
                  .getRepository(), ourSelection.getRevision(), commitComment, author);

              PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException {
                  try {
                    command.run(monitor);
                  } catch(SVNException e) {
                    throw new InvocationTargetException(e);
                  } finally {
                       if(ourSelection instanceof LogEntry) {
                          LogEntry logEntry = (LogEntry) ourSelection;
                          if (command.isLogMessageChanged()) logEntry.setComment(commitComment);
                          if (command.isAuthorChanged()) logEntry.setAuthor(author);
                        }
                        getSite().getShell().getDisplay().asyncExec(new Runnable() {
                          public void run() {
                            tableHistoryViewer.refresh();
                            tableHistoryViewer.setSelection(selection, true);
                          }
                        });                	  
                  }
                }
              });
            }
          } catch(InvocationTargetException e) {
            SVNUIPlugin.openError(getSite().getShell(), null, null, e, SVNUIPlugin.LOG_NONTEAM_EXCEPTIONS);
          } catch(InterruptedException e) {
            // Do nothing
          } catch(SVNException e) {
            // TODO Auto-generated catch block
            SVNUIPlugin.openError(getSite().getShell(), null, null, e, SVNUIPlugin.LOG_TEAM_EXCEPTIONS);
          }
        }

        // we don't allow multiple selection
        public boolean isEnabled() {
          ISelection selection = getSelection();
          return selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() == 1;
        }
      };
    }
    return setCommitPropertiesAction;
  }

  // get revert changes action (context menu)
  private IAction getRevertChangesAction() {
    if(revertChangesAction == null) {
      revertChangesAction = new Action() {
        public void run() {
          ISelection selection = getSelection();
          if( !(selection instanceof IStructuredSelection))
            return;
          final IStructuredSelection ss = (IStructuredSelection) selection;
          if(ss.size() == 1) {
            if( !MessageDialog.openConfirm(getSite().getShell(), getText(), Policy.bind(
                "HistoryView.confirmRevertRevision", resource.getFullPath().toString())))
              return;
          } else {
            if( !MessageDialog.openConfirm(getSite().getShell(), getText(), Policy.bind(
                "HistoryView.confirmRevertRevisions", resource.getFullPath().toString())))
              return;
          }
          BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
            public void run() {
              ILogEntry firstElement = getFirstElement();
              ILogEntry lastElement = getLastElement();
              final SVNUrl path1 = firstElement.getResource().getUrl();
              final SVNRevision revision1 = firstElement.getRevision();
              final SVNUrl path2 = lastElement.getResource().getUrl();
              final SVNRevision revision2 = new SVNRevision.Number(lastElement.getRevision().getNumber() - 1);
              final IResource[] resources = { resource};
              try {
                WorkspaceAction mergeAction = new WorkspaceAction() {
                  protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
                    new MergeOperation(getSite().getPage().getActivePart(), resources, path1, revision1, path2,
                        revision2).run();
                  }
                };
                mergeAction.run(null);
              } catch(Exception e) {
                MessageDialog.openError(getSite().getShell(), revertChangesAction.getText(), e.getMessage());
              }
            }
          });
        }
      };
    }
    ISelection selection = getSelection();
    if(selection instanceof IStructuredSelection) {
      IStructuredSelection ss = (IStructuredSelection) selection;
      if(ss.size() == 1) {
        ILogEntry currentSelection = getLogEntry(ss);
        revertChangesAction.setText(Policy.bind("HistoryView.revertChangesFromRevision", ""
            + currentSelection.getRevision().getNumber()));
      }
      if(ss.size() > 1) {
        ILogEntry firstElement = getFirstElement();
        ILogEntry lastElement = getLastElement();
        revertChangesAction.setText(Policy.bind("HistoryView.revertChangesFromRevisions", ""
            + lastElement.getRevision().getNumber(), "" + firstElement.getRevision().getNumber()));
      }
    }
    revertChangesAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MENU_MARKMERGED));
    return revertChangesAction;
  }

  // Refresh action (toolbar)
  private IAction getRefreshAction() {
    if(refreshAction == null) {
      SVNUIPlugin plugin = SVNUIPlugin.getPlugin();
      refreshAction = new Action(
          Policy.bind("HistoryView.refreshLabel"), plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH_ENABLED)) { //$NON-NLS-1$
        public void run() {
          refresh();
        }
      };
      refreshAction.setToolTipText(Policy.bind("HistoryView.refresh")); //$NON-NLS-1$
      refreshAction.setDisabledImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH_DISABLED));
      refreshAction.setHoverImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_REFRESH));
    }
    return refreshAction;
  }

  // Search action (toolbar)
  private IAction getSearchAction() {
	  if (searchAction == null) {
	      SVNUIPlugin plugin = SVNUIPlugin.getPlugin();
		  searchAction = new Action(
				  Policy.bind("HistoryView.search"), plugin.getImageDescriptor(ISVNUIConstants.IMG_FILTER_HISTORY)) { //$NON-NLS-1$
			  public void run() {
				  if (historySearchDialog == null) {
					  historySearchDialog = new HistorySearchDialog(getSite().getShell(), remoteResource);
				  }
				  historySearchDialog.setRemoteResource(remoteResource);
				  if (historySearchDialog.open() == Window.OK) {
					  searchAction.setEnabled(false);
					  Utils.schedule(new SearchHistoryJob(), SVNUIPlugin.getPlugin().getWorkbench().getActiveWorkbenchWindow()
					            .getActivePage().getActivePart().getSite());
				  }
			  }
		  };
		  searchAction.setDisabledImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_FILTER_HISTORY_DISABLED));
	  }
	  return searchAction;
  }
  
  // Clear search action (toolbar)
  private IAction getClearSearchAction() {
	  if (clearSearchAction == null) {
	      SVNUIPlugin plugin = SVNUIPlugin.getPlugin();
		  clearSearchAction = new Action(
				  Policy.bind("HistoryView.clearSearch"), plugin.getImageDescriptor(ISVNUIConstants.IMG_CLEAR)) { //$NON-NLS-1$
			  public void run() {
				  BusyIndicator.showWhile(tableHistoryViewer.getTable().getDisplay(), new Runnable() {
					  public void run() {
						  ViewerFilter[] filters = tableHistoryViewer.getFilters();
						  for (int i=0; i<filters.length; i++) {
							  ViewerFilter filter = filters[i];
							  if (filter instanceof HistorySearchViewerFilter) {
								  tableHistoryViewer.removeFilter(filter);
							  }
							  else if (filter instanceof EmptySearchViewerFilter) {
								  tableHistoryViewer.removeFilter(filter);
							  }
				  		  }
						  setEnabled(false);
					  }
				  });
			  }
		  };
		  clearSearchAction.setDisabledImageDescriptor(plugin.getImageDescriptor(ISVNUIConstants.IMG_CLEAR_DISABLED));
	  }
	  return clearSearchAction;
  }
  
    // Get Get All action (toolbar)
  private IAction getGetAllAction() {
    if(getAllAction == null) {
      SVNUIPlugin plugin = SVNUIPlugin.getPlugin();
      getAllAction = new Action(
          Policy.bind("HistoryView.getAll"), plugin.getImageDescriptor(ISVNUIConstants.IMG_GET_ALL)) { //$NON-NLS-1$
        public void run() {
          final ISVNRemoteResource remoteResource = historyTableProvider.getRemoteResource();
          if(fetchAllLogEntriesJob == null) {
            fetchAllLogEntriesJob = new FetchAllLogEntriesJob();
          }
          if(fetchAllLogEntriesJob.getState() != Job.NONE) {
            fetchAllLogEntriesJob.cancel();
            try {
              fetchAllLogEntriesJob.join();
            } catch(InterruptedException e) {
              SVNUIPlugin.log(new SVNException(Policy
                  .bind("HistoryView.errorFetchingEntries", remoteResource.getName()), e)); //$NON-NLS-1$
            }
          }
          fetchAllLogEntriesJob.setRemoteFile(remoteResource);
          Utils.schedule(fetchAllLogEntriesJob, getSite());
        }
      };
      getAllAction.setToolTipText(Policy.bind("HistoryView.getAll")); //$NON-NLS-1$
    }
    return getAllAction;
  }
  
  // Get Get Next action (toolbar)
  public IAction getGetNextAction() {
    if(getNextAction == null) {
      getNextAction = new GetNextAction();
    }
    return getNextAction;
  }

  /**
   * All context menu actions use this class This action : - updates
   * currentSelection - action.run
   */
  private Action getContextMenuAction(String title, final IWorkspaceRunnable action) {
    return new Action(title) {
      public void run() {
        try {
          if(resource == null)
            return;
          PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
              try {
                action.run(monitor);
              } catch(CoreException e) {
                throw new InvocationTargetException(e);
              }
            }
          });
        } catch(InvocationTargetException e) {
          SVNUIPlugin.openError(getSite().getShell(), null, null, e, SVNUIPlugin.LOG_NONTEAM_EXCEPTIONS);
        } catch(InterruptedException e) {
          // Do nothing
        }
      }

      // we don't allow multiple selection
      public boolean isEnabled() {
        ISelection selection = getSelection();
        return selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() == 1;
      }
    };
  }

  /**
   * Ask the user to confirm the overwrite of the file if the file has been
   * modified since last commit
   */
  private boolean confirmOverwrite() {
    IFile file = (IFile) resource;
    if(file != null && file.exists()) {
      ISVNLocalFile svnFile = SVNWorkspaceRoot.getSVNFileFor(file);
      try {
        if(svnFile.isDirty()) {
          String title = Policy.bind("HistoryView.overwriteTitle"); //$NON-NLS-1$
          String msg = Policy.bind("HistoryView.overwriteMsg"); //$NON-NLS-1$
          final MessageDialog dialog = new MessageDialog(getSite().getShell(), title, null, msg,
              MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
          final int[] result = new int[ 1];
          getSite().getShell().getDisplay().syncExec(new Runnable() {
            public void run() {
              result[ 0] = dialog.open();
            }
          });
          if(result[ 0] != 0) {
            // cancel
            return false;
          }
        }
      } catch(SVNException e) {
        SVNUIPlugin.log(e.getStatus());
      }
    }
    return true;
  }

  private ISelection getSelection() {
    return selection;
  }

  private ILogEntry getFirstElement() {
    ILogEntry firstElement = null;
    ISelection selection = getSelection();
    if(selection instanceof IStructuredSelection) {
      IStructuredSelection ss = (IStructuredSelection) selection;
      Iterator iter = ss.iterator();
      while(iter.hasNext()) {
        ILogEntry element = (ILogEntry) iter.next();
        if(firstElement == null || element.getRevision().getNumber() > firstElement.getRevision().getNumber())
          firstElement = element;
      }
    }
    return firstElement;
  }

  private ILogEntry getLastElement() {
    ILogEntry lastElement = null;
    ISelection selection = getSelection();
    if(selection instanceof IStructuredSelection) {
      IStructuredSelection ss = (IStructuredSelection) selection;
      Iterator iter = ss.iterator();
      while(iter.hasNext()) {
        ILogEntry element = (ILogEntry) iter.next();
        if(lastElement == null || element.getRevision().getNumber() < lastElement.getRevision().getNumber())
          lastElement = element;
      }
    }
    return lastElement;
  }

  
  private final class GetNextAction extends Action implements IPropertyChangeListener {
    GetNextAction() {
      super(Policy.bind("HistoryView.getNext"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_GET_NEXT));
      updateFromProperties();
      SVNUIPlugin.getPlugin().getPreferenceStore().addPropertyChangeListener(this);
    }

    public void run() {
      final ISVNRemoteResource remoteResource = historyTableProvider.getRemoteResource();
      if(fetchNextLogEntriesJob == null) {
        fetchNextLogEntriesJob = new FetchNextLogEntriesJob();
      }
      if(fetchNextLogEntriesJob.getState() != Job.NONE) {
        fetchNextLogEntriesJob.cancel();
        try {
          fetchNextLogEntriesJob.join();
        } catch(InterruptedException e) {
          SVNUIPlugin.log(new SVNException(Policy
              .bind("HistoryView.errorFetchingEntries", remoteResource.getName()), e)); //$NON-NLS-1$
        }
      }
      fetchNextLogEntriesJob.setRemoteFile(remoteResource);
      Utils.schedule(fetchNextLogEntriesJob, getSite());
    }

    public void propertyChange(PropertyChangeEvent event) {
      if(ISVNUIConstants.PREF_LOG_ENTRIES_TO_FETCH.equals(event.getProperty())) {
        updateFromProperties();
      }
    }

    private void updateFromProperties() {
      int entriesToFetch = SVNUIPlugin.getPlugin().getPreferenceStore().getInt(ISVNUIConstants.PREF_LOG_ENTRIES_TO_FETCH);
      setToolTipText(Policy.bind("HistoryView.getNext") + " " + entriesToFetch); //$NON-NLS-1$
      if(entriesToFetch <= 0) {
        setEnabled(false);
      }
    }
  }

  
  private class FetchLogEntriesJob extends AbstractFetchJob {
    public ISVNRemoteResource remoteResource;

    public FetchLogEntriesJob() {
      super(Policy.bind("HistoryView.fetchHistoryJob")); //$NON-NLS-1$;
    }

    public void setRemoteFile(ISVNRemoteResource resource) {
      this.remoteResource = resource;
    }

    public IStatus run(IProgressMonitor monitor) {
      try {
        if(remoteResource != null && !shutdown) {
          if(resource == null) {
            if(remoteResource == null
                || !SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_SHOW_TAGS_IN_REMOTE)) {
              tagManager = null;
            } else {
              tagManager = new AliasManager(remoteResource.getUrl());
            }
          } else {
            tagManager = new AliasManager(resource);
          }
          SVNRevision pegRevision = remoteResource.getRevision();
          SVNRevision revisionEnd = new SVNRevision.Number(0);
          boolean stopOnCopy = toggleStopOnCopyAction.isChecked();
          IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
          int entriesToFetch = store.getInt(ISVNUIConstants.PREF_LOG_ENTRIES_TO_FETCH);
          long limit = entriesToFetch;
          entries = getLogEntries(monitor, remoteResource, pegRevision, revisionStart, revisionEnd, stopOnCopy,
              limit + 1, tagManager);
          long entriesLength = entries.length;
          if(entriesLength > limit) {
            ILogEntry[] fetchedEntries = new ILogEntry[ entries.length - 1];
            for(int i = 0; i < entries.length - 1; i++) {
              fetchedEntries[ i] = entries[ i];
            }
            entries = fetchedEntries;
            getNextAction.setEnabled(true);
          } else {
            getNextAction.setEnabled(false);
          }
          final SVNRevision.Number revisionId = remoteResource.getLastChangedRevision();
          getSite().getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
              if(entries != null && tableHistoryViewer != null && !tableHistoryViewer.getTable().isDisposed()) {
                // once we got the entries, we refresh the table
                if(entries.length > 0) {
                  lastEntry = entries[ entries.length - 1];
                  long lastEntryNumber = lastEntry.getRevision().getNumber();
                  revisionStart = new SVNRevision.Number(lastEntryNumber - 1);
                }
                tableHistoryViewer.refresh();
                selectRevision(revisionId);
              }
            }
          });
        }
        return Status.OK_STATUS;
      } catch(TeamException e) {
        return e.getStatus();
      }
    }
  }

  private class FetchNextLogEntriesJob extends AbstractFetchJob {
    public ISVNRemoteResource remoteResource;

    public FetchNextLogEntriesJob() {
      super(Policy.bind("HistoryView.fetchHistoryJob")); //$NON-NLS-1$;
    }

    public void setRemoteFile(ISVNRemoteResource resource) {
      this.remoteResource = resource;
    }

    public IStatus run(IProgressMonitor monitor) {
      try {
        if(remoteResource != null && !shutdown) {
          SVNRevision pegRevision = remoteResource.getRevision();
          SVNRevision revisionEnd = new SVNRevision.Number(0);
          boolean stopOnCopy = toggleStopOnCopyAction.isChecked();
          IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
          int entriesToFetch = store.getInt(ISVNUIConstants.PREF_LOG_ENTRIES_TO_FETCH);
          long limit = entriesToFetch;
          ILogEntry[] nextEntries = getLogEntries(monitor, remoteResource, pegRevision, revisionStart, revisionEnd,
              stopOnCopy, limit + 1, tagManager);
          long entriesLength = nextEntries.length;
          ILogEntry[] fetchedEntries = null;
          if(entriesLength > limit) {
            fetchedEntries = new ILogEntry[ nextEntries.length - 1];
            for(int i = 0; i < nextEntries.length - 1; i++)
              fetchedEntries[ i] = nextEntries[ i];
            getNextAction.setEnabled(true);
          } else {
              fetchedEntries = new ILogEntry[ nextEntries.length];
              for(int i = 0; i < nextEntries.length; i++)
                fetchedEntries[ i] = nextEntries[ i];
            getNextAction.setEnabled(false);
          }
          ArrayList entryArray = new ArrayList();
          if(entries == null)
            entries = new ILogEntry[ 0];
          for(int i = 0; i < entries.length; i++)
            entryArray.add(entries[ i]);
          for(int i = 0; i < fetchedEntries.length; i++)
            entryArray.add(fetchedEntries[ i]);
          entries = new ILogEntry[ entryArray.size()];
          entryArray.toArray(entries);
          getSite().getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
              if(entries != null && tableHistoryViewer != null && !tableHistoryViewer.getTable().isDisposed()) {
                // once we got the entries, we refresh the table
                ISelection selection = tableHistoryViewer.getSelection();
                tableHistoryViewer.refresh();
                tableHistoryViewer.setSelection(selection);
                if(entries.length > 0) {
                  lastEntry = entries[ entries.length - 1];
                  long lastEntryNumber = lastEntry.getRevision().getNumber();
                  revisionStart = new SVNRevision.Number(lastEntryNumber - 1);
                }
              }
            }
          });
        }
        return Status.OK_STATUS;
      } catch(TeamException e) {
        return e.getStatus();
      }
    }
  }
  
  private abstract class AbstractFetchJob extends Job {
	    public AbstractFetchJob(String name) {
		super(name);
	}

		public abstract void setRemoteFile(ISVNRemoteResource resource);

		protected ILogEntry[] getLogEntries(IProgressMonitor monitor, ISVNRemoteResource remoteResource, SVNRevision pegRevision, SVNRevision revisionStart, SVNRevision revisionEnd, boolean stopOnCopy, long limit, AliasManager tagManager) throws TeamException
		{
			GetLogsCommand logCmd = new GetLogsCommand(remoteResource, pegRevision, revisionStart, revisionEnd, stopOnCopy, limit, tagManager, false);
			logCmd.run(monitor);
			return logCmd.getLogEntries(); 					
		}
  }

  private class FetchAllLogEntriesJob extends AbstractFetchJob {
    public ISVNRemoteResource remoteResource;

    public FetchAllLogEntriesJob() {
      super(Policy.bind("HistoryView.fetchHistoryJob")); //$NON-NLS-1$;
    }

    public void setRemoteFile(ISVNRemoteResource resource) {
      this.remoteResource = resource;
    }

    public IStatus run(IProgressMonitor monitor) {
      try {
        if(remoteResource != null && !shutdown) {
          if(resource == null) {
            if(remoteResource == null
                || !SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_SHOW_TAGS_IN_REMOTE))
              tagManager = null;
            else
              tagManager = new AliasManager(remoteResource.getUrl());
          } else
            tagManager = new AliasManager(resource);
          SVNRevision pegRevision = remoteResource.getRevision();
          revisionStart = SVNRevision.HEAD;
          SVNRevision revisionEnd = new SVNRevision.Number(0);
          boolean stopOnCopy = toggleStopOnCopyAction.isChecked();
          long limit = 0;
          entries = getLogEntries(monitor, remoteResource, pegRevision, revisionStart, revisionEnd, stopOnCopy, limit,
              tagManager);
          final SVNRevision.Number revisionId = remoteResource.getLastChangedRevision();
          getSite().getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
              if(entries != null && tableHistoryViewer != null && !tableHistoryViewer.getTable().isDisposed()) {
                // once we got the entries, we refresh the table
                if(entries.length > 0) {
                  lastEntry = entries[ entries.length - 1];
                  long lastEntryNumber = lastEntry.getRevision().getNumber();
                  revisionStart = new SVNRevision.Number(lastEntryNumber - 1);
                }
                tableHistoryViewer.refresh();
                selectRevision(revisionId);
              }
            }
          });
        }
        return Status.OK_STATUS;
      } catch(TeamException e) {
        return e.getStatus();
      }
    }
  }

  private class SearchHistoryJob extends Job {

	  public SearchHistoryJob() {
		  super(Policy.bind("HistoryView.searchHistoryJob")); //$NON-NLS-1$
	  }
	  
	  public IStatus run(IProgressMonitor monitor) {
		  
		  Date startDate = historySearchDialog.getStartDate();
		  setEmptyViewerFilter();
		  
		  // Fetch log entries until start date
		  if (historySearchDialog.getAutoFetchLogs()) {
			  if (!historySearchDialog.getSearchAllLogs()) {
				  Date lastDate = null;
				  if (lastEntry != null) {
					  lastDate = lastEntry.getDate();
				  }
				  int numEntries = entries.length;
				  int prevNumEntries = -1;
				  while ((numEntries != prevNumEntries) && 
						  ((lastDate == null) ||
								  (startDate == null) ||
								  (startDate.compareTo(lastDate) <= 0))) {
				  
					  if (monitor.isCanceled()) {
						  getSearchAction().setEnabled(true);
						  removeEmptyViewerFilter();
						  return Status.CANCEL_STATUS;
					  }
				  
					  final ISVNRemoteResource remoteResource = historyTableProvider.getRemoteResource();
					  if(fetchNextLogEntriesJob == null) {
						  fetchNextLogEntriesJob = new FetchNextLogEntriesJob();
					  }
					  if(fetchNextLogEntriesJob.getState() != Job.NONE) {
						  fetchNextLogEntriesJob.cancel();
					  }
					  fetchNextLogEntriesJob.setRemoteFile(remoteResource);
					  Utils.schedule(fetchNextLogEntriesJob, getSite());
					  try {
						  fetchNextLogEntriesJob.join();
					  } catch(InterruptedException e) {
						  SVNUIPlugin.log(new SVNException(
								 Policy.bind("HistoryView.errorFetchingEntries", remoteResource.getName()), e)); //$NON-NLS-1$
					  }
			      
					  if (entries.length == 0) {
						  break;
					  }
					  lastDate = lastEntry.getDate();
					  prevNumEntries = numEntries;
					  numEntries = entries.length;
				  }
			  }
			  else {
		          final ISVNRemoteResource remoteResource = historyTableProvider.getRemoteResource();
		          if(fetchAllLogEntriesJob == null) {
		        	  fetchAllLogEntriesJob = new FetchAllLogEntriesJob();
		          }
		          if(fetchAllLogEntriesJob.getState() != Job.NONE) {
		        	  fetchAllLogEntriesJob.cancel();
		          }
		          fetchAllLogEntriesJob.setRemoteFile(remoteResource);
		          Utils.schedule(fetchAllLogEntriesJob, getSite());
			  }
		  }
		  
		  final HistorySearchViewerFilter viewerFilter = new HistorySearchViewerFilter(
				  historySearchDialog.getUser(), 
				  historySearchDialog.getComment(), 
				  historySearchDialog.getStartDate(), 
				  historySearchDialog.getEndDate(), 
				  historySearchDialog.getRegExp());
		  
		  getSite().getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
				    BusyIndicator.showWhile(tableHistoryViewer.getTable().getDisplay(), new Runnable() {
				        public void run() {
				        	getClearSearchAction().run();
				        	tableHistoryViewer.addFilter(viewerFilter);
				        	getClearSearchAction().setEnabled(true);
				        	getSearchAction().setEnabled(true);
				        }
				    });
				}
		  });
		  
		  removeEmptyViewerFilter();
		  
		  return Status.OK_STATUS;
	  }
	  
	  private void setEmptyViewerFilter() {
		  getSite().getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					tableHistoryViewer.addFilter(new EmptySearchViewerFilter());
				}
			});
	  }

	  private void removeEmptyViewerFilter() {
		  getSite().getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					ViewerFilter[] filters = tableHistoryViewer.getFilters();
					for (int i=0; i<filters.length; i++) {
						if (filters[i] instanceof EmptySearchViewerFilter) {
							tableHistoryViewer.removeFilter(filters[i]);
						}
					}
				}
			});
	  }
}

  class FetchChangePathJob extends Job {
    public ILogEntry logEntry;

    public FetchChangePathJob() {
      super(Policy.bind("HistoryView.fetchChangePathJob")); //$NON-NLS-1$;
    }

    public void setLogEntry(ILogEntry logEntry) {
      this.logEntry = logEntry;
    }

    public IStatus run(IProgressMonitor monitor) {
      if(logEntry.getResource() != null) {
        setCurrentLogEntryChangePath(logEntry.getLogEntryChangePaths());
      }
      return Status.OK_STATUS;
    }
  }


  public static class ToggleAffectedPathsOptionAction extends Action {
    private final SVNHistoryPage page;
    private final String preferenceName;
    private final int value;

    public ToggleAffectedPathsOptionAction(SVNHistoryPage page, 
          String label, String icon, String preferenceName, int value) {
      super(Policy.bind(label), AS_RADIO_BUTTON);
      this.page = page;
      this.preferenceName = preferenceName;
      this.value = value;
      setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(icon));
      
      IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
      setChecked(value==store.getInt(preferenceName));
    }
    
    public int getValue() {
      return this.value;
    }

    public void run() {
      if (isChecked()) {
        SVNUIPlugin.getPlugin().getPreferenceStore().setValue(preferenceName, value);
        page.createAffectedPathsViewer();
      }
    }
    
  }

  /* (non-Javadoc)
   * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#resourceSyncInfoChanged(org.eclipse.core.resources.IResource[])
   */
  public void resourceSyncInfoChanged(IResource[] changedResources) {
      for (int i = 0; i < changedResources.length; i++) {
          IResource changedResource = changedResources[i];
          if( changedResource.equals( resource ) ) {
				resourceChanged();
          }
      }
  }

	/**
	 * This method updates the history table, highlighting the current revison
	 * without refetching the log entries to preserve bandwidth.
	 * The user has to a manual refresh to get the new log entries. 
	 */
  private void resourceChanged() {
      getSite().getShell().getDisplay().asyncExec(new Runnable() {
      	public void run() {
      		revisionStart = SVNRevision.HEAD;
      		ISVNLocalResource localResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
      		try {
                  if (localResource != null && !localResource.getStatus().isAdded()) {
                  	ISVNRemoteResource baseResource = localResource.getBaseResource();
                  	historyTableProvider.setRemoteResource(baseResource);
                  	tableHistoryViewer.refresh();
                  }
              } catch (SVNException e) {
                  SVNUIPlugin.openError(getHistoryPageSite().getShell(), null, null, e);
              }
      	}
      });
  }

  /* (non-Javadoc)
   * @see org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.KeyEvent)
   */
  public void keyPressed(KeyEvent e) {
      if (e.keyCode == 'f' && e.stateMask == SWT.CTRL) {
          getSearchAction().run();
      }
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
   */
  public void keyReleased(KeyEvent e) {
  }


  /* (non-Javadoc)
   * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#resourceModified(org.eclipse.core.resources.IResource[])
   */
  public void resourceModified(IResource[] changedResources) {
  }

  /* (non-Javadoc)
   * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#projectConfigured(org.eclipse.core.resources.IProject)
   */
  public void projectConfigured(IProject project) {
  }

  /* (non-Javadoc)
   * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#projectDeconfigured(org.eclipse.core.resources.IProject)
   */
  public void projectDeconfigured(IProject project) {
  }

}
