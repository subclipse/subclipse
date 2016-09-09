## master

Moved project to Github and pruned out binaries and some of the old content.
Split svnClientAdapter and svnAnt into their own repositories.  Rearranged
folder structure to take advantage of all of this and setup build using Maven
and Tycho.  Setup automated builds using TravisCI and post snapshots and
release builds to Bintray.

As part of moving the build to Tycho, made some additional changes that just
made this easier:

*   Updated minimum required Eclipse version to 4.2/Juno
*   Updated Java requirement to Java 1.6 or later
*   Increased Subclipse version number to 4.2.0
*   Simplified feature/plugin structure.  Everything required is part of the Subclipse feature.  This might have some impact on upgrades.
*   Moved the CollabNet Merge plugin into this repository and included it in the feature.
*   Setup a repository for m2eclipse plugin and include it on our update site


Fixes:

*   Fix NPE after attempting to switch to bad URL ([1654](http://subclipse.tigris.org/issues/show_bug.cgi?id=1654))
*   Fix recursive revert of folders that were removed outside Eclipse ([1649](http://subclipse.tigris.org/issues/show_bug.cgi?id=1649))


## Version 1.10.13
March 18, 2016

*   Don't set working copy depth when updating from sync view - follow up. ([1648](http://subclipse.tigris.org/issues/show_bug.cgi?id=1648))


## Version 1.10.12
March 9, 2016

*   Show children of incoming folder additions in sync view. ([1646](http://subclipse.tigris.org/issues/show_bug.cgi?id=1646))
*   Don't set working copy depth when updating from sync view. ([1642](http://subclipse.tigris.org/issues/show_bug.cgi?id=1642))
*   Avoid redundant updates of status cache. ([1540](http://subclipse.tigris.org/issues/show_bug.cgi?id=1540))


## Version 1.10.11
February 4, 2016

*   JavaHL 1.8.15/1.9.3
*   SVNKit 1.8.12
*   Only include resources from active change set in commit dialog
*   Do not update recursively from Synchronize view. ([1642](http://subclipse.tigris.org/issues/show_bug.cgi?id=1642))
*   Fix recursive revert of deleted folders from explorer views. ([1643](http://subclipse.tigris.org/issues/show_bug.cgi?id=1643))


## Version 1.10.10
August 21, 2015

*   JavaHL 1.8.14
*   Fix potential deadlocks between switch and other operations. ([1633](http://subclipse.tigris.org/issues/show_bug.cgi?id=1633))


## Version 1.10.9
February 12, 2015

*   SVNKit 1.8.8
*   Exception proof repository sorter. ([1616](http://subclipse.tigris.org/issues/show_bug.cgi?id=1616))


## Version 1.10.8
January 7, 2015

*   Use a version number-aware sort algorithm for path names. ([1608](http://subclipse.tigris.org/issues/show_bug.cgi?id=1608))
*   Fix possible NPE on Linux startup when JavaHL is not available. ([1609](http://subclipse.tigris.org/issues/show_bug.cgi?id=1609))


## Version 1.10.7
December 19, 2014

*   JavaHL 1.8.11
*   Support gnome-keyring on Linux when SVN 1.8.11 is used. ([1606](http://subclipse.tigris.org/issues/show_bug.cgi?id=1606))
*   Remove obsolete workaround that sets depth=infinite when committing folder deletions. ([1607](http://subclipse.tigris.org/issues/show_bug.cgi?id=1607))


## Version 1.10.6
October 22, 2014

*   JavaHL 1.8.10
*   SVNKit 1.8.6
*   Don't try to refresh closed projects. If refresh fails for any reason, log the error and continue. ([1602](http://subclipse.tigris.org/issues/show_bug.cgi?id=1602))
*   Fix compare from history view when from/to URL are changed from file to directory or vice versa. ([1597](http://subclipse.tigris.org/issues/show_bug.cgi?id=1597))
*   Fix URI encoding to handle square brackets. ([1604](http://subclipse.tigris.org/issues/show_bug.cgi?id=1604))


## Version 1.10.5
May 15, 2014

*   JavaHL 1.8.9
*   SVNKit 1.8.5
*   Fix potential deadlocks between update and other operations. ([1541](http://subclipse.tigris.org/issues/show_bug.cgi?id=1541))
*   Don't do a local refresh after a failed commit. ([1541](http://subclipse.tigris.org/issues/show_bug.cgi?id=1541))
*   Fix compare of multiple files to base revision when one or more file is unversioned. ([1590](http://subclipse.tigris.org/issues/show_bug.cgi?id=1590))
*   Fix show annotations NPE when revision range includes unchanged lines. ([1589](http://subclipse.tigris.org/issues/show_bug.cgi?id=1589))
*   Automatically map nested projects to repository provider when parent project shared. ([1593](http://subclipse.tigris.org/issues/show_bug.cgi?id=1593))


## Version 1.10.4
Feb 25, 2014

*   JavaHL 1.8.8
*   SVNKit 1.8.3
*   Ensure that label decorations refresh automatically after switch. ([1565](http://subclipse.tigris.org/issues/show_bug.cgi?id=1565))
*   Improvements in nested resource support. ([1567](http://subclipse.tigris.org/issues/show_bug.cgi?id=1567))
*   Preference to control how many previously-entered comments are saved. ([1569](http://subclipse.tigris.org/issues/show_bug.cgi?id=1569))
*   Include working sets selection in checkout wizard (Eclipse 3.4+). ([1167](http://subclipse.tigris.org/issues/show_bug.cgi?id=1167))
*   Null check before all JhlConverter switch statements. ([1570](http://subclipse.tigris.org/issues/show_bug.cgi?id=1570))
*   Check that project is accessible before trying to autoshare it.
*   Fix logic for finding closest property for bugtraq and other properties. ([1578](http://subclipse.tigris.org/issues/show_bug.cgi?id=1578))


## Version 1.10.3
Oct 16, 2013

*   SVNKit 1.8.0-Beta2
*   Performance - make check for read-only file decorators optional and off by default. ([1540](http://subclipse.tigris.org/issues/show_bug.cgi?id=1540))
*   Improvements to compare with branch to resolve tree conflict scenarios. ([1549](http://subclipse.tigris.org/issues/show_bug.cgi?id=1549))
*   Enhance Replace with Branch/Tag dialog. ([1552](http://subclipse.tigris.org/issues/show_bug.cgi?id=1552))


## Version 1.10.2
Sept 3, 2013

*   Update JavaHL to SVN 1.8.3
*   SVNKit 1.8.0-Beta
*   Fix RevisionRange passed to merge API so that it will correctly determine type of merge to run. ([1544](http://subclipse.tigris.org/issues/show_bug.cgi?id=1544))
*   Don't log an error when trying to get info for an unversioned folder. ([1538](http://subclipse.tigris.org/issues/show_bug.cgi?id=1538))
*   Fix SVNMoveDeleteHook so that it does not interfere with moving a file or folder to a project that is not under version control. ([1536](http://subclipse.tigris.org/issues/show_bug.cgi?id=1536))
*   Null proof CleanupResourcesCommand. ([1535](http://subclipse.tigris.org/issues/show_bug.cgi?id=1535))
*   Fix potential NPE in history view when log entry containing a bug id is selected (with certain bugtraq:logregex patterns). ([1539](http://subclipse.tigris.org/issues/show_bug.cgi?id=1539))
*   Don't show multiple prompts when files from multiple projects added to svn:ignore from sync view. ([1543](http://subclipse.tigris.org/issues/show_bug.cgi?id=1543))


## Version 1.10.1
July 25, 2013

*   Update JavaHL to SVN 1.8.1
*   Fix commit error when bugtraq:message is defined, but not bugtraq:label. ([1526](http://subclipse.tigris.org/issues/show_bug.cgi?id=1526))
*   Automatically refesh SVN Properties view after property is added, deleted or modified. ([1527](http://subclipse.tigris.org/issues/show_bug.cgi?id=1527))
*   When updating status cache, do not attempt to get statuses from unversioned folder. ([1531](http://subclipse.tigris.org/issues/show_bug.cgi?id=1531))
*   Null proof CleanupResourcesCommand. ([1535](http://subclipse.tigris.org/issues/show_bug.cgi?id=1535))


## Version 1.10.0
June 18, 2013

*   Update JavaHL to SVN 1.8.0
*   Include svn:auto-props and svn:global-ignores in list of known properties. ([1512](http://subclipse.tigris.org/issues/show_bug.cgi?id=1512))
*   Custom decorator for moved resources. ([1510](http://subclipse.tigris.org/issues/show_bug.cgi?id=1510))
*   Show where item was moved from/to in Commit/Revert dialogs, and Sync view. ([1510](http://subclipse.tigris.org/issues/show_bug.cgi?id=1510))
*   Tree conflict resolution enhancements. ([1517](http://subclipse.tigris.org/issues/show_bug.cgi?id=1517))
*   Use new inherited properties support to get bugtraq properties rather than walking the working copy ourselves. ([1511](http://subclipse.tigris.org/issues/show_bug.cgi?id=1511))
*   Use new inherited properties support to check for DeferFileDelete property, rather than walking up the working copy ourselves. ([1511](http://subclipse.tigris.org/issues/show_bug.cgi?id=1511))
*   Use diff --summarize to compare working copy to latest from repository. ([1516](http://subclipse.tigris.org/issues/show_bug.cgi?id=1516))
*   Use diff --summarize to compare with Branch/Tag. ([1516](http://subclipse.tigris.org/issues/show_bug.cgi?id=1516))
*   Fix exporting SVN Repositories preferences. ([1520](http://subclipse.tigris.org/issues/show_bug.cgi?id=1520))
*   Fix potential deadlocks between RevertResourceManager jobs and other Subclipse jobs. ([1523](http://subclipse.tigris.org/issues/show_bug.cgi?id=1523))# 


## Version 1.8.22
May 31, 2013

*   Update JavaHL to SVN 1.7.10
*   Fix problems where multiple status cache refresh jobs could lock up Eclipse. ([1473](http://subclipse.tigris.org/issues/show_bug.cgi?id=1473))
*   Respect timezone when getting annotation times from revision properties. ([1509](http://subclipse.tigris.org/issues/show_bug.cgi?id=1509))


## Version 1.8.21
May 24, 2013

*   Provide extension point that enables 3rd party plug-ins to allow selection from repository hosting provider.
*   Avoid call to File.getCanonicalPath. ([1507](http://subclipse.tigris.org/issues/show_bug.cgi?id=1507))
*   Don't try to parse a null or incorrectly formatted date.
*   Thread safe usage of DateFormat and SimpleDateFormat.
*   Add link to CloudForge signup to repository creation pages.


## Version 1.8.20
April 15, 2013

*   Provide context menu for commit message area. ([981](http://subclipse.tigris.org/issues/show_bug.cgi?id=981))
*   Fix NPE in Replace with Latest From Repository. ([1494](http://subclipse.tigris.org/issues/show_bug.cgi?id=1494))
*   Fix incorrect decoration of svn:ignore folders after update. ([1499](http://subclipse.tigris.org/issues/show_bug.cgi?id=1499))
*   Delegate RemoteFileEditorInput.getAdapter to platform adapter manager so that custom editors can adapt the input to IStorageEditorInput. ([1501](http://subclipse.tigris.org/issues/show_bug.cgi?id=1501))
*   Fix properties page so that it doesn't generate error log entry when resource is ignored. ([1503](http://subclipse.tigris.org/issues/show_bug.cgi?id=1503))


## Version 1.8.19
April 5, 2013

*   Update JavaHL to 1.7.9
*   Fix NPE in SVNHistoryPage during start up when "link to editor and selection" is selected and one or more editors was open at previous shutdown. ([1493](http://subclipse.tigris.org/issues/show_bug.cgi?id=1493))
*   Fix open/focus of History view using shortcuts. ([1491](http://subclipse.tigris.org/issues/show_bug.cgi?id=1491))
*   Use String.replace rather than String.replaceAll when resolving commit message. ([1482](http://subclipse.tigris.org/issues/show_bug.cgi?id=1482))
*   Provide a scheduling rule when scheduling update operation. ([1494](http://subclipse.tigris.org/issues/show_bug.cgi?id=1494))
*   Additional null proofing in decorator. ([1440](http://subclipse.tigris.org/issues/show_bug.cgi?id=1440))
*   Fix refresh of Sync view after updating all incoming changes with Show Out of Date Folders preference turned on. ([1490](http://subclipse.tigris.org/issues/show_bug.cgi?id=1490))
*   Use MultiRule scheduling rule when refreshing status cache. ([1473](http://subclipse.tigris.org/issues/show_bug.cgi?id=1473))
*   Remove old code for storing username and password in the keyring. ([1485](http://subclipse.tigris.org/issues/show_bug.cgi?id=1485))
*   When comparing versions of a file from the history view, set the encoding based on the local workspace resource. ([1442](http://subclipse.tigris.org/issues/show_bug.cgi?id=1442))


## Version 1.8.18
January 9, 2013

*   Update SVNKit to 1.7.8
*   Fix branch/tag for single resource selection. ([1476](http://subclipse.tigris.org/issues/show_bug.cgi?id=1476))
*   Fix NPE building revision graph cache.


## Version 1.8.17
December 20, 2012

*   Windows JavaHL binaries for SVN 1.7.8
*   Update SVNKit to 1.7.6
*   Fix NPE on certain actions when workspace includes remote projects. ([1455](http://subclipse.tigris.org/issues/show_bug.cgi?id=1455))
*   Don't show help button on wizards and dialogs.
*   Fix NPE when trying to copy a remote resource for which svn:author property is not set. ([1460](http://subclipse.tigris.org/issues/show_bug.cgi?id=1460))
*   Set merge command peg revision when reverting a revision's changes from History View. ([1467](http://subclipse.tigris.org/issues/show_bug.cgi?id=1467))
*   Improved resolution of target URL for branch/tag when a single resource is selected. ([1462](http://subclipse.tigris.org/issues/show_bug.cgi?id=1462))


## Version 1.8.16
August 21, 2012

*   Schedule a non-locking job to refresh the status cache rather than doing it from within a resource change listener. ([1450](http://subclipse.tigris.org/issues/show_bug.cgi?id=1450))


## Version 1.8.15
August 15, 2012

*   JavaHL binaries updated to Subversion 1.7.6
*   Eliminate recursive retrieval of properties from repository when adjusting svn:externals properties to fixed revisions after creating branch from working copy. ([1436](http://subclipse.tigris.org/issues/show_bug.cgi?id=1436))
*   Fix cleanup action label. ([1437](http://subclipse.tigris.org/issues/show_bug.cgi?id=1437))
*   Workaround for SVNKit bug that results in Status.isConflicted == false for old format working copies.


## Version 1.8.14
July 16, 2012

*   When Tree view is selected for Changes section of Commit Dialog, do not show the resource tree initially collapsed. ([1433](http://subclipse.tigris.org/issues/show_bug.cgi?id=1433))
*   Fix History View get next/all revisions so that they work after "Include merged revisions" option is toggled. ([1434](http://subclipse.tigris.org/issues/show_bug.cgi?id=1434))
*   Remove JDT dependencies.


## Version 1.8.13
July 5, 2012

*   Do not attempt local refresh from move delete hook. ([1430](http://subclipse.tigris.org/issues/show_bug.cgi?id=1430))
*   Make sure reentrant lock is released if checkin fails. ([1432](http://subclipse.tigris.org/issues/show_bug.cgi?id=1432))


## Version 1.8.12
June 21, 2012

*   Fix schedule rule violation after file is locked by file modification validator. ([1427](http://subclipse.tigris.org/issues/show_bug.cgi?id=1427))
*   Only lock the projects that are being committed during a commit. ([1425](http://subclipse.tigris.org/issues/show_bug.cgi?id=1425))
*   Don't use SynchronizationStateTester to check for outgoing changes in Working Sets as this can cause updating of the status cache. Instead, check to see if any of the working set projects are dirty. ([1424](http://subclipse.tigris.org/issues/show_bug.cgi?id=1424))


## Version 1.8.11
June 7, 2012

*   Set peg revision to revision entered in compare dialog when comparing to branch/tag. ([1421](http://subclipse.tigris.org/issues/show_bug.cgi?id=1421))
*   Show post-commit hook error messages. ([1418](http://subclipse.tigris.org/issues/show_bug.cgi?id=1418))
*   Never get status using api when checking to see if folder has dirty children. ([1422](http://subclipse.tigris.org/issues/show_bug.cgi?id=1422))
*   When comparing to a revision, set right (remote) encoding to match left (local) encoding. ([1423](http://subclipse.tigris.org/issues/show_bug.cgi?id=1423))


## Version 1.8.10
May 29, 2012

*   JavaHL binaries for SVN 1.7.5
*   SVNKit library updated to latest version
*   Refresh status cache for resources that are reverted in the process of generating a diff. ([1410](http://subclipse.tigris.org/issues/show_bug.cgi?id=1410))
*   Fix SVNActiveChangeSetCollector to ignore derived resources. ([1411](http://subclipse.tigris.org/issues/show_bug.cgi?id=1411))
*   Enable Override and Update for outgoing deletions. ([1413](http://subclipse.tigris.org/issues/show_bug.cgi?id=1413))


## Version 1.8.9
May 1, 2012

*   Fix Compare SVN Properties when project name does not match repository location. ([1407](http://subclipse.tigris.org/issues/show_bug.cgi?id=1407))
*   Fix Compare with Base Revision to not show hidden resources as local deletions. ([1406](http://subclipse.tigris.org/issues/show_bug.cgi?id=1406))
*   Only read status from cache once when getting URL for LocalResource.
*   Optimizations to context menu option enablement to avoid repeated reading of status cache.
*   Fix potential NPEs in finally blocks when client adapter has not been instantiated. ([1408](http://subclipse.tigris.org/issues/show_bug.cgi?id=1408))
*   Fix potential loop when refreshing status cache. ([1402](http://subclipse.tigris.org/issues/show_bug.cgi?id=1402))
*   Do not show changes in Sync view if they are hidden by resource filters. ([1409](http://subclipse.tigris.org/issues/show_bug.cgi?id=1409))


## Version 1.8.8
April 17, 2012

*   Fix NPE during status update. ([1392](http://subclipse.tigris.org/issues/show_bug.cgi?id=1392))
*   Do not get remote properties recursively. ([1393](http://subclipse.tigris.org/issues/show_bug.cgi?id=1393))
*   Fix exclusion of deletions that are hidden by resource filters during commit, sync. ([1383](http://subclipse.tigris.org/issues/show_bug.cgi?id=1383))
*   Compare SVN Properties option for resources, sync view items, SVN Repositories view remote resources. ([1391](http://subclipse.tigris.org/issues/show_bug.cgi?id=1391))
*   Add a preview section to the Generate ChangeLog dialog. ([1397](http://subclipse.tigris.org/issues/show_bug.cgi?id=1397))
*   Don't do a recursive revert if there are modified resources that are hidden by resource filters. ([1398](http://subclipse.tigris.org/issues/show_bug.cgi?id=1398))
*   Get status from cache when checking to see if resource is ignored. ([1402](http://subclipse.tigris.org/issues/show_bug.cgi?id=1402))
*   Don't auto-commit when Shift-Enter is pressed in the commit dialog's comment area. ([1404](http://subclipse.tigris.org/issues/show_bug.cgi?id=1404))


## Version 1.8.7
March 15, 2012

*   Undo change to condition refresh after Update based on notification of revision,as this change breaks refresh when there are mixed revisions. ([1375](http://subclipse.tigris.org/issues/show_bug.cgi?id=1375))
*   Reliable filtering of child folders. ([1375](http://subclipse.tigris.org/issues/show_bug.cgi?id=1375))
*   Disable "Revert changes from revision X" history view option for first revisions. ([1381](http://subclipse.tigris.org/issues/show_bug.cgi?id=1381))
*   Add SVNSynchronizeParticipant as a property change listener during init rather than in the constructor. ([1380](http://subclipse.tigris.org/issues/show_bug.cgi?id=1380))
*   Added "Ignore managed derived resources" preference. ([535](http://subclipse.tigris.org/issues/show_bug.cgi?id=535))
*   Fix scheduling rule problem when doing a revert on a project that causes changes in nested project to be reverted. ([1387](http://subclipse.tigris.org/issues/show_bug.cgi?id=1387))


## Version 1.8.6
March 9, 2012

*   JavaHL updated to Subversion 1.7.4.
*   SVNKit 1.7.0 updated to Beta3.
*   Prompt to upgrade working copy when a project from an SVN 1.6.x working copy is imported. ([1347](http://subclipse.tigris.org/issues/show_bug.cgi?id=1347))
*   Fix revert of added folders to correctly refresh decorator. ([1372](http://subclipse.tigris.org/issues/show_bug.cgi?id=1372))
*   Fix "Compare with latest from Repository" for project, when project name is not the same as repository folder name. ([1374](http://subclipse.tigris.org/issues/show_bug.cgi?id=1374))
*   Fix Sync view refresh after file system deletions. ([1369](http://subclipse.tigris.org/issues/show_bug.cgi?id=1369))
*   Fix OperationManager to eliminate unnecessary refreshes. ([1375](http://subclipse.tigris.org/issues/show_bug.cgi?id=1375))
*   Do not pass resource deltas to another thread. ([1378](http://subclipse.tigris.org/issues/show_bug.cgi?id=1378))


## Version 1.8.5
February 13, 2012

*   JavaHL updated to Subversion 1.7.3.
*   Fix error when trying to checkout a project that already exists in the workspace to a new location when the existing project is closed and has no .project file. ([1350](http://subclipse.tigris.org/issues/show_bug.cgi?id=1350))
*   Make sure to end operation when checkout fails with an error. ([1351](http://subclipse.tigris.org/issues/show_bug.cgi?id=1351))
*   Validate file name before attempting to create patch. ([1352](http://subclipse.tigris.org/issues/show_bug.cgi?id=1352))
*   Improvements to folder compare from history view. ([1341](http://subclipse.tigris.org/issues/show_bug.cgi?id=1341))
*   Fix sync view to show file system deletions. ([1318](http://subclipse.tigris.org/issues/show_bug.cgi?id=1318))
*   If resource not found at HEAD revision, start looking at last known revision. ([1356](http://subclipse.tigris.org/issues/show_bug.cgi?id=1356))
*   If "Ignore changes to hidden resources" preference is true, then do not include resources that have been filtered out using Resource Filters when doing a commit, revert or synchronize. ([1321](http://subclipse.tigris.org/issues/show_bug.cgi?id=1321))
*   Expose ignore ancestry for switch. ([1358](http://subclipse.tigris.org/issues/show_bug.cgi?id=1358))
*   Do not contact contact repository when determining if all selected resources are from the same repository. ([1366](http://subclipse.tigris.org/issues/show_bug.cgi?id=1366))
*   Improvements to multiple project sharing. ([1371](http://subclipse.tigris.org/issues/show_bug.cgi?id=1371))


## Version 1.8.4
December 5, 2011

*   JavaHL updated to Subversion 1.7.2.
*   Automatically refresh current revision in history view when history review resource is updated. ([1338](http://subclipse.tigris.org/issues/show_bug.cgi?id=1338))
*   Checkout project to custom location. ([1339](http://subclipse.tigris.org/issues/show_bug.cgi?id=1339))
*   Null-proof change set collector. ([1345](http://subclipse.tigris.org/issues/show_bug.cgi?id=1345))
*   Do not do a recursive revert when selection is from a change set. ([1346](http://subclipse.tigris.org/issues/show_bug.cgi?id=1346))
*   Don't do recursive revert if selection includes externals. ([1346](http://subclipse.tigris.org/issues/show_bug.cgi?id=1346))
*   Refresh status cache after lock/unlock. ([1344](http://subclipse.tigris.org/issues/show_bug.cgi?id=1344))
*   Make sure scheduling rule used for repository operations accounts for nested projects. ([1348](http://subclipse.tigris.org/issues/show_bug.cgi?id=1348))


## Version 1.8.3
November 21, 2011

*   Fix Commit dialog so that unversioned decorator is not incorrecly applied to deletes.
*   Fix NPE when trying to revert Change Set. ([1326](http://subclipse.tigris.org/issues/show_bug.cgi?id=1326))
*   Fix local refresh of subfolders after Cleanup.
*   Fix NPE when updating with "local unversioned, incoming add" conflicts. ([1333](http://subclipse.tigris.org/issues/show_bug.cgi?id=1333))
*   Date format preference for label text decorations. ([1177](http://subclipse.tigris.org/issues/show_bug.cgi?id=1177),[1332](http://subclipse.tigris.org/issues/show_bug.cgi?id=1332))
*   Use SelectionListener on Tree rather than SelectionChangedListener on TreeViewer. ([1327](http://subclipse.tigris.org/issues/show_bug.cgi?id=1327))
*   Fix project/folder decoration to indicate that it is dirty if there are hidden, versioned children that are dirty. ([1335](http://subclipse.tigris.org/issues/show_bug.cgi?id=1335))
*   Add a preference to ignore hidden resources. ([1335](http://subclipse.tigris.org/issues/show_bug.cgi?id=1335))
*   Fix problem with caching of HEAD revision contents of remote file. ([1334](http://subclipse.tigris.org/issues/show_bug.cgi?id=1334))
*   Fix refresh of Sync view after update. ([1336](http://subclipse.tigris.org/issues/show_bug.cgi?id=1336))
*   Correctly set project location for new Eclipse project after checkout to a custom location. ([1339](http://subclipse.tigris.org/issues/show_bug.cgi?id=1339))


## Version 1.8.2
October 25, 2011

*   Fix bug where Subclipse could delete .svn folder in parent of Eclipse projects.
*   Do not show files and folders as outgoing deletions in the Sync view if they have not actually been deleted but are just filtered using Eclipse resource filters. ([1321](http://subclipse.tigris.org/issues/show_bug.cgi?id=1321))


## Version 1.8.1
October 23, 2011

*   JavaHL Win32/Win64 binaries from SVN 1.7.1
*   Check for test mode before putting up modal dialogs.
*   Look for commit comment properties by starting at selected resource and working up through ancestors. ([1317](http://subclipse.tigris.org/issues/show_bug.cgi?id=1317))
*   Catch all exceptions when calling out to SynchronizationStateTester from decorator. ([1315](http://subclipse.tigris.org/issues/show_bug.cgi?id=1315))
*   Fix sync view to include deletions made from file system. ([1318](http://subclipse.tigris.org/issues/show_bug.cgi?id=1318))
*   Eliminate a NPE in LocalResource.getUrl. ([1319](http://subclipse.tigris.org/issues/show_bug.cgi?id=1319))


## Version 1.8.0
October 17, 2011

*   JavaHL Win32/Win64 binaries from SVN 1.7.0 GA
*   Check for test mode before putting up modal dialogs.
*   Fix revert from Sync view to revert recursively if nothing was removed from revert dialog. ([1303](http://subclipse.tigris.org/issues/show_bug.cgi?id=1303))
*   Fix NPE trying to compare a new incoming file from sync view. ([1312](http://subclipse.tigris.org/issues/show_bug.cgi?id=1312))
*   Enable cancellation of Synchronize job.


## Version 1.7.5
September 13, 2011

*   JavaHL Win32/Win64 binaries from SVN 1.7.0-rc3
*   Fix display of log messages with non-ASCII characters. ([1299](http://subclipse.tigris.org/issues/show_bug.cgi?id=1299))
*   Properly encode to URI format before passing URL to API. ([1298](http://subclipse.tigris.org/issues/show_bug.cgi?id=1298))
*   When file encoding is not known, tell Eclipse compare UI it is UTF8.
*   Fix error handling in repository browse dialog. ([1301](http://subclipse.tigris.org/issues/show_bug.cgi?id=1301))


## Version 1.7.4
September 6, 2011

*   Fixed missing VS 2010 DLL's with JavaHL
*   Support for mine-conflict, theirs-conflict conflict resolution.
*   Fix NPE in String initialization when revprops author or message is null.


## Version 1.7.3
August 30, 2011

*   JavaHL Win32/Win64 binaries from SVN 1.7.0-rc2
*   Fix NPE trying trying to branch/tag from working copy with multiple resources selected.
*   Option to pin external revisions when creating branch from working copy. ([1294](http://subclipse.tigris.org/issues/show_bug.cgi?id=1294))
*   Refresh new project locally after checkout using New Project Wizard.
*   Support interactive conflict resolution for Update/Switch operations ([1295](http://subclipse.tigris.org/issues/show_bug.cgi?id=1295))
*   Refresh local status cache after Cleanup operation.
*   Notify the resource tree of the deleted folder so that proper notifications will be sent.
*   Fix refresh of synchronize view after commit of folder deletion.
*   Force refresh of parent folder decorator after deletion.
*   Include phantoms when asking eclipse for folder. ([1297](http://subclipse.tigris.org/issues/show_bug.cgi?id=1297))
*   Fix Sync view refresh after commit of deleted folders.
*   Fix status cache refresh after folder added.


## Version 1.7.2
August 10, 2011

*   JavaHL binaries from SVN 1.7.0-beta3
*   Addition of JavaHL Windows 64-bit binaries
*   Fix drag/drop, copy/paste over existing file so that target file does not end up as a scheduled delete. ([1275](http://subclipse.tigris.org/issues/show_bug.cgi?id=1275))
*   Fix missing borders in affected paths tables. ([1257](http://subclipse.tigris.org/issues/show_bug.cgi?id=1257))
*   When showing annotation, check for already open editor on resource that implements ITextEditorExtension4 (showRevisionInformation) rather the more specific AbstractDecoratedTextEditor. ([1282](http://subclipse.tigris.org/issues/show_bug.cgi?id=1282))
*   When resolving a "local add, incoming add upon merge" tree conflict, include the option to compare the working copy with merge source and optionally resolve the tree conflict when the compare editor is closed.. ([1279](http://subclipse.tigris.org/issues/show_bug.cgi?id=1279))
*   Fix suggestMergeSources. It was always returning an empty array.
*   "Suggest merge sources" preference. ([1250](http://subclipse.tigris.org/issues/show_bug.cgi?id=1250))
*   Checkout project and then create/open project over the checkout, rather than vice versa, to ensure that resource delta is not fired when project is in an invalid state.([1201](http://subclipse.tigris.org/issues/show_bug.cgi?id=1201))
*   When multiple projects checked out, defer opening them all until all of them are checked out. ([1201](http://subclipse.tigris.org/issues/show_bug.cgi?id=1201))
*   Friendly handling of failures to write to temp directory when opening a file in an external editor from the Repositories View. ([1135](http://subclipse.tigris.org/issues/show_bug.cgi?id=1135))
*   If project cannot be deleted due to locks on SQLite database, use SVNMoveDeleteHook to show a meaningful error message and cancel the project deletion.
*   If compare pane is open in commit dialog, then automatically populate it when selection changes. Likewise, if a file is selected when compare pane is opened, then automatically populate it. ([1265](http://subclipse.tigris.org/issues/show_bug.cgi?id=1265))
*   Truncate cached log messages for revision graph at 64K to work around Java limitation. ([1289](http://subclipse.tigris.org/issues/show_bug.cgi?id=1289))
*   Don't prompt to save dirty files if they are not related to the operation. ([1290](http://subclipse.tigris.org/issues/show_bug.cgi?id=1290))
*   When checking out a specific revision, check to see if the location has changed in the repository and adjust the URL if it has. ([1274](http://subclipse.tigris.org/issues/show_bug.cgi?id=1274))
*   Fix error when viewing revision of a deleted file from history view. ([1267](http://subclipse.tigris.org/issues/show_bug.cgi?id=1267))
*   Fix history view compare problem when comparing revisions from before branch was created. ([1248](http://subclipse.tigris.org/issues/show_bug.cgi?id=1248))
*   When switching to a specific revision, use that revision as the peg revision. ([1291](http://subclipse.tigris.org/issues/show_bug.cgi?id=1291))
*   Fix scheduling rule error in move delete hook when resolving a tree conflict and selecting option to delete a resource. ([1230](http://subclipse.tigris.org/issues/show_bug.cgi?id=1230))


## Version 1.7.1
July 20, 2011

*   JavaHL binaries from SVN 1.7.x branch @ r1148902
*   Fix typo in working copy upgrade message. ([1284](http://subclipse.tigris.org/issues/show_bug.cgi?id=1284))
*   Working copy upgrade notification message. ([1284](http://subclipse.tigris.org/issues/show_bug.cgi?id=1284))
*   Refresh target of update operation. ([1285](http://subclipse.tigris.org/issues/show_bug.cgi?id=1285))
*   Auto share imported project if it lives inside a working copy. ([1286](http://subclipse.tigris.org/issues/show_bug.cgi?id=1286))


## Version 1.7.0
July 15, 2011

*   JavaHL binaries for Subversion 1.7.0-beta1
*   New Upgrade option to upgrade working copy to 1.7 format.
*   Support for changes in SVN 1.7 working copy design.