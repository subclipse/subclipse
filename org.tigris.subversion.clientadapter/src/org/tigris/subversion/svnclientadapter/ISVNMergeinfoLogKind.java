package org.tigris.subversion.svnclientadapter;

public interface ISVNMergeinfoLogKind {
    /** does not exist */
    public static final int eligible = 0;

    /** exists, but uninteresting */
    public static final int merged = 1;

}
