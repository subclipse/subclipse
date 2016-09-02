package org.tigris.subversion.svnclientadapter.javahl;

import org.apache.subversion.javahl.ConflictDescriptor;
import org.apache.subversion.javahl.ConflictResult;
import org.apache.subversion.javahl.SubversionException;
import org.apache.subversion.javahl.callback.ConflictResolverCallback;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNConflictResult;

public class JhlConflictResolver implements ConflictResolverCallback {
	
	ISVNConflictResolver worker;

	public JhlConflictResolver(ISVNConflictResolver worker) {
		super();
		this.worker = worker;
	}

	public ConflictResult resolve(ConflictDescriptor descrip)
			throws SubversionException {
		try {
			SVNConflictResult svnConflictResult = worker.resolve(JhlConverter.convertConflictDescriptor(descrip));
			return new ConflictResult(JhlConverter.convert(svnConflictResult), svnConflictResult.getMergedPath());
		} catch (SVNClientException e) {
			throw new JhlException(e);
		}
	}

}
