/**
 * ***************************************************************************** Copyright (c) 2009
 * CollabNet. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: CollabNet - initial API and implementation
 * ****************************************************************************
 */
package com.collabnet.subversion.merge;

import com.collabnet.subversion.merge.xml.DOMUtil;
import com.collabnet.subversion.merge.xml.MergeOutputDocument;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.SVNRevisionRange;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class MergeOutput implements IPropertySource, IAdaptable {

  private String target;
  private IResource resource;
  private MergeOptions mergeOptions;
  private String mergeCommand;
  private Date mergeDate;
  private String description;
  private String workspaceUrl;
  private long workspaceRevision;
  private MergeResult[] mergeResults;
  private MergeSummaryResult[] mergeSummaryResults;
  private MergeResult[] conflictedMergeResults;
  private MergeResult[] nonSkippedMergeResults;
  private MergeResultsFolder[] compressedFolders;
  private MergeResultsFolder[] conflictedCompressedFolders;
  private long lowerRevision;
  private long upperRevision;
  private boolean incomplete = false;
  private boolean normalEnd = false;
  private boolean abnormalEnd = false;
  private static boolean inProgress = false;
  private static final String LAST_TEXT_CONFLICT_CHOICE =
      "MergeWizardMainPage.lastTextConflictChoice"; //$NON-NLS-1$
  private static final String LAST_PROPERTY_CONFLICT_CHOICE =
      "MergeWizardMainPage.lastPropertyConflictChoice"; //$NON-NLS-1$
  private static final String LAST_BINARY_CONFLICT_CHOICE =
      "MergeWizardMainPage.lastBinaryConflictChoice"; //$NON-NLS-1$
  private static final String LAST_TREE_CONFLICT_CHOICE =
      "MergeWizardMainPage.lastTreeConflictChoice"; //$NON-NLS-1$

  public static final int EXPORT_FORMAT_DEFAULT = 0;

  public static String P_ID_DESCRIPTION = "desc"; // $NON-NLS-1$
  public static String P_DESCRIPTION = Messages.MergeOutput_description;
  public static String P_ID_DATE = "date"; // $NON-NLS-1$
  public static String P_DATE = Messages.MergeOutput_date;
  public static String P_ID_RESOURCE = "resource"; // $NON-NLS-1$
  public static String P_RESOURCE = Messages.MergeOutput_resource;
  public static String P_ID_COMMAND = "command"; // $NON-NLS-1$
  public static String P_COMMAND = Messages.MergeOutput_command;
  public static String P_ID_WORKSPACE_URL = "workspaceUrl"; // $NON-NLS-1$
  public static String P_WORKSPACE_URL = Messages.MergeOutput_url;
  public static String P_ID_WORKSPACE_REVISION = "workspaceRevision"; // $NON-NLS-1$
  public static String P_WORKSPACE_REVISION = Messages.MergeOutput_revision;
  public static List descriptors;

  static {
    descriptors = new ArrayList();
    descriptors.add(new PropertyDescriptor(P_ID_DESCRIPTION, P_DESCRIPTION));
    descriptors.add(new PropertyDescriptor(P_ID_DATE, P_DATE));
    descriptors.add(new PropertyDescriptor(P_ID_RESOURCE, P_RESOURCE));
    descriptors.add(new PropertyDescriptor(P_ID_COMMAND, P_COMMAND));
    descriptors.add(new PropertyDescriptor(P_ID_WORKSPACE_URL, P_WORKSPACE_URL));
    descriptors.add(new PropertyDescriptor(P_ID_WORKSPACE_REVISION, P_WORKSPACE_REVISION));
  }

  public MergeOutput() {}

  public MergeOutput(String mergeCommand, Date mergeDate, MergeResult[] mergeResults) {
    this();
    this.mergeCommand = mergeCommand;
    this.mergeDate = mergeDate;
    this.mergeResults = mergeResults;
  }

  public void setMergeCommand(String mergeCommand) {
    this.mergeCommand = mergeCommand;
  }

  public void setMergeDate(Date mergeDate) {
    this.mergeDate = mergeDate;
  }

  public void setMergeResults(MergeResult[] mergeResults) {
    this.mergeResults = mergeResults;
  }

  public String getMergeCommand() {
    return mergeCommand;
  }

  public Date getMergeDate() {
    return mergeDate;
  }

  public MergeResult[] getConflictedMergeResults() {
    if (conflictedMergeResults == null) {
      ArrayList conflictList = new ArrayList();
      mergeResults = getNonSkippedMergeResults();
      for (int i = 0; i < mergeResults.length; i++) {
        if (mergeResults[i].hasTreeConflict()
            || mergeResults[i].isConflicted()
            || mergeResults[i].isPropertyConflicted()) conflictList.add(mergeResults[i]);
      }
      conflictedMergeResults = new MergeResult[conflictList.size()];
      conflictList.toArray(conflictedMergeResults);
    }
    return conflictedMergeResults;
  }

  public MergeResult[] getNonSkippedMergeResults() {
    if (nonSkippedMergeResults == null) {
      ArrayList nonSkippedList = new ArrayList();
      mergeResults = getMergeResults();
      for (int i = 0; i < mergeResults.length; i++) {
        if (!mergeResults[i].isSkip()) nonSkippedList.add(mergeResults[i]);
      }
      nonSkippedMergeResults = new MergeResult[nonSkippedList.size()];
      nonSkippedList.toArray(nonSkippedMergeResults);
    }
    return nonSkippedMergeResults;
  }

  public MergeResult[] getMergeResults() {
    if (mergeResults == null) {
      mergeResults = loadMergeResults();
    }
    return mergeResults;
  }

  public MergeResult[] getMergeResults(boolean conflictsOnly) {
    if (conflictsOnly) return getConflictedMergeResults();
    else return getMergeResults();
  }

  public IResource getResource() {
    return resource;
  }

  public void setResource(IResource resource) {
    this.resource = resource;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public boolean equals(Object obj) {
    if (obj instanceof MergeOutput) {
      MergeOutput compareTo = (MergeOutput) obj;
      if (compareTo.getResource() == null
          || compareTo.getResource().getFullPath() == null
          || resource == null
          || resource.getFullPath() == null
          || compareTo.getMergeDate() == null
          || mergeDate == null) return false;
      return compareTo.getResource().getFullPath().equals(resource.getFullPath())
          && compareTo.getMergeDate().getTime() == mergeDate.getTime();
    }
    return super.equals(obj);
  }

  public int hashCode() {
    if (resource == null) return super.hashCode();
    String hashString = resource.getFullPath().toString() + mergeDate.getTime();
    return hashString.hashCode();
  }

  public String toString() {

    if (resource == null) return super.toString();

    StringBuffer stringBuffer = new StringBuffer();
    if (abnormalEnd)
      stringBuffer.append(
          ":abnormalEnd:" + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
    if (normalEnd)
      stringBuffer.append(
          ":normalEnd:" + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
    if (incomplete)
      stringBuffer.append(
          ":incomplete:" + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
    if (description != null)
      stringBuffer.append(
          "description: "
              + description
              + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
    if (resource.getType() == IResource.PROJECT)
      stringBuffer.append(
          "project: "
              + resource.getFullPath().toOSString()
              + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
    else if (resource.getType() == IResource.FOLDER)
      stringBuffer.append(
          "folder: "
              + resource.getFullPath().toOSString()
              + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
    else if (resource.getType() == IResource.FILE)
      stringBuffer.append(
          "file: "
              + resource.getFullPath().toOSString()
              + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
    if (mergeOptions != null) {
      if (mergeOptions.getFromUrl() != null)
        stringBuffer.append(
            "fromUrl: "
                + mergeOptions.getFromUrl().toString()
                + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
      if (mergeOptions.getFromRevision() != null)
        stringBuffer.append(
            "fromRevision: "
                + mergeOptions.getFromRevision()
                + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
      if (mergeOptions.getToUrl() != null)
        stringBuffer.append(
            "toUrl: "
                + mergeOptions.getToUrl().toString()
                + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
      if (mergeOptions.getToRevision() != null)
        stringBuffer.append(
            "toRevision: "
                + mergeOptions.getToRevision()
                + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
      if (mergeOptions.getRevisions() != null) {
        stringBuffer.append("revisionRanges: "); // $NON-NLS-1$
        SVNRevisionRange[] revisionRanges = mergeOptions.getRevisions();
        for (int i = 0; i < revisionRanges.length; i++) {
          if (i > 0) stringBuffer.append(","); // $NON-NLS-1$
          stringBuffer.append(revisionRanges[i].toString());
        }
        stringBuffer.append(System.getProperty("line.separator")); // $NON-NLS-1$
      }
      stringBuffer.append(
          "force: "
              + mergeOptions.isForce()
              + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
      stringBuffer.append(
          "recurse: "
              + mergeOptions.isRecurse()
              + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
      stringBuffer.append(
          "ignore: "
              + mergeOptions.isIgnoreAncestry()
              + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
      stringBuffer.append(
          "depth: "
              + mergeOptions.getDepth()
              + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
      if (mergeOptions.getRevisions() != null && mergeOptions.getRevisions().length > 0) {
        stringBuffer.append("revisions: "); // $NON-NLS-1$
        SVNRevisionRange[] revisions = mergeOptions.getRevisions();
        for (int i = 0; i < revisions.length; i++) {
          if (i > 0) stringBuffer.append(","); // $NON-NLS-1$
          stringBuffer.append(revisions[i].toString());
        }
        stringBuffer.append(System.getProperty("line.separator")); // $NON-NLS-1$
      }
    }
    if (mergeSummaryResults != null) {
      for (int i = 0; i < mergeSummaryResults.length; i++)
        stringBuffer.append(
            mergeSummaryResults[i] + System.getProperty("line.separator")); // $NON-NLS-1$
      stringBuffer.append(System.getProperty("line.separator")); // $NON-NLS-1$	
    }
    if (workspaceUrl != null)
      stringBuffer.append(
          "workspaceUrl: "
              + workspaceUrl
              + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
    stringBuffer.append(
        "workspaceRevision: "
            + workspaceRevision
            + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
    stringBuffer.append(
        "lowerRevision: "
            + lowerRevision
            + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
    stringBuffer.append(
        "upperRevision: "
            + upperRevision
            + System.getProperty("line.separator")); // $NON-NLS-1$ //$NON-NLS-2$
    stringBuffer.append(mergeCommand);
    if (mergeResults != null) {
      for (int i = 0; i < mergeResults.length; i++)
        stringBuffer.append(System.getProperty("line.separator") + mergeResults[i]); // $NON-NLS-1$
    }
    return stringBuffer.toString();
  }

  public String export(int format) {
    MergeOutputDocument document = new MergeOutputDocument(this);
    return DOMUtil.toString(document.getDocument().getDocumentElement());
  }

  public boolean store() {
    boolean error = false;
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(mergeDate);
    File mergeOutputFile =
        new File(
            Activator.getMergeResultsLocation()
                + File.separator
                + "m"
                + calendar.getTimeInMillis()); // $NON-NLS-1$
    Writer output = null;
    try {
      File mergeOutputDirectory = new File(Activator.getMergeResultsLocation());
      if (!mergeOutputDirectory.exists()) {
        mergeOutputDirectory.mkdirs();
      }
      if (!mergeOutputFile.exists()) mergeOutputFile.createNewFile();
      output =
          new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mergeOutputFile), "UTF8"));
      output.write(toString());
    } catch (IOException e) {
      Activator.handleError(e);
      error = true;
    } finally {
      try {
        if (output != null) output.close();
      } catch (Exception e) {
        Activator.handleError(e);
      }
    }
    return !error;
  }

  public boolean delete() {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(mergeDate);
    File mergeOutputFile =
        new File(
            Activator.getMergeResultsLocation()
                + File.separator
                + "m"
                + calendar.getTimeInMillis()); // $NON-NLS-1$
    return mergeOutputFile.delete();
  }

  public void resume() {
    IDialogSettings settings = Activator.getDefault().getDialogSettings();
    IResource[] resources = {resource};
    SVNUrl[] fromUrls = {mergeOptions.getFromUrl()};
    SVNUrl[] toUrls = {mergeOptions.getToUrl()};
    MergeOperation mergeOperation =
        new MergeOperation(
            SVNUIPlugin.getActivePage().getActivePart(),
            resources,
            fromUrls,
            mergeOptions.getFromRevision(),
            toUrls,
            mergeOptions.getToRevision(),
            mergeOptions.getRevisions(),
            this);
    mergeOperation.setForce(mergeOptions.isForce());
    mergeOperation.setIgnoreAncestry(mergeOptions.isIgnoreAncestry());
    mergeOperation.setDepth(mergeOptions.getDepth());
    int lastTextConflictChoice = ISVNConflictResolver.Choice.chooseMerged;
    try {
      lastTextConflictChoice = settings.getInt(LAST_TEXT_CONFLICT_CHOICE);
    } catch (Exception e) {
    }
    int lastPropertyConflictChoice = lastTextConflictChoice;
    try {
      lastPropertyConflictChoice = settings.getInt(LAST_PROPERTY_CONFLICT_CHOICE);
    } catch (Exception e) {
    }
    int lastBinaryConflictChoice = ISVNConflictResolver.Choice.chooseMerged;
    try {
      lastBinaryConflictChoice = settings.getInt(LAST_BINARY_CONFLICT_CHOICE);
    } catch (Exception e) {
    }
    int lastTreeConflictChoice = ISVNConflictResolver.Choice.postpone;
    try {
      lastTreeConflictChoice = settings.getInt(LAST_TREE_CONFLICT_CHOICE);
    } catch (Exception e) {
    }
    mergeOperation.setTextConflictHandling(lastTextConflictChoice);
    mergeOperation.setBinaryConflictHandling(lastBinaryConflictChoice);
    mergeOperation.setPropertyConflictHandling(lastPropertyConflictChoice);
    mergeOperation.setTreeConflictHandling(lastTreeConflictChoice);
    try {
      mergeOperation.run();
    } catch (Exception e) {
      Activator.handleError(Messages.MergeOutput_resumeError, e);
      MessageDialog.openError(
          Display.getCurrent().getActiveShell(), Messages.MergeOutput_resume, e.getMessage());
    }
  }

  private MergeResult[] loadMergeResults() {
    ArrayList results = new ArrayList();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(mergeDate);
    File mergeOutputFile =
        new File(
            Activator.getMergeResultsLocation()
                + File.separator
                + "m"
                + calendar.getTimeInMillis()); // $NON-NLS-1$	
    BufferedReader input = null;
    try {
      input =
          new BufferedReader(new InputStreamReader(new FileInputStream(mergeOutputFile), "UTF8"));
      String line = null;
      while ((line = input.readLine()) != null) {
        if (line.startsWith("Message: ")
            || line.startsWith("Error:   ")) { // $NON-NLS-1$ //$NON-NLS-2$			
          if (line.length() < 24) {
            Activator.handleError(
                new Exception(
                    "Unexpected error while loading merge results from "
                        + mergeOutputFile.getName()
                        + ".  line: "
                        + line));
          } else {
            String action = null;
            String propertyAction = null;
            String treeConflictAction = null;
            String conflictResolution = null;
            String propertyResolution = null;
            String treeConflictResolution = null;
            String path = null;
            int type = MergeResult.FILE;
            boolean error = line.startsWith("Error: "); // $NON-NLS-1$
            type = Integer.parseInt(line.substring(9, 10));
            action = line.substring(11, 12);
            propertyAction = line.substring(13, 14);
            conflictResolution = line.substring(15, 16);
            propertyResolution = line.substring(17, 18);
            treeConflictAction = line.substring(19, 20);
            treeConflictResolution = line.substring(21, 22);
            path = line.substring(23);

            MergeResult result = null;
            if (action.equals(MergeResult.ACTION_SKIP))
              result =
                  new SkippedMergeResult(action, propertyAction, treeConflictAction, path, error);
            else
              result =
                  new AdaptableMergeResult(action, propertyAction, treeConflictAction, path, error);
            result.setType(type);
            result.setMergeOutput(this);
            IResource resource = null;
            //					String resourcePath = path.substring(root.length());
            if (this.resource.getLocation().toString().equals(path)) resource = this.resource;
            //					if (this.resource.getFullPath().toString().equals(resourcePath)) resource =
            // this.resource;
            //					else if (type == MergeResult.FOLDER) resource =
            // ResourcesPlugin.getWorkspace().getRoot().getFolder(new Path(resourcePath));
            //					else resource = ResourcesPlugin.getWorkspace().getRoot().getFile(new
            // Path(resourcePath));
            else resource = SVNWorkspaceRoot.getResourceFor(this.resource, new Path(path));
            result.setResource(resource);
            if (error) {
              result.setConflictResolution(conflictResolution);
              result.setPropertyResolution(propertyResolution);
              result.setTreeConflictResolution(treeConflictResolution);
              if (resource != null && !inProgress) {
                LocalResourceStatus status =
                    SVNWorkspaceRoot.getSVNResourceFor(resource).getStatus();
                if (conflictResolution.trim().length() == 0 && !status.isTextConflicted())
                  result.setConflictResolution("X"); // $NON-NLS-1$
                if (propertyResolution.trim().length() == 0 && !status.isPropConflicted())
                  result.setPropertyResolution("X"); // $NON-NLS-1$
                if (treeConflictResolution.trim().length() == 0 && !status.hasTreeConflict())
                  result.setTreeConflictResolution("X"); // $NON-NLS-1$						
              }
            }
            results.add(result);
          }
        }
      }
    } catch (Exception e) {
      Activator.handleError(e);
    } finally {
      try {
        if (input != null) {
          input.close();
        }
      } catch (IOException e) {
        Activator.handleError(e);
      }
    }
    MergeResult[] mergeResultArray = new MergeResult[results.size()];
    results.toArray(mergeResultArray);
    Arrays.sort(mergeResultArray);
    return mergeResultArray;
  }

  public static MergeOutput getIncompleteMerge(IResource resource, String fromUrl, String toUrl) {
    MergeOutput[] mergeOutputs = getMergeOutputs();
    for (int i = 0; i < mergeOutputs.length; i++) {
      if (mergeOutputs[i].isIncomplete()) {
        if (mergeOutputs[i].getResource().getFullPath().equals(resource.getFullPath())) {
          if (mergeOutputs[i].getMergeOptions().getFromUrl().toString().equals(fromUrl)) {
            if (mergeOutputs[i].getMergeOptions().getToUrl().toString().equals(toUrl)) {
              return mergeOutputs[i];
            }
          }
        }
      }
    }
    return null;
  }

  public static MergeOutput[] getMergeOutputs() {
    Calendar calendar = Calendar.getInstance();
    File mergeOutputDirectory = new File(Activator.getMergeResultsLocation());
    if (!mergeOutputDirectory.exists()) {
      mergeOutputDirectory.mkdirs();
    }
    File[] mergeOutputs = mergeOutputDirectory.listFiles();
    ArrayList mergeOutputList = new ArrayList();
    for (int i = 0; i < mergeOutputs.length; i++) {
      MergeOutput mergeOutput = new MergeOutput();
      ArrayList mergeSummaryResults = new ArrayList();

      // If file name doesn't start with "m", it doesn't belong here.
      boolean goodMergeResultsFile = mergeOutputs[i].getName().startsWith("m"); // $NON-NLS-1$
      if (goodMergeResultsFile) {
        try {
          calendar.setTimeInMillis(Long.parseLong(mergeOutputs[i].getName().substring(1)));
        } catch (Exception e) {
          // If file name does not end with a valid timestamp, it doesn't belong here.
          goodMergeResultsFile = false;
        }
      }
      if (goodMergeResultsFile) {
        mergeOutput.setMergeDate(calendar.getTime());
        BufferedReader input = null;
        try {
          input = new BufferedReader(new FileReader(mergeOutputs[i]));
          String line = null;
          MergeOptions mergeOptions = new MergeOptions();
          while ((line = input.readLine()) != null) {
            if (line.startsWith(":abnormalEnd:")) // $NON-NLS-1$
            mergeOutput.setAbnormalEnd(true);
            if (line.startsWith(":normalEnd:")) // $NON-NLS-1$
            mergeOutput.setNormalEnd(true);
            if (line.startsWith(":incomplete:")) // $NON-NLS-1$
            mergeOutput.setIncomplete(true);
            if (line.startsWith("description: ")) // $NON-NLS-1$
            mergeOutput.setDescription(line.substring(13));
            if (line.startsWith("project: ")) { // $NON-NLS-1$
              mergeOutput.setResource(
                  ResourcesPlugin.getWorkspace().getRoot().getProject(line.substring(10)));
              mergeOutput.setTarget(line.substring(10));
            }
            if (line.startsWith("folder: ")) { // $NON-NLS-1$
              mergeOutput.setResource(
                  ResourcesPlugin.getWorkspace().getRoot().getFolder(new Path(line.substring(8))));
              mergeOutput.setTarget(line.substring(8));
            }
            if (line.startsWith(Messages.MergeOutput_57)) {
              mergeOutput.setResource(
                  ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(line.substring(6))));
              mergeOutput.setTarget(line.substring(6));
            }
            if (line.startsWith("fromUrl: "))
              mergeOptions.setFromUrl(line.substring(9)); // $NON-NLS-1$
            if (line.startsWith("toUrl: ")) mergeOptions.setToUrl(line.substring(7)); // $NON-NLS-1$
            if (line.startsWith("fromRevision: "))
              mergeOptions.setFromRevision(line.substring(14)); // $NON-NLS-1$
            if (line.startsWith("toRevision: "))
              mergeOptions.setToRevision(line.substring(12)); // $NON-NLS-1$
            if (line.startsWith("force: "))
              mergeOptions.setForce(line.substring(7).equals("true")); // $NON-NLS-1$ //$NON-NLS-2$
            if (line.startsWith("recurse: "))
              mergeOptions.setRecurse(
                  line.substring(9).equals("true")); // $NON-NLS-1$ //$NON-NLS-2$
            if (line.startsWith("ignore: "))
              mergeOptions.setIgnoreAncestry(
                  line.substring(8).equals("true")); // $NON-NLS-1$ //$NON-NLS-2$
            if (line.startsWith("depth: ")) { // $NON-NLS-1$
              String depthString = line.substring(7);
              int depth = Integer.parseInt(depthString.trim());
              mergeOptions.setDepth(depth);
            }
            if (line.startsWith("revisionRanges: ")) { // $NON-NLS-1$
              mergeOptions.setRevisions(line.substring(16));
            }
            if (line.startsWith("lowerRevision: ")) { // $NON-NLS-1$
              mergeOutput.setLowerRevision(Long.parseLong(line.substring(15)));
            }
            if (line.startsWith("upperRevision: ")) { // $NON-NLS-1$
              mergeOutput.setUpperRevision(Long.parseLong(line.substring(15)));
            }
            if (line.startsWith("workspaceUrl: "))
              mergeOutput.setWorkspaceUrl(line.substring(14)); // $NON-NLS-1$
            if (line.startsWith("workspaceRevision: ")) { // $NON-NLS-1$
              mergeOutput.setWorkspaceRevision(Long.parseLong(line.substring(19)));
            }
            if (line.startsWith("Summary result: "))
              mergeSummaryResults.add(new MergeSummaryResult(line)); // $NON-NLS-1$
            if (line.startsWith("merge ")) { // $NON-NLS-1$
              mergeOutput.setMergeCommand(line);
              break;
            }
            mergeOutput.setMergeOptions(mergeOptions);
          }
          MergeSummaryResult[] mergeSummaryResultArray =
              new MergeSummaryResult[mergeSummaryResults.size()];
          mergeSummaryResults.toArray(mergeSummaryResultArray);
          mergeOutput.setMergeSummaryResults(mergeSummaryResultArray);
          if (mergeOutput.getResource() != null) mergeOutputList.add(mergeOutput);
        } catch (Exception e) {
          Activator.handleError(e);
        } finally {
          try {
            if (input != null) {
              input.close();
            }
          } catch (IOException e) {
            Activator.handleError(e);
          }
        }
      }
    }
    MergeOutput[] mergeOutputArray = new MergeOutput[mergeOutputList.size()];
    mergeOutputList.toArray(mergeOutputArray);
    return mergeOutputArray;
  }

  public Object getEditableValue() {
    if (description == null) return resource.getName();
    else return description.toString();
  }

  public IPropertyDescriptor[] getPropertyDescriptors() {
    return (IPropertyDescriptor[])
        getDescriptors().toArray(new IPropertyDescriptor[getDescriptors().size()]);
  }

  private static List getDescriptors() {
    return descriptors;
  }

  public Object getPropertyValue(Object propKey) {
    if (P_ID_DESCRIPTION.equals(propKey)) {
      if (description == null) return ""; // $NON-NLS-1$
      else return description;
    }
    if (P_ID_DATE.equals(propKey)) return mergeDate.toString();
    if (P_ID_RESOURCE.equals(propKey)) return resource.getFullPath().makeRelative().toOSString();
    if (P_ID_COMMAND.equals(propKey)) return mergeCommand;
    if (P_ID_WORKSPACE_URL.equals(propKey)) {
      if (workspaceUrl == null) return ""; // $NON-NLS-1$
      else return workspaceUrl;
    }
    if (P_ID_WORKSPACE_REVISION.equals(propKey)) {
      if (workspaceRevision > 0) return Long.toString(workspaceRevision);
      else return ""; // $NON-NLS-1$
    }
    return null;
  }

  public boolean isPropertySet(Object id) {
    return false;
  }

  public void resetPropertyValue(Object id) {}

  public void setPropertyValue(Object id, Object value) {}

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public MergeOptions getMergeOptions() {
    return mergeOptions;
  }

  public void setMergeOptions(MergeOptions mergeOptions) {
    this.mergeOptions = mergeOptions;
  }

  public MergeSummaryResult[] getMergeSummaryResults() {
    return mergeSummaryResults;
  }

  public void setMergeSummaryResults(MergeSummaryResult[] mergeSummaryResults) {
    this.mergeSummaryResults = mergeSummaryResults;
  }

  public boolean hasUnresolvedConflicts() {
    MergeResult[] conflicts = getMergeResults(true);
    for (int i = 0; i < conflicts.length; i++) {
      if ((conflicts[i].hasTreeConflict() && !conflicts[i].isTreeConflictResolved())
          || (conflicts[i].isConflicted() && !conflicts[i].isResolved())
          || (conflicts[i].isPropertyConflicted() && !conflicts[i].isPropertyResolved()))
        return true;
    }
    return false;
  }

  public MergeResult[] getRootMergeResults(boolean conflictsOnly) {
    ArrayList rootMergeResults = new ArrayList();
    MergeResult[] mergeResults;
    if (conflictsOnly) mergeResults = getConflictedMergeResults();
    else mergeResults = getMergeResults();
    for (int i = 0; i < mergeResults.length; i++) {
      MergeResult mergeResult = mergeResults[i];
      IResource resource = mergeResult.getResource();
      if (resource instanceof IFile) {
        IContainer parent = resource.getParent();
        if (parent != null
            && parent.getFullPath().toString().equals(this.resource.getFullPath().toString()))
          rootMergeResults.add(mergeResult);
      }
    }
    MergeResult[] resultArray = new MergeResult[rootMergeResults.size()];
    rootMergeResults.toArray(resultArray);
    return resultArray;
  }

  public MergeResultsFolder[] getCompressedFolders(boolean conflictsOnly) {
    if (conflictsOnly) return getConflictedCompressedFolders();
    else return getCompressedFolders();
  }

  public MergeResultsFolder[] getCompressedFolders() {
    if (compressedFolders == null) {
      HashMap map = new HashMap();
      ArrayList folderList = new ArrayList();
      mergeResults = getMergeResults();
      for (int i = 0; i < mergeResults.length; i++) {
        IResource resource = mergeResults[i].getResource();
        if (resource instanceof IContainer && !folderList.contains(resource)) {
          folderList.add(resource);
          map.put(resource, mergeResults[i]);
        }
        if (!(resource instanceof IContainer)) {
          IContainer parent = resource.getParent();
          if (parent != null
              && !(parent instanceof IWorkspaceRoot)
              && !folderList.contains(parent)) {
            folderList.add(parent);
          }
        }
      }
      IContainer[] folders = new IContainer[folderList.size()];
      folderList.toArray(folders);
      compressedFolders = new MergeResultsFolder[folders.length];
      for (int i = 0; i < folders.length; i++) {
        MergeResult mergeResult = (MergeResult) map.get(folders[i]);
        MergeResultsFolder folder = null;
        if (mergeResult != null && mergeResult.getAction().equals(MergeResult.ACTION_SKIP))
          folder = new SkippedMergeResultsFolder();
        else folder = new AdaptableMergeResultsFolder();
        folder.setFolder(folders[i]);
        folder.setCompressed(true);
        folder.setRootFolderLength(
            this.resource.getFullPath().makeRelative().toOSString().length());
        folder.setMergeOutput(this);
        folder.setMergeResult(mergeResult);
        compressedFolders[i] = folder;
      }
    }
    return compressedFolders;
  }

  public MergeResultsFolder[] getConflictedCompressedFolders() {
    if (conflictedCompressedFolders == null) {
      HashMap map = new HashMap();
      ArrayList folderList = new ArrayList();
      conflictedMergeResults = getConflictedMergeResults();
      for (int i = 0; i < conflictedMergeResults.length; i++) {
        IResource resource = conflictedMergeResults[i].getResource();
        if (resource instanceof IContainer && !folderList.contains(resource)) {
          folderList.add(resource);
          map.put(resource, conflictedMergeResults[i]);
        }
        if (!(resource instanceof IContainer)) {
          IContainer parent = resource.getParent();
          if (parent != null
              && !folderList.contains(parent)
              && !parent.getFullPath().toString().equals(this.resource.getFullPath().toString())) {
            folderList.add(parent);
          }
        }
      }
      IContainer[] folders = new IContainer[folderList.size()];
      folderList.toArray(folders);
      conflictedCompressedFolders = new MergeResultsFolder[folders.length];
      for (int i = 0; i < folders.length; i++) {
        MergeResult mergeResult = (MergeResult) map.get(folders[i]);
        MergeResultsFolder folder = null;
        if (mergeResult != null && mergeResult.getAction().equals(MergeResult.ACTION_SKIP))
          folder = new SkippedMergeResultsFolder();
        else folder = new AdaptableMergeResultsFolder();
        folder.setFolder(folders[i]);
        folder.setCompressed(true);
        folder.setRootFolderLength(
            this.resource.getFullPath().makeRelative().toOSString().length());
        folder.setMergeOutput(this);
        folder.setMergeResult(mergeResult);
        conflictedCompressedFolders[i] = folder;
      }
    }
    return conflictedCompressedFolders;
  }

  public Object getAdapter(Class adapter) {
    Object object = new MergeAdapterFactory().getAdapter(this, adapter);
    return object;
  }

  public static void setInProgress(boolean inProgress) {
    MergeOutput.inProgress = inProgress;
  }

  public boolean isIncomplete() {
    return incomplete;
  }

  public void setIncomplete(boolean incomplete) {
    this.incomplete = incomplete;
  }

  public boolean isNormalEnd() {
    return normalEnd;
  }

  public void setNormalEnd(boolean normalEnd) {
    this.normalEnd = normalEnd;
  }

  public boolean isAbnormalEnd() {
    return abnormalEnd;
  }

  public void setAbnormalEnd(boolean abnormalEnd) {
    this.abnormalEnd = abnormalEnd;
  }

  public boolean isInProgress() {
    return !abnormalEnd && !normalEnd;
  }

  public void setLowerRevision(long lowerRevision) {
    this.lowerRevision = lowerRevision;
  }

  public void setUpperRevision(long upperRevision) {
    this.upperRevision = upperRevision;
  }

  public long getLowerRevision() {
    return lowerRevision;
  }

  public long getUpperRevision() {
    return upperRevision;
  }

  public String getWorkspaceUrl() {
    return workspaceUrl;
  }

  public void setWorkspaceUrl(String workspaceUrl) {
    this.workspaceUrl = workspaceUrl;
  }

  public long getWorkspaceRevision() {
    return workspaceRevision;
  }

  public void setWorkspaceRevision(long workspaceRevision) {
    this.workspaceRevision = workspaceRevision;
  }
}
