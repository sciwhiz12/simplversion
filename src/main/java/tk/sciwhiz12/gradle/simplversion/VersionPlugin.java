package tk.sciwhiz12.gradle.simplversion;

import org.gradle.api.Project;
import org.gradle.api.Plugin;

public class VersionPlugin implements Plugin<Project> {
    public void apply(Project project) {
        project.getTasks().register("greeting", task -> {
            task.doLast(s -> System.out.println("Hello from plugin 'tk.sciwhiz12.gradle.simplversion.greeting'"));
        });
    }
}
