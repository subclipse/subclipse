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
package org.tigris.subversion.subclipse.ui.history;

import java.util.Date;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * The HistorySearchViewerFilter is a ViewerFilter that can be applied
 * to the table of the history search in order to filter entries from 
 * the table. Entries matching the select-method will be displayed in the
 * table. The entries that don't match will be hidden.
 * 
 * The filter takes a user-string, comment-string, start date and end date
 * as parameters. All entries matching these parameters will be selected.
 * If any of the parameters is null the parameter is treated as a don't care.
 * 
 * Using '*', '?' and '\' simple pattern matching can be used for the user and
 * comment fields. * is used for any string, ? for any character and \ is the 
 * escape character.
 */
public class HistorySearchViewerFilter extends ViewerFilter {

	private final String user;
	private final String comment;
	private final Date startDate;
	private final Date endDate;
	private final boolean regExp;
	private final SVNRevision.Number startRevision;
	private final SVNRevision.Number endRevision;

	private final char CHAR_MATCH_MANY = '*';
	private final char CHAR_MATCH_ONE = '?';
	private final char CHAR_MATCH_ESCAPE = '\\';
	
	/***
	 * Construct a new HistorySearchViewerFilter
	 * @param user user to filter on
	 * @param comment comment to filter on
	 * @param startDate filtered entries must be dated after this date
	 * @param endDate filtered entries must be dated before this date
	 * @param regExp whether or not pattern matching should be used
	 * @param startRevision
	 * @param endRevision
	 */
	public HistorySearchViewerFilter(String user, String comment,
			Date startDate, Date endDate, boolean regExp, SVNRevision.Number startRevision, SVNRevision.Number endRevision) {
		this.user = user;
		this.comment = comment;
		this.startDate = startDate;
		this.endDate = endDate;
		this.regExp = regExp;
		this.startRevision = startRevision;
		this.endRevision = endRevision;
	}

	/**
     * Returns whether the given element matches the user, comment, start and 
     * end date of this filter.
     * @param viewer the viewer
     * @param parentElement the parent element
     * @param element the element
     * @return <code>true</code> if element is included in the
     *   filtered set, and <code>false</code> if excluded
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof ILogEntry) {
			ILogEntry logEntry = (ILogEntry)element;
			return (filterDate(logEntry, startDate, endDate)
					&& filterRevision(logEntry, startRevision, endRevision)
					&& filterUser(logEntry, user) 
					&& filterComment(logEntry, comment));
		}
		return false;
	}

	/**
	 * Returns if the logEntry's author matches the given user string. 
	 * @param logEntry log entry to be checked
	 * @param user user string to be filtered on
	 * @return <code>true</code> iff the logEntry matches the user string, 
	 * 		   <code>false</code> otherwise
	 */
	private boolean filterUser(ILogEntry logEntry, String user) {
		if (user == null) return true;
		if (regExp) {
			return matchesExpression(logEntry.getAuthor(), user + CHAR_MATCH_MANY);
		}
		else {
			return containsExpression(logEntry.getAuthor(), user);
		}
	}

	/**
	 * Returns if the logEntry's comment matches the given comment string. 
	 * @param logEntry log entry to be checked
	 * @param comment comment string to be filtered on
	 * @return <code>true</code> iff the logEntry matches the comment string, 
	 * 		   <code>false</code> otherwise
	 */
	private boolean filterComment(ILogEntry logEntry, String comment) {
		if (comment == null) return true;
		if (regExp) {
			return matchesExpression(logEntry.getComment(), comment + CHAR_MATCH_MANY);
		}
		else {
			return containsExpression(logEntry.getComment(), comment);
		}
	}
	
	/**
	 * Returns if the logEntry's date matches the given start and end date. 
	 * @param logEntry log entry to be checked
	 * @param startDate start date to be filtered on
	 * @param endDate end date to be filtered on
	 * @return <code>true</code> iff the logEntry matches the start and end date, 
	 * 		   <code>false</code> otherwise
	 */
	private boolean filterDate(ILogEntry logEntry, Date startDate, Date endDate) {
		Date date = logEntry.getDate();
		if (date == null) {
			return true;
		}
		if ((startDate != null) && date.before(startDate)) {
			return false;
		}
		if ((endDate != null) && date.after(endDate)) {
			return false;
		}
		return true;
	}
	
	private boolean filterRevision(ILogEntry logEntry, SVNRevision.Number startRevision, SVNRevision.Number endRevision) {
		SVNRevision.Number revision = logEntry.getRevision();
		if (revision == null) return true;
		if (startRevision != null && revision.getNumber() < startRevision.getNumber()) return false;
		if (endRevision != null && revision.getNumber() > endRevision.getNumber()) return false;
		return true;
	}

	/**
	 * Checks if the toSearch string matches the given expression.
	 * @param toSearch string to be searched
	 * @param expression string to match the toSearch string against
	 * @return <code>true</code> iff toSearch matches expression,
	 *         <code>false</code> otherwise
	 */
	private boolean matchesExpression(String toSearch, String expression) {
		if (toSearch == null || expression == null) {
			return false;
		}
		int toSearchIndex = 0;
		int exprIndex = 0;
		while ((exprIndex < expression.length()) && (toSearchIndex < toSearch.length())) {
			// Match the escape operator
			if (expression.charAt(exprIndex) == CHAR_MATCH_ESCAPE) {
				exprIndex++;
				
				if (exprIndex == expression.length()) {
					return false;
				}
				if (expression.charAt(exprIndex) == toSearch.charAt(toSearchIndex)) {
					exprIndex++;
					toSearchIndex++;
				}
				else {
					return false;
				}
			}
			// Match the many operator
			else if (expression.charAt(exprIndex) == CHAR_MATCH_MANY) {
				String partExpr = expression.substring(exprIndex+1);
				for (int j=toSearchIndex; j<=toSearch.length(); j++) {
					if (matchesExpression(toSearch.substring(j), partExpr)) {
						return true;
					}
				}
				return false;
			}
			// Match the one random character operator
			else if (expression.charAt(exprIndex) == CHAR_MATCH_ONE) {
				return matchesExpression(toSearch.substring(toSearchIndex+1), expression.substring(exprIndex+1));
			}
			// Check if the character matches the expressions character
			else if (toSearch.charAt(toSearchIndex) == expression.charAt(exprIndex)) {
				toSearchIndex++;
				exprIndex++;
			}
			// No matches, so return false
			else {
				return false;
			}
		}
		for (int i=exprIndex; i<expression.length(); i++) {
			if (expression.charAt(i) != CHAR_MATCH_MANY) {
				break;
			}
			exprIndex++;
		}
		
		return ((toSearch.length() == toSearchIndex) && (expression.length() == exprIndex)); 
	}
	
	/**
	 * Checks if the toSearch string contains the expression.
	 * @param toSearch string to be checked for containing the expression
	 * @param expression expression the toSearch string might match
	 * @return <code>true</code> iff toSearch contains expression,
	 *         <code>false</code> otherwise
	 */
	private boolean containsExpression(String toSearch, String expression) {
		if (toSearch == null) {
			return false;
		}
		return toSearch.equals(expression) || toSearch.indexOf(expression) >= 0;
	}

	public SVNRevision.Number getStartRevision() {
		return startRevision;
	}

	public SVNRevision.Number getEndRevision() {
		return endRevision;
	}
	
}
