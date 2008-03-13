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
package org.tigris.subversion.subclipse.ui.dialogs;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * Dialog for searching the history of a resource in the repository.
 * The dialog is available from the history view. A search with this 
 * dialog results in a filter being created for the table viewed in the
 * history view. Only history entries matching the dialog's fields 
 * will be displayed in the history view.
 */
public class HistorySearchDialog extends SvnDialog {

	private ISVNRemoteResource remoteResource;
	
	private Text selectedResourceText;
	private Text userText;
	private Combo commentCombo;
	private Text startDateText;
	private Text endDateText;
	private Button regExpButton;
	private Button searchAllButton;
	private Button fetchButton;
	private Text startRevisionText;
	private Button startLogButton;
	private Text endRevisionText;
	private Button endLogButton;
	
	private String user;
	private String comment;
	private Date startDate;
	private Date endDate;
	private SVNRevision.Number startRevision;
	private SVNRevision.Number endRevision;
	private boolean searchAll = true;
	private boolean regExp;
	private boolean autoFetchLogs;
	private String oldStartRevision;
	private String oldEndRevision;
	private boolean revisionsChanged;
	
	private List previousComments;

	/**
	 * Constructs a new dialog for searching revision history.
	 * @param shell shell used for this dialog
	 * @param remoteResource resource for which the search is selected
	 */
	public HistorySearchDialog(Shell shell, ISVNRemoteResource remoteResource) {
		super(shell, "HistorySearchDialog"); //$NON-NLS-1$
		this.remoteResource = remoteResource;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("HistorySearchDialog.title")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.SEARCH_HISTORY_DIALOG);
		
		Composite top = new Composite(composite, SWT.NULL);
		GridLayout topLayout = new GridLayout();
		topLayout.numColumns = 2;
		top.setLayout(topLayout);
		top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Label resourceLabel = new Label(top, SWT.NONE);
		resourceLabel.setText(Policy.bind("HistorySearchDialog.resource")); //$NON-NLS-1$
		selectedResourceText = new Text(top, SWT.BORDER);
		selectedResourceText.setText(remoteResource.getUrl().toString());
		selectedResourceText.setEditable(false);
		GridData gdSelectedResourceText = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		selectedResourceText.setLayoutData(gdSelectedResourceText);
		
		regExpButton = new Button(top, SWT.CHECK);
		regExpButton.setText(Policy.bind("HistorySearchDialog.regExp")); //$NON-NLS-1$
		GridData gdRegExpButton = new GridData(GridData.FILL_HORIZONTAL);
		gdRegExpButton.horizontalSpan = 2;
		regExpButton.setLayoutData(gdRegExpButton);
		regExpButton.setSelection(regExp);
		
