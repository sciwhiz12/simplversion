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
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class VersionExtension {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final Logger LOGGER = Logging.getLogger(VersionExtension.class);

    private final Project project;

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

        this.getStripBranchPrefix().convention(true);
        this.getSnapshotIncrementPosition().convention(0);
    }

    private void calculateVersion() {
        if (parsedVersion) return;
        parsedVersion = true;

        this.getStripBranchPrefix().finalizeValue();
        this.getCustomPrefixes().finalizeValue();
        this.getDirtySuffix().finalizeValue();
        this.getSnapshotIncrementPosition().finalizeValue();

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

                if (getStripBranchPrefix().get()) {
                    descVer = tryStripPrefix(descVer, branchName);
                }

                for (String prefix : this.getCustomPrefixes().get()) {
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
                } else if (dirty) {
                    final String suffix = getDirtySuffix().getOrNull();
                    if (suffix != null && suffix.isEmpty()) {
                        classifiers += suffix;
                    }
                }

            }

            final VersionInformation skipIncrementVerisonInfo =
                    new VersionInformation(rawVersion, snapshot, classifiers, timestamp, commitId);

            if (snapshot) {

                final int snapshotIncrementPosition = getSnapshotIncrementPosition().get();
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

    /**
     * Whether to strip the current branch from the version as a prefix. This is related to but exists separately
     * from the {@link #getCustomPrefixes() custom prefixes}, and runs before any custom prefix is checked.
     */
    public abstract Property<Boolean> getStripBranchPrefix();

    /**
     * The list of custom version prefixes which will be removed from the version.
     *
     * <p>Each custom prefix is checked in the order specified in the list. Only a single custom prefix will be removed.
     * If a custom prefix has been found and stripped, any following custom prefixes are left unchecked.</p>
     *
     * <p>The prefix is only recognized if it is separated from the rest of the version by a separator character
     * ({@code -} or {@code /}).</p>
     *
     * @see #getStripBranchPrefix()
     */
    public abstract ListProperty<String> getCustomPrefixes();

    /**
     * Adds a new custom prefix.
     *
     * @param customPrefix a custom prefix
     * @see #getCustomPrefixes()
     */
    public void setCustomPrefix(String customPrefix) {
        getCustomPrefixes().add(customPrefix);
    }

    /**
     * Adds a new custom prefix.
     *
     * @param customPrefix a custom prefix
     * @see #getCustomPrefixes()
     */
    public void customPrefix(String customPrefix) {
        getCustomPrefixes().add(customPrefix);
    }

    /**
     * The suffix to be appended to the version string as a classifier if the environment has uncommitted changes. If
     * this is an empty or absent string, no suffix will be appended. This suffix will not be appended if the current
     * version is a tagged version; in that case, the environment's dirtiness will be reflected by marking the version
     * as a snapshot.
     */
    public abstract Property<String> getDirtySuffix();

    /**
     * The position in the version to be incremented if the version is marked as a snapshot.
     *
     * <p>Positive values are interpreted starting from the beginning, while negative values are interpreted starting
     * from the end. For example, {@code 2} would be taken as the second position from the beginning ({@code 2} in
     * {@code 1.2.3.4}), while {@code -2} would be taken as the second-to-last position or second position from the end
     * ({@code 3} in {@code 1.2.3.4}).</p>
     *
     * <p>A value of zero means no position will be incremented. If the value indicates a position which cannot be
     * present in the version (e.g. {@code 5} in {@code 1.2.3}), then no position will be incremented.</p>
     *
     * @see #getSkipIncrement()
     */
    public abstract Property<Integer> getSnapshotIncrementPosition();

    /**
     * @param incrementPositionIfSnapshot the one-based position to increment, or {@code 0} to disable. Negative values
     *                                    are interpreted as positions starting from the end; {@code -1} would be the last number in the version, {@code -2}
     *                                    would be the second-to-last position, and so on.
     * @see #getSnapshotIncrementPosition()
     */
    public void incrementPositionIfSnapshot(int incrementPositionIfSnapshot) {
        this.setIncrementPositionIfSnapshot(incrementPositionIfSnapshot);
    }

    /**
     * @param snapshotIncrementPosition the one-based position to increment, or {@code 0} to disable. Negative values
     *                                  are interpreted as positions starting from the end; {@code -1} would be the last number in the version, {@code -2}
     *                                  would be the second-to-last position, and so on.
     * @see #getSnapshotIncrementPosition()
     */
    public void setIncrementPositionIfSnapshot(int snapshotIncrementPosition) {
        this.getSnapshotIncrementPosition().set(snapshotIncrementPosition);
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
