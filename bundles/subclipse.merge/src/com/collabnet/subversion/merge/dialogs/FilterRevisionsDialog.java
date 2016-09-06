package com.collabnet.subversion.merge.dialogs;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeSet;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.ui.dialogs.DateSelectionDialog;
import org.tigris.subversion.subclipse.ui.dialogs.HistoryDialog;
import org.tigris.subversion.svnclientadapter.SVNRevision;

import com.collabnet.subversion.merge.Messages;

public class FilterRevisionsDialog extends TrayDialog {

	private ISVNRemoteResource remoteResource;
	private Combo authors;
	private Text commentText;
	private Text startDateText;
	private Text endDateText;
	private Button regExpButton;

	private Text startRevisionText;
	private Button startLogButton;
	private Text endRevisionText;
	private Button endLogButton;
	private TreeSet<String> allAuthorSet;
	
	private Button clearButton;

	private String selectedAuthor;
	private String comment;
	private Date startDate;
	private Date endDate;
	private SVNRevision.Number startRevision;
	private SVNRevision.Number endRevision;
	private boolean regExp;

	private final static String ALL_AUTHORS = Messages.FilterRevisionsDialog_0;

	public FilterRevisionsDialog(Shell shell) {
		super(shell);
	}

	public FilterRevisionsDialog(Shell shell, TreeSet<String> allAuthorsSet) {
		super(shell);
		this.allAuthorSet = allAuthorsSet;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Messages.FilterRevisionsDialog_filter); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite top = new Composite(composite, SWT.NULL);
		GridLayout topLayout = new GridLayout();
		top.setLayout(topLayout);
		top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		regExpButton = new Button(top, SWT.CHECK);
		regExpButton.setText(Messages.FilterRevisionsDialog_regExp); //$NON-NLS-1$
		GridData gdRegExpButton = new GridData(GridData.FILL_HORIZONTAL);
		regExpButton.setLayoutData(gdRegExpButton);
		regExpButton.setSelection(regExp);

		Group search = new Group(composite, SWT.NONE);
		search.setText(Messages.FilterRevisionsDialog_searchTitle); //$NON-NLS-1$
		GridLayout searchLayout = new GridLayout();
		searchLayout.numColumns = 2;
		search.setLayout(searchLayout);
		search.setLayoutData(new GridData(GridData.FILL_BOTH
				| GridData.GRAB_HORIZONTAL));

