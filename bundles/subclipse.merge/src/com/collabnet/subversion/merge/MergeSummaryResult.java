/**
 * ***************************************************************************** Copyright (c) 2009
 * CollabNet. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: CollabNet - initial API and implementation
 * ****************************************************************************
 */
package com.collabnet.subversion.merge;

public class MergeSummaryResult {
  private int type;
  private String category;
  private String number;

  public static final int FILE = 0;
  public static final int PROPERTY = 1;
  public static final int TREE = 2;

  public MergeSummaryResult(int type, String category, String number) {
    super();
    this.type = type;
    this.category = category;
    this.number = number;
  }

  public MergeSummaryResult(String mergeSummaryResult) {
    type = Integer.parseInt(mergeSummaryResult.substring(22, 23));
    int categoryIndex = mergeSummaryResult.indexOf("category: "); // $NON-NLS-1$
    int numberIndex = mergeSummaryResult.indexOf("number: "); // $NON-NLS-1$
    category = mergeSummaryResult.substring(categoryIndex + 10, numberIndex - 1);
    number = mergeSummaryResult.substring(numberIndex + 8).trim();
  }

  public int getType() {
    return type;
  }

  public String getCategory() {
    return category;
  }

  public String getNumber() {
    return number;
  }

  public String toString() {
    return "Summary result: type: "
        + type
        + " category: "
        + category
        + " number: "
        + number; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  }
}
