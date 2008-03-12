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
import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.util.DaySelectionCanvas;

/**
 * Dialog for selecting a date. The dialog can be used to select
 * a day, month and year. The dialog is based on the GregorianCalendar.
 * Double clicking the day will result in closing the dialog and
 * storing the selected date.
 */
public class DateSelectionDialog extends Dialog {

	private Date date;
	private Composite dateComp;
	private DaySelectionCanvas daysComp;
	private Spinner yearSpinner;
	private Combo monthCombo;
	private boolean refreshing;
	
	/**
	 * Constructs a new dialog for selecting a date
	 * @param shell shell used for this dialog
	 * @param dateString date to be initially selected in the dialog 
	 */
	public DateSelectionDialog(Shell parent, String dateString) {
		super(parent);
		this.setShellStyle(SWT.CLOSE);
		this.date = parseDate(dateString);
	}
	
	public Date getDate() {
		return date;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		getShell().setText(Policy.bind("DateSelectionDialog.title")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		
		// Upper component for displaying the month and year
		dateComp = new Composite(composite, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		dateComp.setLayout(gridLayout);
		dateComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		monthCombo = new Combo(dateComp, SWT.SINGLE | SWT.READ_ONLY);
		monthCombo.add(Policy.bind("DateSelectionDialog.january")); //$NON-NLS-1$
		monthCombo.add(Policy.bind("DateSelectionDialog.february")); //$NON-NLS-1$
		monthCombo.add(Policy.bind("DateSelectionDialog.march")); //$NON-NLS-1$
		monthCombo.add(Policy.bind("DateSelectionDialog.april")); //$NON-NLS-1$
		monthCombo.add(Policy.bind("DateSelectionDialog.may")); //$NON-NLS-1$
		monthCombo.add(Policy.bind("DateSelectionDialog.june")); //$NON-NLS-1$
		monthCombo.add(Policy.bind("DateSelectionDialog.july")); //$NON-NLS-1$
		monthCombo.add(Policy.bind("DateSelectionDialog.august")); //$NON-NLS-1$
		monthCombo.add(Policy.bind("DateSelectionDialog.september")); //$NON-NLS-1$
		monthCombo.add(Policy.bind("DateSelectionDialog.october")); //$NON-NLS-1$
		monthCombo.add(Policy.bind("DateSelectionDialog.november")); //$NON-NLS-1$
		monthCombo.add(Policy.bind("DateSelectionDialog.december")); //$NON-NLS-1$
		monthCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		Calendar calendar = DateFormat.getDateTimeInstance().getCalendar();
		calendar.setTime(date);
		yearSpinner = new Spinner(dateComp, SWT.BORDER);
		yearSpinner.setValues(calendar.get(Calendar.YEAR), 
				calendar.getActualMinimum(Calendar.YEAR), 
				calendar.getActualMaximum(Calendar.YEAR), 
				0, 1, 1);
		yearSpinner.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
		
		// Lower component for displaying the day selection
		daysComp = new DaySelectionCanvas(composite, SWT.BORDER);
		daysComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		String[] header = new String[7];
		header[0] = Policy.bind("DateSelectionDialog.monday"); //$NON-NLS-1$
		header[1] = Policy.bind("DateSelectionDialog.tuesday"); //$NON-NLS-1$
		header[2] = Policy.bind("DateSelectionDialog.wednesday"); //$NON-NLS-1$
		header[3] = Policy.bind("DateSelectionDialog.thursday"); //$NON-NLS-1$
		header[4] = Policy.bind("DateSelectionDialog.friday"); //$NON-NLS-1$
		header[5] = Policy.bind("DateSelectionDialog.saturday"); //$NON-NLS-1$
		header[6] = Policy.bind("DateSelectionDialog.sunday"); //$NON-NLS-1$
		daysComp.setHeader(header);

		monthCombo.addModifyListener(new ModifyListener() {
			/*
			 * (non-Javadoc)
			 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
			 */
			public void modifyText(ModifyEvent e) {
				if (!refreshing) refreshDays(monthCombo.getSelectionIndex(), yearSpinner.getSelection());
			}
		});
		yearSpinner.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			public void widgetDefaultSelected(SelectionEvent e) {
				if (!refreshing) refreshDays(monthCombo.getSelectionIndex(), yearSpinner.getSelection());
			}
		});
		yearSpinner.addFocusListener(new FocusAdapter() {
			/*
			 * (non-Javadoc)
			 * @see org.eclipse.swt.events.FocusAdapter#focusLost(org.eclipse.swt.events.FocusEvent)
			 */
			public void focusLost(FocusEvent e) {
				refreshDays(monthCombo.getSelectionIndex(), yearSpinner.getSelection());
			}
		});
		yearSpinner.addMouseListener(new MouseAdapter() {
			/*
			 * (non-Javadoc)
			 * @see org.eclipse.swt.events.MouseAdapter#mouseDown(org.eclipse.swt.events.MouseEvent)
			 */
			public void mouseDown(MouseEvent e) {
				refreshDays(monthCombo.getSelectionIndex(), yearSpinner.getSelection());
			}
		});
		daysComp.addMouseListener(new MouseAdapter() {
			/*
			 * (non-Javadoc)
			 * @see org.eclipse.swt.events.MouseAdapter#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
			 */
			public void mouseDoubleClick(MouseEvent e) {
				int day = daysComp.getSelectedDay();
				if (day > 0) {
					Calendar calendar = DateFormat.getDateTimeInstance().getCalendar();
					calendar.setTime(date);
					calendar.set(Calendar.DAY_OF_MONTH, day);
					calendar.set(Calendar.MONTH, monthCombo.getSelectionIndex());
					calendar.set(Calendar.YEAR, yearSpinner.getSelection());
					date = calendar.getTime();
					close();
				}
			}
		});
		
		// Display the date
		refreshDate();
		
		return composite;
	}
	
