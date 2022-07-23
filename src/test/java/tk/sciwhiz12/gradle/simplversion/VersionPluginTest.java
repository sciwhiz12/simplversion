package tk.sciwhiz12.gradle.simplversion;

import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.api.Project;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionPluginTest {
    @Test
    void pluginRegistersATask() {
        // Create a test project and apply the plugin
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("tk.sciwhiz12.gradle.simplversion.greeting");

        // Verify the result
        assertNotNull(project.getTasks().findByName("greeting"));
    }
}
