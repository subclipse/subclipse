package org.tigris.subversion.svnclientadapter.javahl;

import java.util.Set;

import org.apache.subversion.javahl.CommitItem;
import org.apache.subversion.javahl.callback.CommitMessageCallback;

public class JhlCommitMessage implements CommitMessageCallback {

	private String message;
	
	public JhlCommitMessage(String message) {
		super();
		this.message = message;
	}


	public String getLogMessage(Set<CommitItem> elementsToBeCommited) {
		return message;
	}

}
