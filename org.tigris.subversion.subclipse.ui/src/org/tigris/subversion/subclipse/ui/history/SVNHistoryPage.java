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
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.team.ui.history.IHistoryPageSite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
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
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.core.resources.RemoteResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.CancelableSVNLogMessageCallback;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.actions.ExportRemoteFolderAction;
import org.tigris.subversion.subclipse.ui.actions.GenerateChangeLogAction;
import org.tigris.subversion.subclipse.ui.actions.OpenRemoteFileAction;
import org.tigris.subversion.subclipse.ui.actions.ShowAnnotationAction;
import org.tigris.subversion.subclipse.ui.actions.ShowDifferencesAsUnifiedDiffAction;
import org.tigris.subversion.subclipse.ui.actions.ShowHistoryAction;
import org.tigris.subversion.subclipse.ui.actions.WorkspaceAction;
import org.tigris.subversion.subclipse.ui.console.TextViewerAction;
import org.tigris.subversion.subclipse.ui.dialogs.HistorySearchDialog;
import org.tigris.subversion.subclipse.ui.dialogs.SetCommitPropertiesDialog;
import org.tigris.subversion.subclipse.ui.dialogs.ShowRevisionsDialog;
import org.tigris.subversion.subclipse.ui.internal.Utils;
import org.tigris.subversion.subclipse.ui.operations.BranchTagOperation;
import org.tigris.subversion.subclipse.ui.operations.MergeOperation;
import org.tigris.subversion.subclipse.ui.operations.ReplaceOperation;
import org.tigris.subversion.subclipse.ui.operations.SwitchOperation;
import org.tigris.subversion.subclipse.ui.settings.ProjectProperties;
import org.tigris.subversion.subclipse.ui.svnproperties.SvnRevPropertiesView;
import org.tigris.subversion.subclipse.ui.util.EmptySearchViewerFilter;
import org.tigris.subversion.subclipse.ui.util.LinkList;
import org.tigris.subversion.subclipse.ui.wizards.BranchTagWizard;
import org.tigris.subversion.subclipse.ui.wizards.ClosableWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardSwitchPage;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNProperty;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNRevisionRange;
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

  private Composite tableParent;
  
  private static HistorySearchViewerFilter historySearchViewerFilter;

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
  
  private SVNRevisionRange[] revisionRanges;
  private boolean revertEnabled;

  private IAction searchAction;
  private IAction clearSearchAction;
  private IAction getNextAction;
  private IAction getAllAction;
  private IAction toggleStopOnCopyAction;
  private IAction toggleIncludeMergedRevisionsAction;
  private IAction toggleShowComments;
  private IAction toggleWrapCommentsAction;
  private IAction toggleShowAffectedPathsAction;

  private IAction openAction;
  private IAction getContentsAction;
  private IAction updateToRevisionAction;
  private IAction openChangedPathAction;
  private IAction showHistoryAction;
  private IAction showRevisionPropertiesAction;  
  private IAction compareAction;
  private IAction showAnnotationAction;
  private IAction exportAction;
  private IAction createTagFromRevisionChangedPathAction;
  private IAction copyChangedPathAction;
 
