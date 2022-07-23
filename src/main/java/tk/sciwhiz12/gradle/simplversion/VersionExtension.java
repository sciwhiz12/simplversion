package tk.sciwhiz12.gradle.simplversion;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VersionExtension {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final Logger LOGGER = Logging.getLogger(VersionExtension.class);

    private final Project project;

    private boolean stripBranchPrefix = true;
    private List<String> customPrefixes = new ArrayList<>();
    private boolean useDirty = false;
    private int snapshotIncrementPosition = 0;
    private Spec<VersionInformation> skipIncrement = Specs.satisfyNone();

    private boolean parsedVersion = false;

    private String timestamp = "1970-01-01T00:00:00+00:00";
    private String commitId = "0000000000000000000000000000000000000000";
    private String abbrevId = "000000";
    private String rawVersion = "0.0.0";
    private boolean snapshot = true;
    private String classifiers = "-UNKNOWN";

    public VersionExtension(Project project) {
        this.project = project;
    }

    private void calculateVersion() {
        if (parsedVersion) return;
        parsedVersion = true;

        try (Repository repository = new FileRepositoryBuilder()
                .readEnvironment()
                .findGitDir(project.getProjectDir())
                .setMustExist(true).build()) {
            boolean hasCommitId = false;
            String branchName = null;

            Ref head = repository.exactRef(Constants.HEAD);
            if (head != null && head.isSymbolic()) {
                branchName = Repository.shortenRefName(head.getTarget().getName());
            }

            // Find HEAD commit and extract ID and timestamp
            try (ObjectReader reader = repository.newObjectReader()) {
                final ObjectId headCommitId = repository.resolve(Constants.HEAD);
                final RevCommit headCommit = repository.parseCommit(headCommitId);

                final PersonIdent identity = headCommit.getCommitterIdent();
                final OffsetDateTime date = OffsetDateTime.ofInstant(identity.getWhen().toInstant(), identity.getTimeZone().toZoneId());

                timestamp = FORMATTER.format(date);
                commitId = headCommit.name();
                abbrevId = reader.abbreviate(headCommitId).name();
                hasCommitId = true;
            } catch (Exception e) {
                LOGGER.info("Failed to get commit ID, may be in detached HEAD state: %s", e);
            }

            final Git git = Git.wrap(repository);
            final String describe = git.describe().setTags(true).setLong(true).call();

            // By default, the classifier is set to the commit ID if available
            classifiers = hasCommitId ? '+' + (abbrevId != null ? abbrevId : commitId) : "";

            if (describe != null) {
                String descVer;
                String descCount;

                final int lastSep = describe.lastIndexOf("-");
                final String allExceptLast = describe.substring(0, lastSep);
                final int secondToLastSep = allExceptLast.lastIndexOf("-");
                descVer = allExceptLast.substring(0, secondToLastSep);
                descCount = allExceptLast.substring(secondToLastSep + 1);

                if (stripBranchPrefix) {
                    descVer = tryStripPrefix(descVer, branchName);
                }

                for (String prefix : customPrefixes) {
                    String prevVersion = descVer;
                    descVer = tryStripPrefix(descVer, prefix);
                    if (!prevVersion.equals(descVer)) break; // Changed, so skip out
                }

                rawVersion = descVer.startsWith("v") ? descVer.substring(1) : descVer;
                final int commitCount = Integer.parseInt(descCount);
                final boolean dirty = !git.status().call().isClean();

                if (commitCount == 0) {
                    snapshot = dirty;
                    classifiers = "";
                } else if (dirty && useDirty) {
                    classifiers += ".dirty";
                }

            }

            final VersionInformation skipIncrementVerisonInfo =
                    new VersionInformation(rawVersion, snapshot, classifiers, timestamp, commitId);

            if (snapshot) {
                if (!classifiers.equals("-UNKNOWN") && !rawVersion.equals("0.0.0")
                        && snapshotIncrementPosition != 0
                        && !skipIncrement.isSatisfiedBy(skipIncrementVerisonInfo)) {
                    final String[] rawVersionSplit = rawVersion.split("[\\-+_]", 2);
                    String[] versionSplit = rawVersionSplit[0].split("\\.");
                    if (versionSplit.length >= Math.abs(snapshotIncrementPosition)) {
                        int i = snapshotIncrementPosition < 0
                                ? versionSplit.length + snapshotIncrementPosition
                                : snapshotIncrementPosition - 1;
                        try {
                            versionSplit[i] = Integer.toString(Integer.parseInt(versionSplit[i]) + 1);

                            boolean hasExtra = rawVersionSplit.length > 1;
                            if (hasExtra) {
                                rawVersion = String.join(String.valueOf(getRawVersion().charAt(rawVersionSplit[0].length())),
                                        new ArrayList<>(Arrays.asList(String.join(".", versionSplit), rawVersionSplit[1])));
                            } else {
                                rawVersion = String.join(".", versionSplit);
                            }

                        } catch (NumberFormatException ignored) {
                        }

                    }

                }

                classifiers = "-SNAPSHOT" + classifiers;
            }

        } catch (Exception e) {
            LOGGER.warn("Exception while getting version info from Git: {}", e.toString());
        }
    }

    private String tryStripPrefix(String version, @Nullable String prefix) {
        if (prefix == null) return version;

        // If the version is shorter or equal to the prefix, the prefix + sep won't match
        if (version.length() <= prefix.length() + 1) return version;

        if (!version.startsWith(prefix)) return version;

        char separator = version.charAt(prefix.length());
        // Only accept these two as possible separators between prefix and version
        if (separator != '/' && separator != '-') return version;

        // Strip prefix and separator
        return version.substring(prefix.length() + 1);
    }

    public void setStripBranchPrefix(boolean stripBranchPrefix) {
        this.stripBranchPrefix = stripBranchPrefix;
    }

    public boolean getStripBranchPrefix() {
        return stripBranchPrefix;
    }

    public void setCustomPrefix(@Nullable String customPrefix) {
        if (customPrefix != null && customPrefix.isEmpty()) return;

        if (customPrefix == null) {
            this.customPrefixes.clear();
        } else {
            this.customPrefixes.add(customPrefix);
        }
    }

    public void customPrefix(@Nullable String customPrefix) {
        if (customPrefix != null && customPrefix.isEmpty()) return;

        if (customPrefix != null) this.customPrefixes.add(customPrefix);
    }

    public List<String> getCustomPrefixes() {
        return customPrefixes;
    }

    public void setUseDirty(boolean useDirty) {
        this.useDirty = useDirty;
    }

    public boolean getUseDirty() {
        return useDirty;
    }

    /**
     * @param incrementPositionIfSnapshot the one-based position to increment, or {@code 0} to disable. Negative values
     *                                    are interpreted as positions starting from the end; {@code -1} would be the last number in the version, {@code -2}
     *                                    would be the second-to-last position, and so on.
     */
    public void incrementPositionIfSnapshot(int incrementPositionIfSnapshot) {
        this.setIncrementPositionIfSnapshot(incrementPositionIfSnapshot);
    }

    /**
     * @param snapshotIncrementPosition the one-based position to increment, or {@code 0} to disable. Negative values
     *                                  are interpreted as positions starting from the end; {@code -1} would be the last number in the version, {@code -2}
     *                                  would be the second-to-last position, and so on.
     */
    public void setIncrementPositionIfSnapshot(int snapshotIncrementPosition) {
        this.snapshotIncrementPosition = snapshotIncrementPosition;
    }

    public int getSnapshotIncrementPosition() {
        return snapshotIncrementPosition;
    }

    public void skipIncrementForClassifiers(final List<String> classifiers) {
        skipIncrement(ver -> classifiers.stream().anyMatch(classifier -> ver.getRawVersion().contains("-" + classifier)));
    }

    public void skipIncrementForClassifiers(String... classifiers) {
        skipIncrementForClassifiers(Arrays.asList(classifiers));
    }

    public void skipIncrement(Spec<VersionInformation> skipIncrement) {
        this.setSkipIncrement(skipIncrement);
    }

    public void setSkipIncrement(Spec<VersionInformation> skipIncrement) {
        this.skipIncrement = skipIncrement;
    }

    public Spec<VersionInformation> getSkipIncrement() {
        return skipIncrement;
    }

    public String getVersion() {
        calculateVersion();
        return this.rawVersion + this.classifiers;
    }

    public String getRawVersion() {
        calculateVersion();
        return this.rawVersion;
    }

    public String getSimpleVersion() {
        calculateVersion();
        return this.rawVersion + (this.snapshot ? "-SNAPSHOT" : "");
    }

    public String getCommitTimestamp() {
        calculateVersion();
        return this.timestamp;
    }

    public String getAbbreviatedCommitId() {
        calculateVersion();
        return this.abbrevId;
    }

    public String getCommitId() {
        calculateVersion();
        return this.commitId;
    }

    public String getClassifiers() {
        calculateVersion();
        return this.classifiers;
    }

    public boolean isSnapshot() {
        calculateVersion();
        return this.snapshot;
    }

    public void setCustomPrefixes(List<String> customPrefixes) {
        this.customPrefixes = customPrefixes;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getAbbrevId() {
        return abbrevId;
    }

    public void setAbbrevId(String abbrevId) {
        this.abbrevId = abbrevId;
    }

    public void setRawVersion(String rawVersion) {
        this.rawVersion = rawVersion;
    }

    public boolean getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(boolean snapshot) {
        this.snapshot = snapshot;
    }

    public void setClassifiers(String classifiers) {
        this.classifiers = classifiers;
    }

    public static class VersionInformation {
        private final String rawVersion;
        private final boolean snapshot;
        /**
         * The classifiers of the version. This does not include the automatically prepended {@code -SNAPSHOT} classifier
         * when {@link #snapshot} is {@code true}.
         */
        private final String classifiers;
        private final String commitTimestamp;
        private final String commitId;

        public VersionInformation(String rawVersion, boolean snapshot, String classifiers, String commitTimestamp, String commitId) {
            this.rawVersion = rawVersion;
            this.snapshot = snapshot;
            this.classifiers = classifiers;
            this.commitTimestamp = commitTimestamp;
            this.commitId = commitId;
        }

        public String getRawVersion() {
            return rawVersion;
        }

        public boolean getSnapshot() {
            return snapshot;
        }

        public boolean isSnapshot() {
            return snapshot;
        }

        public String getClassifiers() {
            return classifiers;
        }

        public String getCommitTimestamp() {
            return commitTimestamp;
        }

        public String getCommitId() {
            return commitId;
        }
    }
}
