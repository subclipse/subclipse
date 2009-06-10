/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.history;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IFontProvider;
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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.history.AliasManager;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.settings.ProjectProperties;
import org.tigris.subversion.subclipse.ui.util.LinkList;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * This class provides the table and it's required components for a file's revision
 * history
 * This is used from HistoryView and SVNCompareRevisionsInput
 */
public class HistoryTableProvider {

	private ISVNRemoteResource currentRemoteResource;
	private SVNRevision.Number currentRevision;
	private TableViewer viewer;
	private Font currentRevisionFont;
	
	private boolean includeMergeRevisions = true;
	private boolean includeTags = true;
	private boolean includeBugs = false;
	
	private int style;
	
	private IDialogSettings settings = SVNUIPlugin.getPlugin().getDialogSettings();
	private String id;
		
	/**
	 * Constructor for HistoryTableProvider.
	 */
	public HistoryTableProvider() {
		this(SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION, null);
	}
	
	public HistoryTableProvider(int style, String id) {
		super();
		this.style = style;
		this.id = id;
	}

	ProjectProperties projectProperties = null;

	//column constants
	private final static int COL_REVISION = 0;
	private final static int COL_MERGED_REVISIONS = 1;
	private final static int COL_TAGS = 2;
	private final static int COL_DATE = 3;
	private final static int COL_AUTHOR = 4;
	private final static int COL_COMMENT = 5;
	private final static int COL_BUGS = 6;

	/**
	 * The history label provider.
	 */
	class HistoryLabelProvider extends LabelProvider implements ITableLabelProvider, IFontProvider {
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
		public String getColumnText(Object element, int columnIndex) {
            ILogEntry entry = adaptToLogEntry(element);
			if (entry == null) return ""; //$NON-NLS-1$
			int index = columnIndex;
			if (columnIndex > 0 && !includeMergeRevisions) index++;
			if (index > 1 && !includeTags) index++;
			switch (index) {
				case COL_REVISION:
					String revision = entry.getRevision().toString();
					if (currentRemoteResource != null && entry.getRevision().equals( currentRemoteResource.getLastChangedRevision())) {
						revision = Policy.bind("currentRevision", revision); //$NON-NLS-1$
					}
					return revision;
				case COL_MERGED_REVISIONS:
					return entry.getMergedRevisionsAsString();					
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
					else return comment.replaceAll("\r", " ").replaceAll("\n", " "); //$NON-NLS-1$ //$NON-NLS-2$
//					int rIndex = comment.indexOf("\r");  //$NON-NLS-1$
//					int nIndex = comment.indexOf("\n");	 //$NON-NLS-1$
//					if( (rIndex == -1) && (nIndex == -1) )
//						return comment;
//						
//					if( (rIndex == 0) || (nIndex == 0) )
//						return Policy.bind("HistoryView.[...]_4"); //$NON-NLS-1$
//						
//					if(rIndex != -1)
//						return Policy.bind("SVNCompareRevisionsInput.truncate", comment.substring(0, rIndex)); //$NON-NLS-1$
//					else
//						return Policy.bind("SVNCompareRevisionsInput.truncate", comment.substring(0, nIndex)); //$NON-NLS-1$
				case COL_BUGS:
					return getBugstringFromComment( entry.getComment() );
			}
			return ""; //$NON-NLS-1$
		}
		
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
		 */
		public Font getFont(Object element) {
			ILogEntry entry = adaptToLogEntry(element);
			if (entry == null)
				return null;
			SVNRevision revision = entry.getRevision();
			SVNRevision currentRevision = getCurrentRevision();
			if (currentRevision != null && currentRevision.equals(revision)) {
				if (currentRevisionFont == null) {
					Font defaultFont = JFaceResources.getDefaultFont();
					FontData[] data = defaultFont.getFontData();
					for (int i = 0; i < data.length; i++) {
						data[i].setStyle(SWT.BOLD);
					}               
					currentRevisionFont = new Font(viewer.getTable().getDisplay(), data);
				}
				return currentRevisionFont;
			}
			return null;
		}
		
	}
	
	private String getBugstringFromComment( String comment ) {
		String display = "";
		if( projectProperties != null ) {
			LinkList ll =  projectProperties.getLinkList( comment );
			String[] texts =  ll.getTexts();
			for( int i = 0; texts != null && texts.length > i ; i++ ) {
				display += texts[i];
				display += " ";
			}
		}
		return display;
	}

