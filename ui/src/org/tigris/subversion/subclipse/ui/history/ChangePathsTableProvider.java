/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.history;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntryChangePath;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * This class provides the table and it's required components for a change path
 * This is used from HistoryView
 */
public class ChangePathsTableProvider extends TableViewer {
    ILogEntry currentLogEntry;
    Font currentPathFont;
        
    public ChangePathsTableProvider(Composite parent, IContentProvider contentProvider) {
      super(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
      
      TableLayout layout = new TableLayout();
      GridData data = new GridData(GridData.FILL_BOTH);
      
      Table table = (Table) getControl();
      table.setHeaderVisible(true);
      table.setLinesVisible(true);
      table.setLayoutData(data);    
      table.setLayout(layout);
      table.addDisposeListener(new DisposeListener() {
        public void widgetDisposed(DisposeEvent e) {
          if(currentPathFont != null) {
            currentPathFont.dispose();
          }
        }
      });
      
      createColumns(table, layout);
      
      setLabelProvider(new ChangePathLabelProvider());
      setContentProvider(contentProvider);
      
      ChangePathsSorter sorter = new ChangePathsSorter(COL_PATH);
      setSorter(sorter);
      table.setSortDirection(SWT.UP);
    }
    /**
     * Constructor for HistoryTableProvider.
     */
    public ChangePathsTableProvider(Composite parent, SVNHistoryPage page) {
      this(parent, new ChangePathsTableContentProvider(page));
    }

    protected void inputChanged(Object input, Object oldInput) {
        super.inputChanged(input, oldInput);
        this.currentLogEntry = (ILogEntry) input;
    }

    /**
     * Creates the columns for the history table.
     */
    private void createColumns(Table table, TableLayout layout) {
    	SelectionListener headerListener = getColumnListener();
        // action
        TableColumn col = new TableColumn(table, SWT.NONE);
        col.setResizable(true);
        col.setText(Policy.bind("ChangePathsTableProvider.action")); //$NON-NLS-1$
        col.addSelectionListener(headerListener);
        layout.addColumnData(new ColumnWeightData(10, true));
    
        // path
        col = new TableColumn(table, SWT.NONE);
        col.setResizable(true);
        col.setText(Policy.bind("ChangePathsTableProvider.path")); //$NON-NLS-1$
        col.addSelectionListener(headerListener);
        layout.addColumnData(new ColumnWeightData(45, true));
        table.setSortColumn(col);
    
        // description
        col = new TableColumn(table, SWT.NONE);
        col.setResizable(true);
        col.setText(Policy.bind("ChangePathsTableProvider.description")); //$NON-NLS-1$
        col.addSelectionListener(headerListener);          
        layout.addColumnData(new ColumnWeightData(50, true));
    }
    
	/**
	 * Adds the listener that sets the sorter.
	 */
	private SelectionListener getColumnListener() {
		/**
	 	 * This class handles selections of the column headers.
		 * Selection of the column header will cause resorting
		 * of the shown paths using that column's sorter.
		 * Repeated selection of the header will toggle
		 * sorting order (ascending versus descending).
		 */
		return new SelectionAdapter() {
			/**
			 * Handles the case of user selecting the
			 * header area.
			 * <p>If the column has not been selected previously,
			 * it will set the sorter of that column to be
			 * the current sorter. Repeated
			 * presses on the same column header will
			 * toggle sorting order (ascending/descending).
			 */
			public void widgetSelected(SelectionEvent e) {
				// column selected - need to sort
				int column = getTable().indexOf((TableColumn) e.widget);
				ChangePathsSorter oldSorter = (ChangePathsSorter)getSorter();
				if (oldSorter != null && column == oldSorter.getColumnNumber()) {
					oldSorter.setReversed(!oldSorter.isReversed());
					refresh();
				} else {
					setSorter(new ChangePathsSorter(column));
				}
				getTable().setSortColumn((TableColumn)e.widget);
				if (getTable().getSortDirection() == SWT.UP)
					getTable().setSortDirection(SWT.DOWN);
				else
					getTable().setSortDirection(SWT.UP);
			}
		};
	}
    
    //column constants
    private static final int COL_ACTION = 0;
    private static final int COL_PATH = 1;
    private static final int COL_DESCRIPTION = 2;

    /**
     * The label provider.
     */
    class ChangePathLabelProvider extends LabelProvider implements ITableLabelProvider, IFontProvider, IColorProvider {
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }
        public String getColumnText(Object element, int columnIndex) {
            LogEntryChangePath changePath = (LogEntryChangePath)element;
            if (changePath == null) return ""; //$NON-NLS-1$
            switch (columnIndex) {
                case COL_ACTION:
                    return ""+changePath.getAction(); //$NON-NLS-1$
                case COL_PATH:
                	return changePath.getPath();
                case COL_DESCRIPTION:
                    if (changePath.getCopySrcPath() != null) {
                    	return Policy.bind("ChangePathsTableProvider.copiedfrom",  //$NON-NLS-1$
                                changePath.getCopySrcPath(),
                                changePath.getCopySrcRevision().toString());
                    }
                    return ""; //$NON-NLS-1$
            }
            return ""; //$NON-NLS-1$
        }
        
        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
         */
        public Font getFont(Object element) {
            if(currentLogEntry==null || element==null) {
                return null;
            }
            
            SVNUrl url = ((LogEntryChangePath)element).getUrl();
            
            ISVNRemoteResource remoteResource = currentLogEntry.getRemoteResource();
            if (remoteResource == null) {
                return null;
            }
            
            SVNUrl currentUrl = remoteResource.getUrl();
            if (currentUrl == null || !currentUrl.equals(url)) {
                return null;
            }

            if (currentPathFont == null) {
                Font defaultFont = JFaceResources.getDefaultFont();
                FontData[] data = defaultFont.getFontData();
                for (int i = 0; i < data.length; i++) {
                    data[i].setStyle(SWT.BOLD);
                }               
                currentPathFont = new Font(getControl().getDisplay(), data);
            }
            return currentPathFont;
        }
        
    	public Color getBackground(Object element) {
    		return null;
    	}

		public Color getForeground(Object element) {
			if (currentLogEntry == null) {
				return null;
			}
			ISVNResource resource = currentLogEntry.getResource();
			if (resource == null) return null;
			boolean isPartOfSelection = false;
			if (element instanceof HistoryFolder) {
				HistoryFolder historyFolder = (HistoryFolder)element;				
				isPartOfSelection = (resource.getRepository().getUrl().toString() + historyFolder.getPath()).startsWith(currentLogEntry.getResource().getUrl().toString());
			}
			if (element instanceof LogEntryChangePath) {
				LogEntryChangePath logEntryChangePath = (LogEntryChangePath)element;
				isPartOfSelection = (resource.getRepository().getUrl().toString() + logEntryChangePath.getPath()).startsWith(currentLogEntry.getResource().getUrl().toString());
			}
			if (!isPartOfSelection) return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
			return null;
		}
        
    }

    
    static final LogEntryChangePath[] EMPTY_CHANGE_PATHS = new LogEntryChangePath[ 0];

