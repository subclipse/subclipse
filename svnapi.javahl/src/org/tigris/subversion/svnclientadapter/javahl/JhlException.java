package org.tigris.subversion.svnclientadapter.javahl;

import org.apache.subversion.javahl.SubversionException;
import org.tigris.subversion.svnclientadapter.SVNClientException;

public class JhlException extends SubversionException {

	private static final long serialVersionUID = 328804379812661422L;

	protected JhlException(String message) {
		super(message);
	}
	
	public JhlException(SVNClientException e) {
		super(e.getMessage());
	}

}