//  private IAction switchChangedPathAction;
//  private IAction revertChangesChangedPathAction;

  private IAction createTagFromRevisionAction;
  private IAction setCommitPropertiesAction;
  private IAction showRevisionsAction;
  private IAction revertChangesAction;
  private IAction refreshAction;
  private IAction switchAction;
  private GenerateChangeLogAction generateChangeLogAction;

  private ToggleAffectedPathsOptionAction[] toggleAffectedPathsLayoutActions;
  private ToggleAffectedPathsOptionAction[] toggleAffectedPathsModeActions;

  private TextViewerAction copyAction;
  private TextViewerAction selectAllAction;

  private LinkList linkList;
  private Cursor handCursor;
  private Cursor busyCursor;

  private IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
  
  private boolean includeTags = true;
  private boolean includeBugs = false;
  
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
    return remoteResource == null ? null : remoteResource.getRepositoryRelativePath() + Policy.bind("SVNHistoryPage.0")  //$NON-NLS-1$
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
              historyTableProvider.setProjectProperties(projectProperties);
          } catch(SVNException e) {
          }
        }
        if (tableHistoryViewer.getInput() == null) tableHistoryViewer.setInput(remoteResource);
        tableHistoryViewer.refresh();
       	tableHistoryViewer.resetFilters();
       	Object firstElement = tableHistoryViewer.getElementAt(0);
       	if (firstElement != null) {
       		tableHistoryViewer.reveal(firstElement);
       	}
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
        	boolean includeBugs = projectProperties != null;
            boolean includeTags = tagsPropertySet(res);
            if (includeTags != this.includeTags || this.includeBugs != includeBugs ) {
            	this.includeTags = includeTags;
        		this.includeBugs = includeBugs;
        		refreshTable();
        	}
            this.historyTableProvider.setRemoteResource(this.remoteResource);
			this.historyTableProvider.setProjectProperties(this.projectProperties);
            if (historySearchViewerFilter != null) {
            	
//            	HistorySearchViewerFilter[] filters = { historySearchViewerFilter };         	
//            	this.tableHistoryViewer.setFilters(filters);
            	this.tableHistoryViewer.resetFilters();
            	this.tableHistoryViewer.addFilter(historySearchViewerFilter);
            	
              	historySearchDialog = new HistorySearchDialog(getSite().getShell(), remoteResource);
              	historySearchDialog.setSearchAll(false);
              	historySearchDialog.setStartRevision(historySearchViewerFilter.getStartRevision());
              	historySearchDialog.setEndRevision(historySearchViewerFilter.getEndRevision());           	
            	historySearchViewerFilter = null;
            	getClearSearchAction().setEnabled(true);
            }
            else {
            	this.tableHistoryViewer.resetFilters();
            	getClearSearchAction().setEnabled(false);
            }
            this.tableHistoryViewer.setInput(this.remoteResource);
        	
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

      boolean includeTags = tagsPropertySet(remoteResource);
      if (includeTags != this.includeTags) {
    	  this.includeTags = includeTags;
    	  refreshTable();
      }
      
      try {
		this.projectProperties = ProjectProperties.getProjectProperties(this.remoteResource);
	  } catch (SVNException e) {
		  if (!e.operationInterrupted()) {
			  SVNUIPlugin.openError(getSite().getShell(), null, null, e);
		  }
  	  }
  	  boolean includeBugs = projectProperties != null;
      if (includeTags != this.includeTags || this.includeBugs != includeBugs ) {
      	  this.includeTags = includeTags;
  		  this.includeBugs = includeBugs;
  		  refreshTable();
      }
      this.historyTableProvider.setRemoteResource(this.remoteResource);
   	  this.historyTableProvider.setProjectProperties(this.projectProperties);
      if (historySearchViewerFilter != null) {
//      HistorySearchViewerFilter[] filters = { historySearchViewerFilter };         	
//      this.tableHistoryViewer.setFilters(filters);
      	this.tableHistoryViewer.resetFilters();
      	this.tableHistoryViewer.addFilter(historySearchViewerFilter);
      	historySearchDialog = new HistorySearchDialog(getSite().getShell(), remoteResource);
      	historySearchDialog.setSearchAll(false);
      	historySearchDialog.setStartRevision(historySearchViewerFilter.getStartRevision());
      	historySearchDialog.setEndRevision(historySearchViewerFilter.getEndRevision());
      	historySearchViewerFilter = null;
      	getClearSearchAction().setEnabled(true);
      }
      else {
    	  this.tableHistoryViewer.resetFilters();
    	  getClearSearchAction().setEnabled(false);
      }
      this.tableHistoryViewer.setInput(this.remoteResource);
      // setContentDescription(Policy.bind("HistoryView.titleWithArgument",
      // remoteResource.getName())); //$NON-NLS-1$
      // setTitleToolTip(remoteResource.getRepositoryRelativePath());
      return true;

    }

    return false;
  }
  
  private boolean tagsPropertySet(IResource resource) {
	  if (resource == null) return false;
	  ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
	  try {
		  if (svnResource.isManaged()) {
			  ISVNProperty property = null;
			  property = svnResource.getSvnProperty("subclipse:tags"); //$NON-NLS-1$
			  if (property != null && property.getValue() != null) return true;
		  }
	  } catch (SVNException e) {}
	  
	  IResource checkResource = resource;
      while (checkResource.getParent() != null) {
          checkResource = checkResource.getParent();
          if (checkResource.getParent() == null) return false;
          svnResource = SVNWorkspaceRoot.getSVNResourceFor(checkResource);
    	  try {
    		  if (svnResource.isManaged()) {
    			  ISVNProperty property = null;
    			  property = svnResource.getSvnProperty("subclipse:tags"); //$NON-NLS-1$
    			  if (property != null && property.getValue() != null) return true;
    		  }
    	  } catch (SVNException e) {}          
      }
	  
	  return false;
  }
  
  private boolean tagsPropertySet(ISVNRemoteResource resource) {
	    ISVNClientAdapter client = null;
		try {
			client = SVNProviderPlugin.getPlugin().getSVNClient();
			ISVNProperty property = null;
	        SVNProviderPlugin.disableConsoleLogging(); 
			property = client.propertyGet(resource.getUrl(), "subclipse:tags"); //$NON-NLS-1$
			if (property != null && property.getValue() != null) {
				SVNProviderPlugin.enableConsoleLogging(); 
				return true;
			}
			ISVNRemoteResource checkResource = resource;
			while (checkResource.getParent() != null) {
	          checkResource = checkResource.getParent();		
	          property = client.propertyGet(checkResource.getUrl(), "subclipse:tags"); //$NON-NLS-1$
			  if (property != null && property.getValue() != null) {
				SVNProviderPlugin.enableConsoleLogging(); 
				return true;		          
			  }
			}
		} catch (Exception e) {        
			SVNProviderPlugin.enableConsoleLogging(); 
		} finally {
			SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(client);
		}
		return false;
  }

  public void createControl(Composite parent) {
    this.busyCursor = new Cursor(parent.getDisplay(), SWT.CURSOR_WAIT);
    this.handCursor = new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);

    this.showComments = store.getBoolean(ISVNUIConstants.PREF_SHOW_COMMENTS);
    this.wrapCommentsText = store.getBoolean(ISVNUIConstants.PREF_WRAP_COMMENTS);
    this.showAffectedPaths = store.getBoolean(ISVNUIConstants.PREF_SHOW_PATHS);

    this.svnHistoryPageControl = new SashForm(parent, SWT.VERTICAL);
    this.svnHistoryPageControl.setLayoutData(new GridData(GridData.FILL_BOTH));

    this.toggleAffectedPathsModeActions = new ToggleAffectedPathsOptionAction[] {
        new ToggleAffectedPathsOptionAction(this, "HistoryView.affectedPathsTableLayout",  //$NON-NLS-1$
            ISVNUIConstants.IMG_AFFECTED_PATHS_TABLE_MODE, 
            ISVNUIConstants.PREF_AFFECTED_PATHS_MODE, 
            ISVNUIConstants.MODE_FLAT),
        new ToggleAffectedPathsOptionAction(this, "HistoryView.affectedPathsFlatLayout",  //$NON-NLS-1$
            ISVNUIConstants.IMG_AFFECTED_PATHS_FLAT_MODE, 
            ISVNUIConstants.PREF_AFFECTED_PATHS_MODE, 
            ISVNUIConstants.MODE_FLAT2),
        new ToggleAffectedPathsOptionAction(this, "HistoryView.affectedPathsCompressedLayout",  //$NON-NLS-1$
            ISVNUIConstants.IMG_AFFECTED_PATHS_COMPRESSED_MODE, 
            ISVNUIConstants.PREF_AFFECTED_PATHS_MODE, 
            ISVNUIConstants.MODE_COMPRESSED),
      };
    
    this.toggleAffectedPathsLayoutActions = new ToggleAffectedPathsOptionAction[] {
        new ToggleAffectedPathsOptionAction(this, "HistoryView.affectedPathsHorizontalLayout",  //$NON-NLS-1$
            ISVNUIConstants.IMG_AFFECTED_PATHS_HORIZONTAL_LAYOUT, 
            ISVNUIConstants.PREF_AFFECTED_PATHS_LAYOUT, 
            ISVNUIConstants.LAYOUT_HORIZONTAL),
        new ToggleAffectedPathsOptionAction(this, "HistoryView.affectedPathsVerticalLayout",  //$NON-NLS-1$
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
	boolean redraw = false;
	if (tableParent == null) {
		tableParent = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		tableParent.setLayout(layout);
		GridData gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
		gridData.horizontalIndent = 0;
		gridData.verticalIndent = 0;
    tableParent.setLayoutData(gridData);
	} else {
		Control[] children = tableParent.getChildren();
		for (int i = 0; i < children.length; i++) {
			children[i].dispose();
		}
		redraw = true;
	}
    this.historyTableProvider = new HistoryTableProvider(SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION, "SVNHistoryPage"); //$NON-NLS-1$
    this.historyTableProvider.setRemoteResource(remoteResource);
    this.historyTableProvider.setProjectProperties( this.projectProperties );
    historyTableProvider.setIncludeMergeRevisions(store.getBoolean(ISVNUIConstants.PREF_INCLUDE_MERGED_REVISIONS));
    historyTableProvider.setIncludeTags(includeTags);
    historyTableProvider.setIncludeBugs(includeBugs);
    this.tableHistoryViewer = historyTableProvider.createTable(tableParent);
    if (redraw) {
    	tableParent.layout(true);
    	tableParent.redraw();
    }
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

        int entriesToFetch = store.getInt(ISVNUIConstants.PREF_LOG_ENTRIES_TO_FETCH);
        
        // If we are filtering by revision range, override entries to fetch.
        if (historySearchDialog != null && !historySearchDialog.getSearchAllLogs()) {
        	if (historySearchDialog.getStartRevision() != null || historySearchDialog.getEndRevision() != null) {
        		if (getClearSearchAction().isEnabled()) entriesToFetch = 0;
        	}
        }
        
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
      private int currentSelectionSize = -1;

      public void selectionChanged(SelectionChangedEvent event) {
        ISelection selection = event.getSelection();
        ILogEntry logEntry = getLogEntry((IStructuredSelection) selection);
        if(logEntry != currentLogEntry || ((IStructuredSelection) selection).size() != currentSelectionSize) {
          this.currentLogEntry = logEntry;
          this.currentSelectionSize = ((IStructuredSelection) selection).size();
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
    if (redraw) {
    	parent.layout(true);
    	parent.redraw();
    }
  }
  
  public void refreshTable() {
	  createTableHistory(svnHistoryPageControl);
  }
  
  private void fillChangePathsMenu(IMenuManager manager) {
//
// Commented out Get Contents, Revert and Switch options until when/if
// they can be fixed.  Problem is that we need a way to get the local
// resource from the LogEntryChangePath.
//
	  IStructuredSelection sel = (IStructuredSelection)changePathsViewer.getSelection();
	  if (sel.size() == 1) {
		  if (sel.getFirstElement() instanceof LogEntryChangePath) {
//			  manager.add(getGetContentsAction());
		  }
		  manager.add(getCreateTagFromRevisionChangedPathAction());
	  }
//	  manager.add(getRevertChangesChangedPathAction());
//	  manager.add(getSwitchChangedPathAction());
	  manager.add(new Separator("exportImportGroup")); //$NON-NLS-1$
	  if (sel.size() == 1) {
		  if (sel.getFirstElement() instanceof LogEntryChangePath) {
			  manager.add(getExportAction());
			  if (((LogEntryChangePath)sel.getFirstElement()).getAction() == 'D') {
				  manager.add(getCopyChangedPathAction());
			  }
		  }		  
	  }
	  manager.add(new Separator("openGroup")); //$NON-NLS-1$
	  if (sel.size() == 1) {
		  if (sel.getFirstElement() instanceof LogEntryChangePath) {
			  manager.add(getShowAnnotationAction());
		  }
		  manager.add(getCompareAction());
	  }
	  if (sel.getFirstElement() instanceof LogEntryChangePath) {
		  manager.add(getOpenChangedPathAction());
	  }
	  if (sel.size() == 1) manager.add(getShowHistoryAction());	
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
//          manager.add(getShowDifferencesAsUnifiedDiffAction());
          // if (resource != null) {
          manager.add(getCreateTagFromRevisionAction());
          // }
          manager.add(getSetCommitPropertiesAction());
          
          manager.add(getShowRevisionPropertiesAction());
          
          ILogEntry logEntry = (ILogEntry)((IStructuredSelection)sel).getFirstElement();
          if (logEntry.getNumberOfChildren() > 0)
        	  manager.add(getShowRevisionsAction());
        }
        if(resource != null) {
          manager.add(getRevertChangesAction());
          if(((IStructuredSelection) sel).size() == 1) manager.add(getSwitchAction());
        }
        
        manager.add(new Separator("exportImportGroup")); //$NON-NLS-1$
        getGenerateChangeLogAction().setEnabled(!store.getBoolean(ISVNUIConstants.PREF_FETCH_CHANGE_PATH_ON_DEMAND));
        manager.add(getGenerateChangeLogAction());
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
      case ISVNUIConstants.MODE_FLAT2:  
        changePathsViewer = new ChangePathsFlatViewer(innerSashForm, this);
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
    
    // Contribute actions to changed paths pane
	MenuManager menuMgr = new MenuManager();
	Menu menu = menuMgr.createContextMenu(changePathsViewer.getControl());
	menuMgr.addMenuListener(new IMenuListener() {
	  public void menuAboutToShow(IMenuManager menuMgr) {
	    fillChangePathsMenu(menuMgr);
	  }
	});
	menuMgr.setRemoveAllWhenShown(true);
	changePathsViewer.getControl().setMenu(menu);    

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
        return Collections.singletonMap("org.eclipse.ui.DefaultTextEditor.Subclipse", //$NON-NLS-1$
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
                  PlatformUI.getWorkbench().getBrowserSupport().createBrowser("Subclipse").openURL(url); //$NON-NLS-1$
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
    
    // Toggle include merged revisions action
    toggleIncludeMergedRevisionsAction = new Action(Policy.bind("HistoryView.includeMergedRevisions")) { //$NON-NLS-1$
      public void run() {
        store.setValue(ISVNUIConstants.PREF_INCLUDE_MERGED_REVISIONS, toggleIncludeMergedRevisionsAction.isChecked());
    	refreshTable();
    	refresh();
      }
    };
    toggleIncludeMergedRevisionsAction.setChecked(store.getBoolean(ISVNUIConstants.PREF_INCLUDE_MERGED_REVISIONS));    
    
    IHistoryPageSite parentSite = getHistoryPageSite();
    IPageSite pageSite = parentSite.getWorkbenchPageSite();
    IActionBars actionBars = pageSite.getActionBars();

    // Contribute toggle text visible to the toolbar drop-down
    IMenuManager actionBarsMenu = actionBars.getMenuManager();
    actionBarsMenu.add(getGetNextAction());
    actionBarsMenu.add(getGetAllAction());
    actionBarsMenu.add(toggleStopOnCopyAction);
    actionBarsMenu.add(toggleIncludeMergedRevisionsAction);
    actionBarsMenu.add(new Separator());
    actionBarsMenu.add(toggleWrapCommentsAction);
    actionBarsMenu.add(new Separator());
    actionBarsMenu.add(toggleShowComments);
    actionBarsMenu.add(toggleShowAffectedPathsAction);
    actionBarsMenu.add(new Separator());
    for (int i = 0; i < toggleAffectedPathsModeActions.length; i++) {
      actionBarsMenu.add(toggleAffectedPathsModeActions[i]);
    }
    actionBarsMenu.add(new Separator());
    for (int i = 0; i < toggleAffectedPathsLayoutActions.length; i++) {
      actionBarsMenu.add(toggleAffectedPathsLayoutActions[i]);
    }
    
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
    if (changePathsViewer instanceof ChangePathsTreeViewer) {
    	((ChangePathsTreeViewer)changePathsViewer).setCurrentLogEntry(entry);
    }
    if (changePathsViewer instanceof ChangePathsFlatViewer) {
    	((ChangePathsFlatViewer)changePathsViewer).setCurrentLogEntry(entry);
    }
    if (changePathsViewer instanceof ChangePathsTableProvider) {
    	((ChangePathsTableProvider)changePathsViewer).setCurrentLogEntry(entry);
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
      svnHistoryPageControl.setMaximizedControl(tableParent);
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
    	SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);  
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
  
  private SVNRevision getSelectedRevision() {
	  IStructuredSelection sel = (IStructuredSelection)tableHistoryViewer.getSelection();
	  if (sel.getFirstElement() instanceof ILogEntry) {
		  return ((ILogEntry)sel.getFirstElement()).getRevision();
	  }
	  return null;
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
  
  private boolean isFile() {
	  IStructuredSelection sel = (IStructuredSelection)changePathsViewer.getSelection();
	  if (sel.size() == 1 && sel.getFirstElement() instanceof LogEntryChangePath) {
		  LogEntryChangePath changePath = (LogEntryChangePath)sel.getFirstElement();
		  try {
			return changePath.getRemoteResource() instanceof ISVNRemoteFile;
		} catch (SVNException e) {}
	  }
	  return false;
  }
  
  // open changed Path (double-click)
  private IAction getOpenChangedPathAction() {
    if(openChangedPathAction == null) {
      openChangedPathAction = new Action("Open") { //$NON-NLS-1$
        public void run() {
          if (!isFile()) { 
        	  MessageDialog.openError(Display.getDefault().getActiveShell(), Policy.bind("SVNHistoryPage.7"), Policy.bind("SVNHistoryPage.8")); //$NON-NLS-1$ //$NON-NLS-2$
          	  return;
          }
          OpenRemoteFileAction delegate = new OpenRemoteFileAction();
          delegate.setUsePegRevision(true);
          delegate.init(this);
          delegate.selectionChanged(this, changePathsViewer.getSelection());
          if(isEnabled()) {
            try {
              delegate.run(this);
            } finally {
              // disableEditorActivation = false;
            }
          }
        }
      };
    }
//  openChangedPathAction.setEnabled(isFile());
    return openChangedPathAction;

  }
  
  private IAction getShowHistoryAction() {
    if(showHistoryAction == null) {
      showHistoryAction = new Action("Show History", SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MENU_SHOWHISTORY)) { //$NON-NLS-1$
        public void run() {
          HistoryAction delegate = new HistoryAction();
          delegate.selectionChanged(this, changePathsViewer.getSelection());
          delegate.run(this);
        }
      };	    	
    }
    return showHistoryAction;
  }
  
  private IAction getCopyChangedPathAction() {
	  if (copyChangedPathAction == null) {
		  copyChangedPathAction = new Action(Policy.bind("HistoryView.copyChangedPath")) { //$NON-NLS-1$
			  public void run() {
				  ContainerSelectionDialog dialog = new ContainerSelectionDialog(Display.getDefault().getActiveShell(), null, false, Policy.bind("CopyAction.selectionLabel")); //$NON-NLS-1$
				  if (dialog.open() == ContainerSelectionDialog.OK) {
					Object[] result = dialog.getResult();
					if (result == null || result.length == 0) return;
					final Path path = (Path)result[0];
					IProject selectedProject;
					File target = null;
					if (path.segmentCount() == 1) {
						selectedProject = ResourcesPlugin.getWorkspace().getRoot().getProject(path.toString());
						target = selectedProject.getLocation().toFile();
					} else {
						IFile targetFile = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
						selectedProject = targetFile.getProject();
						target = targetFile.getLocation().toFile();
					}
					final IProject targetProject = selectedProject;
					final File destPath = target;
					BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
						public void run() {
							ISVNClientAdapter client = null;
							try {		
								IStructuredSelection sel = (IStructuredSelection)changePathsViewer.getSelection();
								if (sel.getFirstElement() instanceof LogEntryChangePath) {
									LogEntryChangePath changePath = (LogEntryChangePath)sel.getFirstElement();
									SVNRevision revision = changePath.getRevision();
									if (changePath.getAction() == 'D') {
								   		long rev = Long.parseLong(revision.toString());
							    		rev--;
							    		revision = new SVNRevision.Number(rev);
									}
									client = SVNProviderPlugin.getPlugin().getSVNClient();
									client.copy(changePath.getUrl(), destPath, revision, revision, true, false);
									targetProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
									SVNUIPlugin.getPlugin().getRepositoryManager().resourceCreated(null, null);
								}
							} catch (Exception e) {
								MessageDialog.openError(Display.getDefault().getActiveShell(), Policy.bind("HistoryView.copyError"), e.getMessage()); //$NON-NLS-1$
							} finally {
								SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(client);
							}
						}						
					});
				  }
			  }
		  };
	  }
	  return copyChangedPathAction;
  }
  
  private IAction getExportAction() {
    if(exportAction == null) {
      exportAction = new Action("Export...", SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MENU_EXPORT)) { //$NON-NLS-1$
        public void run() {
          ExportAction delegate = new ExportAction();
          delegate.selectionChanged(this, changePathsViewer.getSelection());
          delegate.run(this);
        }
      };	    	
    }
    return exportAction;
  }  
  
  private IAction getShowAnnotationAction() {
    if(showAnnotationAction == null) {
      showAnnotationAction = new Action("Show Annotation", SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MENU_ANNOTATE)) { //$NON-NLS-1$
        public void run() {
          if (!isFile()) { 
          	MessageDialog.openError(Display.getDefault().getActiveShell(), Policy.bind("SVNHistoryPage.11"), Policy.bind("SVNHistoryPage.12")); //$NON-NLS-1$ //$NON-NLS-2$
            	return;
          }
          AnnotationAction delegate = new AnnotationAction();
          delegate.selectionChanged(this, changePathsViewer.getSelection());
          delegate.run(this);
        }
      };	    	
    }
//	showAnnotationAction.setEnabled(isFile());
	return showAnnotationAction;
  }  
  
  private IAction getCompareAction() {
    if(compareAction == null) {
      compareAction = new Action("Compare...") { //$NON-NLS-1$
        public void run() {
          CompareAction delegate = new CompareAction();
          delegate.selectionChanged(this, changePathsViewer.getSelection());
          delegate.run(this);
        }
      };	    	
    }
	return compareAction;
  }
  
  private IAction getCreateTagFromRevisionChangedPathAction() {
    if(createTagFromRevisionChangedPathAction == null) {
    	createTagFromRevisionChangedPathAction = new Action() { //$NON-NLS-1$
        public void run() {
        	SVNRevision selectedRevision = null;
            ISelection selection = changePathsViewer.getSelection();
            if( !(selection instanceof IStructuredSelection))
              return;         
            IStructuredSelection sel = (IStructuredSelection)selection;
            ISVNRemoteResource remoteResource = null;
	  		  if (sel.getFirstElement() instanceof LogEntryChangePath) {
				  try {
					remoteResource = ((LogEntryChangePath)sel.getFirstElement()).getRemoteResource();
					selectedRevision = remoteResource.getRevision();
				} catch (SVNException e) {}
			  }
			  else if (sel.getFirstElement() instanceof HistoryFolder) {
				  HistoryFolder historyFolder = (HistoryFolder)sel.getFirstElement();
				  Object[] children = historyFolder.getChildren();
				  if (children != null && children.length > 0 && children[0] instanceof LogEntryChangePath) {
					  LogEntryChangePath changePath = (LogEntryChangePath)children[0];
					  try {
						remoteResource = changePath.getRemoteResource().getRepository().getRemoteFolder(historyFolder.getPath());
						selectedRevision = getSelectedRevision();
					} catch (SVNException e) {}
				  }
			  }  
	  		  if (remoteResource == null) return;
	  		ISVNRemoteResource[] remoteResources = { remoteResource };
	  		BranchTagWizard wizard = new BranchTagWizard(remoteResources);
	  		wizard.setRevisionNumber(Long.parseLong(selectedRevision.toString()));
        	WizardDialog dialog = new ClosableWizardDialog(getSite().getShell(), wizard);
        	if (dialog.open() == WizardDialog.OK) {	
                final SVNUrl sourceUrl = wizard.getUrl();
                final SVNUrl destinationUrl = wizard.getToUrl();
                final String message = wizard.getComment();
                final SVNRevision revision = wizard.getRevision();
                final boolean makeParents = wizard.isMakeParents();
                try {
                    BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
                        public void run() {
                          ISVNClientAdapter client = null;
                          try {
                            client = SVNProviderPlugin.getPlugin().getSVNClientManager().getSVNClient();
                            client.copy(sourceUrl, destinationUrl, message, revision, makeParents);
                            SVNUIPlugin.getPlugin().getRepositoryManager().resourceCreated(null, null);                         
                          } catch(Exception e) {
                            MessageDialog.openError(getSite().getShell(), Policy.bind("HistoryView.createTagFromRevision"), e //$NON-NLS-1$
                                .getMessage());
                          } finally {
                        	SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(client); 
                          }
                        }
                      });
                } catch(Exception e) {
                  MessageDialog.openError(getSite().getShell(), Policy.bind("HistoryView.createTagFromRevision"), e //$NON-NLS-1$
                      .getMessage());
                }        		
        	}
//	  		SvnWizardBranchTagPage branchTagPage = new SvnWizardBranchTagPage(remoteResource);
//            branchTagPage.setRevisionNumber(Long.parseLong(selectedRevision.toString()));
//        	SvnWizard wizard = new SvnWizard(branchTagPage);
//            SvnWizardDialog dialog = new SvnWizardDialog(getSite().getShell(), wizard);
//            wizard.setParentDialog(dialog); 
//            if (!(dialog.open() == SvnWizardDialog.OK)) return;
//            final SVNUrl sourceUrl = branchTagPage.getUrl();
//            final SVNUrl destinationUrl = branchTagPage.getToUrl();
//            final String message = branchTagPage.getComment();
//            final SVNRevision revision = branchTagPage.getRevision();
//            final boolean makeParents = branchTagPage.isMakeParents();
//            try {
//                BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
//                    public void run() {
//                      try {
//                        ISVNClientAdapter client = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
//                        client.copy(sourceUrl, destinationUrl, message, revision, makeParents);
//                      } catch(Exception e) {
//                        MessageDialog.openError(getSite().getShell(), Policy.bind("HistoryView.createTagFromRevision"), e
//                            .getMessage());
//                      }
//                    }
//                  });
//            } catch(Exception e) {
//              MessageDialog.openError(getSite().getShell(), Policy.bind("HistoryView.createTagFromRevision"), e
//                  .getMessage());
//            }
        }
      };	    	
    }
    
    ISelection selection = changePathsViewer.getSelection();
    if(selection instanceof IStructuredSelection) {
      IStructuredSelection sel = (IStructuredSelection) selection;
      SVNRevision selectedRevision = null;
      if(sel.size() == 1) {
//        ISVNRemoteResource remoteResource = null;
  		  if (sel.getFirstElement() instanceof LogEntryChangePath && ((LogEntryChangePath)sel.getFirstElement()).getAction() != 'D') {
//			  try {
//				remoteResource = ((LogEntryChangePath)sel.getFirstElement()).getRemoteResource();
				selectedRevision = ((LogEntryChangePath)sel.getFirstElement()).getRevision();
//				selectedRevision = remoteResource.getRevision();
//			} catch (SVNException e) {}
		  }
		  else if (sel.getFirstElement() instanceof HistoryFolder) {
			  HistoryFolder historyFolder = (HistoryFolder)sel.getFirstElement();
			  Object[] children = historyFolder.getChildren();
			  if (children != null && children.length > 0 && children[0] instanceof LogEntryChangePath) {
				selectedRevision = getSelectedRevision();
			  }
		  } 
  		createTagFromRevisionChangedPathAction.setEnabled(selectedRevision != null);
  		if (selectedRevision == null) {
	        createTagFromRevisionChangedPathAction.setText(Policy.bind("HistoryView.createTagFromRevision", "" //$NON-NLS-1$ //$NON-NLS-2$
		            + ((LogEntryChangePath)sel.getFirstElement()).getRevision()));
  		} else {
	        createTagFromRevisionChangedPathAction.setText(Policy.bind("HistoryView.createTagFromRevision", "" //$NON-NLS-1$ //$NON-NLS-2$
	            + selectedRevision));
  		}
      }
    }	
    createTagFromRevisionChangedPathAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MENU_BRANCHTAG));        
	    
	return createTagFromRevisionChangedPathAction;
  }  
  
  class AnnotationAction extends ShowAnnotationAction {
	  public IStructuredSelection fSelection;
	  
	  public AnnotationAction() {
		 super();
	  }
	  
	  protected ISVNRemoteFile getSingleSelectedSVNRemoteFile() {
		  ISVNRemoteResource remoteResource = null;
		  if (fSelection.getFirstElement() instanceof LogEntryChangePath) {
			  try {
				remoteResource = ((LogEntryChangePath)fSelection.getFirstElement()).getRemoteResource();
				if (remoteResource instanceof RemoteResource) {
					((RemoteResource)remoteResource).setPegRevision(((LogEntryChangePath)fSelection.getFirstElement()).getRevision());
				}
			} catch (SVNException e) {}
		  }
		  return (ISVNRemoteFile)remoteResource; 
	  }
	  
	  protected boolean isEnabled() {
		  return true;
	  }
	  
	  public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			fSelection= (IStructuredSelection) sel;
		}
	  }			  
  }  
  
  class HistoryAction extends ShowHistoryAction {
	  public IStructuredSelection fSelection;
	  
	  public HistoryAction() {
		 super();
	  }
	  
	  protected ISVNRemoteResource[] getSelectedRemoteResources() {
		  ISVNRemoteResource remoteResource = null;
		  if (fSelection.getFirstElement() instanceof LogEntryChangePath) {
			  try {
				remoteResource = ((LogEntryChangePath)fSelection.getFirstElement()).getRemoteResource();
			} catch (SVNException e) {}
		  }
		  else if (fSelection.getFirstElement() instanceof HistoryFolder) {
			  HistoryFolder historyFolder = (HistoryFolder)fSelection.getFirstElement();
			  Object[] children = historyFolder.getChildren();
			  if (children != null && children.length > 0 && children[0] instanceof LogEntryChangePath) {
				  LogEntryChangePath changePath = (LogEntryChangePath)children[0];
				  try {
					remoteResource = changePath.getRemoteResource().getRepository().getRemoteFolder(historyFolder.getPath());
				} catch (SVNException e) {}
			  }
		  }
		  if (remoteResource != null) {
			   ISVNRemoteResource[] selectedResource = { remoteResource };
			  return selectedResource;
		  }		  
		  return new ISVNRemoteResource[0];
	  }
	  
	  protected boolean isEnabled() {
		  return true;
	  }
	  
	  public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			fSelection= (IStructuredSelection) sel;
		}
	  }			  
  }
  
  class ExportAction extends ExportRemoteFolderAction {
	  public IStructuredSelection fSelection;
	  
	  public ExportAction() {
		 super();
	  }
	  
	  protected ISVNRemoteResource[] getSelectedRemoteResources() {
		  ISVNRemoteResource remoteResource = null;
		  if (fSelection.getFirstElement() instanceof LogEntryChangePath) {
			  try {
				remoteResource = ((LogEntryChangePath)fSelection.getFirstElement()).getRemoteResource();
			} catch (SVNException e) {}
		  }
		  if (remoteResource != null) {
			   ISVNRemoteResource[] selectedResource = { remoteResource };
			  return selectedResource;
		  }		  
		  return new ISVNRemoteResource[0];
	  }
	  
	  protected boolean isEnabled() {
		  return true;
	  }
	  
	  public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			fSelection= (IStructuredSelection) sel;
		}
	  }			  
  }  
  
  class CompareAction extends ShowDifferencesAsUnifiedDiffAction {
	  public IStructuredSelection fSelection;
	  
	  public CompareAction() {
		 super();
		 setUsePegRevision(true);
	  }
	  
	  protected ISVNRemoteResource[] getSelectedRemoteResources() {
		  ISVNRemoteResource remoteResource = null;
		  if (fSelection.getFirstElement() instanceof LogEntryChangePath) {
			  try {
				remoteResource = ((LogEntryChangePath)fSelection.getFirstElement()).getRemoteResource();
				if (remoteResource instanceof RemoteResource) {
					((RemoteResource) remoteResource).setPegRevision(((LogEntryChangePath)fSelection.getFirstElement()).getRevision());
				}			
			  } catch (SVNException e) {}
		  }
		  else if (fSelection.getFirstElement() instanceof HistoryFolder) {
			  HistoryFolder historyFolder = (HistoryFolder)fSelection.getFirstElement();
			  Object[] children = historyFolder.getChildren();
			  if (children != null && children.length > 0 && children[0] instanceof LogEntryChangePath) {
				  LogEntryChangePath changePath = (LogEntryChangePath)children[0];
				  try {
					  ISVNRemoteResource changePathResource = changePath.getRemoteResource();
					  ISVNRemoteResource remoteFolder = changePathResource.getRepository().getRemoteFolder(historyFolder.getPath());					  
					  remoteResource = new RemoteFolder(null, changePathResource.getRepository(), remoteFolder.getUrl(), changePathResource.getRevision(), (SVNRevision.Number)changePathResource.getRevision(), null, null); 
				} catch (SVNException e) {}
			  }
		  }
		  if (remoteResource != null) {
			   ISVNRemoteResource[] selectedResource = { remoteResource };
			  return selectedResource;
		  }		  
		  return new ISVNRemoteResource[0];
	  }
	  
	  protected boolean isEnabled() {
		  return true;
	  }
	  
	  public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			fSelection= (IStructuredSelection) sel;
		}
	  }			  
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
                	if (remoteFile instanceof RemoteResource) {
                		if (resource != null) {
                			ISVNLocalResource localResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
                			((RemoteResource)remoteFile).setPegRevision(localResource.getRevision());
                		} else {
                			((RemoteResource)remoteFile).setPegRevision(SVNRevision.HEAD);
                		}
                	}
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
        			historyTableProvider.setProjectProperties(ProjectProperties.getProjectProperties(resource));
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
  
  // get switch action (context menu)
  private IAction getSwitchAction() {
	  if (switchAction == null) {
		  switchAction = new Action() {
			  public void run() {
				  if(selection instanceof IStructuredSelection) {
					  IStructuredSelection ss = (IStructuredSelection) selection;
					  if(ss.size() == 1) {
						  ILogEntry currentSelection = getLogEntry(ss);
						  IResource[] resources = { resource };
						  SvnWizardSwitchPage switchPage = new SvnWizardSwitchPage(resources, currentSelection.getRevision().getNumber());
					        SvnWizard wizard = new SvnWizard(switchPage);
					        SvnWizardDialog dialog = new SvnWizardDialog(getSite().getShell(), wizard);
					        wizard.setParentDialog(dialog);
					        if (dialog.open() == SvnWizardDialog.OK) {
					            SVNUrl[] svnUrls = switchPage.getUrls();
					            SVNRevision svnRevision = switchPage.getRevision();
					            SwitchOperation switchOperation = new SwitchOperation(getSite().getPage().getActivePart(), resources, svnUrls, svnRevision);
					            switchOperation.setDepth(switchPage.getDepth());
					            switchOperation.setSetDepth(switchPage.isSetDepth());
					            switchOperation.setIgnoreExternals(switchPage.isIgnoreExternals());
					            switchOperation.setForce(switchPage.isForce());
					            switchOperation.setIgnoreAncestry(switchPage.isIgnoreAncestry());
					            switchOperation.setConflictResolver(switchPage.getConflictResolver());
					            try {
									switchOperation.run();
								} catch (Exception e) {
						            MessageDialog.openError(getSite().getShell(), switchAction.getText(), e
						                    .getMessage());
								}
					        }					  
					  }
				  }
			  }		  
		  };
	  }
      ISelection selection = getSelection();
      if(selection instanceof IStructuredSelection) {
        IStructuredSelection ss = (IStructuredSelection) selection;
        if(ss.size() == 1) {
          ILogEntry currentSelection = getLogEntry(ss);
          switchAction.setText(Policy.bind("HistoryView.switchToRevision", "" //$NON-NLS-1$ //$NON-NLS-2$
              + currentSelection.getRevision().getNumber()));
        }
      }	
      switchAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MENU_SWITCH));
	  return switchAction;
  }

  // get create tag from revision action (context menu)
  private IAction getCreateTagFromRevisionAction() {
    if(createTagFromRevisionAction == null) {
      createTagFromRevisionAction = new Action() {
        public void run() {
          ISelection selection = getSelection();
          if( !(selection instanceof IStructuredSelection))
            return;
          ILogEntry currentSelection = getLogEntry((IStructuredSelection) selection); 
          BranchTagWizard wizard;
          if (resource == null) {
        	  ISVNRemoteResource[] remoteResources = { historyTableProvider.getRemoteResource() };
        	  wizard = new BranchTagWizard(remoteResources);
          } else {
        	  IResource[] resources = { resource };
        	  wizard = new BranchTagWizard(resources);
          }
          wizard.setRevisionNumber(currentSelection.getRevision().getNumber());
      	  WizardDialog dialog = new ClosableWizardDialog(getSite().getShell(), wizard);
    	  if (dialog.open() == WizardDialog.OK) {	
              final SVNUrl sourceUrl =wizard.getUrl();
              final SVNUrl destinationUrl = wizard.getToUrl();
              final String message = wizard.getComment();
              final SVNRevision revision = wizard.getRevision();
              final boolean makeParents = wizard.isMakeParents();
              boolean createOnServer = wizard.isCreateOnServer();
              IResource[] resources = { resource };
              try {
                if(resource == null) {
                  BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
                    public void run() {
                      ISVNClientAdapter client = null;
                      try {
                        client = SVNProviderPlugin.getPlugin().getSVNClientManager().getSVNClient();
                        client.copy(sourceUrl, destinationUrl, message, revision, makeParents);
                        SVNUIPlugin.getPlugin().getRepositoryManager().resourceCreated(null, null);
                      } catch(Exception e) {
                        MessageDialog.openError(getSite().getShell(), Policy.bind("HistoryView.createTagFromRevision"), e //$NON-NLS-1$
                            .getMessage());
                      } finally {
                    	  SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(client);
                      }
                    }
                  });
                } else {
                	BranchTagOperation branchTagOperation = new BranchTagOperation(getSite().getPage().getActivePart(), resources, new SVNUrl[] { sourceUrl }, destinationUrl,
                            createOnServer, wizard.getRevision(), message);
                	branchTagOperation.setMakeParents(makeParents);
                	branchTagOperation.setNewAlias(wizard.getNewAlias());
                	branchTagOperation.run();
                }
              } catch(Exception e) {
                MessageDialog.openError(getSite().getShell(), Policy.bind("HistoryView.createTagFromRevision"), e //$NON-NLS-1$
                    .getMessage());
              }    		
    	  }
        }
      };
    }
    ISelection selection = getSelection();
    if(selection instanceof IStructuredSelection) {
      IStructuredSelection ss = (IStructuredSelection) selection;
      if(ss.size() == 1) {
        ILogEntry currentSelection = getLogEntry(ss);
        createTagFromRevisionAction.setText(Policy.bind("HistoryView.createTagFromRevision", "" //$NON-NLS-1$ //$NON-NLS-2$
            + currentSelection.getRevision().getNumber()));
      }
    }	
    createTagFromRevisionAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MENU_BRANCHTAG));    
    return createTagFromRevisionAction;
  }

  private IAction getSetCommitPropertiesAction() {
    // set Action (context menu)
    if(setCommitPropertiesAction == null) {
      setCommitPropertiesAction = new Action(Policy.bind("HistoryView.setCommitProperties")) { //$NON-NLS-1$
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
                .getProjectProperties(resource) : (ourSelection.getRemoteResource() != null) ? ProjectProperties.getProjectProperties(ourSelection
                .getRemoteResource()) : ProjectProperties.getProjectProperties(remoteResource); // will return null!

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
  
  private IAction getShowRevisionPropertiesAction() {
  if(showRevisionPropertiesAction == null) {
    showRevisionPropertiesAction = new Action(Policy.bind("HistoryView.showRevisionProperties")) { //$NON-NLS-1$
      public void run() {
        try {
          final ISelection selection = getSelection();
          if( !(selection instanceof IStructuredSelection))
            return;
          final ILogEntry ourSelection = getLogEntry((IStructuredSelection) selection);

          ISVNRemoteResource selectionRemoteResource = ourSelection.getRemoteResource();
          SvnRevPropertiesView revPropsView = null;     

          try {
            revPropsView = (SvnRevPropertiesView)getSite().getPage().showView(SvnRevPropertiesView.VIEW_ID);
          } catch (PartInitException e) {
            SVNUIPlugin.openError(getSite().getShell(), null, null, e, SVNUIPlugin.LOG_TEAM_EXCEPTIONS);
          }

          if (revPropsView != null) {
	          revPropsView.showSvnProperties(selectionRemoteResource);
	          revPropsView.refresh();
          }
        } catch(SVNException e) {
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
  return showRevisionPropertiesAction;
}
  
  private IAction getShowRevisionsAction() {
	  if (showRevisionsAction == null) {
		  showRevisionsAction = new Action(Policy.bind("HistoryView.showMergedRevisions")) { //$NON-NLS-1$
			  public void run() {
		          ISelection selection = getSelection();
		          if( !(selection instanceof IStructuredSelection))
		            return;
		          IStructuredSelection ss = (IStructuredSelection) selection;
		          ILogEntry logEntry = (ILogEntry)ss.getFirstElement();
		          ShowRevisionsDialog dialog = null;
		          if (resource != null) dialog = new ShowRevisionsDialog(getSite().getShell(), logEntry, resource, includeTags, SVNHistoryPage.this);
		          else if (remoteResource != null) dialog = new ShowRevisionsDialog(getSite().getShell(), logEntry, remoteResource, includeTags, SVNHistoryPage.this);
		          if (dialog != null) dialog.open();
			  }
		  };
	  }
	  return showRevisionsAction;
  }
  
  private SVNRevisionRange[] getRevisionRanges() {
	  List revisionRanges = new ArrayList();
	  ISelection selection = getSelection();
	  if (selection instanceof IStructuredSelection) {
		  IStructuredSelection ss = (IStructuredSelection)selection;
		  List selectionList = ss.toList();
		  TableItem[] items = tableHistoryViewer.getTable().getItems();
		  SVNRevision revision1 = null;
		  SVNRevision revision2 = null;
		  for (int i = 0; i < items.length; i++) {
			  if (items[i].getData() instanceof ILogEntry) {
				  ILogEntry logEntry = (ILogEntry)items[i].getData();
				  if (selectionList.contains(logEntry)) {
					  if (revision1 == null) {
						  revision1 = logEntry.getRevision();
					  }
					  revision2 = logEntry.getRevision();
					  revertEnabled = true;
					  LogEntryChangePath[] changePaths = logEntry.getLogEntryChangePaths();
					  if (changePaths != null) {
						  for (LogEntryChangePath changePath : changePaths) {
							  if (changePath.getPath().equals(remoteResource.getRepositoryRelativePath())) {
								  if (changePath.getAction() == 'A') {
									  revertEnabled = false;
								  }
								  break;
							  }
						  }
					  }
				  }
				  else {
					  if (revision1 != null) {
						  SVNRevisionRange revisionRange = new SVNRevisionRange(revision1, revision2);
						  revisionRanges.add(revisionRange);
						  revision1 = null;
						  revision2 = null;
					  }
				  }
			  }
		  }	  
		  if (revision1 != null) {
			  SVNRevisionRange revisionRange = new SVNRevisionRange(revision1, revision2);
			  revisionRanges.add(revisionRange);
		  }
	  }
	  SVNRevisionRange[] revisionRangeArray = new SVNRevisionRange[revisionRanges.size()];
	  revisionRanges.toArray(revisionRangeArray);
	  return revisionRangeArray;
  }

  // get revert changes action (context menu)
  private IAction getRevertChangesAction() {
    revisionRanges = getRevisionRanges();
    if(revertChangesAction == null) {
      revertChangesAction = new Action() {
        public void run() {
          ISelection selection = getSelection();
          if( !(selection instanceof IStructuredSelection))
            return;
          final IStructuredSelection ss = (IStructuredSelection) selection;
          if(ss.size() == 1) {
            if( !MessageDialog.openConfirm(getSite().getShell(), Policy.bind("HistoryView.revertRevision"), Policy.bind(
                "HistoryView.confirmRevertRevision", resource.getFullPath().toString()))) //$NON-NLS-1$
              return;
          } else {
            if( !MessageDialog.openConfirm(getSite().getShell(), Policy.bind("HistoryView.revertRevisions"), Policy.bind(
                "HistoryView.confirmRevertRevisions", resource.getFullPath().toString()))) //$NON-NLS-1$
              return;
          }
          BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
            public void run() {
              ILogEntry firstElement = getFirstElement();
              ILogEntry lastElement = getLastElement();            
              final IResource[] resources = { resource};
              try {
            	  final SVNUrl path1 = new SVNUrl(firstElement.getResource().getUrl() + "@HEAD");
                  final SVNUrl path2 = new SVNUrl(lastElement.getResource().getUrl() + "@HEAD");
                for (int i = 0; i < revisionRanges.length; i++) {
                	final SVNRevision revision1 = revisionRanges[i].getFromRevision();
                	final SVNRevision revision2 = new SVNRevision.Number(((SVNRevision.Number)revisionRanges[i].getToRevision()).getNumber() - 1);
                    WorkspaceAction mergeAction = new WorkspaceAction() {
                        protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
                          new MergeOperation(getSite().getPage().getActivePart(), resources, path1, revision1, path2,
                              revision2).run();
                        }
                    };  
                    mergeAction.run(null);
                }
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
        revertChangesAction.setText(Policy.bind("HistoryView.revertChangesFromRevision", "" //$NON-NLS-1$ //$NON-NLS-2$
            + currentSelection.getRevision().getNumber()));
      }
      if(ss.size() > 1) {
        revertChangesAction.setText(Policy.bind("HistoryView.revertChangesFromRevisions")); //$NON-NLS-1$
      }
    }
    revertChangesAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MENU_MARKMERGED));
    revertChangesAction.setEnabled(revertEnabled);
    return revertChangesAction;
  }
  
  private GenerateChangeLogAction getGenerateChangeLogAction() {
	  if (generateChangeLogAction == null) generateChangeLogAction = new GenerateChangeLogAction(new ISelectionProvider() {
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
		}
		public ISelection getSelection() {
			return SVNHistoryPage.this.getSelection();
		}
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		}
		public void setSelection(ISelection selection) {
		}
	  });
	  return generateChangeLogAction;
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
						  if (!historySearchDialog.getSearchAllLogs() && (historySearchDialog.getStartRevision() != null || historySearchDialog.getEndRevision() != null)) {
							  getRefreshAction().run();
						  }
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
      super(Policy.bind("HistoryView.getNext"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_GET_NEXT)); //$NON-NLS-1$
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
      setToolTipText(Policy.bind("HistoryView.getNext") + " " + entriesToFetch); //$NON-NLS-1$ //$NON-NLS-2$
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
          final SVNRevision pegRevision = remoteResource.getRevision();
          SVNRevision revisionEnd = new SVNRevision.Number(0);
          boolean stopOnCopy = toggleStopOnCopyAction.isChecked();
          boolean includeMergedRevisions = toggleIncludeMergedRevisionsAction.isChecked();
          int entriesToFetch = store.getInt(ISVNUIConstants.PREF_LOG_ENTRIES_TO_FETCH);
          long limit = entriesToFetch;
          try {
	          entries = getLogEntries(monitor, remoteResource, pegRevision, revisionStart, revisionEnd, stopOnCopy,
	          limit + 1, tagManager, includeMergedRevisions); 
          } catch (TeamException e) {
        	  if (revisionStart.equals(SVNRevision.HEAD) && pegRevision != null && e.getMessage() != null && e.getMessage().contains("svn: Unable to find repository location")) {
        		  revisionStart = pegRevision;
        		  entries = getLogEntries(monitor, remoteResource, pegRevision, revisionStart, revisionEnd, stopOnCopy,
    	    	          limit + 1, tagManager, includeMergedRevisions);  
        	  }
        	  else {
        		  throw e;
        	  }
          }
          
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
    	if (e instanceof SVNException) {
    		if (((SVNException)e).operationInterrupted()) {
    			return Status.OK_STATUS;
    		}
    	}
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
          boolean includeMergedRevisions = toggleIncludeMergedRevisionsAction.isChecked();
          int entriesToFetch = store.getInt(ISVNUIConstants.PREF_LOG_ENTRIES_TO_FETCH);
          long limit = entriesToFetch;
          ILogEntry[] nextEntries = getLogEntries(monitor, remoteResource, pegRevision, revisionStart, revisionEnd,
              stopOnCopy, limit + 1, tagManager, includeMergedRevisions);
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
              }
            }
          });
          if(entries.length > 0) {
              lastEntry = entries[ entries.length - 1];
              long lastEntryNumber = lastEntry.getRevision().getNumber();
              revisionStart = new SVNRevision.Number(lastEntryNumber - 1);
          }
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

		protected ILogEntry[] getLogEntries(IProgressMonitor monitor, ISVNRemoteResource remoteResource, SVNRevision pegRevision, SVNRevision revisionStart, SVNRevision revisionEnd, boolean stopOnCopy, long limit, AliasManager tagManager, boolean includeMergedRevisions) throws TeamException
		{
			// If filtering by revision range, pass upper/lower revisions to API and override limit.
			SVNRevision start = revisionStart;
			SVNRevision end = revisionEnd;
			long fetchLimit = limit;
	        if (historySearchDialog != null && !historySearchDialog.getSearchAllLogs()) {
	        	if (historySearchDialog.getStartRevision() != null || historySearchDialog.getEndRevision() != null) {
	        		if (getClearSearchAction().isEnabled()) {
	        			if (historySearchDialog.getStartRevision() != null) end = historySearchDialog.getStartRevision();
	        			if (historySearchDialog.getEndRevision() != null) start = historySearchDialog.getEndRevision();
	        			fetchLimit = 0;
	        			getGetNextAction().setEnabled(false);
	        		}
	        	}
	        }		
	        ISVNClientAdapter svnClient = remoteResource.getRepository().getSVNClient();
	        try {
		        CancelableSVNLogMessageCallback callback = new CancelableSVNLogMessageCallback(monitor, svnClient);
				GetLogsCommand logCmd = new GetLogsCommand(remoteResource, pegRevision, start, end, stopOnCopy, fetchLimit, tagManager, includeMergedRevisions);
				logCmd.setCallback(callback);
				logCmd.run(monitor);
				return logCmd.getLogEntries(); 	
	        }
	        finally {
	        	remoteResource.getRepository().returnSVNClient(svnClient);
	        }
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
          SVNRevision revisionEnd = new SVNRevision.Number(0);
          boolean stopOnCopy = toggleStopOnCopyAction.isChecked();
          boolean includeMergedRevisions = toggleIncludeMergedRevisionsAction.isChecked();
          long limit = 0;
          entries = getLogEntries(monitor, remoteResource, pegRevision, SVNRevision.HEAD, revisionEnd, stopOnCopy, limit,
                  tagManager, includeMergedRevisions);
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
		  super(""); //$NON-NLS-1$
	  }
	  
	  public IStatus run(IProgressMonitor monitor) {
		  if (!historySearchDialog.getSearchAllLogs() && (historySearchDialog.getStartRevision() != null || historySearchDialog.getEndRevision() != null)) {
	          final ISVNRemoteResource remoteResource = historyTableProvider.getRemoteResource();
	          if(fetchAllLogEntriesJob == null) {
	        	  fetchAllLogEntriesJob = new FetchAllLogEntriesJob();
	          }
	          if(fetchAllLogEntriesJob.getState() != Job.NONE) {
	        	  fetchAllLogEntriesJob.cancel();
	          }
	          fetchAllLogEntriesJob.setRemoteFile(remoteResource);
	          Utils.schedule(fetchAllLogEntriesJob, getSite());			 
		  } else {
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
		  }
		  
		  final HistorySearchViewerFilter viewerFilter = new HistorySearchViewerFilter(
				  historySearchDialog.getUser(), 
				  historySearchDialog.getComment(), 
				  historySearchDialog.getStartDate(), 
				  historySearchDialog.getEndDate(), 
				  historySearchDialog.getRegExp(),
				  historySearchDialog.getStartRevision(),
				  historySearchDialog.getEndRevision());
		  
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
      		// Preserve the original starting revision so that when Next is pressed
      		// the correct log entries will be fetched.
      		SVNRevision holdRevisionStart = revisionStart;
        	revisionStart = SVNRevision.HEAD;
      		ISVNLocalResource localResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
      		try {
                  if (localResource != null && !localResource.getStatus().isAdded()) {
                  	ISVNRemoteResource baseResource = localResource.getBaseResource();
                  	historyTableProvider.setRemoteResource(baseResource);
                  	historyTableProvider.setProjectProperties(null);
                  	tableHistoryViewer.refresh();
                  }
              } catch (SVNException e) {
            	  if (!e.operationInterrupted()) {
            		  SVNUIPlugin.openError(getHistoryPageSite().getShell(), null, null, e);
            	  }
              }
              revisionStart = holdRevisionStart;
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

  public void initialize() {	  
  }
  
public static void setHistorySearchViewerFilter(
		HistorySearchViewerFilter historySearchViewerFilter) {
	SVNHistoryPage.historySearchViewerFilter = historySearchViewerFilter;
}

}