    static class ChangePathsTableContentProvider implements IStructuredContentProvider {

      private final SVNHistoryPage page;

      ChangePathsTableContentProvider(SVNHistoryPage page) {
        this.page = page;
      }

      public Object[] getElements(Object inputElement) {
        if( !this.page.isShowChangePaths() || !(inputElement instanceof ILogEntry)) {
          return EMPTY_CHANGE_PATHS;
        }

        ILogEntry logEntry = (ILogEntry) inputElement;
        if(SVNProviderPlugin.getPlugin().getSVNClientManager().isFetchChangePathOnDemand()) {
          if(this.page.currentLogEntryChangePath != null) {
            return this.page.currentLogEntryChangePath;
          }
          this.page.scheduleFetchChangePathJob(logEntry);
          return EMPTY_CHANGE_PATHS;
        }

        return logEntry.getLogEntryChangePaths();
      }

      public void dispose() {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        this.page.currentLogEntryChangePath = null;
      }

    }
    
	/**
	 * The change paths sorter
	 */
	class ChangePathsSorter extends ViewerSorter {
		private boolean reversed = false;
		private int columnNumber;
		
		// column headings:	"Revision" "Tags" "Date" "Author" "Comment"
		private int[][] SORT_ORDERS_BY_COLUMN = {
			{COL_ACTION, COL_PATH, COL_DESCRIPTION },	/* action */
			{COL_PATH, COL_ACTION, COL_DESCRIPTION },	/* path */ 
			{COL_DESCRIPTION, COL_ACTION, COL_PATH}   /* description */
		};
		
		/**
		 * The constructor.
		 */
		public ChangePathsSorter(int columnNumber) {
			this.columnNumber = columnNumber;
		}
		/**
		 * Compares two changed paths, sorting first by the main column of this sorter,
		 * then by subsequent columns, depending on the column sort order.
		 */
		public int compare(Viewer viewer, Object o1, Object o2) {
            LogEntryChangePath p1 = (LogEntryChangePath)o1;
            LogEntryChangePath p2 = (LogEntryChangePath)o2;
			int result = 0;
			if (p1 == null || p2 == null) {
				result = super.compare(viewer, o1, o2);
			} else {
				int[] columnSortOrder = SORT_ORDERS_BY_COLUMN[columnNumber];
				for (int i = 0; i < columnSortOrder.length; ++i) {
					result = compareColumnValue(columnSortOrder[i], p1, p2);
					if (result != 0)
						break;
				}
			}
			if (reversed)
				result = -result;
			return result;
		}
		
		/**
		 * Compares two markers, based only on the value of the specified column.
		 */
		int compareColumnValue(int columnNumber, LogEntryChangePath p1, LogEntryChangePath p2) {
			switch (columnNumber) {
				case COL_ACTION: /* action */
                    return getCollator().compare("" + p1.getAction(), "" + p2.getAction());
				case COL_PATH: /* path */
					return getCollator().compare(p1.getPath(), p2.getPath());
				case COL_DESCRIPTION: /* description */
					String d1;
					String d2;
                    if (p1.getCopySrcPath() != null) {
                    	d1 = Policy.bind("ChangePathsTableProvider.copiedfrom",  //$NON-NLS-1$
                                p1.getCopySrcPath(),
                                p1.getCopySrcRevision().toString());
                    } else {
                    	d1 = ""; //$NON-NLS-1$
                    }
                    if (p2.getCopySrcPath() != null) {
                    	d2 = Policy.bind("ChangePathsTableProvider.copiedfrom",  //$NON-NLS-1$
                                p2.getCopySrcPath(),
                                p2.getCopySrcRevision().toString());
                    } else {
                    	d2 = ""; //$NON-NLS-1$
                    }
					return getCollator().compare(d1, d2);
				default:
					return 0;
			}
		}
		/**
		 * Returns the number of the column by which this is sorting.
		 */
		public int getColumnNumber() {
			return columnNumber;
		}
		/**
		 * Returns true for descending, or false
		 * for ascending sorting order.
		 */
		public boolean isReversed() {
			return reversed;
		}
		/**
		 * Sets the sorting order.
		 */
		public void setReversed(boolean newReversed) {
			reversed = newReversed;
		}
	}
	
	public void setCurrentLogEntry(ILogEntry currentLogEntry) {
		this.currentLogEntry = currentLogEntry;
	}  	
    
}
