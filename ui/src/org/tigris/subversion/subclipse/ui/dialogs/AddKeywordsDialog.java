/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.dialogs;

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.help.WorkbenchHelp;
import org.tigris.subversion.subclipse.core.ISVNFile;
import org.tigris.subversion.subclipse.core.ISVNFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalFile;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNResourceVisitor;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.SVNKeywords;

/**
 * dialog to add keywords on given resources
 * you can use it using :
 *      AddKeywordsDialog dialog = new AddKeywordsDialog(getShell(),getSelectedResources());
 *      if (dialog.open() != AddKeywordsDialog.OK) return;
 *      dialog.updateKeywords(); 
 */
public class AddKeywordsDialog extends Dialog {

	private static final int WIDTH_HINT = 700; 
	private final static int SELECTION_HEIGHT_HINT = 150;
	
    // files on which to apply keywords
	private ISVNLocalFile[] files;
	
	private CheckboxTableViewer listViewer;

    private KeywordItem lastChangedDateKeyword;
    private KeywordItem lastChangedRevisionKeyword;
    private KeywordItem lastChangedByKeyword;
    private KeywordItem headUrlKeyword;
    private KeywordItem idKeyword;
        
    /**
     * the model : describes a keyword and the initial state and wanted state 
     */
    private class KeywordItem {
        public static final int UNKNOWN = 0;
        public static final int CHECKED = 1;
        public static final int UNCHECKED = 2; 
        public static final int GRAYED = 3;    // when some files have the keyword set but we don't want to have it on all files
        
        public String keywordName; // name of the keyword
        public String description; // description of it
        public String sample;      // a sample for the keyword 
        public int initialState;   // the initial state
        public int currentState;   // the wanted state
        
        public KeywordItem(String keywordName,String description,String sample, int initialState) {
            this.keywordName = keywordName;
            this.description = description;
            this.sample = sample;
            this.initialState = initialState;
            this.currentState =  initialState;
        }
    }

    /**
     * Constructor for AddToVersionControlDialog.
     * @param parentShell
     */
    public AddKeywordsDialog(Shell parentShell, IResource[] resources) throws SVNException {
        super(parentShell);
        files = getSvnLocalFiles(resources);
        
        createKeywordItems(KeywordItem.UNKNOWN,KeywordItem.UNKNOWN,KeywordItem.UNKNOWN,KeywordItem.UNKNOWN,KeywordItem.UNKNOWN);
        
        for (int i = 0; i < files.length;i++) {
            SVNKeywords svnKeywords = files[i].getKeywords();

            updateKeywordItemInitialState(lastChangedDateKeyword,svnKeywords.isLastChangedDate());
            updateKeywordItemInitialState(lastChangedRevisionKeyword,svnKeywords.isLastChangedRevision());            
            updateKeywordItemInitialState(lastChangedByKeyword,svnKeywords.isLastChangedBy());
            updateKeywordItemInitialState(headUrlKeyword,svnKeywords.isHeadUrl());
            updateKeywordItemInitialState(idKeyword,svnKeywords.isId());            
        }
    }

    /**
     * update the initial state for the keyword item, used only from AddKeywordsDialog 
     * @param currentState
     * @param isKeywordSet
     * @return
     */
    private void updateKeywordItemInitialState(KeywordItem keywordItem, boolean isKeywordSet) {
        if (isKeywordSet) {
            if (keywordItem.initialState == KeywordItem.UNKNOWN)
                keywordItem.initialState = KeywordItem.CHECKED;
            else
            if (keywordItem.initialState == KeywordItem.UNCHECKED)
                keywordItem.initialState = KeywordItem.GRAYED;
        }
        else
        {
            if (keywordItem.initialState == KeywordItem.UNKNOWN)
                keywordItem.initialState = KeywordItem.UNCHECKED;
            else
            if (keywordItem.initialState == KeywordItem.CHECKED)
                keywordItem.initialState = KeywordItem.GRAYED;
        }
        keywordItem.currentState = keywordItem.initialState;
    }

    /**
     * get all the svn files in given resources 
     * - recursively if a resource is a directory
     * - add file if managed    
     * @param resources
     * @return
     * @throws SVNException
     */
    private ISVNLocalFile[] getSvnLocalFiles(IResource[] resources) throws SVNException {
        final Set svnResources = new TreeSet(); 
        ISVNResourceVisitor visitor = new ISVNResourceVisitor() {
            public void visitFile(ISVNFile file) throws SVNException {
                ISVNLocalFile svnLocalFile = (ISVNLocalFile)file; 
                if (svnLocalFile.isManaged())
                    svnResources.add(svnLocalFile); 
            }
            public void visitFolder(ISVNFolder folder) throws SVNException {
                ISVNLocalFolder svnLocalFolder = (ISVNLocalFolder)folder; 
                svnLocalFolder.acceptChildren(this);
            }             
        };
        
        
        for (int i=0; i<resources.length; i++) {
            ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
            svnResource.accept(visitor);
        }
        return (ISVNLocalFile[])svnResources.toArray(new ISVNLocalFile[svnResources.size()]);        
    }
    
