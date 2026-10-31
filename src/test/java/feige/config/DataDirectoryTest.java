package feige.config;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataDirectoryTest {

    @Test
    void usesWorkingDirectoryWhenClassesAreNotInsideAJar() {
        File resolved = DataDirectory.resolve();
        assertEquals(new File(System.getProperty("user.dir"), "data").getAbsolutePath(), resolved.getAbsolutePath());
    }
}
