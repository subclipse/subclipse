package org.tigris.subversion.subclipse.ui;



public class SvnVersion {
	private int major;
	private int minor;
	private int patch;
	private int revision;
	
	public static final SvnVersion VERSION_1_8_11 = new SvnVersion(1, 8, 11, 0);

	public SvnVersion() {
		super();
	}
	
	public SvnVersion(int major, int minor, int patch, int revision) {
		super();
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.revision = revision;
	}
	
	public SvnVersion(String version) {
		String[] split = version.split("\\.");
		if (split.length > 0) {
			major = Integer.parseInt(split[0]);
		}
		if (split.length > 1) {
			minor = Integer.parseInt(split[1]);
		}
		if (split.length > 2) {
			int index = split[2].indexOf(" ");
			if (index == -1) {
				patch = Integer.parseInt(split[2]);
			}
			else {
				patch = Integer.parseInt(split[2].substring(0, index));
				int revIndex = split[2].indexOf("(r");
				if (revIndex != -1) {
					revision = Integer.parseInt(split[2].substring(revIndex + 2).replace(")",""));
				}
			}
		}
	}
	
	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getPatch() {
		return patch;
	}

	public int getRevision() {
		return revision;
	}
	
	public boolean isNewerThanOrEqualTo(SvnVersion compareToVersion) {
		return this.compareTo(compareToVersion) >= 0;
	}
	
	public int compareTo(SvnVersion compareToVersion) {
		int from = 0;
		int to = 0;
		if (compareToVersion.getMajor() != major) {
			from = major;
			to = compareToVersion.getMajor();
		}
		else if (compareToVersion.getMinor() != minor) {
			from = minor;
			to = compareToVersion.getMinor();
		}
		else if (compareToVersion.getPatch() != patch) {
			from = patch;
			to = compareToVersion.getPatch();
		}
		else {
			from = revision;
			to = compareToVersion.getRevision();
		}
		return Integer.valueOf(from).compareTo(Integer.valueOf(to));
	}

	public String toString() {
		return major + "." + minor + "." + patch + " (r" + revision + ")";
	}

}
