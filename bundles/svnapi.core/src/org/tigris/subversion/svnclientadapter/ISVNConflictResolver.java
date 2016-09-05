package org.tigris.subversion.svnclientadapter;

public interface ISVNConflictResolver {
    /**
     * The callback method invoked for each conflict during a
     * merge/update/switch operation.
     *
     * @param descrip A description of the conflict.
     * @return The result of any conflict resolution.
     * @throws SubversionException If an error occurs.
     */
    public SVNConflictResult resolve(SVNConflictDescriptor descrip) throws SVNClientException;

    /**
     * From JavaHL
     */
    public final class Choice
    {
        /**
         * User did nothing; conflict remains.
         */
        public static final int postpone = 0;

        /**
         * User chooses the base file.
         */
        public static final int chooseBase = 1;

        /**
         * User chooses the repository file.
         */
        public static final int chooseTheirsFull = 2;

        /**
         * User chooses own version of file.
         */
        public static final int chooseMineFull = 3;

        /**
         * Resolve the conflict by choosing the incoming (repository)
         * version of the object (for conflicted hunks only).
         */
        public static final int chooseTheirs = 4;

        /**
         * Resolve the conflict by choosing own (local) version of the
         * object (for conflicted hunks only).
         */
        public static final int chooseMine = 5;

        /**
         * Resolve the conflict by choosing the merged object
         * (potentially manually edited).
         */
        public static final int chooseMerged = 6;
    }

}
