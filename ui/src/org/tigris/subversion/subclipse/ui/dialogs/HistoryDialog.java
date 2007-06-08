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

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.commands.GetLogsCommand;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntry;
import org.tigris.subversion.subclipse.core.history.AliasManager;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.history.ChangePathsTableProvider;
import org.tigris.subversion.subclipse.ui.history.HistoryTableProvider;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class HistoryDialog extends TrayDialog {
    private IResource resource;
    private ISVNRemoteResource remoteResource;
    private SashForm sashForm;
	private HistoryTableProvider historyTableProvider;
	private ChangePathsTableProvider changePathsTableProvider;
	private TableViewer tableHistoryViewer;
	private TextViewer textViewer;
	private Button stopOnCopyButton;
	private Button getAllButton;
	private Button getNextButton;
	private AliasManager tagManager;
	private ILogEntry[] entries;
	private IDialogSettings settings;
	private ILogEntry[] selectedEntries;
	private IPreferenceStore store;
	private SVNRevision revisionStart = SVNRevision.HEAD;
	private boolean getNextEnabled = true;
	
	private static final int WIDTH_HINT = 500;
	private final static int LOG_HEIGHT_HINT = 200;
	private final static int COMMENT_HEIGHT_HINT = 100;

    public HistoryDialog(Shell parentShell, IResource resource) {
        super(parentShell);
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);
        this.resource = resource;
		settings = SVNUIPlugin.getPlugin().getDialogSettings();
		store = SVNUIPlugin.getPlugin().getPreferenceStore();
    }
    
    public HistoryDialog(Shell parentShell, ISVNRemoteResource remoteResource) {
        super(parentShell);
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);
        this.remoteResource = remoteResource;
		settings = SVNUIPlugin.getPlugin().getDialogSettings();
		store = SVNUIPlugin.getPlugin().getPreferenceStore();
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
		
        sashForm = new SashForm(composite, SWT.VERTICAL);
        sashForm.setLayout(new GridLayout());
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite historyGroup = new Composite(sashForm, SWT.NULL);
        historyGroup.setLayout(new GridLayout());
        historyGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		historyTableProvider = new HistoryTableProvider();
		historyTableProvider.setRemoteResource(remoteResource);
		tableHistoryViewer = historyTableProvider.createTable(historyGroup);
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
                    changePathsTableProvider.setInput(null);
					return;
				}
				IStructuredSelection ss = (IStructuredSelection)selection;
				if (ss.size() != 1) {
					textViewer.setDocument(new Document("")); //$NON-NLS-1$
                    changePathsTableProvider.setInput(null);
					return;
				}
				LogEntry entry = (LogEntry)ss.getFirstElement();
				textViewer.setDocument(new Document(entry.getComment()));
                changePathsTableProvider.setInput(entry);                     
            }		    
		});
		tableHistoryViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                okPressed();
            }	    
		});
		
		tableHistoryViewer.setInput(remoteResource);
		tableHistoryViewer.resetFilters();
		
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
		
		changePathsTableProvider = new ChangePathsTableProvider(pathGroup, 
            new IStructuredContentProvider() {
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
		
		stopOnCopyButton = new Button(composite, SWT.CHECK);
		stopOnCopyButton.setText(Policy.bind("HistoryView.stopOnCopy"));
		stopOnCopyButton.setSelection(store.getBoolean(ISVNUIConstants.PREF_STOP_ON_COPY));
		stopOnCopyButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				store.setValue(ISVNUIConstants.PREF_STOP_ON_COPY, stopOnCopyButton.getSelection());				
				revisionStart = SVNRevision.HEAD;
				getLogEntries();
				tableHistoryViewer.refresh();
			}
		});
		
		try {
			int[] weights = new int[3];
			weights[0] = settings.getInt("HistoryDialog.weights.0"); //$NON-NLS-1$
			weights[1] = settings.getInt("HistoryDialog.weights.1"); //$NON-NLS-1$
			weights[2] = settings.getInt("HistoryDialog.weights.2"); //$NON-NLS-1$
			sashForm.setWeights(weights);
		} catch (Exception e) {}
		
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.HISTORY_DIALOG);	

		return composite;
	}
	
	private void getLogEntries() {
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
		            if (remoteResource != null) {
		            	if (SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_SHOW_TAGS_IN_REMOTE))
		            		tagManager = new AliasManager(remoteResource.getUrl());
						SVNRevision pegRevision = remoteResource.getRevision();
						SVNRevision revisionEnd = new SVNRevision.Number(0);
						boolean stopOnCopy = store.getBoolean(ISVNUIConstants.PREF_STOP_ON_COPY);
						int entriesToFetch = store.getInt(ISVNUIConstants.PREF_LOG_ENTRIES_TO_FETCH);
						long limit = entriesToFetch;
						entries = getLogEntries(remoteResource, pegRevision, revisionStart, revisionEnd, stopOnCopy, limit + 1, tagManager);
						long entriesLength = entries.length;
						if (entriesLength > limit) {
							ILogEntry[] fetchedEntries = new ILogEntry[entries.length - 1];
							for (int i = 0; i < entries.length - 1; i++)
								fetchedEntries[i] = entries[i];
							entries = fetchedEntries;
						} else getNextEnabled = false;
						if (entries.length > 0) {
							ILogEntry lastEntry = entries[entries.length - 1];
							long lastEntryNumber = lastEntry.getRevision().getNumber();
							revisionStart = new SVNRevision.Number(lastEntryNumber - 1);
						}		
		            }
				} catch (TeamException e) {
					SVNUIPlugin.openError(Display.getCurrent().getActiveShell(), null, null, e);
				}	
	        }       
	   });
	}
	
	private void getNextLogEntries() {
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
			            if (remoteResource != null) {
							SVNRevision pegRevision = remoteResource.getRevision();
							SVNRevision revisionEnd = new SVNRevision.Number(0);
							boolean stopOnCopy = store.getBoolean(ISVNUIConstants.PREF_STOP_ON_COPY);
							int entriesToFetch = store.getInt(ISVNUIConstants.PREF_LOG_ENTRIES_TO_FETCH);
							long limit = entriesToFetch;
							ILogEntry[] nextEntries = getLogEntries(remoteResource, pegRevision, revisionStart, revisionEnd, stopOnCopy, limit + 1, tagManager);
							long entriesLength = nextEntries.length;
							if (entriesLength > limit) {
								ILogEntry[] fetchedEntries = new ILogEntry[nextEntries.length - 1];
								for (int i = 0; i < nextEntries.length - 1; i++)
									fetchedEntries[i] = nextEntries[i];
								getNextButton.setEnabled(true);
							} else getNextButton.setEnabled(false);
							ArrayList entryArray = new ArrayList();
							if (entries == null) entries = new ILogEntry[0];
							for (int i = 0; i < entries.length; i++) entryArray.add(entries[i]);
							for (int i = 0; i < nextEntries.length; i++) entryArray.add(nextEntries[i]);
							entries = new ILogEntry[entryArray.size()];
							entryArray.toArray(entries);							
							if (entries.length > 0) {
								ILogEntry lastEntry = entries[entries.length - 1];
								long lastEntryNumber = lastEntry.getRevision().getNumber();
								revisionStart = new SVNRevision.Number(lastEntryNumber - 1);
							}		
			            }
					} catch (TeamException e) {
						SVNUIPlugin.openError(Display.getCurrent().getActiveShell(), null, null, e);
					}	
		        }       
		   });
			ISelection selection = tableHistoryViewer.getSelection();
            tableHistoryViewer.refresh();
            tableHistoryViewer.setSelection(selection);
	}
	
	private void getAllLogEntries() {
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
			            if (remoteResource != null) {
			            	if (SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_SHOW_TAGS_IN_REMOTE))
			            		tagManager = new AliasManager(remoteResource.getUrl());
							SVNRevision pegRevision = remoteResource.getRevision();
							SVNRevision revisionEnd = new SVNRevision.Number(0);
							revisionStart = SVNRevision.HEAD;
							boolean stopOnCopy = store.getBoolean(ISVNUIConstants.PREF_STOP_ON_COPY);
							long limit = 0;
							entries = getLogEntries(remoteResource, pegRevision, revisionStart, revisionEnd, stopOnCopy, limit, tagManager);
							getNextButton.setEnabled(false);	
			            }
					} catch (TeamException e) {
						SVNUIPlugin.openError(Display.getCurrent().getActiveShell(), null, null, e);
					}	
		        }       
		   });
		   tableHistoryViewer.refresh();
		}

	protected ILogEntry[] getLogEntries(ISVNRemoteResource remoteResource, SVNRevision pegRevision, SVNRevision revisionStart, SVNRevision revisionEnd, boolean stopOnCopy, long limit, AliasManager tagManager) throws TeamException
	{
		GetLogsCommand logCmd = new GetLogsCommand(remoteResource, pegRevision, revisionStart, revisionEnd, stopOnCopy, limit, tagManager, false);
		logCmd.run(null);
		return logCmd.getLogEntries(); 					
	}
	
    protected void cancelPressed() {
        saveLocation();
        super.cancelPressed();
    }
 
    protected void okPressed() {
        saveLocation();
        store.setValue(ISVNUIConstants.PREF_STOP_ON_COPY, stopOnCopyButton.getSelection());
        IStructuredSelection selection = (IStructuredSelection)tableHistoryViewer.getSelection();
        selectedEntries = new ILogEntry[selection.size()];
        Iterator iter = selection.iterator();
        int i = 0;
        while (iter.hasNext()) selectedEntries[i++] = (ILogEntry)iter.next();
        super.okPressed();
    }
       
    protected void createButtonsForButtonBar(Composite parent) {
    	getAllButton = createButton(parent, 2, Policy.bind("HistoryView.getAll"), false); //$NON-NLS-1$
		getAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getAllLogEntries();
			}
		});    	
    	int entriesToFetch = store.getInt(ISVNUIConstants.PREF_LOG_ENTRIES_TO_FETCH);
    	if (entriesToFetch > 0) {
    		getNextButton = createButton(parent, 3, Policy.bind("HistoryView.getNext") + " " + entriesToFetch, false); //$NON-NLS-1$
    		getNextButton.setEnabled(getNextEnabled);
    		getNextButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					getNextLogEntries();
				}
    		});
    	}
    	super.createButtonsForButtonBar(parent);
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
        int[] weights = sashForm.getWeights();
        for (int i = 0; i < weights.length; i++) 
        	settings.put("HistoryDialog.weights." + i, weights[i]); //$NON-NLS-1$ 
    }

    public ILogEntry[] getSelectedLogEntries() {
        return selectedEntries;
    }
}
