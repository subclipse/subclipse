package org.tigris.subversion.subclipse.graph.cache;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageCallback;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class Cache {
	
	private static final int MAX_LOG_MESSAGES = 1024;
	
	private File revisionsFile;
	private File logMessagesFile;

	private File root;
	
	private List children = new ArrayList();
	private int level;
	private long refreshRevision;

	// used in updates
	private RandomAccessFile revisionsRaf = null;
	private RandomAccessFile logMessagesRaf = null;
	
	// used in refresh
	private File revisionsTempFile;
	private File logMessagesTempFile;
	private RandomAccessFile revisionsTempRaf = null;
	private RandomAccessFile logMessagesTempRaf = null;	
	
	private List writtenChildren = new ArrayList();
	
	public Cache(File f, String uuid) {
		createDirectory(f);
		f = new File(f, uuid);
		createDirectory(f);
		this.root = f;

		revisionsFile = new File(root, "revisions");
		logMessagesFile = new File(root, "logMessages");
	}
	
	public Cache(File f, String uuid, long refreshRevision) {
		this(f, uuid);
		this.refreshRevision = refreshRevision;
	}
	
	public void refresh(List refreshedNodes, ISVNInfo info, IProgressMonitor monitor, int unitWorked) {
		ISVNLogMessageCallback callback = new ISVNLogMessageCallback() {
			public void singleMessage(ISVNLogMessage message) {
				update(message, true);
			}			
		};
		
		revisionsTempFile = new File(root, "revisionsTemp");
		logMessagesTempFile = new File(root, "logMessagesTemp");
		revisionsTempFile.delete();
		logMessagesTempFile.delete();
		try {
			revisionsTempFile.createNewFile();
			logMessagesTempFile.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		List revisions = new ArrayList();
		Iterator iter = refreshedNodes.iterator();
		while (iter.hasNext()) {
			Node node = (Node)iter.next();
			revisions.add(Long.toString(node.getRevision()));
		}	
		
		startTempUpdate();
		RandomAccessFile file = null;
		try {
			ISVNClientAdapter client = SVNProviderPlugin.getPlugin().getSVNClient();
			file = new RandomAccessFile(logMessagesFile, "r");
			int revInt = new Long(getLatestRevision()).intValue();
			while(file.getFilePointer() < file.length()) {
				ISVNLogMessage lm = readNext(file, true);

				level = 0;
				
				int index = revisions.indexOf(lm.getRevision().toString());
				if (index == -1) {
					update(lm, true);
					if (lm.hasChildren() && lm.getChildMessages() != null) {
						updateChildren(lm, true);
					}	
					monitor.worked(unitWorked/revInt);
				} else {
					Node updateNode = (Node)refreshedNodes.get(index);
					SVNRevision updateRevision = new SVNRevision.Number(updateNode.getRevision());
					client.getLogMessages(new SVNUrl(info.getRepository() + updateNode.getPath()),
							updateRevision,
							updateRevision,
							updateRevision,
							false, true, 0, true,
							ISVNClientAdapter.DEFAULT_LOG_PROPERTIES,
							callback);
					monitor.worked(unitWorked);
//					updateRevision = (ISVNLogMessage)refreshedMessages.get(index);
//					monitor.worked(unitWorked);
				}
				if (monitor.isCanceled()) {
					break;
				}
			}
		} catch (Exception e) {
			
		} finally {
			closeFile(file);
		}
		finishTempUpdate();
		if (monitor.isCanceled()) {
			revisionsTempFile.delete();
			logMessagesTempFile.delete();
			return;
		}
		revisionsFile.delete();
		logMessagesFile.delete();
		revisionsTempFile.renameTo(revisionsFile);
		logMessagesTempFile.renameTo(logMessagesFile);		
	}
	
	public void refresh(List refreshedMessages, IProgressMonitor monitor, int unitWorked) {
		revisionsTempFile = new File(root, "revisionsTemp");
		logMessagesTempFile = new File(root, "logMessagesTemp");
		revisionsTempFile.delete();
		logMessagesTempFile.delete();
		try {
			revisionsTempFile.createNewFile();
			logMessagesTempFile.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		List revisions = new ArrayList();
		Iterator iter = refreshedMessages.iterator();
		while (iter.hasNext()) {
			ISVNLogMessage message = (ISVNLogMessage)iter.next();
			revisions.add(message.getRevision().toString());
		}	
		
		startTempUpdate();
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(logMessagesFile, "r");
			int revInt = new Long(getLatestRevision()).intValue();
			while(file.getFilePointer() < file.length()) {
				ISVNLogMessage lm = readNext(file, true);

				level = 0;
				
				ISVNLogMessage updateRevision = null;
				int index = revisions.indexOf(lm.getRevision().toString());
				if (index == -1) {
					updateRevision = lm;
					monitor.worked(unitWorked/revInt);
				} else {
					updateRevision = (ISVNLogMessage)refreshedMessages.get(index);
					monitor.worked(unitWorked);
//					System.out.println("Update: " + updateRevision.getRevision() + " (level =" + level + ")");
				}
				update(updateRevision, true);
				if (updateRevision.hasChildren() && updateRevision.getChildMessages() != null) {
					updateChildren(updateRevision, true);
				}	
				if (monitor.isCanceled()) {
					break;
				}
			}
		} catch (Exception e) {
			
		} finally {
			closeFile(file);
		}
		finishTempUpdate();
		if (monitor.isCanceled()) {
			revisionsTempFile.delete();
			logMessagesTempFile.delete();
			return;
		}
		revisionsFile.delete();
		logMessagesFile.delete();
		revisionsTempFile.renameTo(revisionsFile);
		logMessagesTempFile.renameTo(logMessagesFile);
	}

	private void updateChildren(ISVNLogMessage logMessage, boolean writingTempFile) {
		ISVNLogMessage[] childMessages = logMessage.getChildMessages();
		for (int j = 0; j < childMessages.length; j++) {
			update(childMessages[j], writingTempFile);
			if (childMessages[j].hasChildren() && childMessages[j].getChildMessages() != null) updateChildren(childMessages[j], writingTempFile);
		}
		update(null, writingTempFile);		
	}
	
	private void createDirectory(File f) {
		if(!f.exists()) {
			if(!f.mkdir()) {
				throw new CacheException("Couldn't create directory: "+f.getAbsolutePath());
			}
		} else if(!f.isDirectory()) {
			throw new CacheException("Should be a directory: "+f.getAbsolutePath());
		}
	}
	
	public void close() {
		closeFile(revisionsRaf);
		closeFile(logMessagesRaf);
	}
	
	public void startUpdate() {
		level = 0;
		try {
			revisionsRaf = new RandomAccessFile(revisionsFile, "rw");
			logMessagesRaf = new RandomAccessFile(logMessagesFile, "rw");
			
			if (refreshRevision == 0) {
				revisionsRaf.seek(revisionsRaf.length());
				logMessagesRaf.seek(logMessagesRaf.length());
			}
		} catch(IOException e) {
			throw new CacheException("Error while opening file", e);
		}
	}
	
	public void startTempUpdate() {
		level = 0;
		try {
			revisionsTempRaf = new RandomAccessFile(revisionsTempFile, "rw");
			logMessagesTempRaf = new RandomAccessFile(logMessagesTempFile, "rw");
		} catch(IOException e) {
			throw new CacheException("Error while opening file", e);
		}
	}
	
	private String notNull(String s) {
		if(s == null)
			return "";
		return s;
	}
	
	private void writeLogMessage(ISVNLogMessage logMessage, int level, boolean writingTempFile) throws IOException {
		RandomAccessFile revRaf = null;
		RandomAccessFile logRaf = null;
		if (writingTempFile) {
			revRaf = revisionsTempRaf;
			logRaf = logMessagesTempRaf;
		} else {
			revRaf = revisionsRaf;
			logRaf = logMessagesRaf;			
		}
		
		long revision = logMessage.getRevision().getNumber();
		long fp = logRaf.getFilePointer();
//		if (writingTempFile) System.out.println("writing rev "+revision+" at "+fp+" "+revisionsTempRaf.getFilePointer());
//		else System.out.println("writing rev "+revision+" at "+fp+" "+revisionsRaf.getFilePointer());
		revRaf.writeLong(fp);
		logRaf.writeLong(revision);
		logRaf.writeLong(logMessage.getDate().getTime());
		logRaf.writeUTF(notNull(logMessage.getAuthor()));
		logRaf.writeUTF(notNull(logMessage.getMessage()));
		
		ISVNLogMessageChangePath[] changePaths = logMessage.getChangedPaths();
		logRaf.writeInt(changePaths.length);
		
		// common starting path in all changed paths
		int cc = 0;
		if(changePaths.length > 1) {
			String a = changePaths[0].getPath();
			String b = null;
			for (int i = 1; i < changePaths.length; i++) {
				b = changePaths[i].getPath();
				cc = commonChars(a, b, cc);
				a = b;
			}
			logRaf.writeUTF(a.substring(0, cc));
		}
		
		
		for (int i = 0; i < changePaths.length; i++) {
			ISVNLogMessageChangePath changePath = changePaths[i];

			logRaf.writeChar(changePath.getAction());
			logRaf.writeUTF(changePath.getPath().substring(cc));
			long copySrcRevision = 0;
			if(changePath.getCopySrcRevision() != null && changePath.getCopySrcPath() != null) {
				copySrcRevision = changePath.getCopySrcRevision().getNumber();
				logRaf.writeLong(copySrcRevision);
				logRaf.writeUTF(changePath.getCopySrcPath());
			} else {
				logRaf.writeLong(copySrcRevision);
			}
		}
		
		if(level == 0 && (!logMessage.hasChildren())) {
			logRaf.writeInt(0);
//			System.out.println("A. Children: 0");
		}
	}
	
	public void finishUpdate() {
		closeFile(revisionsRaf);
		closeFile(logMessagesRaf);
		revisionsRaf = null;
		logMessagesRaf = null;
	}
	
	public void finishTempUpdate() {
		closeFile(revisionsTempRaf);
		closeFile(logMessagesTempRaf);
		revisionsTempRaf = null;
		logMessagesTempRaf = null;
	}
	
	public void update(ISVNLogMessage logMessage) {
		update(logMessage, false);
	}
	
	public void update(ISVNLogMessage logMessage, boolean writingTempFile) {
		try {
			if(logMessage == null) {
				if(level == 1) {
					writeChildren(level, writingTempFile);
					children.clear();
				}
				level--;
			} else {
				if(level == 1) {
					children.add(logMessage);
//					dump(logMessage, level);
				} else if(level == 0) {
					writeLogMessage(logMessage, level, writingTempFile);
//					dump(logMessage, level);
				}
				
				if(logMessage.hasChildren()) {
					level++;
				}
			}
		} catch (IOException e) {
			throw new CacheException("Error while saving log message", e);
		}
//		if(logMessage != null)
//			dump(logMessage, "\t");
	}
	
	private void writeChildren(int level, boolean writingTempFile) throws IOException {
		List nonWrittenChildren = getNonWrittenChildren();
//		logMessagesRaf.writeInt(children.size());
		
//		System.out.println("B. Children: " + nonWrittenChildren.size());
		if (writingTempFile) logMessagesTempRaf.writeInt(nonWrittenChildren.size());
		else logMessagesRaf.writeInt(nonWrittenChildren.size());
//		for (Iterator it = children.iterator(); it.hasNext();) {
		for (Iterator it = nonWrittenChildren.iterator(); it.hasNext();) {
			ISVNLogMessage logMessage = (ISVNLogMessage) it.next();
			writeLogMessage(logMessage, level, writingTempFile);
		}
	}

	private List getNonWrittenChildren() {
		List nonWrittenChildren = new ArrayList();
		for (Iterator it = children.iterator(); it.hasNext();) {
			ISVNLogMessage logMessage = (ISVNLogMessage) it.next();
			if (!writtenChildren.contains(logMessage.getRevision().toString())) {
				nonWrittenChildren.add(logMessage);
				writtenChildren.add(logMessage.getRevision().toString());
			}
		}		
		return nonWrittenChildren;
	}
	
	/*
	private void dump(ISVNLogMessage logMessage, int level) {
		char[] c = new char[level];
		Arrays.fill(c, '\t');
		String p = new String(c);
		System.out.println(p+"rev   : "+logMessage.getRevision().getNumber());
		System.out.println(p+"author: "+logMessage.getAuthor());
		ISVNLogMessageChangePath[] cps = logMessage.getChangedPaths();
		for (int i = 0; i < cps.length; i++) {
			ISVNLogMessageChangePath cp = cps[i];
			System.out.println(p+cp.getAction()+" "+cp.getPath());
			if(cp.getCopySrcPath() != null)
				System.out.println(p+"copy: "+cp.getCopySrcPath());
		}
		System.out.println(p+"has children: "+logMessage.hasChildren());
		System.out.println();
	}
	*/
	
	public long getLatestRevision() {
		return revisionsFile.length() / 8;
	}
	
	public void readNext(RandomAccessFile file, ISVNLogMessage[] buffer, int number) {
		do {
			number--;
			buffer[number] = readNext(file, true);
		} while(number > 0);
	}
	
	/**
	 * This method finds the revision number and path where the file was first created.
	 * 
	 * @param path The path of the selected file
	 * @param revision The revision number of the selected file
	 * @param listener A listener to implement a progress bar for example
	 * @return a Node object just containing the path and revision properties setted
	 */
	public Node findRootNode(String path, long revision, WorkListener listener) {
		long r = revision; // r means "current revision"
		long pr = r; // pr means "previous revision"
		RandomAccessFile logMessages = null;
		try {
			/*
			 * We need to read the logMessagesFiles backwards, from the selected revision
			 * to the root revision.
			 * It is done by jumping backwards and reading a
			 * maximmun of MAX_LOG_MESSAGES number of messages inside an array
			 * that acts as a buffer.
			 */
			logMessages = new RandomAccessFile(logMessagesFile, "r");
			ISVNLogMessage[] buffer = new ISVNLogMessage[MAX_LOG_MESSAGES];
			do {
				// We are going to jump to a previous revision.
				// Exactly MAX_LOG_MESSAGES revisions before
				r -= (MAX_LOG_MESSAGES)-1;
				if(r < 1) {
					r = 1; // Well, we cannot jump to a revision lower than 1
				}
				// It moves the file pointer to the revision. Here is where it jumps backwards
				logMessages.seek(getSeek(r));
				// It calculates how many log messages should be read
				int size = (int) (pr - r + 1); // this won't be higher than MAX_LOG_MESSAGES
				if(size == 0) break;
				// It reads the log messages to the buffer
				readNext(logMessages, buffer, size);
				
				// It iterates over all log messages
				for (int k = 0; k < size; k++) {
					ISVNLogMessage lm = buffer[k];
//					System.out.println("revision: "+lm.getRevision().getNumber());
					
					// It iterates over all changed paths
					ISVNLogMessageChangePath[] changedPaths = lm.getChangedPaths();
					for(int n=0; n<changedPaths.length; n++) {
						ISVNLogMessageChangePath cp = changedPaths[n];

						/*
						 * It is only interested on 'A' actions.
						 * If copySrcPath is not null it compares the paths to know
						 * if the changedPath is equals or parent to the current path.
						 * For example if it is finding the root node for /branches/a/foo.txt
						 * "A /branches/a from /trunk" 
						 * In this case /branches/a is parent of /branches/a/foo.txt
						 * so now we know that /branches/a/foo.txt was copied from /trunk/foo.txt
						 * If copySrcPath is null and the changed path is equal to the path
						 * we are looking for then we have found the root node.
						 */ 
						if(lm.getRevision().getNumber() <= revision && cp.getAction() == 'A') {
							if(cp.getCopySrcPath() != null) {
								if(isEqualsOrParent(cp.getPath(), Util.unescape(path))) {
									revision = lm.getRevision().getNumber();
									path = cp.getCopySrcPath() + Util.unescape(path).substring(cp.getPath().length());
									// TODO: here I could seek to 'revision'
									// because all other revisions in between will be ignored
								}
							} else {
								if(cp.getPath().equals(Util.unescape(path))) {
									revision = lm.getRevision().getNumber();

									Node node = new Node();
									node.setPath(Util.unescape(path));
									node.setRevision(revision);
									return node;
								}
							}
						}
					}

					if(listener != null)
						listener.worked();
				}
				pr = r; // previous revision is the current revision
			} while(true);
		} catch(IOException e) {
			throw new CacheException("Error while finding root node", e);
		} finally {
			closeFile(logMessages);
		}
		
		Node n = new Node();
		n.setPath(Util.unescape(path));
		n.setRevision(revision);
		return n;
	}
	
	private LogMessage readNext(RandomAccessFile file, boolean nested) {
		try {
			long revision = file.readLong();
//			System.out.println("reading revision: "+revision+" "+(file.getFilePointer()-8));
			long date = file.readLong();
			String author = file.readUTF();
			String message = file.readUTF();
			LogMessage logMessage = new LogMessage(revision, author, new Date(date), message);
			
//			System.out.println("logMessage: " + revision + " " + author + " " + new Date(date).toString() + " " + message);
			
			int length = file.readInt();
			String cp = "";
			if(length > 1) {
				cp = file.readUTF();
			}
			LogMessageChangePath[] changedPaths = new LogMessageChangePath[length];
			logMessage.setChangedPaths(changedPaths);
			for(int i=0; i<length; i++) {
				char action = file.readChar();
				String path = cp+file.readUTF();
				long copySrcRevision = file.readLong();
				String copySrcPath = null;
				if(copySrcRevision > 0) {
					copySrcPath = file.readUTF();
				}
				
				changedPaths[i] = new LogMessageChangePath(action, path, copySrcPath, copySrcRevision);
			}
//			System.out.println("changedPaths set for " + logMessage.getRevision().getNumber() + " - nested = " + nested);
			if(nested) {
				int children = file.readInt();
//				System.out.println(logMessage.getRevision().getNumber() + " children: " + children);
				if(children > 0) {
					LogMessage[] childMessages = new LogMessage[children];
					for (int i = 0; i < children; i++) {
						childMessages[i] = readNext(file, false);
					}
					logMessage.setChildMessages(childMessages);
				}
			}
//			System.out.println("return logMessage: " + logMessage.getRevision().getNumber());
			return logMessage;
		} catch (IOException e) {
			throw new CacheException("Error while reading log messages from file", e);
		}
	}
	
	public long getSeek(long revision) {
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(revisionsFile, "r");
			file.seek((revision-1)*8);
			long seek = file.readLong();
//			System.out.println("seek: " + seek);
			return seek;
		} catch (IOException e) {
			throw new CacheException("Error while reading revisions file", e);
		} finally {
			closeFile(file);
		}
	}
	
	public Graph createGraph(String rootPath, long revision, WorkListener listener) {
//		System.out.println("create graph");
		Graph graph = new Graph(rootPath);
		// root path is the first opened branch
		graph.addBranch(rootPath);
		
		long seek = getSeek(revision);
		
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(logMessagesFile, "r");
			file.seek(seek);

			while(file.getFilePointer() < file.length()) {
				ISVNLogMessage lm = readNext(file, true);

				ISVNLogMessageChangePath[] changedPaths = lm.getChangedPaths();
				String[] pa = graph.getPathsAsArray();
				Node node = null;
				for(int n=0; n<changedPaths.length; n++) {
					ISVNLogMessageChangePath cp = changedPaths[n];

					String nodePath = cp.getPath();
					String copySrcPath = cp.getCopySrcPath();

					for (int i = 0; i < pa.length; i++) {
						String branchPath = pa[i];

						//					System.out.println();
						//					System.out.println(node.getRevision());
						//					System.out.println("cpsrc: "+copySrcPath);
						//					System.out.println("nodep: "+nodePath);
						//					System.out.println("brnch: "+branchPath);

						if(copySrcPath == null) {
							if((cp.getAction() == 'A' && nodePath.equals(branchPath))
									|| (cp.getAction() == 'D' && isEqualsOrParent(nodePath, branchPath))
									|| (cp.getAction() == 'M' && nodePath.equals(branchPath))) {
								
								Branch branch = graph.getBranch(branchPath);
								if(branch.isEnded()) {
									// the branch was ended with a D action
									continue;
								}
								node = toNode(lm, cp);
								node.setParent(branch.getLastNode());
								branch.addNode(node);
								if(node.getAction() == 'D') {
									branch.end();
								}
							}
						} else if(copySrcPath != null && isEqualsOrParent(copySrcPath, branchPath)) {
							Branch branch = graph.getBranch(branchPath);
							Node source = branch.getSource(cp.getCopySrcRevision().getNumber());
							if(source == null)
								continue;
							node = toNode(lm, cp);
							node.setSource(source);
							String path = nodePath + branchPath.substring(copySrcPath.length());
							Branch newBranch = graph.getBranch(path);
							if(newBranch == null) {
								newBranch = graph.addBranch(path);
							}
							newBranch.addNode(node);
						}
					}
				}
				
				if(node != null && lm.hasChildren()) {
					ISVNLogMessage[] cm = lm.getChildMessages();
					for (int i = 0; i < cm.length; i++) {
						ISVNLogMessage child = cm[i];
						ISVNLogMessageChangePath[] cp = child.getChangedPaths();
						
						for (int j = 0; j < cp.length; j++) {
							ISVNLogMessageChangePath changePath = cp[j];
							
							for(int k=0; k<pa.length; k++) {
								String path = pa[k];
								if(path.equals(changePath.getPath())) {
									Branch branch = graph.getBranch(path);
									Node source = branch.getSource(child.getRevision().getNumber());
									if(source == null)
										continue;
									// add connection between "node" and "source"
									node.addMergedRevision(source);
								}
							}
						}
					}
				}
				
				
				if(listener != null)
					listener.worked();
			}
		} catch(IOException e) {
			throw new CacheException("Error while calculating graph", e);
		} finally {
			closeFile(file);
		}
		
		// Tags
		List paths = graph.getPaths();
		Iterator iter = paths.iterator();
		while (iter.hasNext()) {
			String path = (String) iter.next();
			Branch branch = graph.getBranch(path);
			if(branch.getNodes().size() == 1) {
				Node firstNode = (Node) branch.getNodes().iterator().next();
				if(firstNode.getSource() != null && firstNode.getChildCount() == 0) {
					// is not the root node and is not the target of any arrow
					// therefore is a tag
					if (firstNode.getSource() != null) {
						firstNode.getSource().addTag(firstNode);
					}
				}
			}			
		}
		
		return graph;
	}
	
	public Node toNode(ISVNLogMessage lm, ISVNLogMessageChangePath cp) {
		Node node = new Node();
		node.setAction(cp.getAction());
		node.setAuthor(lm.getAuthor());
		node.setCopySrcPath(cp.getCopySrcPath());
		node.setCopySrcRevision(cp.getCopySrcRevision().getNumber());
		node.setMessage(lm.getMessage());
		node.setPath(cp.getPath());
		node.setRevision(lm.getRevision().getNumber());
		node.setRevisionDate(lm.getDate());
		return node;
	}
	
	private static int commonChars(String a, String b, int max) {
		int i=0;
		int ml;
		if(max > 0)
			ml = max;
		else
			ml = a.length();
		
		if(b.length() < ml) {
			ml = b.length();
		}
		while(i<ml) {
			if(a.charAt(i) != b.charAt(i))
				break;
			i++;
		}
		return i;
	}
	
	public static void main(String[] args) {
//		System.out.println(commonStart("", ""));
//		System.out.println();
//		System.out.println(commonStart("foo", "bar"));
//		System.out.println();
//		System.out.println(commonStart("foo", "foo bar"));
//		System.out.println();
//		System.out.println(commonStart("/src/org/apache", "/src/org/apache"));
//		System.out.println();
//		System.out.println(commonStart("/src/org/apache", "/src/org"));
//		System.out.println();
//		System.out.println(commonStart("/src/org/apache", "/src"));
//		System.out.println();
//		System.out.println(commonStart("/src/org/apache", "/srcc"));
//		System.out.println(isEqualsOrParent("/foo/bar/hello.java", "/foo/bar/hello.java")); // true
//		System.out.println(isEqualsOrParent("/foo/bar", "/foo/bar/hello.java")); // true
//		System.out.println(isEqualsOrParent("/foo/b", "/foo/bar/hello.java")); // false
//		System.out.println(isEqualsOrParent("/foo", "/foo/bar/hello.java")); // true
//		System.out.println(isEqualsOrParent("/fo", "/foo/bar/hello.java")); // false
//		System.out.println(isEqualsOrParent("/f", "/foo/bar/hello.java")); // false
	}
	
	public static boolean isEqualsOrParent(String parent, String path) {
		if(parent.length() == path.length())
			return parent.equals(path);
		return path.startsWith(parent+"/");
	}
	
	public void clearCache() {
		deleteFile(revisionsFile);
		deleteFile(logMessagesFile);
	}
	
	public static File getCacheDirectory(IResource resource) {
		File f;
		if (resource == null) f = ResourcesPlugin.getWorkspace().getRoot().getRawLocation().toFile();
		else f = resource.getWorkspace().getRoot().getRawLocation().toFile();
		f = new File(f, ".metadata");
		f = new File(f, ".plugins");
		f = new File(f, "org.tigris.subversion.subclipse.graph");
		return f;
	}
	
	private void deleteFile(File f) {
		if(f.exists() && !f.delete()) {
			System.err.println("Couldn't delete file: "+f.getAbsolutePath());
		}
	}
	
	private void closeFile(RandomAccessFile file) {
		if(file == null) return;
		try {
			file.close();
		} catch (IOException e) {
			throw new CacheException("Error while closing file", e);
		}
	}

}
