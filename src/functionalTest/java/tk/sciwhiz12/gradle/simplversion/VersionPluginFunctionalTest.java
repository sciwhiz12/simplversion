package tk.sciwhiz12.gradle.simplversion;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.io.FileWriter;

import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class VersionPluginFunctionalTest {
    @TempDir
    File projectDir;

    private File getBuildFile() {
        return new File(projectDir, "build.gradle");
    }

    private File getSettingsFile() {
        return new File(projectDir, "settings.gradle");
    }

    @Test
    void canRunTask() throws IOException {
        writeString(getSettingsFile(), "");
        writeString(getBuildFile(),
                "plugins {" +
                        "  id('tk.sciwhiz12.gradle.simplversion')" +
                        "}");

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("greeting");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

//        // Verify the result
//        assertTrue(result.getOutput().contains("Hello from plugin 'tk.sciwhiz12.gradle.simplversion'"));
    }

    private void writeString(File file, String string) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            writer.write(string);
        }
    }
}
