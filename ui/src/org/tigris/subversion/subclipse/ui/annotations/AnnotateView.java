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
package org.tigris.subversion.subclipse.ui.annotations;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.registry.EditorDescriptor;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.history.SVNHistoryPage;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * A view showing the results of the SVN Annotate Command.  A linked
 * combination of a View of annotations, a source editor and the
 * Resource History View
 */
public class AnnotateView extends ViewPart implements ISelectionChangedListener {

	ITextEditor editor;
	IHistoryView historyView;
	IWorkbenchPage page;

	ListViewer viewer;
	IDocument document;
	Collection svnAnnotateBlocks;
	ISVNRemoteFile svnFile;
	InputStream contents;
	
	IStructuredSelection previousListSelection;
	ITextSelection previousTextSelection;
	boolean lastSelectionWasText = false;
	
	
	public static final String VIEW_ID = "org.tigris.subversion.subclipse.ui.annotations.AnnotateView"; //$NON-NLS-1$
	private Composite top;
	
	private IPartListener partListener = new IPartListener() {
		public void partActivated(IWorkbenchPart part) {
		}
		public void partBroughtToTop(IWorkbenchPart part) {
		}
		public void partClosed(IWorkbenchPart part) {
			if (editor != null && part == editor) {
				disconnect();
			}
		}
		public void partDeactivated(IWorkbenchPart part) {
		}
		public void partOpened(IWorkbenchPart part) {
		}
	};

	public AnnotateView() {
		super();
	}

