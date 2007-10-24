package org.tigris.subversion.subclipse.test.core;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.test.SubclipseTest;
import org.tigris.subversion.subclipse.test.TestProject;

public class SVNProviderPluginTest extends SubclipseTest {

	public SVNProviderPluginTest(String name) {
		super(name);
	}

	public void testGetRepository() throws Exception
	{
		TestProject testProject = new TestProject("testProject");
		shareProject(testProject.getProject());
		// create a file
		IPackageFragment package1 = testProject.createPackage("pack1");
		IType type = testProject.createJavaType(package1, "AClass.java",
				"public class AClass { \n" + "  public void m() {}\n" + "}");
		IResource resource = testProject.getProject().getFile(
				new Path("src/pack1/AClass.java"));
		ISVNLocalResource svnResource = SVNWorkspaceRoot
				.getSVNResourceFor(resource);
	
		assertEquals(super.url.toString(), svnResource.getRepository().getLocation());
		assertEquals(super.url.toString(), SVNProviderPlugin.getPlugin().getRepository(svnResource.getUrl().toString()).getLocation());
	}
}
