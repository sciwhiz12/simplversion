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

package tk.sciwhiz12.gradle.simplversion;

import org.gradle.api.Project;
import org.gradle.api.Plugin;

public class VersionPlugin implements Plugin<Project> {
    public static final String EXTENSION_NAME = "versions";

    @Override
    public void apply(Project project) {
        final VersionExtension extension = project.getExtensions().create(EXTENSION_NAME, VersionExtension.class);

        // Some defaults, for maximum efficiency
        extension.incrementPositionIfSnapshot(-2); // Second to last version: #.#.(#).#
        extension.skipIncrementForClassifiers("alpha", "beta", "pre", "rc");
    }
}