	/**
	 * Parse the date string to a new Date object. If the date
	 * string is not parsable the current date is returned.
	 * @param date date string to be parsed
	 * @return Date object representing the date string
	 */
	private Date parseDate(String date) {
		Calendar calendar = Calendar.getInstance();
		try {
			calendar.setTime(DateFormat.getDateInstance(DateFormat.SHORT).parse(date));
		} catch (ParseException e) {
		}
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		return calendar.getTime();
	}
	
	/**
	 * Refresh the days displayed in this dialog. The days of the given
	 * month combined with the year will be displayed.
	 * @param month month for which to display the days
	 * @param year year for which to display the days
	 */
	private void refreshDays(int month, int year) {
		Calendar calendar = DateFormat.getDateTimeInstance().getCalendar();
		calendar.setTime(date);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.MONTH, month);
		calendar.set(Calendar.YEAR, year);
		int day = daysComp.getSelectedDay();
		if (day > 0) {
			if (day > calendar.getActualMaximum(Calendar.DAY_OF_MONTH)) {
				day = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
			}
			calendar.set(Calendar.DAY_OF_MONTH, day);
		}
		date = calendar.getTime();
		refreshDate();
	}
	
	/**
	 * Sets the days to be displayed. The last days of the
	 * previous month are included, just like the first days 
	 * of the next month.
	 * 
	 * The size of a DaySelectionCanvas is assumed to be 6 x 7 (rows x columns).
	 */
	private void refreshDate() {
		refreshing = true;
		Calendar calendar = DateFormat.getDateTimeInstance().getCalendar();
		calendar.setTime(date);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		yearSpinner.setSelection(calendar.get(Calendar.YEAR));
		monthCombo.select(calendar.get(Calendar.MONTH));

		int maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		calendar.add(Calendar.MONTH, -1);
		int prevMaxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		calendar.add(Calendar.MONTH, 1);
		
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		int weekDay = calendar.get(Calendar.DAY_OF_WEEK);
		int convertedWeekDay = (weekDay + 5) % 7;
		boolean addFirstWeek = false;
		int remainingDays = (42 - maxDays) - convertedWeekDay;
		if ((remainingDays - convertedWeekDay) > 7) {
			addFirstWeek = true;
		}
		int[] days = new int[42];
		int i=0;
		int numPrefix = convertedWeekDay; 
		if (addFirstWeek) {
			numPrefix += 7;
		}
		for (; i < numPrefix; i++) {
			days[i] = prevMaxDays - numPrefix + i + 1;
		}
		int numMonth = numPrefix + maxDays;
		for (; i < numMonth; i++) {
			days[i] = i - numPrefix + 1;
		}
		for (; i < days.length; i++) {
			days[i] = i - numMonth + 1;
		}
		
		daysComp.setDays(days);
		daysComp.setSelectedDay(day);
		refreshing = false;
	}
	
}