		Label userLabel = new Label(search, SWT.NONE);
		userLabel.setText(Messages.FilterRevisionsDialog_user); //$NON-NLS-1$
		authors = new Combo(search, SWT.BORDER | SWT.READ_ONLY);
		authors.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
				| GridData.GRAB_HORIZONTAL));
		if (allAuthorSet != null && allAuthorSet.size() > 0) {
			authors.setItems(allAuthorSet.toArray(new String[0]));
			if (selectedAuthor != null && allAuthorSet.contains(selectedAuthor)) {
				authors.setText(selectedAuthor);
			} else {
				authors.setText(ALL_AUTHORS);
			}
			if (allAuthorSet.size() == 1) {
				Iterator<String> it = allAuthorSet.iterator();
				String firstElement = null;
				while (it.hasNext()) {
					firstElement = it.next();
					break;
				}
				authors.setText(firstElement);
				authors.setEnabled(false);
			}
		}

		Label commentLabel = new Label(search, SWT.NONE);
		commentLabel.setText(Messages.FilterRevisionsDialog_comment); //$NON-NLS-1$
		commentText = new Text(search, SWT.BORDER);
		commentText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
				| GridData.GRAB_HORIZONTAL));

		if (comment != null) {
			commentText.setText(comment);
		}
		final Label dateLabel = new Label(search, SWT.NONE);
		dateLabel.setText(Messages.FilterRevisionsDialog_date); //$NON-NLS-1$
		Composite dateComp = new Composite(search, SWT.NONE);
		GridLayout dateCompLayout = new GridLayout();
		dateCompLayout.numColumns = 5;
		dateCompLayout.marginHeight = 0;
		dateCompLayout.marginWidth = 0;
		dateComp.setLayout(dateCompLayout);
		dateComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		startDateText = new Text(dateComp, SWT.BORDER);
		startDateText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (startDate != null) {
			startDateText.setText(formatDate(startDate));
			startDateText.setData(startDate);
		}
		final Button selectStartDateButton = new Button(dateComp, SWT.NONE);
		selectStartDateButton
				.setText(Messages.FilterRevisionsDialog_startDateButton);
		final Label midDataLabel = new Label(dateComp, SWT.NONE);
		midDataLabel.setText(Messages.FilterRevisionsDialog_midDate); //$NON-NLS-1$
		endDateText = new Text(dateComp, SWT.BORDER);
		endDateText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (endDate != null) {
			endDateText.setText(formatDate(endDate));
			endDateText.setData(endDate);
		}
		final Button selectEndDateButton = new Button(dateComp, SWT.NONE);
		selectEndDateButton
				.setText(Messages.FilterRevisionsDialog_endDateButton); //$NON-NLS-1$

		final Label revisionLabel = new Label(search, SWT.NONE);
		revisionLabel.setText(Messages.FilterRevisionsDialog_revision); //$NON-NLS-1$
		Composite revisionComp = new Composite(search, SWT.NONE);
		GridLayout revisionCompLayout = new GridLayout();
		revisionCompLayout.numColumns = 5;
		revisionCompLayout.marginHeight = 0;
		revisionCompLayout.marginWidth = 0;
		revisionComp.setLayout(revisionCompLayout);
		revisionComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		startRevisionText = new Text(revisionComp, SWT.BORDER);
		startRevisionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (startRevision != null)
			startRevisionText.setText(startRevision.toString());
		startLogButton = new Button(revisionComp, SWT.PUSH);
		startLogButton
				.setText(Messages.FilterRevisionsDialog_startRevisionButton); //$NON-NLS-1$
		final Label endRevisionLabel = new Label(revisionComp, SWT.NONE);
		endRevisionLabel.setText(Messages.FilterRevisionsDialog_endRevision); //$NON-NLS-1$
		endRevisionText = new Text(revisionComp, SWT.BORDER);
		endRevisionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (endRevision != null)
			endRevisionText.setText(endRevision.toString());
		endLogButton = new Button(revisionComp, SWT.PUSH);
		endLogButton.setText(Messages.FilterRevisionsDialog_endRevisionButton); //$NON-NLS-1$
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
				DateSelectionDialog dsd = new DateSelectionDialog(getShell(),
						start); //$NON-NLS-1$
				if (dsd.open() == Window.OK) {
					Date startDate = dsd.getDate();
					startDateText.setText(formatDate(startDate));
					startDateText.setData(startDate);
					if (endDateText.getText().trim().length() > 0) {
						Date endDate = (Date) endDateText.getData();
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
				DateSelectionDialog dsd = new DateSelectionDialog(getShell(),
						end); //$NON-NLS-1$
				if (dsd.open() == Window.OK) {
					Date endDate = dsd.getDate();
					endDateText.setText(formatDate(endDate));
					endDateText.setData(endDate);
					if (startDateText.getText().trim().length() > 0) {
						Date startDate = (Date) startDateText.getData();
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
				if (e.getSource() == startLogButton)
					showLog(startRevisionText);
				else
					showLog(endRevisionText);
			}
		};
		startLogButton.addSelectionListener(selectionListener);
		endLogButton.addSelectionListener(selectionListener);

		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text) e.getSource()).selectAll();
			}

			public void focusLost(FocusEvent e) {
				((Text) e.getSource())
						.setText(((Text) e.getSource()).getText());
			}
		};
		authors.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				selectedAuthor = authors.getText();
				clearButton.setEnabled(isFiltering());
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		startDateText.addFocusListener(focusListener);
		endDateText.addFocusListener(focusListener);
		
		clearButton = new Button(composite, SWT.PUSH);
		clearButton.setText(Messages.FilterRevisionsDialog_1);
		clearButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		clearButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				authors.setText(ALL_AUTHORS);
				selectedAuthor = ALL_AUTHORS;
				commentText.setText(Messages.FilterRevisionsDialog_2);
				startDateText.setText(""); //$NON-NLS-1$
				endDateText.setText(""); //$NON-NLS-1$
				startRevisionText.setText(""); //$NON-NLS-1$
				endRevisionText.setText(""); //$NON-NLS-1$
				clearButton.setEnabled(false);
			}
		});
		
		ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				clearButton.setEnabled(isFiltering());
			}
		};
		commentText.addModifyListener(modifyListener);
		startDateText.addModifyListener(modifyListener);
		endDateText.addModifyListener(modifyListener);
		startRevisionText.addModifyListener(modifyListener);
		endRevisionText.addModifyListener(modifyListener);
		
		clearButton.setEnabled(isFiltering());

		return composite;
	}

	/**
	 * Called when the OK-button of the dialog is pressed. The data entered in
	 * the fields is stored in attributes. The attributes can later be accessed
	 * by calls to the get-functions.
	 */
	protected void okPressed() {

		Date tempStartDate = null;
		Date tempEndDate = null;
		if (startDateText.getText().trim().length() == 0)
			startDate = null;
		else {
			tempStartDate = parseDate(startDateText.getText());
			if (tempStartDate == null)
				return;
		}
		if (endDateText.getText().trim().length() == 0)
			endDate = null;
		else {
			tempEndDate = parseDate(endDateText.getText());
			if (tempEndDate == null)
				return;
		}
		if (tempStartDate != null && tempEndDate != null) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(tempEndDate);
			calendar.set(Calendar.HOUR_OF_DAY, calendar
					.getActualMaximum(Calendar.HOUR_OF_DAY));
			calendar.set(Calendar.MINUTE, calendar
					.getActualMaximum(Calendar.MINUTE));
			calendar.set(Calendar.SECOND, calendar
					.getActualMaximum(Calendar.SECOND));
			tempEndDate = calendar.getTime();
			if (!tempEndDate.after(tempStartDate)) {
				MessageDialog
						.openError(
								getShell(),
								Messages.FilterRevisionsDialog_dateSequenceErrorTitle,
								Messages.FilterRevisionsDialog_dateSequenceErrorMessage); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}
		}
		endDate = tempEndDate;
		startDate = tempStartDate;
		if (startRevisionText.getText().trim().length() > 0) {
			try {
				startRevision = (SVNRevision.Number) SVNRevision
						.getRevision(startRevisionText.getText().trim());
			} catch (ParseException e) {
				MessageDialog
						.openError(
								getShell(),
								Messages.FilterRevisionsDialog_revisionParseErrorTitle,
								Messages.FilterRevisionsDialog_revisionParseErrorMessage); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}
		} else
			startRevision = null;
		if (endRevisionText.getText().trim().length() > 0) {
			try {
				endRevision = (SVNRevision.Number) SVNRevision
						.getRevision(endRevisionText.getText().trim());
			} catch (ParseException e) {
				MessageDialog
						.openError(
								getShell(),
								Messages.FilterRevisionsDialog_revisionParseErrorTitle,
								Messages.FilterRevisionsDialog_revisionParseErrorMessage); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}
		} else
			endRevision = null;
		if (startRevision != null && endRevision != null
				&& startRevision.getNumber() > endRevision.getNumber()) {
			MessageDialog
					.openError(
							getShell(),
							Messages.FilterRevisionsDialog_revisionSequenceErrorTitle,
							Messages.FilterRevisionsDialog_revisionSequenceErrorMessage); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		if (commentText.getText().trim().length() > 0)
			comment = commentText.getText();
		else
			comment = null;
		if (selectedAuthor != null && selectedAuthor.equals(ALL_AUTHORS)) {
			selectedAuthor = null;
		}
		regExp = regExpButton.getSelection();
		super.okPressed();
	}

	/**
	 * Parses a date in a string to a Date
	 *
	 * @param date
	 *            date string to be parsed
	 * @return a new Date object which represents the parsed date
	 */
	private Date parseDate(String date) {
		try {
			return DateFormat.getDateInstance(DateFormat.SHORT).parse(date);
		} catch (ParseException e) {
			MessageDialog.openError(getShell(),
					Messages.FilterRevisionsDialog_dateParseErrorTitle,
					Messages.FilterRevisionsDialog_dateParseErrorMessage); //$NON-NLS-1$ //$NON-NLS-2$			return null;
			return null;
		}
	}

	private void showLog(Text revisionText) {
		HistoryDialog dialog = new HistoryDialog(getShell(), remoteResource);
		if (dialog.open() == HistoryDialog.CANCEL)
			return;
		ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
		if (selectedEntries.length == 0)
			return;
		revisionText.setText(Long
				.toString(selectedEntries[selectedEntries.length - 1]
						.getRevision().getNumber()));
	}

	@Override
	public boolean isHelpAvailable() {
		return false;
	}

	/**
	 * Format a date in the local representation.
	 *
	 * @param date
	 *            date to be formatted
	 * @return a string containing the formatted date
	 */
	private String formatDate(Date date) {
		return DateFormat.getDateInstance(DateFormat.SHORT).format(date);
	}

	public void setRemoteResource(ISVNRemoteResource remoteResource) {
		this.remoteResource = remoteResource;
	}

	public String getUser() {
		return selectedAuthor;
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
	
	private boolean isFiltering() {
		return commentText.getText().length() > 0 ||
				startDateText.getText().length() > 0 ||
				endDateText.getText().length() > 0 ||
				startRevisionText.getText().length() > 0 ||
				endRevisionText.getText().length() > 0 ||
				!authors.getText().equals(ALL_AUTHORS);
	}
}