		Group search = new Group(composite, SWT.NONE);
		search.setText(Policy.bind("HistorySearchDialog.searchTitle")); //$NON-NLS-1$
		GridLayout searchLayout = new GridLayout();
		searchLayout.numColumns = 2;
		search.setLayout(searchLayout);
		search.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL));
		
		Label userLabel = new Label(search, SWT.NONE);
		userLabel.setText(Policy.bind("HistorySearchDialog.user")); //$NON-NLS-1$
		userText = new Text(search, SWT.BORDER);
		userText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
		if (user != null) {
			userText.setText(user);
		}
		
		Label commentLabel = new Label(search, SWT.NONE);
		commentLabel.setText(Policy.bind("HistorySearchDialog.comment")); //$NON-NLS-1$
		commentCombo = new Combo(search, SWT.DROP_DOWN);
		commentCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
		fillCommentCombo(commentCombo);
		
		fetchButton = new Button(search, SWT.CHECK);
		GridData gdFetchButton = new GridData(GridData.FILL_HORIZONTAL);
		gdFetchButton.horizontalSpan = 2;
		fetchButton.setLayoutData(gdFetchButton);
		fetchButton.setText(Policy.bind("HistorySearchDialog.fetchLogIfNeeded")); //$NON-NLS-1$
		fetchButton.setSelection(autoFetchLogs);
		
		searchAllButton = new Button(search, SWT.CHECK);
		GridData gdSearchAllButton = new GridData(GridData.FILL_HORIZONTAL);
		gdSearchAllButton.horizontalSpan = 2;
		searchAllButton.setLayoutData(gdSearchAllButton);
		searchAllButton.setText(Policy.bind("HistorySearchDialog.searchAllLogs")); //$NON-NLS-1$
		searchAllButton.setSelection(searchAll);

		final Label dateLabel = new Label(search, SWT.NONE);
		dateLabel.setText(Policy.bind("HistorySearchDialog.date")); //$NON-NLS-1$
		dateLabel.setEnabled(!searchAll);
		Composite dateComp = new Composite(search, SWT.NONE);
		GridLayout dateCompLayout = new GridLayout();
		dateCompLayout.numColumns = 5;
		dateCompLayout.marginHeight = 0;
		dateCompLayout.marginWidth = 0;
		dateComp.setLayout(dateCompLayout);
		dateComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		startDateText = new Text(dateComp, SWT.BORDER);
		startDateText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (startDate == null && startRevision == null && endRevision == null) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.MONTH, -1);
			startDate = calendar.getTime();
		}
		if (startDate != null) {
			startDateText.setText(formatDate(startDate));
			startDateText.setData(startDate);
		}
		startDateText.setEnabled(!searchAll);
		final Button selectStartDateButton = new Button(dateComp, SWT.NONE);
		selectStartDateButton.setText(Policy.bind("HistorySearchDialog.startDateButton")); //$NON-NLS-1$
		selectStartDateButton.setEnabled(!searchAll);
		final Label midDataLabel = new Label(dateComp, SWT.NONE);
		midDataLabel.setText(Policy.bind("HistorySearchDialog.midDate")); //$NON-NLS-1$
		midDataLabel.setEnabled(!searchAll);
		endDateText = new Text(dateComp, SWT.BORDER);
		endDateText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (endDate == null && startRevision == null && endRevision == null) {
			endDate = Calendar.getInstance().getTime();
		}
		if (endDate != null) {
			endDateText.setText(formatDate(endDate));
			endDateText.setData(endDate);
		}
		endDateText.setEnabled(!searchAll);
		final Button selectEndDateButton = new Button(dateComp, SWT.NONE);
		selectEndDateButton.setText(Policy.bind("HistorySearchDialog.endDateButton")); //$NON-NLS-1$
		selectEndDateButton.setEnabled(!searchAll);
		
		final Label revisionLabel = new Label(search, SWT.NONE);
		revisionLabel.setText(Policy.bind("HistorySearchDialog.revision")); //$NON-NLS-1$
		revisionLabel.setEnabled(!searchAll);	
		Composite revisionComp = new Composite(search, SWT.NONE);
		GridLayout revisionCompLayout = new GridLayout();
		revisionCompLayout.numColumns = 5;
		revisionCompLayout.marginHeight = 0;
		revisionCompLayout.marginWidth = 0;
		revisionComp.setLayout(revisionCompLayout);
		revisionComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		startRevisionText = new Text(revisionComp, SWT.BORDER);
		startRevisionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));	
		if (startRevision != null) startRevisionText.setText(startRevision.toString());
		startRevisionText.setEnabled(!searchAll);
		startLogButton = new Button(revisionComp, SWT.PUSH);
		startLogButton.setText(Policy.bind("HistorySearchDialog.startRevisionButton")); //$NON-NLS-1$
		startLogButton.setEnabled(!searchAll);
		final Label endRevisionLabel = new Label(revisionComp, SWT.NONE);
		endRevisionLabel.setText(Policy.bind("HistorySearchDialog.endRevision")); //$NON-NLS-1$
		endRevisionLabel.setEnabled(!searchAll);	
		endRevisionText = new Text(revisionComp, SWT.BORDER);
		endRevisionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));	
		if (endRevision != null) endRevisionText.setText(endRevision.toString());
		endRevisionText.setEnabled(!searchAll);
		endLogButton = new Button(revisionComp, SWT.PUSH);
		endLogButton.setText(Policy.bind("HistorySearchDialog.endRevisionButton")); //$NON-NLS-1$
		endLogButton.setEnabled(!searchAll);		
		
		// Enabling and disabling the widgets when the search all button
		//  is checked or unchecked
		searchAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (searchAllButton.getSelection()) {
					dateLabel.setEnabled(false);
					midDataLabel.setEnabled(false);
					selectStartDateButton.setEnabled(false);
					selectEndDateButton.setEnabled(false);
					startDateText.setEnabled(false);
					endDateText.setEnabled(false);
					revisionLabel.setEnabled(false);
					startRevisionText.setEnabled(false);
					startLogButton.setEnabled(false);
					endRevisionLabel.setEnabled(false);
					endRevisionText.setEnabled(false);
					endLogButton.setEnabled(false);					
				}
				else {
					dateLabel.setEnabled(true);
					midDataLabel.setEnabled(true);
					selectStartDateButton.setEnabled(true);
					selectEndDateButton.setEnabled(true);
					startDateText.setEnabled(true);
					endDateText.setEnabled(true);
					revisionLabel.setEnabled(true);
					startRevisionText.setEnabled(true);
					startLogButton.setEnabled(true);
					endRevisionLabel.setEnabled(true);
					endRevisionText.setEnabled(true);
					endLogButton.setEnabled(true);										
				}
			}
		});
		// Open the date selection dialog to select a start date
		selectStartDateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String start = null;
				if (startDateText.getText().trim().length() == 0) {
					Calendar calendar = Calendar.getInstance();
					calendar.add(Calendar.MONTH, -1);
					Date date = calendar.getTime();
					start = formatDate(date);
				} else {
					start = startDateText.getText();
					if (parseDate(start) == null) {
						return;
					}
				}
				DateSelectionDialog dsd = new DateSelectionDialog(getShell(), start); //$NON-NLS-1$
				if (dsd.open() == Window.OK) {
					Date startDate = dsd.getDate();
					startDateText.setText(formatDate(startDate));
					startDateText.setData(startDate);
					if (endDateText.getText().trim().length() > 0) {
						Date endDate = (Date)endDateText.getData();
						if (!startDate.before(endDate)) {
							endDateText.setText(formatDate(startDate));
							endDateText.setData(startDate);
						}
					}
				}
			}
		});
		// Open the date selection dialog to select an end date
		selectEndDateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String end = null;
				if (endDateText.getText().trim().length() == 0) {
					Date date = Calendar.getInstance().getTime();
					end = formatDate(date);
				} else {
					end = endDateText.getText();
					if (parseDate(end) == null) {
						return;
					}
				}
				DateSelectionDialog dsd = new DateSelectionDialog(getShell(), end); //$NON-NLS-1$
				if (dsd.open() == Window.OK) {
					Date endDate = dsd.getDate();
					endDateText.setText(formatDate(endDate));
					endDateText.setData(endDate);
					if (startDateText.getText().trim().length() > 0) {
						Date startDate = (Date)startDateText.getData();
						if (!endDate.after(startDate)) {
							startDateText.setText(formatDate(endDate));
							startDateText.setData(endDate);
						}
					}
				}
			}
		});
		
		SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() == startLogButton) showLog(startRevisionText);
				else showLog(endRevisionText);
			}
		};
		startLogButton.addSelectionListener(selectionListener);
		endLogButton.addSelectionListener(selectionListener);
		
		// Set focus to the user search field
		userText.setFocus();
		userText.setSelection(0, userText.getText().length());
		
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		userText.addFocusListener(focusListener);
		startDateText.addFocusListener(focusListener);
		endDateText.addFocusListener(focusListener);
		
		oldStartRevision = startRevisionText.getText();
		oldEndRevision = endRevisionText.getText();
		
		return composite;
	}
	
	private void showLog(Text revisionText) {
		HistoryDialog dialog = new HistoryDialog(getShell(), remoteResource);
        if (dialog.open() == HistoryDialog.CANCEL) return;
        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
        if (selectedEntries.length == 0) return;
        revisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));		
	}
	
	public void setRemoteResource(ISVNRemoteResource remoteResource) {
		this.remoteResource = remoteResource;
	}
	
	public String getUser() {
		return user;
	}
	
	public String getComment() {
		return comment;
	}
	
	public Date getStartDate() {
		return startDate;
	}
	
	public Date getEndDate() {
		return endDate;
	}
	
	public SVNRevision.Number getStartRevision() {
		return startRevision;
	}
	
	public SVNRevision.Number getEndRevision() {
		return endRevision;
	}
	
	public boolean getRegExp() {
		return regExp;
	}
	
	public boolean getAutoFetchLogs() {
		return autoFetchLogs;
	}
	
	public boolean getSearchAllLogs() {
		return searchAll;
	}
	
	/**
	 * Called when the OK-button of the dialog is pressed. The data entered
	 * in the fields is stored in attributes. The attributes can later be 
	 * accessed by calls to the get-functions.
	 */
	protected void okPressed() {
		if (searchAllButton.getSelection()) {
			startDate = null;
			endDate = null;
			startRevision = null;
			endRevision = null;
		}
		else {
			Date tempStartDate = null;
			Date tempEndDate = null;
			if (startDateText.getText().trim().length() == 0) startDate = null;
			else {
				tempStartDate = parseDate(startDateText.getText());
				if (tempStartDate == null) return;
			}
			if (endDateText.getText().trim().length() == 0) endDate = null;
			else {
				tempEndDate = parseDate(endDateText.getText());
				if (tempEndDate == null) return;
			}
			if (tempStartDate != null && tempEndDate != null) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(tempEndDate);
				calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY));
				calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE));
				calendar.set(Calendar.SECOND, calendar.getActualMaximum(Calendar.SECOND));
				tempEndDate = calendar.getTime();
				if (!tempEndDate.after(tempStartDate)) {
					MessageDialog.openError(getShell(), Policy.bind("HistorySearchDialog.dateSequenceErrorTitle"), Policy.bind("HistorySearchDialog.dateSequenceErrorMessage")); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
			}
			endDate = tempEndDate;
			startDate = tempStartDate;
			if (startRevisionText.getText().trim().length() > 0) {
				try {
					startRevision = (SVNRevision.Number)SVNRevision.getRevision(startRevisionText.getText().trim());
				} catch (ParseException e) {
					MessageDialog.openError(getShell(), Policy.bind("HistorySearchDialog.revisionParseErrorTitle"), Policy.bind("HistorySearchDialog.revisionParseErrorMessage")); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
			} else startRevision = null;
			if (endRevisionText.getText().trim().length() > 0) {
				try {
					endRevision = (SVNRevision.Number)SVNRevision.getRevision(endRevisionText.getText().trim());
				} catch (ParseException e) {
					MessageDialog.openError(getShell(), Policy.bind("HistorySearchDialog.revisionParseErrorTitle"), Policy.bind("HistorySearchDialog.revisionParseErrorMessage")); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
			} else endRevision = null;
			if (startRevision != null && endRevision != null && startRevision.getNumber() > endRevision.getNumber()) {
				MessageDialog.openError(getShell(), Policy.bind("HistorySearchDialog.revisionSequenceErrorTitle"), Policy.bind("HistorySearchDialog.revisionSequenceErrorMessage")); //$NON-NLS-1$ //$NON-NLS-2$				
				return;
			}
		}
		if (userText.getText().trim().length() > 0) user = userText.getText();
		else user = null;
		if (commentCombo.getText().trim().length() > 0) comment = commentCombo.getText();
		else comment = null;
		autoFetchLogs = fetchButton.getSelection();
		searchAll = searchAllButton.getSelection();
		regExp = regExpButton.getSelection();
		if (comment != null && comment.trim().length() > 0) {
			previousComments.add(comment);
		}
		revisionsChanged = !startRevisionText.getText().trim().equals(oldStartRevision) || !endRevisionText.getText().trim().equals(oldEndRevision);
		super.okPressed();
	}
	
	/**
	 * Format a date in the local representation.
	 * @param date date to be formatted
	 * @return a string containing the formatted date
	 */
	private String formatDate(Date date) {
		return DateFormat.getDateInstance(DateFormat.SHORT).format(date);
	}
	
	/**
	 * Parses a date in a string to a Date
	 * @param date date string to be parsed 
	 * @return a new Date object which represents the parsed date
	 */
	private Date parseDate(String date) {
		try {
			return DateFormat.getDateInstance(DateFormat.SHORT).parse(date);
		}
		catch (ParseException e) {
			MessageDialog.openError(getShell(), Policy.bind("HistorySearchDialog.dateParseErrorTitle"), Policy.bind("HistorySearchDialog.dateParseErrorMessage")); //$NON-NLS-1$ //$NON-NLS-2$			return null;
			return null;
		}
	}
	
	/**
	 * Fill the comments combobox with previous search entries. 
	 * @param combo combobox to be filled
	 */
	private void fillCommentCombo(Combo combo) {
		if (previousComments == null) {
			previousComments = new ArrayList();
		}
		for (int i=previousComments.size()-1; i >= 0; i--) {
			combo.add(((String)previousComments.get(i)));
		}
		combo.select(0);
	}

	public void setStartRevision(SVNRevision.Number startRevision) {
		this.startRevision = startRevision;
	}

	public void setEndRevision(SVNRevision.Number endRevision) {
		this.endRevision = endRevision;
	}

	public void setSearchAll(boolean searchAll) {
		this.searchAll = searchAll;
	}

	public boolean isRevisionsChanged() {
		return revisionsChanged;
	}
	
}