    /**
     * create the keywords items
     * @param lastChangedDateKeywordState
     * @param lastChangedRevisionKeywordState
     * @param lastChangedByKeywordState
     * @param headUrlKeywordState
     * @param idKeywordState
     */
	private void createKeywordItems(
        int lastChangedDateKeywordState,
        int lastChangedRevisionKeywordState,
        int lastChangedByKeywordState,
        int headUrlKeywordState,
        int idKeywordState) {
            
        lastChangedDateKeyword = new KeywordItem(
            SVNKeywords.LAST_CHANGED_DATE,
            Policy.bind("AddKeywordsDialog.lastChangedDate.description"), //$NON-NLS-1$
            Policy.bind("AddKeywordsDialog.lastChangedDate.sample"), //$NON-NLS-1$
            lastChangedDateKeywordState    
        );
        
        lastChangedRevisionKeyword = new KeywordItem(
            SVNKeywords.LAST_CHANGED_REVISION,
            Policy.bind("AddKeywordsDialog.lastChangedRevision.description"), //$NON-NLS-1$
            Policy.bind("AddKeywordsDialog.lastChangedRevision.sample"), //$NON-NLS-1$
            lastChangedRevisionKeywordState
        );
        
        lastChangedByKeyword = new KeywordItem(
            SVNKeywords.LAST_CHANGED_BY,
            Policy.bind("AddKeywordsDialog.lastChangedBy.description"), //$NON-NLS-1$
            Policy.bind("AddKeywordsDialog.lastChangedBy.sample"), //$NON-NLS-1$
            lastChangedByKeywordState
        );
        
        headUrlKeyword = new KeywordItem(
            SVNKeywords.HEAD_URL,
            Policy.bind("AddKeywordsDialog.headUrl.description"), //$NON-NLS-1$
            Policy.bind("AddKeywordsDialog.headUrl.sample"), //$NON-NLS-1$
            headUrlKeywordState
        );
        
        idKeyword = new KeywordItem(
            SVNKeywords.ID,
            Policy.bind("AddKeywordsDialog.id.description"), //$NON-NLS-1$
            Policy.bind("AddKeywordsDialog.id.sample"), //$NON-NLS-1$
            idKeywordState
        );        
        
	}

