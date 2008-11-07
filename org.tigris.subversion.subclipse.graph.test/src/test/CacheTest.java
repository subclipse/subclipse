package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.tigris.subversion.sublicpse.graph.cache.Branch;
import org.tigris.subversion.sublicpse.graph.cache.Cache;
import org.tigris.subversion.sublicpse.graph.cache.Graph;
import org.tigris.subversion.sublicpse.graph.cache.Node;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;

public class CacheTest extends TestCase {

	private final int BLOCK_NONE = 0;
	private final int BLOCK_LOAD = 1;
	private final int BLOCK_TEST = 2;
	
	public void testFiles() throws IOException, ParseException{
		File dir = new File("testfiles");
//		testFile(new File(dir, "basic.txt"));
//		testFile(new File(dir, "branch-file.txt"));
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			if(f.isFile()) {
				System.out.println("Testing file: "+f);
				testFile(f);
			}
		}
	}
	
	public void testFile(File f) throws IOException, ParseException {
		Cache cache = new Cache(new File("test"), "uuid");
		
		BufferedReader reader = new BufferedReader(new FileReader(f));
		String line = null;
		LogMessage lastLogMessage = null;
		int block = BLOCK_NONE;
//		Long fileId = null;
		Iterator it = null;
		Graph graph = null;
		List messages = new ArrayList();
		int nline = 0;
		while((line = reader.readLine()) != null) {
			nline++;
			if(line.startsWith("#") || line.trim().length() == 0) continue;
			String[] tokens = line.split("\t");
			if("update".equals(tokens[0])) {
				for (Iterator iterator = messages.iterator(); iterator
						.hasNext();) {
					ISVNLogMessage message = (ISVNLogMessage) iterator.next();
					cache.update(message);
				}
				cache.finishUpdate();
//				cache.dumpChangePaths();
				block = BLOCK_NONE;
			} else if("clear".equals(tokens[0])) {
				cache.clearCache();
				block = BLOCK_NONE;
			} else if("load".equals(tokens[0])) {
				block = BLOCK_LOAD;
				cache.startUpdate();
			} else if("test".equals(tokens[0])) {
				String path = tokens[1];
				long revision = Long.parseLong(tokens[2]);
				Node node = cache.findRootNode(path, revision, null);
				graph = cache.createGraph(node.getPath(), node.getRevision(), null);
				block = BLOCK_TEST;
			} else if("testnull".equals(tokens[0])) {
//				String path = tokens[1];
//				long revision = Long.parseLong(tokens[2]);
//				fileId = cache.getFileId(path, revision);
//				assertNull(path+" at revision "+revision+" should be null. At line "+nline, fileId);
			} else if(block == BLOCK_LOAD) {
				if(tokens.length == 1) {
					lastLogMessage = new LogMessage(Long.parseLong(tokens[0]), "author", new Date(), "m");
					messages.add(lastLogMessage);
				} else if(tokens.length == 2) {
					lastLogMessage.addChangePath(
							new LogMessageChangePath(tokens[0].charAt(0),
									tokens[1]));
				} else if(tokens.length == 4) {
					lastLogMessage.addChangePath(
							new LogMessageChangePath(tokens[0].charAt(0),
									tokens[1],
									tokens[2],
									Long.parseLong(tokens[3])));
				}
			} else if(block == BLOCK_TEST) {
				if(tokens.length == 1) {
					if("endtest".equals(tokens[0])) {
						assertFalse("There are more nodes. At line "+nline, it.hasNext());
						block = BLOCK_NONE;
					} else {
						Branch branch = (Branch) graph.getBranch(tokens[0]);
						assertNotNull("no branch for path: "+tokens[0]+" at line "+nline, branch);
						it = branch.getNodes().iterator();
					}
				} else {
					assertNotNull("not selected path at line "+nline, it);
					assertTrue("node not found at line "+nline, it.hasNext());
					Node node = (Node) it.next();
					assertNotNull("node not found at line "+nline, node);
					assertEquals("wrong revision number at line "+nline, Long.parseLong(tokens[0]), node.getRevision());
					assertEquals("wrong action at line "+nline, tokens[1].charAt(0), node.getAction());
				}
			} else {
				fail("error at line "+nline+": "+line);
			}
		}
		reader.close();

//		System.out.println("change paths...");
//		cache.dumpChangePaths();
		cache.close();
	}

}
