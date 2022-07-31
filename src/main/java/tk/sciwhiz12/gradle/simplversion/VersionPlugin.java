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