    /* (non-Javadoc)
     * Method declared on Dialog.
     */
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Policy.bind("AddKeywordsDialog.title")); //$NON-NLS-1$
    }


	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
    protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        
		// set F1 help
		WorkbenchHelp.setHelp(composite, IHelpContextIds.ADD_KEYWORDS_DIALOG);

        createWrappingLabel(composite, Policy.bind("AddKeywordsDialog.selectKeywords"));  //$NON-NLS-1$
			 
		// add the selectable checkbox list
		listViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.FULL_SELECTION);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = SELECTION_HEIGHT_HINT;
		data.widthHint = WIDTH_HINT;
		listViewer.getTable().setLayoutData(data);
        
        TableColumn column = new TableColumn(listViewer.getTable(), SWT.LEFT);
        column.setText(Policy.bind("AddKeywordsDialog.keyword")); //$NON-NLS-1$
        column.setWidth(200);

        column = new TableColumn(listViewer.getTable(), SWT.LEFT);
        column.setText(Policy.bind("AddKeywordsDialog.description")); //$NON-NLS-1$
        column.setWidth(350);
        column.setResizable(true);
        
        column = new TableColumn(listViewer.getTable(), SWT.LEFT);
        column.setText(Policy.bind("AddKeywordsDialog.sample")); //$NON-NLS-1$
        column.setWidth(250);
        column.setResizable(true);        
                        
                        
        final KeywordItem[] items = new KeywordItem[] {
            lastChangedDateKeyword,
            lastChangedRevisionKeyword,
            lastChangedByKeyword,
            headUrlKeyword,
            idKeyword };      
        
        listViewer.getTable().setHeaderVisible(true);

        // set the content provider
        listViewer.setContentProvider(new IStructuredContentProvider() {
            public Object[] getElements(Object inputElement) {
                return items;
            }
            public void dispose() {}
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
        });
        
        // set the label provider
        listViewer.setLabelProvider(new ITableLabelProvider() {
            public Image getColumnImage(Object element, int columnIndex) {
                return null;
            }
            public String getColumnText(Object element, int columnIndex) {
                KeywordItem item = (KeywordItem)element; 
                if (columnIndex == 0)
                    return item.keywordName;
                else
                if (columnIndex == 1)
                    return item.description;
                else
                if (columnIndex == 2)
                    return item.sample;
                return ""; //$NON-NLS-1$
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

        // add a check state listener
        listViewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event){
                KeywordItem item = (KeywordItem)event.getElement();

                if (item.currentState == KeywordItem.UNCHECKED)
                    item.currentState = KeywordItem.CHECKED;
                else
                if ((item.currentState == KeywordItem.CHECKED) &&
                    (item.initialState == KeywordItem.GRAYED))
                    item.currentState = KeywordItem.GRAYED;
                else
                    item.currentState = KeywordItem.UNCHECKED;
                    
                updateCheckbox(item);
            }
        });

        listViewer.setInput(this);
  
		addSelectionButtons(composite);

        for (int i = 0; i < items.length;i++) {
            KeywordItem item = items[i];
            updateCheckbox(item);
        }
        
        return composite;
	}

    /**
     * update the keywords using subversion  
     * @throws SVNException
     */
    public void updateKeywords() throws SVNException {
        for (int i = 0; i < files.length;i++) {
            ISVNLocalFile svnLocalFile = files[i];
            SVNKeywords svnKeyword = svnLocalFile.getKeywords();
                    
            if (lastChangedDateKeyword.currentState == KeywordItem.CHECKED)
                svnKeyword.setLastChangedDate(true);
            else
            if (lastChangedDateKeyword.currentState == KeywordItem.UNCHECKED)
                svnKeyword.setLastChangedDate(false);                        
                    
            if (lastChangedDateKeyword.currentState == KeywordItem.CHECKED)
                svnKeyword.setLastChangedDate(true);
            else 
            if (lastChangedDateKeyword.currentState == KeywordItem.UNCHECKED)
                svnKeyword.setLastChangedDate(false);
                                    
            if (lastChangedRevisionKeyword.currentState == KeywordItem.CHECKED)
                svnKeyword.setLastChangedRevision(true);
            else 
            if (lastChangedRevisionKeyword.currentState == KeywordItem.UNCHECKED)
                svnKeyword.setLastChangedRevision(false);
        
            if (lastChangedByKeyword.currentState == KeywordItem.CHECKED)
                svnKeyword.setLastChangedBy(true);
            else 
            if (lastChangedByKeyword.currentState == KeywordItem.UNCHECKED)
                svnKeyword.setLastChangedBy(false);
        
            if (headUrlKeyword.currentState == KeywordItem.CHECKED)
                svnKeyword.setHeadUrl(true);
            else 
            if (headUrlKeyword.currentState == KeywordItem.UNCHECKED)
                svnKeyword.setHeadUrl(false);
                                                    
            if (idKeyword.currentState == KeywordItem.CHECKED)
                svnKeyword.setId(true);
            else 
            if (idKeyword.currentState == KeywordItem.UNCHECKED)
                svnKeyword.setId(false);
                     
            svnLocalFile.setKeywords(svnKeyword);
        }
    }

    /**
     * update the checkbox corresponding to the keyword item
     * @param item
     */	
    private void updateCheckbox(KeywordItem item)
    {
        if (item.currentState == KeywordItem.GRAYED)
        {
            listViewer.setChecked(item,true);
            listViewer.setGrayed(item,true);    
        }
        else
        if (item.currentState == KeywordItem.CHECKED)
        {
            listViewer.setChecked(item,true);
            listViewer.setGrayed(item,false); 
        }
        else
        if (item.currentState == KeywordItem.UNCHECKED)
        {
            listViewer.setChecked(item,false);
            listViewer.setGrayed(item,false); 
        }            
    }
    
	/**
	 * Add the selection and deselection buttons to the dialog.
	 * @param composite org.eclipse.swt.widgets.Composite
	 */
	private void addSelectionButtons(Composite composite) {
	
		Composite buttonComposite = new Composite(composite, SWT.RIGHT);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		buttonComposite.setLayout(layout);
		GridData data =
			new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		composite.setData(data);
	
		Button selectButton = createButton(buttonComposite, IDialogConstants.SELECT_ALL_ID, Policy.bind("ReleaseCommentDialog.selectAll"), false); //$NON-NLS-1$
		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
                lastChangedDateKeyword.currentState = KeywordItem.CHECKED;  
                lastChangedRevisionKeyword.currentState = KeywordItem.CHECKED;
                lastChangedByKeyword.currentState = KeywordItem.CHECKED;  
                headUrlKeyword.currentState = KeywordItem.CHECKED;
                idKeyword.currentState = KeywordItem.CHECKED;           
				listViewer.setAllChecked(true);
                listViewer.setAllGrayed(false);
			}
		};
		selectButton.addSelectionListener(listener);
	
		Button deselectButton = createButton(buttonComposite, IDialogConstants.DESELECT_ALL_ID, Policy.bind("ReleaseCommentDialog.deselectAll"), false); //$NON-NLS-1$
		listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
                lastChangedDateKeyword.currentState = KeywordItem.UNCHECKED;  
                lastChangedRevisionKeyword.currentState = KeywordItem.UNCHECKED;
                lastChangedByKeyword.currentState = KeywordItem.UNCHECKED;  
                headUrlKeyword.currentState = KeywordItem.UNCHECKED;
                idKeyword.currentState = KeywordItem.UNCHECKED;           
				listViewer.setAllChecked(false);
                listViewer.setAllGrayed(false);                
			}
		};
		deselectButton.addSelectionListener(listener);
	}
	
	protected static final int LABEL_WIDTH_HINT = 400;
	protected Label createWrappingLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.LEFT | SWT.WRAP);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalIndent = 0;
		data.grabExcessHorizontalSpace = true;
		data.widthHint = LABEL_WIDTH_HINT;
		label.setLayoutData(data);
		return label;
	}
	
}