	/**
	 * The history sorter
	 */
	class HistorySorter extends ViewerSorter {
		private boolean reversed = false;
		private int columnNumber;
		
//		private VersionCollator versionCollator = new VersionCollator();
		
		// column headings:	"Revision" "Merged Revisions" "Tags" "Date" "Author" "Comment" "Bug-ID"
		private int[][] SORT_ORDERS_BY_COLUMN = {
			{COL_REVISION, COL_MERGED_REVISIONS, COL_TAGS, COL_DATE, COL_AUTHOR, COL_COMMENT },	/* revision */
			{COL_MERGED_REVISIONS, COL_REVISION, COL_TAGS, COL_DATE, COL_AUTHOR, COL_COMMENT },	/* merged revisions */
			{COL_TAGS, COL_REVISION, COL_MERGED_REVISIONS, COL_DATE, COL_AUTHOR, COL_COMMENT },	/* tags */ 
			{COL_DATE, COL_REVISION, COL_MERGED_REVISIONS, COL_TAGS, COL_AUTHOR, COL_COMMENT},	/* date */
			{COL_AUTHOR, COL_REVISION, COL_MERGED_REVISIONS, COL_TAGS, COL_DATE, COL_COMMENT},	/* author */
			{COL_COMMENT, COL_REVISION, COL_MERGED_REVISIONS, COL_TAGS, COL_DATE, COL_AUTHOR},   /* comment */
			{COL_BUGS, COL_REVISION, COL_MERGED_REVISIONS, COL_TAGS, COL_DATE, COL_COMMENT}   /* Bug-ID */
		};
		
