/*
 * Concord - Copyright (c) 2020 SciWhiz12
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

package tk.sciwhiz12.gradle.simplversion;

import java.util.Objects;

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

    public String getRawVersion() {
        return rawVersion;
    }

    public boolean isSnapshot() {
        return snapshot;
    }

    public String getClassifiers() {
        return classifiers;
    }

    public String getCommitTimestamp() {
        return timestamp;
    }

    public String getFullCommitId() {
        return commitId;
    }

    public String getAbbreviatedCommitId() {
        return abbrevId;
    }

    public String getSimpleVersion() {
        return this.rawVersion + (this.snapshot ? "-SNAPSHOT" : "");
    }

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

    @Override
    public String toString() {
        return getVersion();
    }
}
