package org.tigris.subversion.svnclientadapter.javahl;

import java.util.ArrayList;
import java.util.List;

import org.apache.subversion.javahl.types.Info;
import org.apache.subversion.javahl.callback.InfoCallback;
import org.tigris.subversion.svnclientadapter.ISVNInfo;

public class JhlInfoCallback implements InfoCallback {

	List<ISVNInfo> items = new ArrayList<ISVNInfo>();
	
	public void singleInfo(Info info) {
		items.add(new JhlInfo2(info.getPath(), info));
	}
	
	public ISVNInfo[] getInfo() {
		ISVNInfo[] itemArray = new ISVNInfo[items.size()];
		return items.toArray(itemArray);
	}

}