		/**
		 * The constructor.
		 */
		public HistorySorter(int columnNumber) {
			this.columnNumber = columnNumber;
		}
		/**
		 * Compares two log entries, sorting first by the main column of this sorter,
		 * then by subsequent columns, depending on the column sort order.
		 */
		public int compare(Viewer viewer, Object o1, Object o2) {
            ILogEntry e1 = adaptToLogEntry(o1);
            ILogEntry e2 = adaptToLogEntry(o2);
			int result = 0;
			if (e1 == null || e2 == null) {
				result = super.compare(viewer, o1, o2);
			} else {
				int[] columnSortOrder = SORT_ORDERS_BY_COLUMN[columnNumber];;
				for (int i = 0; i < columnSortOrder.length; ++i) {
					result = compareColumnValue(columnSortOrder[i], e1, e2);
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
		int compareColumnValue(int columnNumber, ILogEntry e1, ILogEntry e2) {
			int column = columnNumber;
			if (column > 0 && !includeMergeRevisions) column++;
			if (column > 1 && !includeTags) column++;
			switch (column) {
				case COL_REVISION: /* revision */
                    return (e2.getRevision().getNumber()<e1.getRevision().getNumber() ? -1 : (e2.getRevision()==e1.getRevision() ? 0 : 1));
				case COL_MERGED_REVISIONS: /* merged revisions */
					return e1.getMergedRevisionsAsString().compareTo(e2.getMergedRevisionsAsString());
//					return getCollator().compare(e1.getMergedRevisionsAsString(), e2.getMergedRevisionsAsString());
				case COL_TAGS: /* tags */
					String tags1 = AliasManager.getAliasesAsString(e1.getTags());
					String tags2 = AliasManager.getAliasesAsString(e2.getTags());
					return tags1.compareTo(tags2);
//					return getCollator().compare(tags1, tags2);
				case COL_DATE: /* date */
					Date date1 = e1.getDate();
					Date date2 = e2.getDate();
					return date1.compareTo(date2);
				case COL_AUTHOR: /* author */
					return e1.getAuthor().compareTo(e2.getAuthor());
//					return getCollator().compare(e1.getAuthor(), e2.getAuthor());
				case COL_COMMENT: /* comment */
					return e1.getComment().compareTo(e2.getComment());
//					return getCollator().compare(e1.getComment(), e2.getComment());
				case COL_BUGS: /* comment */
					if( projectProperties != null ) {
						return getBugstringFromComment(e1.getComment()).compareTo(getBugstringFromComment(e2.getComment()));
					}
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

	protected ILogEntry adaptToLogEntry(Object element) {
		// Get the log entry for the provided object
        ILogEntry entry = null;
		if (element instanceof ILogEntry) {
			entry = (ILogEntry) element;
		} else if (element instanceof IAdaptable) {
			entry = (ILogEntry)((IAdaptable)element).getAdapter(ILogEntry.class);
		}
		return entry;
	}
	
	/**
	 * Create a TableViewer that can be used to display a list of ILogEntry instances.
	 * Ths method provides the labels and sorter but does not provide a content provider
	 * 
	 * @param parent
	 * @return TableViewer
	 */
	public TableViewer createTable(Composite parent) {
		Table table = new Table(parent, style);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.horizontalIndent = 0;
		data.verticalIndent = 0;
		table.setLayoutData(data);
	
		TableLayout layout = new TableLayout();
		table.setLayout(layout);
		
		TableViewer viewer = new TableViewer(table);
		
		createColumns(table, layout, viewer);

		viewer.setLabelProvider(new HistoryLabelProvider());
		
		HistorySorter sorter = new HistorySorter(COL_REVISION);
		viewer.setSorter(sorter);
		table.setSortDirection(SWT.DOWN);
		table.setSortColumn(table.getColumn(0));

        table.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                if(currentRevisionFont != null) {
                    currentRevisionFont.dispose();
                }
            }
        });
        
		this.viewer = viewer;
		return viewer;
	}
	
	/**
	 * Creates the columns for the history table.
	 */
	private void createColumns(Table table, TableLayout layout, TableViewer viewer) {
		DisposeListener disposeListener = new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				TableColumn col = (TableColumn)e.getSource();
				if (col.getWidth() > 0) settings.put("HistoryTableProvider." + id + "." + col.getText(), col.getWidth()); //$NON-NLS-1$ //$NON-NLS-1$
			}			
		};
		
		SelectionListener headerListener = getColumnListener(viewer);
		// revision
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(Policy.bind("HistoryView.revision")); //$NON-NLS-1$
		col.addSelectionListener(headerListener);
		setColumnWidth(layout, disposeListener, col, 10);
		table.setSortColumn(col);

		// merged revisions
		if (includeMergeRevisions) {
			col = new TableColumn(table, SWT.NONE);		
			col.setResizable(true);
			col.setText(Policy.bind("HistoryView.mergedRevisions")); //$NON-NLS-1$
			col.addSelectionListener(headerListener);
			setColumnWidth(layout, disposeListener, col, 30);
		}
		
		// tags
		if (includeTags) {
			col = new TableColumn(table, SWT.NONE);
			col.setResizable(true);
			col.setText(Policy.bind("HistoryView.tags")); //$NON-NLS-1$
			col.addSelectionListener(headerListener);
			setColumnWidth(layout, disposeListener, col, 30);
		}
	
		// creation date
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(Policy.bind("HistoryView.date")); //$NON-NLS-1$
		col.addSelectionListener(headerListener);
		setColumnWidth(layout, disposeListener, col, 25);
	
		// author
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(Policy.bind("HistoryView.author")); //$NON-NLS-1$
		col.addSelectionListener(headerListener);
		setColumnWidth(layout, disposeListener, col, 20);
	
		//comment
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(Policy.bind("HistoryView.comment")); //$NON-NLS-1$
		col.addSelectionListener(headerListener);
		setColumnWidth(layout, disposeListener, col, 50);
		
		//bugs
		if (includeBugs) {
			col = new TableColumn(table, SWT.NONE);
			col.setResizable(true);
			String label = projectProperties.getLabel();
			if (label != null && label.trim().length() > 0)
			{
				label = label.trim();
				// many have i guess labels that ends with : strip that one for the table header.
				if (label.endsWith(":")) label = label.substring(0,label.length()-1);
				col.setText(label);
			}
			else
			{
				col.setText(Policy.bind("HistoryView.bugs")); //$NON-NLS-1$
			}
			col.addSelectionListener(headerListener);
			setColumnWidth(layout, disposeListener, col, 10);
		}
	}

