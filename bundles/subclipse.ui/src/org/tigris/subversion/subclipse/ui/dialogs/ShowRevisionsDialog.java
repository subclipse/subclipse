package org.tigris.subversion.subclipse.ui.dialogs;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.history.AliasManager;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntry;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.history.ChangePathsFlatViewer;
import org.tigris.subversion.subclipse.ui.history.ChangePathsTableProvider;
import org.tigris.subversion.subclipse.ui.history.ChangePathsTreeViewer;
import org.tigris.subversion.subclipse.ui.history.SVNHistoryPage;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class ShowRevisionsDialog extends SubclipseTrayDialog {
	private ILogEntry logEntry;
    private IResource resource;
    private ISVNRemoteResource remoteResource;
    private boolean includeTags;
    private SashForm sashForm;
    private StructuredViewer changePathsViewer;
    private TreeViewer treeHistoryViewer;
    private TextViewer textViewer;
    private IAction showDifferencesAsUnifiedDiffAction;
    private IDialogSettings settings;
    private TreeColumn revisionColumn;
    private TreeColumn tagsColumn;
    private TreeColumn dateColumn;
    private TreeColumn authorColumn;
    private TreeColumn commentColumn;
    private SVNHistoryPage historyPage;
    private String title;
    private boolean selectFirst;

	private static final int WIDTH_HINT = 500;
	private final static int LOG_HEIGHT_HINT = 200;
	private final static int COMMENT_HEIGHT_HINT = 100;
	
	//column constants
	private final static int COL_REVISION = 0;
	private final static int COL_TAGS = 1;
	private final static int COL_DATE = 2;
	private final static int COL_AUTHOR = 3;
	private final static int COL_COMMENT = 4;

    public ShowRevisionsDialog(Shell parentShell, ILogEntry logEntry, IResource resource, boolean includeTags, SVNHistoryPage historyPage) {
        super(parentShell);
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);
        this.logEntry = logEntry;
        this.resource = resource;
        this.includeTags = includeTags;
        this.historyPage = historyPage;
		settings = SVNUIPlugin.getPlugin().getDialogSettings();
    }
    
    public ShowRevisionsDialog(Shell parentShell, ILogEntry logEntry, ISVNRemoteResource remoteResource, boolean includeTags, SVNHistoryPage historyPage) {
        super(parentShell);
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);
        this.logEntry = logEntry;
        this.remoteResource = remoteResource;
        this.includeTags = includeTags;
        this.historyPage = historyPage;
		settings = SVNUIPlugin.getPlugin().getDialogSettings();
    }
    
	protected Control createDialogArea(Composite parent) {
		if (title == null)
			getShell().setText(Policy.bind("HistoryView.showMergedRevisions")); //$NON-NLS-1$
		else
			getShell().setText(title);
		
	    Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);	
		
        sashForm = new SashForm(composite, SWT.VERTICAL);
        sashForm.setLayout(new GridLayout());
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite historyGroup = new Composite(sashForm, SWT.NULL);
        historyGroup.setLayout(new GridLayout());
        historyGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        Tree tree = new Tree(historyGroup, SWT.BORDER | SWT.FULL_SELECTION);
        treeHistoryViewer = new TreeViewer(tree);
		data = new GridData(GridData.FILL_BOTH);
		data.widthHint = WIDTH_HINT;
		data.heightHint = LOG_HEIGHT_HINT;
		treeHistoryViewer.getTree().setLayoutData(data);		
		treeHistoryViewer.getTree().setHeaderVisible(true);
		
		revisionColumn = new TreeColumn(treeHistoryViewer.getTree(), SWT.NONE);
		revisionColumn.setResizable(true);
		revisionColumn.setText(Policy.bind("HistoryView.revision")); //$NON-NLS-1$
		
		int revisionWidth = 75;
		int tagsWidth = 225;
		int dateWidth = 100;
		int authorWidth = 100;
		int commentWidth = 300;
		try {
			revisionWidth = settings.getInt("ShowRevisionsDialog.width_revision");
			dateWidth = settings.getInt("ShowRevisionsDialog.width_date");
			authorWidth = settings.getInt("ShowRevisionsDialog.width_author");
			commentWidth = settings.getInt("ShowRevisionsDialog.width_comment");
			tagsWidth = settings.getInt("ShowRevisionsDialog.width_tag");
		} catch (Exception e) {}
		
		revisionColumn.setWidth(revisionWidth);
		
		if (includeTags) {
			tagsColumn = new TreeColumn(treeHistoryViewer.getTree(), SWT.NONE);
			tagsColumn.setResizable(true);
			tagsColumn.setText(Policy.bind("HistoryView.tags")); //$NON-NLS-1$
			tagsColumn.setWidth(tagsWidth);
		}
		
		dateColumn = new TreeColumn(treeHistoryViewer.getTree(), SWT.NONE);
		dateColumn.setResizable(true);
		dateColumn.setText(Policy.bind("HistoryView.date")); //$NON-NLS-1$
		dateColumn.setWidth(dateWidth);
		
		authorColumn = new TreeColumn(treeHistoryViewer.getTree(), SWT.NONE);
		authorColumn.setResizable(true);
		authorColumn.setText(Policy.bind("HistoryView.author")); //$NON-NLS-1$
		authorColumn.setWidth(authorWidth);				
		
		commentColumn = new TreeColumn(treeHistoryViewer.getTree(), SWT.NONE);
		commentColumn.setResizable(true);
		commentColumn.setText(Policy.bind("HistoryView.comment")); //$NON-NLS-1$
		commentColumn.setWidth(commentWidth);		
		
		treeHistoryViewer.setLabelProvider(new ITableLabelProvider() {

			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}

			public String getColumnText(Object element, int columnIndex) {
	            ILogEntry entry = (ILogEntry)element;	
	            int index = columnIndex;
	            if (index > 0 && !includeTags) index++;
				switch (index) {
				case COL_REVISION:
					String revision = entry.getRevision().toString();
					return revision;			
				case COL_TAGS:
					return AliasManager.getAliasesAsString(entry.getTags());
				case COL_DATE:
					Date date = entry.getDate();
					if (date == null) return Policy.bind("notAvailable"); //$NON-NLS-1$
					return DateFormat.getInstance().format(date);
				case COL_AUTHOR:
					if(entry.getAuthor() == null) return Policy.bind("noauthor"); //$NON-NLS-1$
					return entry.getAuthor();
				case COL_COMMENT:
					String comment = entry.getComment();
					if (comment == null) return "";   //$NON-NLS-1$
					int rIndex = comment.indexOf("\r");  //$NON-NLS-1$
					int nIndex = comment.indexOf("\n");	 //$NON-NLS-1$
					if( (rIndex == -1) && (nIndex == -1) )
						return comment;
						
					if( (rIndex == 0) || (nIndex == 0) )
						return Policy.bind("HistoryView.[...]_4"); //$NON-NLS-1$
						
					if(rIndex != -1)
						return Policy.bind("SVNCompareRevisionsInput.truncate", comment.substring(0, rIndex)); //$NON-NLS-1$
					else
						return Policy.bind("SVNCompareRevisionsInput.truncate", comment.substring(0, nIndex)); //$NON-NLS-1$
				}
				return null;
			}

			public void addListener(ILabelProviderListener listener) {
			}

			public void dispose() {
			}

			public boolean isLabelProperty(Object element, String property) {
				return false;
			}

			public void removeListener(ILabelProviderListener listener) {
			}			
		});
		
		treeHistoryViewer.setContentProvider(new WorkbenchContentProvider() {

			public Object[] getChildren(Object element) {
				if (element instanceof ILogEntry) {
					return ((ILogEntry)element).getChildMessages();
				}
				ILogEntry[] logEntries = { logEntry };
				return logEntries;
			}

			public Object[] getElements(Object element) {
				return getChildren(element);
			}

			public boolean hasChildren(Object element) {
				if (element instanceof ILogEntry) {
					return ((ILogEntry)element).getNumberOfChildren() > 0;
				}
				return false;
			}
			
		});

		treeHistoryViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
				if (selection == null || !(selection instanceof IStructuredSelection)) {
					textViewer.setDocument(new Document("")); //$NON-NLS-1$
                    changePathsViewer.setInput(null);
					return;
				}
				IStructuredSelection ss = (IStructuredSelection)selection;
				if (ss.size() != 1) {
					textViewer.setDocument(new Document("")); //$NON-NLS-1$
                    changePathsViewer.setInput(null);
					return;
				}
				LogEntry entry = (LogEntry)ss.getFirstElement();
				textViewer.setDocument(new Document(entry.getComment()));
                changePathsViewer.setInput(entry);                     
            }		    
		});
		
		if (resource == null)
			treeHistoryViewer.setInput(remoteResource);
		else
			treeHistoryViewer.setInput(resource);
		treeHistoryViewer.resetFilters();
		treeHistoryViewer.expandToLevel(2);
		
	    MenuManager menuMgr = new MenuManager();
	    Menu menu = menuMgr.createContextMenu(treeHistoryViewer.getTree());
	    menuMgr.addMenuListener(new IMenuListener() {
	      public void menuAboutToShow(IMenuManager menuMgr) {
	        fillTreeMenu(menuMgr);
	      }
	    });
	    menuMgr.setRemoveAllWhenShown(true);
	    treeHistoryViewer.getTree().setMenu(menu);
		
        Composite commentGroup = new Composite(sashForm, SWT.NULL);
        commentGroup.setLayout(new GridLayout());
        commentGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		textViewer = new TextViewer(commentGroup, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.READ_ONLY);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = COMMENT_HEIGHT_HINT;
		data.widthHint = WIDTH_HINT;
		textViewer.getControl().setLayoutData(data);
		
        Composite pathGroup = new Composite(sashForm, SWT.NULL);
        pathGroup.setLayout(new GridLayout());
        pathGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
        int mode = store.getInt(ISVNUIConstants.PREF_AFFECTED_PATHS_MODE);
        IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
            public Object[] getElements(Object inputElement) {
                if ((inputElement == null) || (!(inputElement instanceof ILogEntry))) {
                    return null;
                }
                ILogEntry logEntry = (ILogEntry)inputElement;
                  return logEntry.getLogEntryChangePaths();
              }
  
              public void dispose() {
              }
  
              public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
              }        	
        };
        
        switch(mode) {
	        case ISVNUIConstants.MODE_COMPRESSED:
	          changePathsViewer = new ChangePathsTreeViewer(pathGroup, historyPage);
	          break;
	        case ISVNUIConstants.MODE_FLAT2:  
	          changePathsViewer = new ChangePathsFlatViewer(pathGroup, historyPage);
	          break;
	        default:
	          changePathsViewer = new ChangePathsTableProvider(pathGroup, contentProvider);
	          break;
        }
		
		
		try {
			int[] weights = new int[3];
			weights[0] = settings.getInt("ShowRevisionsDialog.weights.0"); //$NON-NLS-1$
			weights[1] = settings.getInt("ShowRevisionsDialog.weights.1"); //$NON-NLS-1$
			weights[2] = settings.getInt("ShowRevisionsDialog.weights.2"); //$NON-NLS-1$
			sashForm.setWeights(weights);
		} catch (Exception e) {}
		
		if (selectFirst && treeHistoryViewer.getTree().getItemCount() > 0) {
			TreeItem item = treeHistoryViewer.getTree().getItem(0);
			treeHistoryViewer.getTree().setSelection(item);
			
// Method not available in 3.3
//			treeHistoryViewer.getTree().select(item);
			
			treeHistoryViewer.setSelection(treeHistoryViewer.getSelection());
			changePathsViewer.refresh();
			if (changePathsViewer instanceof ChangePathsTreeViewer) ((ChangePathsTreeViewer)changePathsViewer).expandAll();
		}
		
		return composite;
	}
	
	private void fillTreeMenu(IMenuManager manager) {
		ISelection sel = treeHistoryViewer.getSelection();
	    if( !sel.isEmpty()) {
	      if(sel instanceof IStructuredSelection) {
	        if(((IStructuredSelection) sel).size() == 1) {
	          manager.add(getShowDifferencesAsUnifiedDiffAction());
	        }
	      }
	    }
	  }	

	    // get differences as unified diff action (context menu)
	    private IAction getShowDifferencesAsUnifiedDiffAction() {
	      if(showDifferencesAsUnifiedDiffAction == null) {
	        showDifferencesAsUnifiedDiffAction = new Action(
	            Policy.bind("HistoryView.showDifferences"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MENU_DIFF)) { //$NON-NLS-1$
	          public void run() {
	            ISelection selection = treeHistoryViewer.getSelection();
	            if( !(selection instanceof IStructuredSelection))
	              return;
	            IStructuredSelection ss = (IStructuredSelection)selection;
	            ILogEntry currentSelection = (ILogEntry)ss.getFirstElement();
	            FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
	            dialog.setText("Select Unified Diff Output File");
	            dialog.setFileName("revision" + currentSelection.getRevision().getNumber() + ".diff"); //$NON-NLS-1$
	            String outFile = dialog.open();
	            if(outFile != null) {
	              final SVNUrl url = currentSelection.getResource().getUrl();
	              final SVNRevision oldUrlRevision = new SVNRevision.Number(currentSelection.getRevision().getNumber() - 1);
	              final SVNRevision newUrlRevision = currentSelection.getRevision();
	              final File file = new File(outFile);
	              if(file.exists()) {
	                if( !MessageDialog.openQuestion(getShell(), Policy.bind("HistoryView.showDifferences"), Policy
	                    .bind("HistoryView.overwriteOutfile", file.getName())))
	                  return;
	              }
	              BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
	                public void run() {
	                  ISVNClientAdapter client = null;
	                  try {
	                    client = SVNProviderPlugin.getPlugin().getSVNClientManager().getSVNClient();
	                    client.diff(url, oldUrlRevision, newUrlRevision, file, true);
	                  } catch(Exception e) {
	                    MessageDialog.openError(getShell(), Policy.bind("HistoryView.showDifferences"), e
	                        .getMessage());
	                  } finally {
	                	  SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(client);
	                  }
	                }
	              });
	            }
	          }
	        };
	      }
	      return showDifferencesAsUnifiedDiffAction;
	    }	    
	    
    protected void cancelPressed() {
        saveLocation();
        super.cancelPressed();
    }
 
    protected void okPressed() {
        saveLocation();
        super.okPressed();
    }
    
	public void setSelectFirst(boolean selectFirst) {
		this.selectFirst = selectFirst;
	}

	public void setTitle(String title) {
		this.title = title;
	}
    
	protected Point getInitialLocation(Point initialSize) {
	    try {
	        int x = settings.getInt("ShowRevisionsDialog.location.x"); //$NON-NLS-1$
	        int y = settings.getInt("ShowRevisionsDialog.location.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
        return super.getInitialLocation(initialSize);
    }
    
    protected Point getInitialSize() {
	    try {
	        int x = settings.getInt("ShowRevisionsDialog.size.x"); //$NON-NLS-1$
	        int y = settings.getInt("ShowRevisionsDialog.size.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
        return new Point(400, 400);
    }
    
    private void saveLocation() {
    	settings.put("ShowRevisionsDialog.width_revision", revisionColumn.getWidth()); //$NON-NLS-1$
    	settings.put("ShowRevisionsDialog.width_date", dateColumn.getWidth()); //$NON-NLS-1$
    	settings.put("ShowRevisionsDialog.width_author", authorColumn.getWidth()); //$NON-NLS-1$
    	settings.put("ShowRevisionsDialog.width_comment", commentColumn.getWidth()); //$NON-NLS-1$
    	if (tagsColumn != null) settings.put("ShowRevisionsDialog.width_tags", tagsColumn.getWidth()); //$NON-NLS-1$
        int x = getShell().getLocation().x;
        int y = getShell().getLocation().y;
        settings.put("ShowRevisionsDialog.location.x", x); //$NON-NLS-1$
        settings.put("ShowRevisionsDialog.location.y", y); //$NON-NLS-1$
        x = getShell().getSize().x;
        y = getShell().getSize().y;
        settings.put("ShowRevisionsDialog.size.x", x); //$NON-NLS-1$
        settings.put("ShowRevisionsDialog.size.y", y); //$NON-NLS-1$ 
        int[] weights = sashForm.getWeights();
        for (int i = 0; i < weights.length; i++) 
        	settings.put("ShowRevisionsDialog.weights." + i, weights[i]); //$NON-NLS-1$ 
    }    

}
