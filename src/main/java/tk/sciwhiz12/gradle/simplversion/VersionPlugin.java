package tk.sciwhiz12.gradle.simplversion;

import org.gradle.api.Project;
import org.gradle.api.Plugin;

public class VersionPlugin implements Plugin<Project> {
    public static final String EXTENSION_NAME = "versions";

    @Override
    public void apply(Project project) {
        project.getExtensions().create(EXTENSION_NAME, VersionExtension.class);
    }
}