	private void setColumnWidth(TableLayout layout,
			DisposeListener disposeListener, TableColumn col, int defaultWidth) {
		String columnWidth = null;
		if (id != null) columnWidth = settings.get("HistoryTableProvider." + id + "." + col.getText()); //$NON-NLS-1$ //$NON-NLS-1$
		if (columnWidth == null || columnWidth.equals("0")) layout.addColumnData(new ColumnWeightData(defaultWidth, true)); //$NON-NLS-1$
		else layout.addColumnData(new ColumnPixelData(Integer.parseInt(columnWidth), true));
		if (id != null) col.addDisposeListener(disposeListener);
	}

	/**
	 * Adds the listener that sets the sorter.
	 */
	private SelectionListener getColumnListener(final TableViewer tableViewer) {
		/**
	 	 * This class handles selections of the column headers.
		 * Selection of the column header will cause resorting
		 * of the shown tasks using that column's sorter.
		 * Repeated selection of the header will toggle
		 * sorting order (ascending versus descending).
		 */
		return new SelectionAdapter() {
			/**
			 * Handles the case of user selecting the
			 * header area.
			 * <p>If the column has not been selected previously,
			 * it will set the sorter of that column to be
			 * the current tasklist sorter. Repeated
			 * presses on the same column header will
			 * toggle sorting order (ascending/descending).
			 */
			public void widgetSelected(SelectionEvent e) {
				List checkedItems = new ArrayList();
				TableItem[] items = tableViewer.getTable().getItems();
				for (int i = 0; i < items.length; i++) {
					if (items[i].getChecked()) checkedItems.add(items[i].getData());
				}
				
				// column selected - need to sort
				int column = tableViewer.getTable().indexOf((TableColumn) e.widget);
				setSortColumn(tableViewer, column);
				
				items = tableViewer.getTable().getItems();
				for (int i = 0; i < items.length; i++) {
					if (checkedItems.contains(items[i].getData())) 
						items[i].setChecked(true);
				}				
			}
		};
	}
	

	private SVNRevision.Number getRevision(ISVNRemoteResource currentEdition) {
		if (currentEdition == null) return SVNRevision.INVALID_REVISION;
		return currentEdition.getLastChangedRevision();
	}
	
	public void setRemoteResource(ISVNRemoteResource remoteResource) {
		this.currentRemoteResource = remoteResource;
		this.currentRevision = getRevision(remoteResource);
	}
	
    /**
     * get the current revision (ie the lastChangedRevision of the remoteResource) 
     */
	public SVNRevision.Number getCurrentRevision() {
		return currentRevision;
	}
	
    /**
     * get the remote resource from which we want the history 
     * @return
     */
	public ISVNRemoteResource getRemoteResource() {
		return this.currentRemoteResource;
	}

	public void setIncludeMergeRevisions(boolean includeMergeRevisions) {
		this.includeMergeRevisions = includeMergeRevisions;
	}

	public void setIncludeTags(boolean includeTags) {
		this.includeTags = includeTags;
	}

	public boolean isIncludeTags() {
		return includeTags;
	}

	public void setIncludeBugs(boolean includeBugs) {
		this.includeBugs = includeBugs;
	}

	public boolean isIncludeBugs() {
		return this.projectProperties != null;
	}

	public void setProjectProperties(ProjectProperties projectProperties) {
		this.projectProperties = projectProperties;
	}

	public void setSortColumn(final TableViewer tableViewer, int column) {
		HistorySorter oldSorter = (HistorySorter)tableViewer.getSorter();
		if (oldSorter != null && column == oldSorter.getColumnNumber()) {
			oldSorter.setReversed(!oldSorter.isReversed());
			if (oldSorter.isReversed()) tableViewer.getTable().setSortDirection(SWT.DOWN);
			else tableViewer.getTable().setSortDirection(SWT.UP);	
			tableViewer.refresh();
		} else {
			HistorySorter newSorter = new HistorySorter(column);
			if (column == 0) newSorter.setReversed(true);
			tableViewer.setSorter(newSorter);
			if (column == 0)tableViewer.getTable().setSortDirection(SWT.DOWN);
			else tableViewer.getTable().setSortDirection(SWT.UP);
		}
//				tableViewer.getTable().setSortColumn((TableColumn)e.widget);
		tableViewer.getTable().setSortColumn(tableViewer.getTable().getColumn(column));
	}

}
