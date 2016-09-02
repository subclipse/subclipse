package org.tigris.subversion.svnclientadapter.javahl;

import java.util.ArrayList;
import java.util.List;

import org.apache.subversion.javahl.DiffSummary;
import org.apache.subversion.javahl.callback.DiffSummaryCallback;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary;

public class JhlDiffSummaryReceiver implements DiffSummaryCallback {
	
	List<SVNDiffSummary> summary = new ArrayList<SVNDiffSummary>();

	public void onSummary(DiffSummary descriptor) {
		summary.add(JhlConverter.convert(descriptor));

	}

	public SVNDiffSummary[] getDiffSummary() {
		SVNDiffSummary[] diffSummary = new SVNDiffSummary[summary.size()];
		summary.toArray(diffSummary);
		return diffSummary;
	}

}
