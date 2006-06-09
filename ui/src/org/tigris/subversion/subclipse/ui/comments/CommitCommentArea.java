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
package org.tigris.subversion.subclipse.ui.comments;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.DialogArea;
import org.tigris.subversion.subclipse.ui.settings.CommentProperties;
import org.tigris.subversion.subclipse.ui.util.RuleredText;

/**
 * This area provides the widgets for providing the SVN commit comment
 * This is used in ReleaseCommentDialog
 */
public class CommitCommentArea extends DialogArea {

	private static final int WIDTH_HINT = 350;
	private static final int HEIGHT_HINT = 50;
	
	private String enterCommentMessage;
	private String oldComment;
	private RuleredText text;
	private Combo previousCommentsCombo;
	
	private String[] comments = new String[0];
	private String comment = null; //$NON-NLS-1$
	private CommentProperties commentProperties;
	private ModifyListener modifyListener;
	
	public static final String OK_REQUESTED = "OkRequested";//$NON-NLS-1$
	
	/**
	 * Constructor for CommitCommentArea.
	 * @param parentDialog
	 * @param settings
	 */
	public CommitCommentArea(Dialog parentDialog, IDialogSettings settings) {
		super(parentDialog, settings);
		comments = SVNUIPlugin.getPlugin().getRepositoryManager().getCommentsManager().getPreviousComments();
	}
	
	public CommitCommentArea(Dialog parentDialog, IDialogSettings settings, CommentProperties commentProperties) {
		this(parentDialog, settings);
		this.commentProperties = commentProperties;
	}
	
	/**
	 * Constructor for CommitCommentArea.
	 * @param parentDialog
	 * @param settings
	 * @param enterCommentMessage
	 */
	public CommitCommentArea(Dialog parentDialog, IDialogSettings settings, String enterCommentMessage) {
		this(parentDialog, settings);
		this.enterCommentMessage = enterCommentMessage;
	}
	
	public CommitCommentArea(Dialog parentDialog, IDialogSettings settings, String enterCommentMessage, CommentProperties commentProperties) {
		this(parentDialog, settings, enterCommentMessage);
		this.commentProperties = commentProperties;
	}	

	/**
	 * @see org.tigris.subversion.subclipse.ui.DialogArea#createArea(org.eclipse.swt.widgets.Composite)
	 */
	public Control createArea(Composite parent) {
		Composite composite = createGrabbingComposite(parent, 1);
		initializeDialogUnits(composite);
						
		Label label = new Label(composite, SWT.NULL);
		label.setLayoutData(new GridData());
		if (enterCommentMessage == null) label.setText(Policy.bind("ReleaseCommentDialog.enterComment")); //$NON-NLS-1$
		else label.setText(enterCommentMessage);
		
		int widthMarker = 0;
		if (commentProperties != null) widthMarker = commentProperties.getLogWidthMarker();
		if (widthMarker == 0)
		    text = new RuleredText(composite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		else
		    text = new RuleredText(composite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL, widthMarker);
		if (oldComment != null) {
		    text.setText(oldComment);
		    text.setCaretOffset(oldComment.length());
		} else if ((commentProperties != null) && (commentProperties.getLogTemplate() != null)) {
		    text.setText(commentProperties.getLogTemplate());
		    text.setCaretOffset(commentProperties.getLogTemplate().length());
		}
		if (modifyListener != null) text.addModifyListener(modifyListener);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = WIDTH_HINT;
		data.heightHint = HEIGHT_HINT;
		
		text.setLayoutData(data);
		if (commentProperties == null && oldComment == null) text.selectAll();
		text.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_RETURN && (e.stateMask & SWT.CTRL) != 0) {
					e.doit = false;
					CommitCommentArea.this.signalCtrlEnter();
				}
			}
		});
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				comment = text.getText();
			}
		});
		
		
		label = new Label(composite, SWT.NULL);
		label.setLayoutData(new GridData());
		label.setText(Policy.bind("ReleaseCommentDialog.choosePrevious")); //$NON-NLS-1$
		
		previousCommentsCombo = new Combo(composite, SWT.READ_ONLY);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		previousCommentsCombo.setLayoutData(data);
		
		// Initialize the values before we register any listeners so
		// we don't get any platform specific selection behavior
		// (see bug 32078: http://bugs.eclipse.org/bugs/show_bug.cgi?id=32078)
		initializeValues();
		
		previousCommentsCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index = previousCommentsCombo.getSelectionIndex();
				if (index != -1)
					text.setText(comments[index]);
			}
		});
		
		return composite;
	}

	/**
	 * Method initializeValues.
	 */
	private void initializeValues() {
		
		// populate the previous comment list
		for (int i = 0; i < comments.length; i++) {
			previousCommentsCombo.add(flattenText(comments[i]));
		}
		
		// We don't want to have an initial selection
		// (see bug 32078: http://bugs.eclipse.org/bugs/show_bug.cgi?id=32078)
		previousCommentsCombo.setText(""); //$NON-NLS-1$
		
		if (commentProperties == null && oldComment == null) text.setText(""); //$NON-NLS-1$
		comment = text.getText();
	}

	/*
	 * Flatten the text in the multiline comment
	 * @param string
	 * @return String
	 */
	private String flattenText(String string) {
		StringBuffer buffer = new StringBuffer(string.length() + 20);
		boolean skipAdjacentLineSeparator = true;
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c == '\r' || c == '\n') {
				if (!skipAdjacentLineSeparator)
					buffer.append(Policy.bind("separator")); //$NON-NLS-1$
				skipAdjacentLineSeparator = true;
			} else {
				buffer.append(c);
				skipAdjacentLineSeparator = false;
			}
		}
		return buffer.toString();
	}

	/**
	 * Method signalCtrlEnter.
	 */
	private void signalCtrlEnter() {
		firePropertyChangeChange(OK_REQUESTED, null, null);
	}

	/**
	 * Return the entered comment
	 * 
	 * @return the comment
	 */
	public String[] getComments() {
		return comments;
	}
	
	/**
	 * Returns the comment.
	 * @return String
	 */
	public String getComment() {
		if (comment != null && comment.length() > 0) finished();
		return comment;
	}

	
	private void finished() {
		// if there is still a comment, remember it
		if (comment.length() > 0) {
			SVNUIPlugin.getPlugin().getRepositoryManager().getCommentsManager().addComment(comment);
		}
	}
 
    public RuleredText getText() {
        return text;
    }
    public void setModifyListener(ModifyListener modifyListener) {
        this.modifyListener = modifyListener;
    }
    
    public void setOldComment(String oldComment) {
    	this.oldComment = oldComment; 
    }
}
