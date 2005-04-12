package org.tigris.subversion.subclipse.ui.dialogs;

import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntry;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.history.ChangePathsTableProvider;
import org.tigris.subversion.subclipse.ui.history.HistoryTableProvider;

public class HistoryDialog extends Dialog {
    private IResource resource;
    private ISVNRemoteResource remoteResource;
	private HistoryTableProvider historyTableProvider;
	private ChangePathsTableProvider changePathsTableProvider;
	private TableViewer tableHistoryViewer;
    private TableViewer tableChangePathViewer;
	private TextViewer textViewer;
	private ILogEntry[] entries;
	private IDialogSettings settings;
	private ILogEntry[] selectedEntries;
	
	private static final int WIDTH_HINT = 500;
	private final static int LOG_HEIGHT_HINT = 200;
	private final static int COMMENT_HEIGHT_HINT = 100;
	private final static int PATHS_HEIGHT_HINT = 200;

    public HistoryDialog(Shell parentShell, IResource resource) {
        super(parentShell);
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);
        this.resource = resource;
		settings = SVNUIPlugin.getPlugin().getDialogSettings();
    }
    
    public HistoryDialog(Shell parentShell, ISVNRemoteResource remoteResource) {
        super(parentShell);
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);
        this.remoteResource = remoteResource;
		settings = SVNUIPlugin.getPlugin().getDialogSettings();
    }    
    
	protected Control createDialogArea(Composite parent) {
	    getLogEntries();
	    if (resource == null)
	        getShell().setText(Policy.bind("HistoryDialog.title") + " - " + remoteResource.getName()); //$NON-NLS-1$ //$NON-NLS-2$
	    else
	        getShell().setText(Policy.bind("HistoryDialog.title") + " - " + resource.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		
		historyTableProvider = new HistoryTableProvider();
		historyTableProvider.setRemoteResource(remoteResource);
		tableHistoryViewer = historyTableProvider.createTable(composite);
		data = new GridData(GridData.FILL_BOTH);
		data.widthHint = WIDTH_HINT;
		data.heightHint = LOG_HEIGHT_HINT;
		tableHistoryViewer.getTable().setLayoutData(data);
		tableHistoryViewer.setContentProvider(new IStructuredContentProvider() {
            public void dispose() {
            }
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }
            public Object[] getElements(Object inputElement) {
                return entries;
            }	    
		});
		tableHistoryViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
				if (selection == null || !(selection instanceof IStructuredSelection)) {
					textViewer.setDocument(new Document("")); //$NON-NLS-1$
                    changePathsTableProvider.setLogEntry(null);
					return;
				}
				IStructuredSelection ss = (IStructuredSelection)selection;
				if (ss.size() != 1) {
					textViewer.setDocument(new Document("")); //$NON-NLS-1$
                    changePathsTableProvider.setLogEntry(null);
					return;
				}
				LogEntry entry = (LogEntry)ss.getFirstElement();
				textViewer.setDocument(new Document(entry.getComment()));
                changePathsTableProvider.setLogEntry(entry);                     
            }		    
		});
		tableHistoryViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                okPressed();
            }	    
		});
		
		tableHistoryViewer.setInput(remoteResource);
		
		textViewer = new TextViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.READ_ONLY);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = COMMENT_HEIGHT_HINT;
		data.widthHint = WIDTH_HINT;
		textViewer.getControl().setLayoutData(data);
		
		changePathsTableProvider = new ChangePathsTableProvider();
		tableChangePathViewer = changePathsTableProvider.createTable(composite);
        tableChangePathViewer.setContentProvider(new IStructuredContentProvider() {

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
            
        });
        
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = PATHS_HEIGHT_HINT;
		data.widthHint = WIDTH_HINT;
		tableChangePathViewer.getTable().setLayoutData(data);
		
		return composite;
	}
	
	private ILogEntry[] getLogEntries() {
	   BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
	        public void run() {
	            try {
		            if (remoteResource == null) {
		                ISVNLocalResource localResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
						if ( localResource != null
						        && !localResource.getStatus().isAdded()
						        && localResource.getStatus().isManaged() ) {
						    remoteResource = localResource.getBaseResource();
						}
		            }
		            if (remoteResource != null) entries = remoteResource.getLogEntries(null);
				} catch (TeamException e) {
					SVNUIPlugin.openError(Display.getCurrent().getActiveShell(), null, null, e);
				}	
	        }       
	   });
	   if (entries == null) return new ILogEntry[0];
	   return entries;
	}

    protected void cancelPressed() {
        saveLocation();
        super.cancelPressed();
    }
 
    protected void okPressed() {
        saveLocation();
        IStructuredSelection selection = (IStructuredSelection)tableHistoryViewer.getSelection();
        selectedEntries = new ILogEntry[selection.size()];
        Iterator iter = selection.iterator();
        int i = 0;
        while (iter.hasNext()) selectedEntries[i++] = (ILogEntry)iter.next();
        super.okPressed();
    }
    protected Point getInitialLocation(Point initialSize) {
	    try {
	        int x = settings.getInt("HistoryDialog.location.x"); //$NON-NLS-1$
	        int y = settings.getInt("HistoryDialog.location.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
        return super.getInitialLocation(initialSize);
    }
    
    protected Point getInitialSize() {
	    try {
	        int x = settings.getInt("HistoryDialog.size.x"); //$NON-NLS-1$
	        int y = settings.getInt("HistoryDialog.size.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
        return new Point(400, 400);
    }
    
    private void saveLocation() {
        int x = getShell().getLocation().x;
        int y = getShell().getLocation().y;
        settings.put("HistoryDialog.location.x", x); //$NON-NLS-1$
        settings.put("HistoryDialog.location.y", y); //$NON-NLS-1$
        x = getShell().getSize().x;
        y = getShell().getSize().y;
        settings.put("HistoryDialog.size.x", x); //$NON-NLS-1$
        settings.put("HistoryDialog.size.y", y); //$NON-NLS-1$   
    }

    public ILogEntry[] getSelectedLogEntries() {
        return selectedEntries;
    }
}
