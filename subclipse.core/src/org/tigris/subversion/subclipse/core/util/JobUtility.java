package org.tigris.subversion.subclipse.core.util;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;

public class JobUtility {
	
	public static Job scheduleJob(String jobName, final Runnable runnable, final ISchedulingRule schedulingRule, boolean system) {
		Job job = new Job(jobName) {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
						public void run(IProgressMonitor monitor) throws CoreException {
							runnable.run();
						}
					}, this.getRule(), IWorkspace.AVOID_UPDATE, monitor);
				} catch (CoreException e) {
					SVNProviderPlugin.log(Status.ERROR, e.getMessage(), e);
				}
				return Status.OK_STATUS;
			}			
		};
		job.setRule(schedulingRule);
		job.setSystem(system);
		job.schedule();
		return job;
	}

}
