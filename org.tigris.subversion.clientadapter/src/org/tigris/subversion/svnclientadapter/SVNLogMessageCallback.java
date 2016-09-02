package org.tigris.subversion.svnclientadapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


public class SVNLogMessageCallback implements ISVNLogMessageCallback {
	
	private List messages = new ArrayList();
	private Stack stack = new Stack();

	public void singleMessage(ISVNLogMessage msg) {
		if (msg == null) {
			if (!stack.empty())
				stack.pop();
			return;
		}
		if (stack.empty()) {
				messages.add(msg);
		} else {
			ISVNLogMessage current = (ISVNLogMessage) stack.peek();
			current.addChild(msg);
		}
		if (msg.hasChildren())
			stack.push(msg);
	}

	public ISVNLogMessage[] getLogMessages() {
		ISVNLogMessage[] array = new ISVNLogMessage[messages.size()];
		return (ISVNLogMessage[]) messages.toArray(array);
	}

}
