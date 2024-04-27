/*
 * simplversion - Copyright (c) 2022 sciwhiz12
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.sciwhiz12.gradle.simplversion;

import org.eclipse.jgit.lib.AnyObjectId;

import java.util.Objects;

/**
 * The version information for the current commit and workspace.
 *
 * @see VersionExtension
 */
public class VersionInformation {
    private final String rawVersion;
    private final boolean snapshot;
    private final String classifiers;
    private final String timestamp;
    private final String commitId;
    private final String abbrevId;

    VersionInformation(String rawVersion, boolean snapshot, String classifiers, String timestamp, String commitId, String abbrevId) {
        this.rawVersion = rawVersion;
        this.snapshot = snapshot;
        this.classifiers = classifiers;
        this.timestamp = timestamp;
        this.commitId = commitId;
        this.abbrevId = abbrevId;
    }

    /**
     * Returns the raw version without any classifiers. This is taken from the nearest tag reachable from the current
     * commit (which may be for the commit itself), after sanitization is done to remove unwanted or configured
     * prefixes and the position increment is done if configured.
     *
     * @return the raw version
     * @see VersionExtension#getSkipIncrement()
     * @see VersionExtension#getCustomPrefixes()
     * @see VersionExtension#getStripBranchPrefix()
     */
    public String getRawVersion() {
        return rawVersion;
    }

    /**
     * Returns whether this is a snapshot version.
     *
     * <p>The version is calculated to be a snapshot version if there are any commits since the last tagged commit
     * version or, if the current commit is a tagged version, if there are any uncommitted changes in the workspace
     * (which may be indicated in the classifiers with the {@linkplain VersionExtension#getDirtySuffix() dirty workspace
     * suffix}).</p>
     *
     * @return {@code true} if this is a snapshot version, {@code false} otherwise
     */
    public boolean isSnapshot() {
        return snapshot;
    }

    /**
     * Returns the classifiers for the version, in a format suitable for appending to the end of the raw version
     * directly.
     *
     * <p>The contents of the classifiers are calculated by the plugin, and may vary from version to version. Some of
     * the classifiers that can be seen include:</p>
     * <ul>
     *     <li><code>{@value VersionExtension#SNAPSHOT_SUFFIX}</code> - appended for {@linkplain #isSnapshot()
     *     snapshot version}</li>
     *     <li>{@code +<commit ID>} - the {@linkplain #getAbbreviatedCommitId() abbreviated commit ID}</li>
     *     <li>the configured {@linkplain VersionExtension#getDirtySuffix() dirty workspace suffix}</li>
     * </ul>
     *
     * <p>When present, the snapshot suffix, abbreviated commit ID, and dirty workspace suffix are appended in that
     * order, to attempt at keeping compatibility with the <a href="https://semver.org/spec/v2.0.0.html">Semantic
     * Versioning 2.0.0 specification</a>.</p>
     *
     * @return the classifiers
     */
    public String getClassifiers() {
        return classifiers;
    }

    /**
     * Returns the timestamp of the current commit, formatted with the ISO-8601 extended offset date-time format.
     *
     * @return the current commit timestamp
     * @see java.time.format.DateTimeFormatter#ISO_OFFSET_DATE_TIME
     */
    public String getCommitTimestamp() {
        return timestamp;
    }

    /**
     * Returns the full SHA-1 of the current commit, in lowercase hexadecimal format.
     *
     * @return the full SHA-1 of the current commit
     * @see AnyObjectId#name()
     * @see #getAbbreviatedCommitId()
     */
    public String getFullCommitId() {
        return commitId;
    }

    /**
     * Returns the abbreviated SHA-1 of the current commit, in lowercase hexadecimal format.
     *
     * @return the abbreviated SHA-1 of the current commit.
     * @see #getFullCommitId()
     * @see org.eclipse.jgit.lib.ObjectReader#abbreviate(AnyObjectId)
     */
    public String getAbbreviatedCommitId() {
        return abbrevId;
    }

    /**
     * Returns the simple version, which is the {@linkplain #getRawVersion() raw version} with the snapshot suffix
     * ({@value VersionExtension#SNAPSHOT_SUFFIX}) appended if this is a {@linkplain #isSnapshot() snapshot version}.
     *
     * <p>This can be used as the version for Maven publications, as this ensures that the snapshot suffix specially
     * recognized by Maven is always present at the end of the version string for non-release versions.</p>
     *
     * @return the simple version, only having the raw version and snapshot suffix for snapshot versions
     * @see #getVersion()
     * @see org.gradle.api.publish.maven.MavenPublication#setVersion(String)
     */
    public String getSimpleVersion() {
        return this.rawVersion + (this.snapshot ? VersionExtension.SNAPSHOT_SUFFIX : "");
    }

    /**
     * Returns the full version, which is the {@linkplain #getRawVersion() raw version} with all
     * {@linkplain #getClassifiers() classifiers}.
     *
     * @return the full version
     */
    public String getVersion() {
        return this.rawVersion + this.classifiers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionInformation that = (VersionInformation) o;
        return snapshot == that.snapshot && rawVersion.equals(that.rawVersion) && classifiers.equals(that.classifiers) && timestamp.equals(that.timestamp) && commitId.equals(that.commitId) && abbrevId.equals(that.abbrevId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawVersion, snapshot, classifiers, timestamp, commitId, abbrevId);
    }

    /**
     * Returns the full version. This effectively calls {@link #getVersion()}.
     *
     * @return the full version
     */
    @Override
    public String toString() {
        return getVersion();
    }
}