	public void createPartControl(Composite parent) {
		
		this.top = parent;
		
		// Create default contents
		Label label = new Label(top, SWT.WRAP);
		label.setText(Policy.bind("SVNAnnotateView.viewInstructions")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_BOTH));
		top.layout();
	}

	/**
	 * Show the annotation view.
	 * @param svnFile
	 * @param svnAnnotateBlocks
	 * @param contents
	 * @throws PartInitException
	 */
	public void showAnnotations(ISVNRemoteFile svnFile, Collection svnAnnotateBlocks, InputStream contents) throws PartInitException {
		showAnnotations(svnFile, svnAnnotateBlocks, contents, true);		
	}
	
	/**
	 * Show the annotation view.
	 * @param svnFile
	 * @param svnAnnotateBlocks
	 * @param contents
	 * @param useHistoryView
	 * @throws PartInitException
	 */
	public void showAnnotations(ISVNRemoteFile svnFile, Collection svnAnnotateBlocks, InputStream contents, boolean useHistoryView) throws PartInitException {

		// Disconnect from old annotation editor
		disconnect();
		
		// Remove old viewer
		Control[] oldChildren = top.getChildren();
		if (oldChildren != null) {
			for (int i = 0; i < oldChildren.length; i++) {
				oldChildren[i].dispose();
			}
		}

		viewer = new ListViewer(top, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		viewer.addSelectionChangedListener(this);
		viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), IHelpContextIds.ANNOTATIONS_VIEW);

		top.layout();
		
		this.svnFile = svnFile;
		this.contents = contents;
		this.svnAnnotateBlocks = svnAnnotateBlocks;
		page = SVNUIPlugin.getActivePage();
		viewer.setInput(svnAnnotateBlocks);
		editor = (ITextEditor) openEditor();
		IDocumentProvider provider = editor.getDocumentProvider();
		document = provider.getDocument(editor.getEditorInput());

		setPartName(Policy.bind("SVNAnnotateView.showFileAnnotation", new Object[] {svnFile.getName()})); //$NON-NLS-1$
		setTitleToolTip(svnFile.getName());
		
		if (!useHistoryView) {
			return;
		}

		// Get hook to the HistoryView
		historyView = (IHistoryView)page.showView(ISVNUIConstants.HISTORY_VIEW_ID);
		if (historyView != null) {
			historyView.showHistoryFor(svnFile);
		}
	}
	
	protected void disconnect() {
		if(editor != null) {
			if (editor.getSelectionProvider() instanceof IPostSelectionProvider) {
				((IPostSelectionProvider) editor.getSelectionProvider()).removePostSelectionChangedListener(this);
			}
			editor.getSite().getPage().removePartListener(partListener);
			editor = null;
			document = null;
		}
	}
	
	/**
	 * Makes the view visible in the active perspective. If there
	 * isn't a view registered <code>null</code> is returned.
	 * Otherwise the opened view part is returned.
	 */
	public static AnnotateView openInActivePerspective() throws PartInitException {
		return (AnnotateView) SVNUIPlugin.getActivePage().showView(VIEW_ID);
	}

	/**
	 * Selection changed in either the Annotate List View or the
	 * Source editor.
	 */
	public void selectionChanged(SelectionChangedEvent event) {
	
		if (event.getSelection() instanceof IStructuredSelection) {
			listSelectionChanged((IStructuredSelection) event.getSelection());
		} else if (event.getSelection() instanceof ITextSelection) {
			textSelectionChanged((ITextSelection) event.getSelection());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	public void dispose() {
		disconnect();
	}
	
	/**
	 * A selection event in the Annotate Source Editor
	 * @param event
	 */
	private void textSelectionChanged(ITextSelection selection) {
		
		// Track where the last selection event came from to avoid
		// a selection event loop.
		lastSelectionWasText = true;
			
		// Locate the annotate block containing the selected line number.
		AnnotateBlock match = null;
		for (Iterator iterator = svnAnnotateBlocks.iterator(); iterator.hasNext();) {
			AnnotateBlock block = (AnnotateBlock) iterator.next();
			if (block.contains(selection.getStartLine())) {
				match = block;
				break;
			}
		}

		// Select the annotate block in the List View.
		if (match == null) {
			return;
		}
		
		StructuredSelection listSelection = new StructuredSelection(match); 
		viewer.setSelection(listSelection, true);
	}

	/**
	 * A selection event in the Annotate List View
	 * @param selection
	 */
	private void listSelectionChanged(IStructuredSelection selection) {

		// If the editor was closed, reopen it.
		if (editor == null || editor.getSelectionProvider() == null) {
			try {
				contents.reset();
				showAnnotations(svnFile, svnAnnotateBlocks, contents, false);
			} catch (PartInitException e) {
				return;
			} catch (IOException e) {
				return;
			}
		}
		
		ISelectionProvider selectionProvider = editor.getSelectionProvider();
		if (selectionProvider == null) {
			// Failed to open the editor but what else can we do.
			return;
		}
		ITextSelection textSelection = (ITextSelection) selectionProvider.getSelection();
		AnnotateBlock listSelection = null;
		try {
			listSelection = (AnnotateBlock) selection.getFirstElement();
		} catch (ClassCastException cce) {
			return;
		}

        // IStructuredSelection#getFirstElement can return null
        if (listSelection == null) {
            return;
        }
        
		/**
		 * Ignore event if the current text selection is already equal to the corresponding
		 * list selection.  Nothing to do.  This prevents infinite event looping.
		 *
		 * Extra check to handle single line deltas 
		 */
		
		if (textSelection.getStartLine() == listSelection.getStartLine() && textSelection.getEndLine() == listSelection.getEndLine() && selection.equals(previousListSelection)) {
			return;
		}
		
		// If the last selection was a text selection then bale to prevent a selection loop.
		if (!lastSelectionWasText) {
			try {
				int start = document.getLineOffset(listSelection.getStartLine());
				int end = document.getLineOffset(listSelection.getEndLine() + 1);
				editor.selectAndReveal(start, end - start);
				if (editor != null && !page.isPartVisible(editor)) {
					page.activate(editor);
				}

			} catch (BadLocationException e) {
				// Ignore - nothing we can do.
			}
		}
		
		
		// Select the revision in the history view.
		if(historyView != null) {
			SVNHistoryPage page = (SVNHistoryPage)historyView.getHistoryPage();
			page.selectRevision(new SVNRevision.Number(listSelection.getRevision()));
		}
		lastSelectionWasText = false;			
	}

	/**
	 * Try and open the correct registered editor type for the file.
	 * @throws PartInitException Unable to create view
	 */
	private IEditorPart openEditor() throws PartInitException {
		// Open the editor
		IEditorPart part;
		IEditorRegistry registry;

		registry = SVNUIPlugin.getPlugin().getWorkbench().getEditorRegistry();
		IEditorDescriptor descriptor = registry.getDefaultEditor(svnFile.getName());

		// Determine if the registered editor is an ITextEditor.	
		// There is currently no support from UI to determine this information. This
		// problem has been logged in: https://bugs.eclipse.org/bugs/show_bug.cgi?id=47362
		// For now, use internal classes.
		String id;
		
		if (descriptor == null || !(descriptor instanceof EditorDescriptor) || !(((EditorDescriptor)descriptor).isInternal())) {
			id = IDEWorkbenchPlugin.DEFAULT_TEXT_EDITOR_ID; //$NON-NLS-1$
		} else {
			try {
				Object obj = IDEWorkbenchPlugin.createExtension(((EditorDescriptor) descriptor).getConfigurationElement(), "class"); //$NON-NLS-1$
				if (obj instanceof ITextEditor) {
					id = descriptor.getId();
				} else {
					id = IDEWorkbenchPlugin.DEFAULT_TEXT_EDITOR_ID;
				}
			} catch (CoreException e) {
				id = IDEWorkbenchPlugin.DEFAULT_TEXT_EDITOR_ID;
			}
		}
		
		// Either reuse an existing editor or open a new editor of the correct type.
		if (editor != null && editor instanceof IReusableEditor && page.isPartVisible(editor) && editor.getSite().getId().equals(id)) {
			// We can reuse the editor
			((IReusableEditor) editor).setInput(new RemoteAnnotationEditorInput(svnFile, contents));
			part = editor;
		} else {
			// We can not reuse the editor so close the existing one and open a new one.
			if (editor != null) {
				page.closeEditor(editor, false);
				editor = null;
			}
			part = page.openEditor(new RemoteAnnotationEditorInput(svnFile, contents), id);
		}
		
		// Hook Editor post selection listener.
		ITextEditor editor = (ITextEditor) part;
		if (editor.getSelectionProvider() instanceof IPostSelectionProvider) {
			((IPostSelectionProvider) editor.getSelectionProvider()).addPostSelectionChangedListener(this);
		}
		part.getSite().getPage().addPartListener(partListener);
		return part;
	}

	// This method implemented to be an ISelectionChangeListener but we
	// don't really care when the List or Editor get focus.
	public void setFocus() {
		return;
	}
}
